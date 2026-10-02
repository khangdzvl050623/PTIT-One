package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendActivationRequest(
        @NotBlank(message = "Nhập tên đăng nhập.") @Size(max = 50, message = "Tên đăng nhập quá dài.") String tenDangNhap) {
}
