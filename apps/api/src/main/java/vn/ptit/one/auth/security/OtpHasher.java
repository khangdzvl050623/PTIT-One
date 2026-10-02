package vn.ptit.one.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mã 6 chữ số một lần (A1) và HMAC-SHA256 của nó.
 *
 * <p>Mã chỉ có 10^6 khả năng: lộ bảng mà hash là SHA-256 trần thì dò ngược trong
 * tích tắc. HMAC với khoá {@code PTITONE_OTP_SECRET} nằm ngoài DB chặn việc đó.
 * Hash gắn cả mục đích và tên đăng nhập, nên mã của tài khoản/mục đích này
 * không dùng được cho cái khác. Phòng thủ chính vẫn là hạn 10 phút + 5 lần sai.
 */
@Component
@Profile("central")
public class OtpHasher {

    private static final int MIN_KEY_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public OtpHasher(AuthProperties properties) {
        String secret = properties.otpSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Thiếu PTITONE_OTP_SECRET (base64, >= 32 byte, khác khoá JWT).");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("PTITONE_OTP_SECRET phải là chuỗi base64.", ex);
        }
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("PTITONE_OTP_SECRET phải dài ít nhất " + MIN_KEY_BYTES + " byte.");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** {@code 000000}–{@code 999999}, giữ số 0 đầu. */
    public String generate() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    public byte[] hash(String purpose, String username, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            String material = purpose + '\n' + username + '\n' + code.trim();
            return mac.doFinal(material.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("JVM thiếu HmacSHA256", ex);
        }
    }

    public boolean matches(String purpose, String username, String presented, byte[] storedHash) {
        return MessageDigest.isEqual(hash(purpose, username, presented), storedHash);
    }
}
