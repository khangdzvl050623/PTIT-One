package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin đặt email mới cho một tài khoản.
 *
 * <p>Khác {@link ChangeEmailRequest} của chủ tài khoản: KHÔNG có
 * {@code matKhauHienTai}. Đây chính là lý do endpoint tồn tại — người đã quên
 * mật khẩu thì không qua được cửa đó, và quyền ở đây là của Admin Master chứ
 * không phải của chủ tài khoản.
 */
public record AdminChangeEmailRequest(
        @NotBlank(message = "Nhập email.") @Email(message = "Email không hợp lệ.")
        @Size(max = 254, message = "Email quá dài.") String email) {
}
