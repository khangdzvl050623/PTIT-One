package vn.ptit.one.auth.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Refresh token opaque 32 byte từ CSPRNG. Chỉ {@link #value()} đi qua cookie;
 * DB giữ {@link #hash()}.
 *
 * <p>SHA-256 hợp ở đây vì bí mật có entropy cao — KHÔNG dùng thay Argon2 cho
 * mật khẩu người.
 */
public record RefreshToken(String value, byte[] hash) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static RefreshToken generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new RefreshToken(value, hashOf(value));
    }

    public static byte[] hashOf(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM thiếu SHA-256", ex);
        }
    }
}
