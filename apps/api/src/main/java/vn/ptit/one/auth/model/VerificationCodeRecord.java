package vn.ptit.one.auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Dòng {@code MaXacThuc} còn sống, đọc dưới khoá để kiểm mã.
 *
 * @param email địa chỉ đã nhận mã
 */
public record VerificationCodeRecord(UUID codeId, String email, byte[] hash, Instant expiresAt, int failedAttempts) {

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
