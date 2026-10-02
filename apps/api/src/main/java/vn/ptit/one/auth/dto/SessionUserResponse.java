package vn.ptit.one.auth.dto;

import java.time.Instant;

import vn.ptit.one.auth.model.AccountEmail;
import vn.ptit.one.auth.model.AuthenticatedUser;

/**
 * Danh tính tối thiểu cho frontend. Không trả token — token chỉ nằm trong cookie HttpOnly.
 *
 * @param expiresAt       hạn tuyệt đối của phiên
 * @param accessExpiresAt hạn access hiện tại; frontend refresh trước mốc này
 * @param email           {@code null} khi chưa có email
 * @param emailDaXacMinh  {@code false} thì UI nhắc xác minh: chưa xác minh thì
 *                        không tự khôi phục mật khẩu được
 */
public record SessionUserResponse(
        String username,
        String role,
        String entityId,
        String homeCampus,
        Instant expiresAt,
        Instant accessExpiresAt,
        String email,
        boolean emailDaXacMinh) {

    public static SessionUserResponse of(AuthenticatedUser user, Instant accessExpiresAt, AccountEmail email) {
        return new SessionUserResponse(user.username(), user.role().name(), user.entityId(),
                user.homeCampus(), user.sessionExpiresAt(), accessExpiresAt, email.email(), email.daXacMinh());
    }
}
