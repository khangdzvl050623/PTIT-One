package vn.ptit.one.enrollment.service;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.enrollment.repository.EnrollmentRepository;
import vn.ptit.one.grade.service.GradeRecords;
import vn.ptit.one.notification.model.AutoNotification;
import vn.ptit.one.notification.model.NotificationTerms;
import vn.ptit.one.notification.model.Recipient;
import vn.ptit.one.notification.service.NotificationPublisher;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.student.model.StudentProfile;
import vn.ptit.one.student.service.StudentDirectory;

/**
 * Admin cơ sở gỡ MỘT sinh viên khỏi MỘT lớp (F08).
 *
 * <p>Khác {@link ClassCancellationService}: bản kia huỷ cả lớp và đẩy mọi sinh
 * viên ra, lớp thành {@code DA_HUY}. Bản này chỉ gỡ một người; lớp vẫn
 * {@code MO} và mở ra một chỗ trống.
 *
 * <p>Vì sao cần: sinh viên chỉ tự huỷ được khi đợt còn mở. Đăng ký sai lớp mà
 * phát hiện sau khi đợt đóng thì không còn đường nào — huỷ cả lớp sẽ ảnh hưởng
 * những người vô can, nên trước đây chỉ còn cách sửa SQL tay.
 *
 * <p><b>Quyết định nhóm 03/10/2026:</b> gỡ được cả sinh viên đang
 * {@code DANG_HOC} — đăng ký sai lớp thường xảy ra với người vẫn đang học, nếu
 * chỉ cho gỡ người bảo lưu/thôi học thì bỏ sót đúng mục đích chính. Giới hạn
 * đặt ở <b>tình trạng lớp và điểm</b>, không ở tình trạng sinh viên.
 *
 * <p>⚠️ Năm bước ghi dưới đây PHẢI giống {@code ClassCancellationService} và
 * {@code EnrollmentService.cancel} — cùng thứ tự khoá, cùng bộ bảng. Sửa một
 * chỗ thì sửa cả ba, nếu không bộ đếm sẽ lệch khỏi số ghi danh thật.
 */
@Service
@Profile("central")
public class EnrollmentRemovalService {

    private static final int LOCK_TIMEOUT_MS = 5_000;

    private final EnrollmentRepository enrollments;
    private final ClassSectionService classes;
    private final GradeRecords grades;
    private final StudentDirectory students;
    private final NotificationPublisher notifications;

    public EnrollmentRemovalService(EnrollmentRepository enrollments, ClassSectionService classes,
            GradeRecords grades, StudentDirectory students, NotificationPublisher notifications) {
        this.enrollments = enrollments;
        this.classes = classes;
        this.grades = grades;
        this.students = students;
        this.notifications = notifications;
    }

    @Transactional
    public ClassSection remove(AuthenticatedUser user, String maLopHP, String maSinhVien, String lyDo) {
        ClassSection lop = classes.requireManageableBy(user, maLopHP);
        StudentProfile sv = students.require(maSinhVien);

        /* Lớp đã khoá điểm thì bảng điểm là kết quả cuối; gỡ ghi danh lúc này
           sẽ bỏ một dòng khỏi bảng điểm đã chốt. */
        if (ClassSectionService.DA_KHOA.equals(lop.trangThai())) {
            throw new ApiException(HttpStatus.CONFLICT, "GRADE_LOCKED",
                    "Lớp %s đã khoá điểm, không gỡ được sinh viên.".formatted(maLopHP));
        }
        if (ClassSectionService.DA_HUY.equals(lop.trangThai())) {
            throw new ApiException(HttpStatus.CONFLICT, "CLASS_CANCELLED",
                    "Lớp %s đã bị huỷ; mọi ghi danh đã được gỡ.".formatted(maLopHP));
        }

        // Cùng thứ tự khoá với đăng ký và huỷ: (sinh viên, học kỳ) trước mọi phép ghi.
        if (!enrollments.acquireStudentTermLock(maSinhVien, lop.maHocKy(), LOCK_TIMEOUT_MS)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ENROLLMENT_BUSY",
                    "Đang xử lý một yêu cầu khác của sinh viên %s. Vui lòng thử lại.".formatted(maSinhVien));
        }

        int soTinChi = enrollments.creditsOfClass(maSinhVien, maLopHP)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ENROLLMENT_NOT_FOUND",
                        "Sinh viên %s không có ghi danh đang hiệu lực ở lớp %s."
                                .formatted(maSinhVien, maLopHP)));

        requireOne(enrollments.subtractCredits(maSinhVien, lop.maHocKy(), soTinChi), "trả tín chỉ");
        classes.releaseSeat(maLopHP);
        requireOne(enrollments.cancelClassEnrollment(maLopHP, maSinhVien), "huỷ ghi danh lớp");
        requireOne(enrollments.cancelCourseEnrollment(maSinhVien, maLopHP), "huỷ ghi danh môn");
        /* Đã có điểm — kể cả điểm nháp — thì không gỡ: giảng viên đã bắt đầu
           ghi nhận kết quả, xoá đi là mất dữ liệu không dựng lại được. */
        if (!grades.removeEmptyRecord(maLopHP, maSinhVien)) {
            throw new ApiException(HttpStatus.CONFLICT, "ENROLLMENT_HAS_GRADE",
                    "Sinh viên %s đã có điểm ở lớp %s, không gỡ được.".formatted(maSinhVien, maLopHP));
        }

        notify(lop, sv, user, lyDo);
        return classes.require(maLopHP);
    }

    /**
     * Sinh viên phải biết ai gỡ và vì sao — đây là thao tác người khác làm trên
     * dữ liệu của họ. Khoá sự kiện gồm thời điểm nên gỡ rồi ghi danh lại rồi gỡ
     * lần nữa sẽ ra thông báo MỚI, không bị chống trùng ăn mất.
     */
    private void notify(ClassSection lop, StudentProfile sv, AuthenticatedUser admin, String lyDo) {
        notifications.publish(new AutoNotification(NotificationTerms.GO_GHI_DANH,
                "GO_GHI_DANH:%s:%s:%d".formatted(lop.maLopHP(), sv.maSinhVien(),
                        System.currentTimeMillis()),
                NotificationTerms.QUAN_TRONG,
                "Bạn đã bị gỡ khỏi lớp %s".formatted(lop.tenMonHoc()),
                ("Quản trị đào tạo (%s) đã gỡ đăng ký lớp %s học kỳ %s của bạn. Lý do: %s. "
                        + "%d tín chỉ đã được trả lại. Liên hệ phòng đào tạo nếu cần.")
                        .formatted(admin.username(), lop.maLopHP(), lop.maHocKy(), lyDo.trim(),
                                lop.soTinChi()),
                "/sinh-vien/dang-ky?maHocKy=" + lop.maHocKy(), lop.maLopHP(),
                List.of(Recipient.student(sv.maSinhVien()))));
    }

    /** Bộ đếm và ghi danh phải khớp; lệch là lỗi dữ liệu, rollback thay vì ghi tiếp. */
    private static void requireOne(int rows, String step) {
        if (rows != 1) {
            throw new IllegalStateException("Dữ liệu ghi danh không khớp ở bước " + step + ".");
        }
    }
}
