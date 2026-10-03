package vn.ptit.one.timetable.model;

import java.util.List;

/**
 * Lịch của nhiều lớp trong một học kỳ, một lần gọi.
 *
 * <p>Vì sao cần: màn đăng ký phải biết lịch của MỌI lớp đang mở để tô trùng
 * giờ. {@code GET /api/classes} không trả lịch — module {@code course} không
 * phụ thuộc {@code timetable}, đảo lại sẽ thành vòng. Không có endpoint này thì
 * giao diện phải gọi {@code /api/classes/{ma}/schedule} cho từng lớp.
 */
public record ClassSchedules(String maHocKy, List<ClassScheduleEntry> lopHocPhan) {

    public record ClassScheduleEntry(String maLopHP, List<ScheduleSlot> buoiHoc) {
    }
}
