package vn.ptit.one.auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Dòng {@code MaKichHoat} còn sống, đọc dưới khoá để kiểm mã.
 *
 * @param emailRecipient địa chỉ đã nhận mã qua thư; {@code null} khi Admin trao mã
 */
public record ActivationCodeRecord(UUID codeId, byte[] hash, Instant expiresAt, int failedAttempts,
        String emailRecipient) {

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
