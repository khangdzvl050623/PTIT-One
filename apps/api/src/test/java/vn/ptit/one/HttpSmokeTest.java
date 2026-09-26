package vn.ptit.one;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Ghim profile mặc định: chạy qua .env thì SPRING_PROFILES_ACTIVE=central sẽ lọt vào.
@ActiveProfiles("default")
class HttpSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @Test
    void healthIsServedOverHttpWithoutDatabaseConfiguration() throws Exception {
        HttpResponse<String> response = get("/api/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertThat(response.body()).contains("\"service\":\"ptit-one-api\"", "\"status\":\"UP\"");
    }

    @Test
    void protectedApiRejectsAnonymousWithJsonError() throws Exception {
        HttpResponse<String> response = get("/api/me");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"AUTH_SESSION_INVALID\"", "\"traceId\"");
    }

    /*
     * Ca duoi chan hoi quy cho profile mac dinh: co JDBC tren classpath
     * nhung KHONG duoc tu mo ket noi. Neu ai do go dong spring.autoconfigure.exclude
     * hoac ghi sai ten class (Boot 4 da doi package), build se do ngay o day
     * thay vi do tren may nguoi chua cai SQL Server.
     */

    @Test
    void defaultProfileStartsWithoutDataSource() {
        assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
    }

    @Test
    void databaseHealthIsLockedForAnonymous() throws Exception {
        // Lộ tên DB và login SQL nên chỉ quản trị được xem (AUTH-03).
        assertThat(get("/api/health/db").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String path) throws Exception {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + path))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
