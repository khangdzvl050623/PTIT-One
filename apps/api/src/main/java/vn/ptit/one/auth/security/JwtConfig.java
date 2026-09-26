package vn.ptit.one.auth.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Ký/kiểm access token bằng HS256 — một ứng dụng vừa phát vừa kiểm nên không
 * cần cặp khóa bất đối xứng. Decoder chỉ nhận HS256 và kiểm chữ ký, issuer,
 * audience, thời hạn.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey key;

    public JwtConfig(AuthProperties properties) {
        this.key = signingKey(properties.jwtSecret());
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(AuthProperties.ISSUER),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(AuthProperties.AUDIENCE))));
        return decoder;
    }

    private static SecretKey signingKey(String secret) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            /* Chỉ xảy ra ở profile mặc định (không DB, không có đăng nhập).
               Profile central bắt buộc PTITONE_JWT_SECRET — thiếu là không khởi động. */
            log.warn("ptitone.auth.jwt-secret trống: dùng khóa tạm, token mất hiệu lực khi khởi động lại.");
            bytes = new byte[MIN_KEY_BYTES];
            new SecureRandom().nextBytes(bytes);
        } else {
            try {
                bytes = Base64.getDecoder().decode(secret.trim());
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("PTITONE_JWT_SECRET phải là chuỗi base64.", ex);
            }
            if (bytes.length < MIN_KEY_BYTES) {
                throw new IllegalStateException("PTITONE_JWT_SECRET phải dài ít nhất " + MIN_KEY_BYTES + " byte.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
