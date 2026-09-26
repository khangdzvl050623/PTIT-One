package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Giới hạn độ dài mật khẩu để không ai gửi chuỗi cực dài bắt Argon2 băm. */
public record LoginRequest(
        @NotBlank(message = "Nhập tên đăng nhập.") @Size(max = 50, message = "Tên đăng nhập quá dài.") String username,
        @NotBlank(message = "Nhập mật khẩu.") @Size(max = 128, message = "Mật khẩu quá dài.") String password) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
