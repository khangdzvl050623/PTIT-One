package vn.ptit.one.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.notification.dto.SaveNotificationRequest;
import vn.ptit.one.notification.model.AuthoredNotification;
import vn.ptit.one.notification.model.NotificationSent;
import vn.ptit.one.notification.model.NotificationTerms;
import vn.ptit.one.notification.model.Recipient;
import vn.ptit.one.notification.model.RecipientCount;
import vn.ptit.one.notification.repository.NotificationRepository;
import vn.ptit.one.notification.repository.NotificationRepository.AuthoredRow;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.shared.outbox.OutboxStore;
import vn.ptit.one.shared.routing.CrossSiteQuery;
import vn.ptit.one.student.service.StudentDirectory;
import vn.ptit.one.teacher.model.Teacher;
import vn.ptit.one.teacher.service.TeacherDirectory;

/**
 * Thông báo soạn tay: nháp → xem trước số người nhận → gửi.
 *
 * <p>Phạm vi được gửi (quyết định nhóm 02/10/2026):
 * <ul>
 *   <li>Admin Master — toàn trường, một cơ sở, một lớp học phần;</li>
 *   <li>Admin cơ sở — cơ sở mình, hoặc lớp học phần do cơ sở mình mở;</li>
 *   <li>Giảng viên — lớp học phần đang được phân công dạy.</li>
 * </ul>
 * Quyền kiểm cả lúc lưu nháp LẪN lúc gửi: giảng viên bị gỡ phân công giữa chừng
 * thì không gửi được bản nháp cũ.
 */
@Service
@Profile("central")
public class NotificationService {

    private final NotificationRepository notifications;
    private final ClassSectionService classes;
    private final ClassRecipients classRecipients;
    private final StudentDirectory students;
    private final TeacherDirectory teachers;
    private final Clock clock;

    private final ObjectProvider<CrossSiteQuery> crossSite;
    private final OutboxStore outbox;
    private final ObjectMapper json;
    private final TransactionTemplate tx;

    public NotificationService(NotificationRepository notifications, ClassSectionService classes,
            ClassRecipients classRecipients, StudentDirectory students, TeacherDirectory teachers, Clock clock,
            ObjectProvider<CrossSiteQuery> crossSite, OutboxStore outbox, ObjectMapper json,
            PlatformTransactionManager txManager) {
        this.notifications = notifications;
        this.classes = classes;
        this.classRecipients = classRecipients;
        this.students = students;
        this.teachers = teachers;
        this.clock = clock;
        this.crossSite = crossSite;
        this.outbox = outbox;
        this.json = json;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Phần 2, Admin Master: người nhận nằm ở các cơ sở. Mọi bước hỏi cơ sở chạy
     * NGOÀI giao dịch — vì vậy các phương thức ghi dưới đây không mang
     * {@code @Transactional}, chỉ đoạn ghi lúc gửi mới bọc giao dịch.
     */
    private CrossSiteQuery masterFanOut() {
        CrossSiteQuery q = crossSite.getIfAvailable();
        return q != null && q.atMaster() ? q : null;
    }

    /** Xem trước số người nhận mà không lưu gì. */
    public RecipientCount preview(AuthenticatedUser user, SaveNotificationRequest body) {
        return count(recipients(user, authorize(user, body), false));
    }

    public AuthoredNotification createDraft(AuthenticatedUser user, SaveNotificationRequest body) {
        AuthoredRow row = authorize(user, body);
        UUID id = UUID.randomUUID();
        notifications.insertDraft(id, row, user.role().name(), clock.instant());
        return view(user, requireOwn(user, id));
    }

    /** Câu UPDATE tự lọc {@code TrangThai = 'NHAP'}: gửi chen giữa thì không ghi đè được. */
    public AuthoredNotification updateDraft(AuthenticatedUser user, UUID id, SaveNotificationRequest body) {
        requireDraft(requireOwn(user, id));
        AuthoredRow row = authorize(user, body);
        if (notifications.updateDraft(id, row) != 1) {
            throw alreadySent();
        }
        return view(user, requireOwn(user, id));
    }

    public void deleteDraft(AuthenticatedUser user, UUID id) {
        requireDraft(requireOwn(user, id));
        notifications.deleteDraft(id, user.username());
    }

    /**
     * Gửi: kiểm lại quyền, CHỐT danh sách người nhận vào {@code ThongBaoNguoiNhan}.
     * Đổi trạng thái trước — điều kiện {@code TrangThai = 'NHAP'} trong câu UPDATE
     * chặn bấm gửi hai lần ra hai bộ người nhận.
     */
    public AuthoredNotification send(AuthenticatedUser user, UUID id) {
        AuthoredRow row = requireOwn(user, id);
        requireDraft(row);
        AuthoredRow checked = authorize(user, toRequest(row));
        List<Recipient> nguoiNhan = recipients(user, checked, true);
        if (nguoiNhan.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "NOTIFICATION_NO_RECIPIENTS",
                    "Phạm vi đã chọn hiện không có người nhận nào.");
        }
        boolean master = masterFanOut() != null;
        Instant now = clock.instant();
        tx.executeWithoutResult(s -> {
            if (notifications.markSent(id, now) != 1) {
                throw alreadySent();
            }
            notifications.insertRecipients(id, nguoiNhan);
            if (master) {
                // Cùng giao dịch với markSent: gửi rồi thì chắc chắn tới được các cơ sở.
                NotificationSent ev = new NotificationSent(id, checked.mucDo(), checked.tieuDe(),
                        checked.noiDung(), checked.lienKet(), checked.phamVi(), checked.maCoSo(),
                        checked.maLopHP(), checked.doiTuong(), checked.nguoiTao(), now, nguoiNhan);
                outbox.enqueue(UUID.randomUUID(), NotificationSent.LOAI, id.toString(), toJson(ev), 1,
                        LocalDateTime.ofInstant(now, ZoneOffset.UTC));
            }
        });
        return view(user, requireOwn(user, id));
    }

