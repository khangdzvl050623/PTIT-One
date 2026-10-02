package vn.ptit.one.timetable.model;

import java.util.List;

/**
 * Lịch của một lớp.
 *
 * @param phienBanLich tăng mỗi lần đổi lịch; UI đối chiếu để biết lịch đã khác
 */
public record ClassSchedule(String maLopHP, int phienBanLich, List<ScheduleSlot> buoiHoc) {
}
