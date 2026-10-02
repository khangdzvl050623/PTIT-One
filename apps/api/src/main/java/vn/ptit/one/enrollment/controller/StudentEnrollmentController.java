package vn.ptit.one.enrollment.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
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

    public StudentEnrollmentController(StudentTimetableService timetables) {
        this.timetables = timetables;
    }

    @GetMapping("/timetable")
    public Timetable timetable(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String maHocKy,
            @RequestParam(required = false) Integer tuan) {
        return timetables.timetable(user, maHocKy, tuan);
    }
}
