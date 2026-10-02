package vn.ptit.one.auth.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Mã kích hoạt dùng một lần: 16 ký tự Crockford base32 (80 bit CSPRNG), hiển
 * thị thành bốn nhóm {@code XXXX-XXXX-XXXX-XXXX} để Admin đọc cho người dùng.
 *
 * <p>DB chỉ giữ {@link #hash()}. SHA-256 đủ vì mã có entropy cao — như
 * {@link RefreshToken}, không dùng cho mật khẩu người.
 */
public record ActivationSecret(String value, byte[] hash) {

    /** Crockford: bỏ I, L, O, U để không đọc nhầm với 1, 0, V. */
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static ActivationSecret generate() {
        StringBuilder code = new StringBuilder(LENGTH + 3);
        for (int i = 0; i < LENGTH; i++) {
            if (i > 0 && i % 4 == 0) {
                code.append('-');
            }
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        String value = code.toString();
        return new ActivationSecret(value, hashOf(value));
    }

    /**
     * Băm sau khi chuẩn hoá: bỏ gạch/khoảng trắng, viết hoa, đổi O→0 và I/L→1.
     * Người dùng gõ thường hay thiếu gạch vẫn khớp.
     */
    public static byte[] hashOf(String presented) {
        String normalized = presented.toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]", "")
                .replace('O', '0')
                .replace('I', '1')
                .replace('L', '1');
        try {
            return MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM thiếu SHA-256", ex);
        }
    }

    public static boolean matches(String presented, byte[] storedHash) {
        return MessageDigest.isEqual(hashOf(presented), storedHash);
    }
}
