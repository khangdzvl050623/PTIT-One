package vn.ptit.one.auth.dto;

import java.time.Instant;

import vn.ptit.one.auth.model.AuthenticatedUser;

/**
 * Danh tính tối thiểu cho frontend. Không trả token — token chỉ nằm trong cookie HttpOnly.
 *
 * @param expiresAt       hạn tuyệt đối của phiên
 * @param accessExpiresAt hạn access hiện tại; frontend refresh trước mốc này
 */
public record SessionUserResponse(
        String username,
        String role,
        String entityId,
        String homeCampus,
        Instant expiresAt,
        Instant accessExpiresAt) {

    public static SessionUserResponse of(AuthenticatedUser user, Instant accessExpiresAt) {
        return new SessionUserResponse(user.username(), user.role().name(), user.entityId(),
                user.homeCampus(), user.sessionExpiresAt(), accessExpiresAt);
    }
}
