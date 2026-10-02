package vn.ptit.one.timetable.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.timetable.dto.SetScheduleRequest;
import vn.ptit.one.timetable.model.ClassSchedule;
import vn.ptit.one.timetable.model.ScheduleSlot;
import vn.ptit.one.timetable.service.ScheduleService;

/**
 * Lịch của một lớp học phần.
 *
 * <p>Đường dẫn nằm dưới {@code /api/classes} cho đúng quan hệ tài nguyên, dù
 * code thuộc module {@code timetable} — module sở hữu bảng {@code LichHoc}.
 */
@RestController
@RequestMapping("/api/classes/{maLopHP}/schedule")
@Profile("central")
public class ScheduleController {

    private final ScheduleService schedules;

    public ScheduleController(ScheduleService schedules) {
        this.schedules = schedules;
    }

    @GetMapping
    public ClassSchedule read(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP) {
        return schedules.read(user, maLopHP);
    }

    /** Thay toàn bộ lịch. Danh sách rỗng nghĩa là xoá hết. */
    @PutMapping
    public ClassSchedule replace(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP, @Valid @RequestBody SetScheduleRequest body) {
        List<ScheduleSlot> slots = (body.buoiHoc() == null ? List.<SetScheduleRequest.SlotRequest>of()
                : body.buoiHoc()).stream()
                .map(s -> new ScheduleSlot(s.thu(), s.tietBatDau(), s.soTiet(), s.phongHoc(),
                        s.tuanBatDau(), s.tuanKetThuc()))
                .toList();
        return schedules.replace(user, maLopHP, slots);
    }
}
