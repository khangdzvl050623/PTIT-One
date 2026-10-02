package vn.ptit.one.enrollment.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.enrollment.model.CancelledClass;
import vn.ptit.one.enrollment.model.RosterEntry;
import vn.ptit.one.enrollment.repository.EnrollmentRepository;
import vn.ptit.one.grade.service.GradeRecords;
import vn.ptit.one.notification.model.AutoNotification;
import vn.ptit.one.notification.model.NotificationTerms;
import vn.ptit.one.notification.model.Recipient;
import vn.ptit.one.notification.service.NotificationPublisher;
import vn.ptit.one.shared.exception.ApiException;

/**
 * Huỷ lớp học phần — đường DUY NHẤT đưa lớp về {@code DA_HUY}.
 *
 * <p>Trong MỘT giao dịch: huỷ mọi ghi danh hai phía, trả tín chỉ, xoá dòng điểm
 * rỗng, đưa sĩ số về 0, báo sinh viên và giảng viên. Đổi trạng thái trần sẽ để
 * sinh viên kẹt trong lớp đã huỷ với tín chỉ không lấy lại được.
 *
 * <p>Thứ tự khoá, để không deadlock với đăng ký/huỷ của từng sinh viên:
 * <ol>
 *   <li>khoá theo (sinh viên, kỳ) của MỌI sinh viên đang trong lớp, xếp theo mã —
 *       từ đây không ai trong số họ chen được thao tác nào khác;</li>
 *   <li>chuyển lớp sang {@code DA_HUY} — từ đây không ai giữ thêm chỗ được;</li>
 *   <li>đọc lại danh sách: có người đăng ký kịp chen giữa (1) và (2) thì dừng
 *       và báo thử lại, KHÔNG khoá thêm khi đang giữ dòng lớp;</li>
 *   <li>ghi từng sinh viên, rồi đưa sĩ số về 0.</li>
 * </ol>
 * Ghi {@code SinhVienHocKy} sau {@code LopHocPhan} ở đây không gây deadlock vì
 * mọi giao dịch khác đụng tới các dòng đó đều phải chờ khoá ở bước (1).
 */
@Service
@Profile("central")
public class ClassCancellationService {

    private static final int LOCK_TIMEOUT_MS = 5_000;

    private final EnrollmentRepository enrollments;
    private final ClassSectionService classes;
    private final GradeRecords grades;
    private final NotificationPublisher notifications;

    public ClassCancellationService(EnrollmentRepository enrollments, ClassSectionService classes,
            GradeRecords grades, NotificationPublisher notifications) {
        this.enrollments = enrollments;
        this.classes = classes;
        this.grades = grades;
        this.notifications = notifications;
    }

    @Transactional
    public CancelledClass cancel(AuthenticatedUser user, String maLopHP, String lyDo) {
        ClassSection lop = classes.requireManageableBy(user, maLopHP);
        if (ClassSectionService.DA_HUY.equals(lop.trangThai())) {
            // Gọi lại: không làm gì, không báo lần hai.
            return new CancelledClass(lop, 0);
        }
        if (ClassSectionService.DA_KHOA.equals(lop.trangThai())) {
            throw new ApiException(HttpStatus.CONFLICT, "GRADE_LOCKED",
                    "Lớp %s đã khoá điểm, không huỷ được.".formatted(maLopHP));
        }

        Set<String> daKhoa = new TreeSet<>();
        for (RosterEntry sv : enrollments.roster(maLopHP)) {
            daKhoa.add(sv.maSinhVien());
        }
        for (String maSinhVien : daKhoa) {
            if (!enrollments.acquireStudentTermLock(maSinhVien, lop.maHocKy(), LOCK_TIMEOUT_MS)) {
                throw retry();
            }
        }

        if (!classes.markCancelled(maLopHP)) {
            throw retry();
        }
        List<RosterEntry> sinhVien = enrollments.roster(maLopHP);
        if (sinhVien.stream().anyMatch(sv -> !daKhoa.contains(sv.maSinhVien()))) {
            throw retry();
        }

        for (RosterEntry sv : sinhVien) {
            String ma = sv.maSinhVien();
            int soTinChi = enrollments.creditsOfClass(ma, maLopHP).orElseThrow(
                    () -> new IllegalStateException("Ghi danh lớp %s của %s lệch hai phía.".formatted(maLopHP, ma)));
            requireOne(enrollments.subtractCredits(ma, lop.maHocKy(), soTinChi), "trả tín chỉ");
            requireOne(enrollments.cancelClassEnrollment(maLopHP, ma), "huỷ ghi danh lớp");
            requireOne(enrollments.cancelCourseEnrollment(ma, maLopHP), "huỷ ghi danh môn");
            if (!grades.removeEmptyRecord(maLopHP, ma)) {
                throw new ApiException(HttpStatus.CONFLICT, "CLASS_HAS_GRADES",
                        "Lớp %s đã có điểm của sinh viên %s, không huỷ được.".formatted(maLopHP, ma));
            }
        }
        classes.clearSeats(maLopHP, sinhVien.size());

        notify(lop, sinhVien, lyDo);
        return new CancelledClass(classes.require(maLopHP), sinhVien.size());
    }

    /** Sinh viên và giảng viên nhận hai nội dung khác nhau: chỉ sinh viên có đăng ký bị huỷ. */
    private void notify(ClassSection lop, List<RosterEntry> sinhVien, String lyDo) {
        String tieuDe = "Lớp %s đã bị huỷ".formatted(lop.tenMonHoc());
        String lyDoText = lyDo == null || lyDo.isBlank() ? "" : " Lý do: " + lyDo.trim() + ".";

        List<Recipient> sv = sinhVien.stream().map(RosterEntry::maSinhVien)
                .sorted(Comparator.naturalOrder()).map(Recipient::student).toList();
        notifications.publish(new AutoNotification(NotificationTerms.LOP_BI_HUY,
                "LOP_BI_HUY:SV:" + lop.maLopHP(), NotificationTerms.QUAN_TRONG, tieuDe,
                ("Lớp %s học kỳ %s đã bị huỷ.%s Đăng ký của bạn đã được huỷ và tín chỉ đã trả lại. "
                        + "Xem lớp còn chỗ để đăng ký bổ sung.").formatted(lop.maLopHP(), lop.maHocKy(), lyDoText),
                "/sinh-vien/dang-ky?maHocKy=" + lop.maHocKy(), lop.maLopHP(), sv));

        if (lop.maGiangVien() != null) {
            notifications.publish(new AutoNotification(NotificationTerms.LOP_BI_HUY,
                    "LOP_BI_HUY:GV:" + lop.maLopHP(), NotificationTerms.QUAN_TRONG, tieuDe,
                    "Lớp %s học kỳ %s bạn phụ trách đã bị huỷ.%s Lớp không còn trong lịch dạy."
                            .formatted(lop.maLopHP(), lop.maHocKy(), lyDoText),
                    "/giang-vien/lop-phu-trach", lop.maLopHP(), List.of(Recipient.teacher(lop.maGiangVien()))));
        }
    }

    private static ApiException retry() {
        return new ApiException(HttpStatus.CONFLICT, "CLASS_CANCEL_RETRY",
                "Lớp vừa có thay đổi đăng ký trong lúc huỷ. Vui lòng thử lại.");
    }

    private static void requireOne(int rows, String step) {
        if (rows != 1) {
            throw new IllegalStateException("Dữ liệu ghi danh không khớp ở bước " + step + ".");
        }
    }
}
