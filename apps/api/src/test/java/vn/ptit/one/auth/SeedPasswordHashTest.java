package vn.ptit.one.auth;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.ptit.one.auth.security.PasswordConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seed phải băm bằng đúng encoder API dùng. Đổi tham số Argon2 mà quên seed
 * thì đỏ ở đây, không phải lúc demo.
 */
class SeedPasswordHashTest {

    private static final Path SEED = Path.of("../../db/central/seed/10-auth-seed.sql");
    private static final String DEMO_PASSWORD = "PtitOne@2026";

    private final PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

    @Test
    void seedHashMatchesDemoPasswordWithApiEncoder() throws Exception {
        Matcher matcher = Pattern.compile("'(\\{argon2id-v1}[^']+)'")
                .matcher(Files.readString(SEED, StandardCharsets.UTF_8));

        assertThat(matcher.find()).as("seed phải chứa hash {argon2id-v1}").isTrue();
        assertThat(encoder.matches(DEMO_PASSWORD, matcher.group(1))).isTrue();
        assertThat(encoder.matches("sai-mat-khau", matcher.group(1))).isFalse();
    }

    @Test
    void unknownEncoderIdIsRejectedInsteadOfThrowing() {
        assertThat(encoder.matches(DEMO_PASSWORD, "{noop}" + DEMO_PASSWORD)).isFalse();
        assertThat(encoder.matches(DEMO_PASSWORD, DEMO_PASSWORD)).isFalse();
    }
}
