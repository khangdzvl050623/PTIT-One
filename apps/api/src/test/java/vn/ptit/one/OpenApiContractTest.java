package vn.ptit.one;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cổng chặn lệch contract: {@code openapi.json} đã commit phải khớp spec mà
 * code hiện sinh ra. Thêm hay đổi endpoint mà quên sinh lại file thì build đỏ
 * ở đây — nhờ vậy frontend đọc được thay đổi contract trong diff của PR thay
 * vì phát hiện lúc chạy lên và vỡ.
 *
 * <p>Sinh lại khi đã đổi API có chủ đích:
 * <pre>cd apps/api &amp;&amp; ./mvnw test -Dtest=OpenApiContractTest -Dptitone.openapi.write=true</pre>
 *
 * <p>Chạy ở profile {@code central} vì mọi controller nghiệp vụ đều
 * {@code @Profile("central")} — profile mặc định chỉ sinh ra spec của
 * {@code /api/health}. KHÔNG cần SQL Server: URL dưới trỏ vào host không tồn
 * tại và {@code initialization-fail-timeout=-1} cho ứng dụng khởi động với DB
 * đang tắt. Nhờ đó CI kiểm được contract dù không có database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        // Ghi thẳng URL, không qua ${PTITONE_DB_URL}: test phải cho cùng kết quả
        // trên máy có .env trỏ DB thật và trên CI không có biến nào.
        "spring.datasource.url=jdbc:sqlserver://localhost:1;databaseName=khong-ton-tai;encrypt=false",
        "spring.datasource.username=khong-dung",
        "spring.datasource.password=khong-dung",
        "spring.flyway.enabled=false",
        // Base64 của 32 byte 0x00: chỉ để bean khởi tạo được, không ký gì thật.
        "ptitone.auth.jwt-secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@ActiveProfiles("central")
class OpenApiContractTest {

    /** Đặt cạnh pom.xml; surefire chạy với thư mục làm việc là apps/api. */
    private static final Path SPEC = Path.of("openapi.json");

    private static final String WRITE_FLAG = "ptitone.openapi.write";

    @LocalServerPort
    private int port;

    @Test
    void committedSpecMatchesGeneratedSpec() throws Exception {
        String generated = normalise(fetchSpec());

        if (Boolean.getBoolean(WRITE_FLAG)) {
            Files.writeString(SPEC, generated, StandardCharsets.UTF_8);
            return;
        }

        assertThat(SPEC)
                .withFailMessage("Thiếu %s. Sinh lại: ./mvnw test -Dtest=OpenApiContractTest -D%s=true",
                        SPEC, WRITE_FLAG)
                .exists();

        assertThat(Files.readString(SPEC, StandardCharsets.UTF_8).replace("\r\n", "\n"))
                .withFailMessage("""
                        %s đã lệch với code.

                        API đổi có chủ đích thì sinh lại rồi commit cùng PR:
                          cd apps/api && ./mvnw test -Dtest=OpenApiContractTest -D%s=true

                        Nhớ sửa docs/PTIT-One-API-Contract.md nếu đổi quyền, mã lỗi
                        hoặc quy tắc nghiệp vụ — springdoc không sinh được ba thứ đó.""",
                        SPEC, WRITE_FLAG)
                .isEqualTo(generated);
    }

    @Test
    void specCoversBusinessEndpointsNotJustHealth() throws Exception {
        JsonNode paths = JsonMapper.builder().build().readTree(fetchSpec()).get("paths");

        /* Chặn hồi quy im lặng: nếu profile `central` không nạp được controller
           nghiệp vụ, spec vẫn hợp lệ nhưng rỗng nghĩa — và lúc đó so sánh ở
           trên sẽ "khớp" với một file rỗng nghĩa y như vậy. */
        assertThat(paths.properties()).map(entry -> entry.getKey())
                .contains("/api/auth/login", "/api/courses", "/api/classes",
                        "/api/classes/{maLopHP}/schedule", "/api/enrollment-periods",
                        "/api/teachers");
    }

    /** Spec mở cho người chưa đăng nhập — Swagger UI phải tải được trước khi login. */
    private String fetchSpec() throws Exception {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v3/api-docs"))
                            .timeout(Duration.ofSeconds(20))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertThat(response.statusCode()).isEqualTo(200);
            return response.body();
        }
    }

    /**
     * In lại có thụt lề và khóa sắp theo thứ tự chữ: diff của file chỉ hiện
     * thay đổi thật, không hiện thứ tự nạp bean đổi giữa hai lần chạy.
     */
    private static String normalise(String json) {
        /* WRITE_PROPERTIES_SORTED, không phải ORDER_MAP_ENTRIES_BY_KEYS: cờ sau
           chỉ tác động lên Map thường, ObjectNode vẫn ra theo thứ tự chèn. */
        ObjectMapper mapper = JsonMapper.builder()
                .enable(JsonNodeFeature.WRITE_PROPERTIES_SORTED)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        ObjectNode spec = (ObjectNode) mapper.readTree(json);
        /* `servers` mang cổng random của test nên đổi mỗi lần chạy — giữ lại là
           file lệch liên tục mà không có thay đổi API nào. Bỏ luôn cũng đúng
           hơn: spec không nên khẳng định API nằm ở host nào, mỗi người tự chạy. */
        spec.remove("servers");
        return mapper.writeValueAsString(spec).replace("\r\n", "\n") + "\n";
    }
}
