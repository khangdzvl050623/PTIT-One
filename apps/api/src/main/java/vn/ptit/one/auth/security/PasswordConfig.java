package vn.ptit.one.auth.security;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Argon2id qua {@link DelegatingPasswordEncoder}, id {@code {argon2id-v1}}.
 *
 * <p>Tham số m=19456 KiB, t=2, p=1, salt 16B, hash 32B là điểm bắt đầu theo
 * OWASP — đo lại trên máy thật. Đổi tham số thì thêm id mới ({@code argon2id-v2})
 * và giữ id cũ để hash cũ vẫn kiểm được. Không đăng ký {@code noop}.
 *
 * <p>Seed phải băm bằng đúng encoder này, nếu không login sai 100%.
 */
@Configuration
public class PasswordConfig {

    public static final String ENCODER_ID = "argon2id-v1";

    @Bean
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder(ENCODER_ID,
                Map.of(ENCODER_ID, new Argon2PasswordEncoder(16, 32, 1, 19456, 2)));
        // Hash không có id đã biết (dữ liệu hỏng, noop, bcrypt lạc vào) → sai mật khẩu, không ném lỗi 500.
        encoder.setDefaultPasswordEncoderForMatches(new RejectingPasswordEncoder());
        return encoder;
    }

    private static final class RejectingPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return false;
        }
    }
}
