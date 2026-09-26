package vn.ptit.one.auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Principal đáng tin mà module khác nhận qua {@code @AuthenticationPrincipal}.
 * Mọi trường lấy từ DB lúc kiểm phiên, không lấy từ tham số client gửi.
 *
 * @param entityId   MaSinhVien/MaGiangVien; {@code null} với tài khoản quản trị
 * @param homeCampus mã cơ sở; {@code null} với {@link Role#ADMIN_MASTER}
 */
public record AuthenticatedUser(
        String username,
        Role role,
        String entityId,
        String homeCampus,
        UUID sessionId,
        int accountVersion,
        Instant sessionExpiresAt) {
}
