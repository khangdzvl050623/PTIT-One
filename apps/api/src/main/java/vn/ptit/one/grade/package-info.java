/**
 * Nhập, công bố, khoá điểm và bảng điểm sinh viên.
 *
 * <p>Sở hữu bảng {@code Diem}. Quy tắc điểm (ngưỡng đạt, sau này công thức tổng
 * kết) nằm ở {@code policy}, không phụ thuộc HTTP hay DB.
 *
 * <p>Phụ thuộc một chiều: {@code enrollment → grade}. Dòng {@code Diem} rỗng được
 * tạo lúc đăng ký (F08), nên bảng điểm chỉ cần đọc {@code Diem}, không đọc ghi danh.
 *
 * <p>Đã có: nhập, công bố, khoá điểm (F06) và sinh viên xem bảng điểm (F07).
 */
package vn.ptit.one.grade;
