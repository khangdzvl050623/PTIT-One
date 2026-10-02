package vn.ptit.one.enrollment.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.course.service.CourseService;
import vn.ptit.one.enrollment.model.EnrolledCourse;
import vn.ptit.one.enrollment.model.RegistrationResult;
import vn.ptit.one.enrollment.model.StudentEnrollments;
import vn.ptit.one.enrollment.repository.EnrollmentRepository;
import vn.ptit.one.enrollment.repository.EnrollmentRepository.TermCredits;
import vn.ptit.one.grade.policy.GradePolicy;
import vn.ptit.one.grade.service.GradeRecords;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.student.model.StudentProfile;
import vn.ptit.one.student.service.StudentDirectory;
import vn.ptit.one.timetable.service.ScheduleService;

/**
 * Đăng ký và huỷ học phần (F08) trên một database — mỗi thao tác là MỘT giao
 * dịch cục bộ, nên đi thẳng {@code DA_DANG_KY}/{@code DA_HUY}, không qua
 * {@code DANG_XU_LY} (trạng thái đó dành cho saga ở Phần 2).
 *
 * <p>Thứ tự trong giao dịch, giống nhau ở cả đăng ký lẫn huỷ:
 * <ol>
 *   <li>{@code sp_getapplock} theo (sinh viên, học kỳ) — TRƯỚC mọi phép kiểm</li>
 *   <li>kiểm điều kiện (chỉ đọc)</li>
 *   <li>{@code SinhVienHocKy} → {@code LopHocPhan} → {@code DangKyHocPhan}
 *       → {@code DangKyMonHoc} → {@code Diem}</li>
 * </ol>
 * Lớp luôn được khoá trước ghi danh ở mọi luồng; đảo ở một chỗ là sinh deadlock
 * ngẫu nhiên.
 */
@Service
@Profile("central")
@EnableConfigurationProperties(EnrollmentProperties.class)
public class EnrollmentService {

    private static final int LOCK_TIMEOUT_MS = 5_000;

    private final EnrollmentRepository enrollments;
    private final ClassSectionService classes;
    private final CourseService courses;
    private final EnrollmentPeriodService periods;
    private final ScheduleService schedules;
    private final GradeRecords grades;
    private final StudentDirectory students;
    private final EnrollmentProperties properties;
    private final Clock clock;

    public EnrollmentService(EnrollmentRepository enrollments, ClassSectionService classes,
            CourseService courses, EnrollmentPeriodService periods, ScheduleService schedules,
            GradeRecords grades, StudentDirectory students, EnrollmentProperties properties, Clock clock) {
        this.enrollments = enrollments;
        this.classes = classes;
        this.courses = courses;
        this.periods = periods;
        this.schedules = schedules;
        this.grades = grades;
        this.students = students;
        this.properties = properties;
        this.clock = clock;
    }

    public StudentEnrollments myEnrollments(AuthenticatedUser user, String maHocKy) {
        String maSinhVien = requireStudentRole(user);
        courses.requireTerm(maHocKy);
        Optional<TermCredits> credits = enrollments.termCredits(maSinhVien, maHocKy);
        return new StudentEnrollments(maHocKy,
                credits.map(TermCredits::daDangKy).orElse(0),
                credits.map(TermCredits::tranTinChi).orElse(null),
                enrollments.enrolledCourses(maSinhVien, maHocKy));
    }

    /** Kết quả đăng ký kèm cờ "vừa tạo" để controller chọn 201 hay 200. */
    public record Registration(RegistrationResult result, boolean created) {
    }

