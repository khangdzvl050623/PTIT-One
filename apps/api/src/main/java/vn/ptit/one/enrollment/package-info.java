/**
 * Đợt đăng ký, đăng ký và hủy đăng ký học phần.
 *
 * <p>Sở hữu {@code DotDangKy}, {@code DangKyHocPhan}, {@code DangKyMonHoc},
 * {@code SinhVienHocKy}. Module khác hỏi "đợt nào đang mở" qua
 * {@code EnrollmentPeriodService}, không tự join vào bảng.
 *
 * <p>Phụ thuộc một chiều sang {@code course}, {@code timetable}, {@code grade},
 * {@code student}.
 * {@code course} hỏi đợt mở qua {@code RegistrationWindow}, không import module này.
 *
 * <p>Đã có: quản lý đợt đăng ký (F04), danh sách sinh viên của lớp (F05),
 * thời khoá biểu sinh viên (F09), đăng ký và huỷ học phần (F08) — nơi tập trung
 * các quy tắc tương tranh, xem {@code EnrollmentService}.
 */
package vn.ptit.one.enrollment;
