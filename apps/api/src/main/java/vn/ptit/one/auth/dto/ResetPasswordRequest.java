package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Nhập tên đăng nhập.") @Size(max = 50, message = "Tên đăng nhập quá dài.") String tenDangNhap,
        @NotBlank(message = "Nhập mã khôi phục.")
        @Pattern(regexp = "[0-9]{6}", message = "Mã khôi phục gồm 6 chữ số.") String maXacThuc,
        @NotBlank(message = "Nhập mật khẩu mới.")
        @Size(min = 8, max = 128, message = "Mật khẩu dài từ 8 đến 128 ký tự.") String matKhauMoi) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[tenDangNhap=" + tenDangNhap + ", maXacThuc=***, matKhauMoi=***]";
    }
}