    /** Worker, tại từng cơ sở: chép thông báo + phần người nhận của cơ sở này. Idempotent. */
    @Transactional
    public void deliverHere(NotificationSent ev) {
        notifications.insertDelivered(ev);
        notifications.insertRecipientsHere(ev.maThongBao(), ev.nguoiNhan());
    }

    private String toJson(NotificationSent ev) {
        try {
            return json.writeValueAsString(ev);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Không ghi được sự kiện gửi thông báo.", ex);
        }
    }

    public List<AuthoredNotification> mine(AuthenticatedUser user) {
        requireAuthorRole(user);
        return notifications.findByAuthor(user.username()).stream().map(row -> view(user, row)).toList();
    }

    public AuthoredNotification detail(AuthenticatedUser user, UUID id) {
        return view(user, requireOwn(user, id));
    }

    // --- Quyền và phạm vi -------------------------------------------------

    /** Kiểm quyền gửi theo phạm vi, chuẩn hoá đích gửi. Trả bản ghi sẵn sàng lưu. */
    private AuthoredRow authorize(AuthenticatedUser user, SaveNotificationRequest body) {
        requireAuthorRole(user);
        if (!NotificationTerms.MUC_DO.contains(body.mucDo())) {
            throw invalid("Mức độ không hợp lệ: %s.".formatted(body.mucDo()));
        }
        if (!NotificationTerms.DOI_TUONG.contains(body.doiTuong())) {
            throw invalid("Đối tượng nhận không hợp lệ: %s.".formatted(body.doiTuong()));
        }
        String maCoSo = blankToNull(body.maCoSo());
        String maLopHP = blankToNull(body.maLopHP());

        switch (body.phamVi()) {
            case NotificationTerms.TOAN_TRUONG -> {
                if (user.role() != Role.ADMIN_MASTER) {
                    throw forbidden("Chỉ Admin Master gửi được thông báo toàn trường.");
                }
                maCoSo = null;
                maLopHP = null;
            }
            case NotificationTerms.CO_SO -> {
                if (maCoSo != null) {
                    String nhap = maCoSo;
                    maCoSo = notifications.findCampus(nhap)
                            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "CAMPUS_NOT_FOUND",
                                    "Không có cơ sở %s.".formatted(nhap)));
                }
                if (user.role() == Role.ADMIN_CO_SO) {
                    if (maCoSo != null && !maCoSo.equals(user.homeCampus())) {
                        throw forbidden("Bạn chỉ gửi được thông báo cho cơ sở %s.".formatted(user.homeCampus()));
                    }
                    maCoSo = user.homeCampus();
                } else if (user.role() != Role.ADMIN_MASTER) {
                    throw forbidden("Giảng viên chỉ gửi thông báo cho lớp học phần mình dạy.");
                }
                if (maCoSo == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "CAMPUS_NOT_FOUND", "Chọn cơ sở nhận thông báo.");
                }
                maLopHP = null;
            }
            case NotificationTerms.LOP_HOC_PHAN -> {
                if (maLopHP == null) {
                    throw invalid("Cần chọn lớp học phần.");
                }
                ClassSection lop = classes.require(maLopHP);
                boolean allowed = switch (user.role()) {
                    case ADMIN_MASTER -> true;
                    case ADMIN_CO_SO -> lop.maCoSoHost().equals(user.homeCampus());
                    case GIANG_VIEN -> user.entityId() != null && user.entityId().equals(lop.maGiangVien());
                    case SINH_VIEN -> false;
                };
                if (!allowed) {
                    throw forbidden("Bạn không được gửi thông báo cho lớp %s.".formatted(maLopHP));
                }
                maCoSo = lop.maCoSoHost();
            }
            default -> throw invalid("Phạm vi không hợp lệ: %s.".formatted(body.phamVi()));
        }
        return new AuthoredRow(null, null, body.mucDo(), body.tieuDe().trim(), body.noiDung().trim(),
                blankToNull(body.lienKet()), body.phamVi(), maCoSo, maLopHP, body.doiTuong(),
                user.username(), null, null);
    }

    /**
     * Ở Master hỏi các cơ sở, nơi khác đọc tại chỗ. {@code chot}: danh sách sắp
     * được CHỐT lúc gửi — cơ sở tắt thì từ chối gửi, không lặng lẽ bỏ sót người
     * của cơ sở đó vĩnh viễn. Xem trước/đếm thì chấp nhận thiếu.
     */
    private <T> List<T> fromSites(Supplier<List<T>> body, boolean chot) {
        CrossSiteQuery q = masterFanOut();
        if (q == null) {
            return body.get();
        }
        return chot ? q.collectAll(body) : q.collect(body);
    }

    /** Người nhận theo phạm vi và đối tượng, bỏ chính người gửi, không trùng. */
    private List<Recipient> recipients(AuthenticatedUser user, AuthoredRow row, boolean chot) {
        boolean sv = !NotificationTerms.GIANG_VIEN.equals(row.doiTuong());
        boolean gv = !NotificationTerms.SINH_VIEN.equals(row.doiTuong());
        Set<Recipient> ket = new LinkedHashSet<>();

        if (NotificationTerms.LOP_HOC_PHAN.equals(row.phamVi())) {
            if (sv) {
                fromSites(() -> classRecipients.studentsOf(row.maLopHP()), chot)
                        .forEach(ma -> ket.add(Recipient.student(ma)));
            }
            String maGiangVien = classes.require(row.maLopHP()).maGiangVien();
            if (gv && maGiangVien != null) {
                ket.add(Recipient.teacher(maGiangVien));
            }
        } else {
            // TOAN_TRUONG: maCoSo null = mọi cơ sở. CO_SO: SV theo cơ sở NHÀ, GV theo cơ sở công tác.
            if (sv) {
                fromSites(() -> students.currentStudentIds(row.maCoSo()), chot)
                        .forEach(ma -> ket.add(Recipient.student(ma)));
            }
            if (gv) {
                fromSites(() -> teachers.search(row.maCoSo(), null), chot).stream().map(Teacher::maGiangVien)
                        .forEach(ma -> ket.add(Recipient.teacher(ma)));
            }
        }
        if (user.role() == Role.GIANG_VIEN && user.entityId() != null) {
            ket.remove(Recipient.teacher(user.entityId()));
        }
        return new ArrayList<>(ket);
    }

    private static RecipientCount count(List<Recipient> recipients) {
        int sv = (int) recipients.stream().filter(r -> NotificationTerms.SINH_VIEN.equals(r.loai())).count();
        return new RecipientCount(sv, recipients.size() - sv);
    }

    // --- Nội bộ ----------------------------------------------------------

    private AuthoredNotification view(AuthenticatedUser user, AuthoredRow row) {
        boolean nhap = NotificationTerms.NHAP.equals(row.trangThai());
        RecipientCount nguoiNhan = nhap
                ? count(recipients(user, row, false))
                : notifications.recipientCount(row.maThongBao());
        return new AuthoredNotification(row.maThongBao(), row.trangThai(), row.mucDo(), row.tieuDe(),
                row.noiDung(), row.lienKet(), row.phamVi(), row.maCoSo(), row.maLopHP(), row.doiTuong(),
                row.ngayTao(), row.ngayGui(), nguoiNhan, nhap ? 0 : readCount(row.maThongBao()));
    }

    /** Ở Master: người nhận đọc tại cơ sở nhà của họ, nên cộng từ các cơ sở. */
    private int readCount(UUID id) {
        CrossSiteQuery q = masterFanOut();
        return q == null ? notifications.readCount(id)
                : q.collect(() -> List.of(notifications.readCount(id))).stream().mapToInt(Integer::intValue).sum();
    }

    /** Chỉ người soạn thấy và sửa bản của mình; của người khác trả 404, không lộ là có tồn tại. */
    private AuthoredRow requireOwn(AuthenticatedUser user, UUID id) {
        requireAuthorRole(user);
        return notifications.findAuthored(id)
                .filter(row -> user.username().equals(row.nguoiTao()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND",
                        "Không tìm thấy thông báo."));
    }

    private static void requireDraft(AuthoredRow row) {
        if (!NotificationTerms.NHAP.equals(row.trangThai())) {
            throw alreadySent();
        }
    }

    private static void requireAuthorRole(AuthenticatedUser user) {
        if (user.role() == Role.SINH_VIEN) {
            throw forbidden("Sinh viên chỉ nhận và đọc thông báo.");
        }
    }

    private static SaveNotificationRequest toRequest(AuthoredRow row) {
        return new SaveNotificationRequest(row.tieuDe(), row.noiDung(), row.mucDo(), row.phamVi(),
                row.maCoSo(), row.maLopHP(), row.doiTuong(), row.lienKet());
    }

    private static ApiException alreadySent() {
        return new ApiException(HttpStatus.CONFLICT, "NOTIFICATION_ALREADY_SENT",
                "Thông báo đã gửi, không sửa hay gửi lại được.");
    }

    private static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", message);
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "NOTIFICATION_INVALID", message);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