    @Transactional
    public Registration register(AuthenticatedUser user, String maLopHP) {
        StudentProfile sv = students.require(requireStudentRole(user));
        ClassSection lop = classes.require(maLopHP);
        String maHocKy = lop.maHocKy();

        acquire(sv.maSinhVien(), maHocKy);

        // --- Kiểm điều kiện: chỉ đọc, sau khi đã giữ khoá ------------------
        if (!sv.dangHoc()) {
            throw new ApiException(HttpStatus.CONFLICT, "STUDENT_NOT_ACTIVE",
                    "Sinh viên đang ở trạng thái %s, không đăng ký được học phần.".formatted(sv.trangThai()));
        }
        // Quyết định nhóm 02/10/2026: Phần 1 chỉ đăng ký lớp cùng cơ sở.
        if (!lop.maCoSoHost().equals(sv.maCoSoNha())) {
            throw new ApiException(HttpStatus.CONFLICT, "ENROLLMENT_CROSS_CAMPUS",
                    "Lớp %s thuộc cơ sở khác. Đăng ký liên cơ sở chưa được hỗ trợ.".formatted(maLopHP));
        }
        requirePeriodOpen(maHocKy, sv.maCoSoNha());
        if (!ClassSectionService.MO.equals(lop.trangThai())) {
            throw new ApiException(HttpStatus.CONFLICT, "CLASS_NOT_OPEN",
                    "Lớp %s chưa mở đăng ký.".formatted(maLopHP));
        }
        if (!courses.isInProgram(sv.maCTDT(), lop.maMonHoc())) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_NOT_IN_PROGRAM",
                    "Môn %s không thuộc chương trình đào tạo %s.".formatted(lop.maMonHoc(), sv.maCTDT()));
        }

        Optional<String> dangGiu = enrollments.activeClassOfCourse(sv.maSinhVien(), maHocKy, lop.maMonHoc());
        if (dangGiu.isPresent()) {
            if (dangGiu.get().equals(maLopHP)) {
                // Bấm hai lần: trả lại đúng kết quả cũ, không cộng bộ đếm lần nữa.
                return new Registration(new RegistrationResult(enrolled(sv.maSinhVien(), maHocKy, maLopHP),
                        loaiDangKy(lop.maMonHoc(), grades.bestResults(sv.maSinhVien()))), false);
            }
            throw new ApiException(HttpStatus.CONFLICT, "ENROLLMENT_DUPLICATE_COURSE",
                    "Bạn đã đăng ký lớp %s của môn %s trong học kỳ này."
                            .formatted(dangGiu.get(), lop.maMonHoc()));
        }

        Map<String, String> ketQua = grades.bestResults(sv.maSinhVien());
        List<String> chuaDat = courses.prerequisitesOf(lop.maMonHoc()).stream()
                .filter(tq -> !GradePolicy.DAT.equals(ketQua.get(tq)))
                .toList();
        if (!chuaDat.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "PREREQUISITE_NOT_MET",
                    "Chưa đạt môn tiên quyết: %s.".formatted(String.join(", ", chuaDat)));
        }

        Optional<String> trung = schedules.firstClash(maLopHP, enrollments.activeClasses(sv.maSinhVien(), maHocKy));
        if (trung.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "SCHEDULE_CLASH",
                    "Lớp %s trùng lịch với lớp %s bạn đã đăng ký.".formatted(maLopHP, trung.get()));
        }

        // --- Ghi: SinhVienHocKy → LopHocPhan → DangKyHocPhan → DangKyMonHoc → Diem
        enrollments.ensureTermRow(sv.maSinhVien(), maHocKy, properties.tranTinChi());
        if (enrollments.addCredits(sv.maSinhVien(), maHocKy, lop.soTinChi()) != 1) {
            TermCredits tc = enrollments.termCredits(sv.maSinhVien(), maHocKy).orElseThrow();
            throw new ApiException(HttpStatus.CONFLICT, "CREDIT_LIMIT_EXCEEDED",
                    "Đã đăng ký %d/%d tín chỉ, thêm %d tín chỉ của %s sẽ vượt trần."
                            .formatted(tc.daDangKy(), tc.tranTinChi(), lop.soTinChi(), lop.maMonHoc()));
        }
        if (!classes.reserveSeat(maLopHP)) {
            throw new ApiException(HttpStatus.CONFLICT, "CLASS_FULL",
                    "Lớp %s đã đủ %d sinh viên.".formatted(maLopHP, lop.soLuongToiDa()));
        }
        enrollments.upsertClassEnrollment(maLopHP, sv.maSinhVien(), sv.maCoSoNha(), sv.hoTen(), clock.instant());
        enrollments.upsertCourseEnrollment(sv.maSinhVien(), maHocKy, lop.maMonHoc(), maLopHP,
                lop.maCoSoHost(), lop.soTinChi(), lop.phienBanLich());
        grades.openRecord(maLopHP, sv.maSinhVien());

        return new Registration(new RegistrationResult(enrolled(sv.maSinhVien(), maHocKy, maLopHP),
                loaiDangKy(lop.maMonHoc(), ketQua)), true);
    }

    /**
     * Huỷ đăng ký: chỉ khi đợt còn mở và chưa có điểm. Trả chỗ, trả tín chỉ,
     * đổi trạng thái hai phía và xoá dòng điểm rỗng — tất cả trong MỘT giao dịch.
     * Không xoá điểm để huỷ.
     */
    @Transactional
    public StudentEnrollments cancel(AuthenticatedUser user, String maLopHP) {
        String maSinhVien = requireStudentRole(user);
        StudentProfile sv = students.require(maSinhVien);
        ClassSection lop = classes.require(maLopHP);
        String maHocKy = lop.maHocKy();

        acquire(maSinhVien, maHocKy);

        int soTinChi = enrollments.creditsOfClass(maSinhVien, maLopHP)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ENROLLMENT_NOT_FOUND",
                        "Bạn không đăng ký lớp %s.".formatted(maLopHP)));
        requirePeriodOpen(maHocKy, sv.maCoSoNha());

        // Cùng thứ tự khoá với đăng ký: SinhVienHocKy → LopHocPhan → DangKyHocPhan → DangKyMonHoc → Diem
        requireOne(enrollments.subtractCredits(maSinhVien, maHocKy, soTinChi), "trả tín chỉ");
        classes.releaseSeat(maLopHP);
        requireOne(enrollments.cancelClassEnrollment(maLopHP, maSinhVien), "huỷ ghi danh lớp");
        requireOne(enrollments.cancelCourseEnrollment(maSinhVien, maLopHP), "huỷ ghi danh môn");
        if (!grades.removeEmptyRecord(maLopHP, maSinhVien)) {
            throw new ApiException(HttpStatus.CONFLICT, "ENROLLMENT_HAS_GRADE",
                    "Lớp %s đã có điểm của bạn, không huỷ được.".formatted(maLopHP));
        }
        return myEnrollments(user, maHocKy);
    }

    // --- Nội bộ ----------------------------------------------------------

    private static String requireStudentRole(AuthenticatedUser user) {
        if (user.role() != Role.SINH_VIEN || user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ sinh viên mới đăng ký học phần.");
        }
        return user.entityId();
    }

    private void acquire(String maSinhVien, String maHocKy) {
        if (!enrollments.acquireStudentTermLock(maSinhVien, maHocKy, LOCK_TIMEOUT_MS)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ENROLLMENT_BUSY",
                    "Đang xử lý một yêu cầu đăng ký khác của bạn. Vui lòng thử lại.");
        }
    }

    /** "Đang mở" cần CẢ trạng thái DANG_MO VÀ thời điểm hiện tại nằm trong khung giờ. */
    private void requirePeriodOpen(String maHocKy, String maCoSo) {
        if (!periods.isOpen(maHocKy, maCoSo)) {
            throw new ApiException(HttpStatus.CONFLICT, "ENROLLMENT_PERIOD_CLOSED",
                    "Cơ sở %s không có đợt đăng ký đang mở cho học kỳ %s.".formatted(maCoSo, maHocKy));
        }
    }

    private EnrolledCourse enrolled(String maSinhVien, String maHocKy, String maLopHP) {
        return enrollments.enrolledCourses(maSinhVien, maHocKy).stream()
                .filter(course -> course.maLopHP().equals(maLopHP))
                .findFirst()
                .orElseThrow();
    }

    /** Đã đạt thì là cải thiện, đã trượt thì là học lại; điểm tính là điểm cao nhất. */
    private static String loaiDangKy(String maMonHoc, Map<String, String> ketQua) {
        String truoc = ketQua.get(maMonHoc);
        if (truoc == null) {
            return RegistrationResult.HOC_MOI;
        }
        return GradePolicy.DAT.equals(truoc) ? RegistrationResult.CAI_THIEN : RegistrationResult.HOC_LAI;
    }

    /** Bộ đếm và ghi danh phải khớp; lệch là lỗi dữ liệu, rollback thay vì ghi tiếp. */
    private static void requireOne(int rows, String step) {
        if (rows != 1) {
            throw new IllegalStateException("Dữ liệu ghi danh không khớp ở bước " + step + ".");
        }
    }
}
