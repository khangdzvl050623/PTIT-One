package vn.ptit.one.auth.model;

import java.time.Instant;
import java.util.UUID;

/** Một dòng {@code TokenLamMoi}. Token đã dùng vẫn giữ để còn phát hiện replay. */
public record RefreshTokenRecord(
        UUID tokenId,
        UUID sessionId,
        Instant expiresAt,
        Instant usedAt,
        Instant revokedAt) {

    /** Đã rotate rồi mà còn được trình lại = dấu hiệu token bị lấy cắp. */
    public boolean isReplay() {
        return usedAt != null;
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
    }
}
