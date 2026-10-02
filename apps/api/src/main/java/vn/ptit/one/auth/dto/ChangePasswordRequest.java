package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Nhập mật khẩu hiện tại.")
        @Size(max = 128, message = "Mật khẩu quá dài.") String matKhauHienTai,
        @NotBlank(message = "Nhập mật khẩu mới.")
        @Size(min = 8, max = 128, message = "Mật khẩu dài từ 8 đến 128 ký tự.") String matKhauMoi) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[***]";
    }
}
