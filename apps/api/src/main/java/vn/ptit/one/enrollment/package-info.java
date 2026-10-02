/**
 * Đợt đăng ký, đăng ký và hủy đăng ký học phần.
 *
 * <p>Sở hữu {@code DotDangKy}, {@code DangKyHocPhan}, {@code DangKyMonHoc},
 * {@code SinhVienHocKy}. Module khác hỏi "đợt nào đang mở" qua
 * {@code EnrollmentPeriodService}, không tự join vào bảng.
 *
 * <p>Đã có: quản lý đợt đăng ký (F04).
 * Chưa có: đăng ký và hủy học phần (F08) — nơi tập trung các quy tắc tương tranh.
 */
package vn.ptit.one.enrollment;
