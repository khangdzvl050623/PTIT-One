/**
 * Lịch học và lịch giảng dạy.
 *
 * <p>Sở hữu bảng {@code LichHoc} và {@code KhungGioTiet}. Phụ thuộc một chiều
 * vào {@code course} để kiểm lớp và quyền; {@code course} không gọi ngược lại.
 *
 * <p>Đã có: xem và thay lịch của lớp (F04), chặn trùng giảng viên và trùng phòng.
 * Lịch dạy của giảng viên. Thời khoá biểu sinh viên (F09) do {@code enrollment}
 * ghép qua {@code ScheduleService.timetable} — module này không hỏi ngược ai đang
 * học lớp nào.
 */
package vn.ptit.one.timetable;
