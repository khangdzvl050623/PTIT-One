package vn.ptit.one.auth.model;

/** Email của tài khoản đang đăng nhập. Chưa xác minh thì chưa khôi phục mật khẩu được. */
public record AccountEmail(String email, boolean daXacMinh) {
}
