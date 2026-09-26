package vn.ptit.one.auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Phiên + trạng thái tài khoản hiện tại, đọc lại ở MỖI request. Đây là thứ
 * làm thu hồi có hiệu lực ngay thay vì chờ access token hết 15 phút.
 */
public record SessionSnapshot(
        UUID sessionId,
        String username,
        Instant expiresAt,
        Instant revokedAt,
        Role role,
        String campus,
        String entityId,
        String accountStatus,
        int accountVersion,
        int versionAtCreate) {

    /**
     * Được rotate refresh khi phiên còn sống, tài khoản còn hoạt động và
     * phiên bản chưa đổi kể từ lúc đăng nhập (logout-all, khóa, đổi mật khẩu
     * đều tăng phiên bản).
     */
    public boolean canRefresh(Instant now) {
        return revokedAt == null
                && now.isBefore(expiresAt)
                && AccountRecord.ACTIVE.equals(accountStatus)
                && accountVersion == versionAtCreate;
    }

    /** Chấp nhận khi phiên còn sống, đúng chủ, đúng phiên bản và tài khoản còn hoạt động. */
    public boolean accepts(String tokenUsername, int tokenVersion, Instant now) {
        return revokedAt == null
                && now.isBefore(expiresAt)
                && username.equalsIgnoreCase(tokenUsername)
                && accountVersion == tokenVersion
                && AccountRecord.ACTIVE.equals(accountStatus);
    }

    public AuthenticatedUser toUser() {
        return new AuthenticatedUser(username, role, entityId, campus, sessionId, accountVersion, expiresAt);
    }
}
