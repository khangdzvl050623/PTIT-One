package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đặt mật khẩu đầu tiên bằng mã kích hoạt Admin Master đã cấp. */
public record ActivateAccountRequest(
        @NotBlank(message = "Nhập tên đăng nhập.") @Size(max = 50, message = "Tên đăng nhập quá dài.") String tenDangNhap,
        @NotBlank(message = "Nhập mã kích hoạt.") @Size(max = 32, message = "Mã kích hoạt quá dài.") String maKichHoat,
        // Trần 128 để không ai gửi chuỗi cực dài bắt Argon2 băm.
        @NotBlank(message = "Nhập mật khẩu mới.")
        @Size(min = 8, max = 128, message = "Mật khẩu dài từ 8 đến 128 ký tự.") String matKhauMoi) {

    @Override
    public String toString() {
        return "ActivateAccountRequest[tenDangNhap=" + tenDangNhap + ", maKichHoat=***, matKhauMoi=***]";
    }
}
