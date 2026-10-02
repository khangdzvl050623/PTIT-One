package vn.ptit.one.enrollment.model;

import java.time.LocalDate;
import java.util.List;

import vn.ptit.one.timetable.model.TimetableEntry;

/**
 * Thời khoá biểu của sinh viên trong một học kỳ (F09).
 *
 * @param ngayBatDau ngày đầu tuần 1; tuần {@code n} bắt đầu từ
 *                   {@code ngayBatDau + 7·(n-1)} ngày
 * @param tuan       tuần đã lọc; {@code null} là cả học kỳ
 */
public record StudentTimetable(
        String maHocKy,
        LocalDate ngayBatDau,
        Integer tuan,
        List<TimetableEntry> buoiHoc) {
}
