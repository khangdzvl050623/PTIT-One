package vn.ptit.one.timetable.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.timetable.model.Timetable;
import vn.ptit.one.timetable.service.ScheduleService;

/** Lịch dạy của giảng viên đang đăng nhập. Cùng hình dạng với thời khoá biểu sinh viên. */
@RestController
@RequestMapping("/api/me")
@Profile("central")
public class TeachingScheduleController {

    private final ScheduleService schedules;

    public TeachingScheduleController(ScheduleService schedules) {
        this.schedules = schedules;
    }

    @GetMapping("/teaching-schedule")
    public Timetable teachingSchedule(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String maHocKy,
            @RequestParam(required = false) Integer tuan) {
        return schedules.teachingSchedule(user, maHocKy, tuan);
    }
}
