package vn.ptit.one.timetable.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Thời khoá biểu trong một học kỳ: của sinh viên (F09) hoặc lịch dạy của
 * giảng viên — cùng một hình dạng để UI dùng chung một màn lịch tuần.
 *
 * @param ngayBatDau ngày đầu tuần 1; tuần {@code n} bắt đầu từ
 *                   {@code ngayBatDau + 7·(n-1)} ngày
 * @param tuan       tuần đã lọc; {@code null} là cả học kỳ
 */
public record Timetable(
        String maHocKy,
        LocalDate ngayBatDau,
        Integer tuan,
        List<TimetableEntry> buoiHoc) {
}
