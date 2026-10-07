package vn.ptit.one.timetable.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.timetable.model.ClassSchedules;
import vn.ptit.one.timetable.service.ScheduleService;

/**
 * Lịch của mọi lớp trong một học kỳ, một lần gọi.
 *
 * <p>Đặt ở {@code /api/schedules}, KHÔNG dưới {@code /api/classes/...}: ở đó
 * {@code /api/classes/schedules} sẽ nhập nhằng với {@code /api/classes/{maLopHP}}.
 *
 * <p>Phạm vi cơ sở theo đúng {@code GET /api/classes} — xem
 * {@code ScheduleService.termSchedules}.
 */
@RestController
@RequestMapping("/api/schedules")
@Profile("central")
public class TermScheduleController {

    private final ScheduleService schedules;

    public TermScheduleController(ScheduleService schedules) {
        this.schedules = schedules;
    }

    @GetMapping
    public ClassSchedules byTerm(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String maHocKy) {
        return schedules.termSchedules(user, maHocKy);
    }
}
