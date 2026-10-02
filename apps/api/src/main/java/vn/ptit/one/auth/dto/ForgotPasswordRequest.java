package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code email} chỉ để ĐỐI CHIẾU với email đã lưu; mã không bao giờ gửi tới địa chỉ này. */
public record ForgotPasswordRequest(
        @NotBlank(message = "Nhập tên đăng nhập.") @Size(max = 50, message = "Tên đăng nhập quá dài.") String tenDangNhap,
        @NotBlank(message = "Nhập email.") @Size(max = 254, message = "Email quá dài.") String email) {
}
