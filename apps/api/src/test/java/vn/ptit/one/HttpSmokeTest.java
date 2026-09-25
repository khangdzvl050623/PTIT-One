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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    void unimplementedApiReturnsNotFoundInsteadOfFakeSuccess() throws Exception {
        assertThat(get("/api/me").statusCode()).isEqualTo(404);
    }

    /*
     * Hai ca duoi chan hoi quy cho profile mac dinh: co JDBC tren classpath
     * nhung KHONG duoc tu mo ket noi. Neu ai do go dong spring.autoconfigure.exclude
     * hoac ghi sai ten class (Boot 4 da doi package), build se do ngay o day
     * thay vi do tren may nguoi chua cai SQL Server.
     */

    @Test
    void defaultProfileStartsWithoutDataSource() {
        assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
    }

    @Test
    void databaseHealthIsAbsentWithoutCentralProfile() throws Exception {
        assertThat(get("/api/health/db").statusCode()).isEqualTo(404);
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
