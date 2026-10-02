package vn.ptit.one.timetable.model;

import java.time.LocalTime;

/**
 * Một buổi học trên thời khoá biểu, kèm đủ thông tin để hiển thị mà không phải
 * gọi thêm API lớp/môn.
 *
 * @param gioBatDau  giờ vào của tiết đầu, theo {@code KhungGioTiet}
 * @param gioKetThuc giờ ra của tiết cuối
 */
public record TimetableEntry(
        String maLopHP,
        String maMonHoc,
        String tenMonHoc,
        String tenGiangVien,
        String hinhThucHoc,
        int thu,
        int tietBatDau,
        int soTiet,
        String phongHoc,
        int tuanBatDau,
        int tuanKetThuc,
        LocalTime gioBatDau,
        LocalTime gioKetThuc) {

    /** Buổi này có diễn ra trong tuần đó không. */
    public boolean coTrongTuan(int tuan) {
        return tuanBatDau <= tuan && tuan <= tuanKetThuc;
    }
}
