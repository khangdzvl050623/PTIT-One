package vn.ptit.one.enrollment.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.enrollment.dto.RegisterRequest;
import vn.ptit.one.enrollment.model.RegistrationResult;
import vn.ptit.one.enrollment.model.StudentEnrollments;
import vn.ptit.one.enrollment.service.EnrollmentService;
import vn.ptit.one.enrollment.service.EnrollmentService.Registration;
import vn.ptit.one.enrollment.service.StudentTimetableService;
import vn.ptit.one.timetable.model.Timetable;

/**
 * Dữ liệu ghi danh của chính sinh viên đang đăng nhập. Mã sinh viên lấy từ
 * principal; không có đường nào nhận mã sinh viên từ client.
 */
@RestController
@RequestMapping("/api/me")
@Profile("central")
public class StudentEnrollmentController {

    private final StudentTimetableService timetables;
    private final EnrollmentService enrollments;

    public StudentEnrollmentController(StudentTimetableService timetables, EnrollmentService enrollments) {
        this.timetables = timetables;
        this.enrollments = enrollments;
    }

    @GetMapping("/timetable")
    public Timetable timetable(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String maHocKy,
            @RequestParam(required = false) Integer tuan) {
        return timetables.timetable(user, maHocKy, tuan);
    }

    @GetMapping("/enrollments")
    public StudentEnrollments enrollments(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String maHocKy) {
        return enrollments.myEnrollments(user, maHocKy);
    }

    /** {@code 201} khi vừa đăng ký; {@code 200} khi gửi lại đúng lớp đang giữ chỗ (bấm hai lần). */
    @PostMapping("/enrollments")
    public ResponseEntity<RegistrationResult> register(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RegisterRequest body) {
        Registration registration = enrollments.register(user, body.maLopHP().trim());
        return ResponseEntity.status(registration.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(registration.result());
    }

    @DeleteMapping("/enrollments/{maLopHP}")
    public StudentEnrollments cancel(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP) {
        return enrollments.cancel(user, maLopHP);
    }
}
