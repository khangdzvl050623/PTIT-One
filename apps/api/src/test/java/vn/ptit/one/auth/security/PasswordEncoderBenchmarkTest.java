package vn.ptit.one.auth.security;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Đo thời gian một lần kiểm mật khẩu Argon2id trên máy thật (m=19456 KiB, t=2, p=1).
 * Tắt mặc định để không làm chậm build. Chạy:
 * {@code $env:PTITONE_BENCHMARK='1'; .\mvnw -q test -Dtest=PasswordEncoderBenchmarkTest}
 *
 * <p>Tham số là mức tối thiểu OWASP khuyến nghị nên chỉ chặn trần: trung vị quá
 * 1000 ms thì login thành điểm nghẽn khi demo nhiều người. Tăng tham số là
 * quyết định của nhóm, dựa trên số đo in ra ở đây.
 */
@EnabledIfEnvironmentVariable(named = "PTITONE_BENCHMARK", matches = ".+")
class PasswordEncoderBenchmarkTest {

    private static final int WARMUP = 3;
    private static final int RUNS = 15;

    @Test
    void argon2idVerifyStaysUnderOneSecond() {
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
        String hash = encoder.encode("PtitOne@2026");
        for (int i = 0; i < WARMUP; i++) {
            encoder.matches("PtitOne@2026", hash);
        }

        long[] millis = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            assertThat(encoder.matches("PtitOne@2026", hash)).isTrue();
            millis[i] = (System.nanoTime() - start) / 1_000_000;
        }
        Arrays.sort(millis);
        long median = millis[RUNS / 2];
        System.out.printf("Argon2id verify: min=%d ms, trung vị=%d ms, max=%d ms (%d lần, %d nhân CPU)%n",
                millis[0], median, millis[RUNS - 1], RUNS, Runtime.getRuntime().availableProcessors());

        assertThat(median).isLessThan(1000L);
    }
}
