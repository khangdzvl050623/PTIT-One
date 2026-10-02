/**
 * Xác thực, phiên đăng nhập và tài khoản.
 *
 * <p>Sở hữu {@code CoSo}, {@code DanhBaNguoiDung}, {@code TaiKhoan},
 * {@code TaiKhoanMaster}, {@code PhienDangNhap}, {@code TokenLamMoi},
 * {@code MaKichHoat}, {@code MaXacThuc}.
 *
 * <p>Đã có: đăng nhập/refresh/logout (A0); cấp tài khoản kèm hồ sơ, kích hoạt
 * bằng mã một lần, khoá/mở tài khoản (F02); email + xác minh, đổi mật khẩu,
 * quên mật khẩu qua thư, giới hạn tần suất (A1). Chưa có: bootstrap Admin Master.
 */
package vn.ptit.one.auth;
