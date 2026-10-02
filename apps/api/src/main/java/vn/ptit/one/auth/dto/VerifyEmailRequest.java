package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank(message = "Nhập mã xác minh.")
        @Pattern(regexp = "[0-9]{6}", message = "Mã xác minh gồm 6 chữ số.") String maXacThuc) {
}
