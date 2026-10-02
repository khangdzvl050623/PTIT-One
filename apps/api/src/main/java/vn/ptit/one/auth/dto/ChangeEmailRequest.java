package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(
        @NotBlank(message = "Nhập email.") @Email(message = "Email không hợp lệ.")
        @Size(max = 254, message = "Email quá dài.") String email,
        @NotBlank(message = "Nhập mật khẩu hiện tại.")
        @Size(max = 128, message = "Mật khẩu quá dài.") String matKhauHienTai) {

    @Override
    public String toString() {
        return "ChangeEmailRequest[email=" + email + ", matKhauHienTai=***]";
    }
}
