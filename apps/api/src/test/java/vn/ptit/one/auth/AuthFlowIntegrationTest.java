package vn.ptit.one.auth;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import vn.ptit.one.auth.security.AuthCookies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Luồng auth THẬT trên SQL Server CENTRAL — không H2, không mock repository.
 *
 * <p>Chỉ chạy khi có {@code PTITONE_DB_URL} (tự bỏ qua trên máy không có DB).
 * Cần DB đã migrate V1 và chạy seed {@code 10-auth-seed.sql}. Chạy kèm .env:
 * {@code .\scripts\dev-api.ps1 -MavenArguments verify}
 *
 * <p>Test có ghi vào DB: tạo phiên, thu hồi phiên, tăng {@code PhienBanTaiKhoan}
 * của GVHN001. Không ảnh hưởng đăng nhập về sau.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";

    @LocalServerPort
    private int port;

    @Test
    void loginMeRefreshLogout() throws Exception {
        Browser browser = new Browser();
        HttpResponse<String> login = browser.login("B26DCCN001", PASSWORD);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("\"role\":\"SINH_VIEN\"", "\"homeCampus\":\"HCM\"");
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(200);
        // Hồi quy: request đã xác thực không được xóa cookie CSRF.
        assertThat(browser.cookie("XSRF-TOKEN")).isNotNull();

        String firstRefresh = browser.cookie(AuthCookies.REFRESH);
        HttpResponse<String> refreshed = browser.post("/api/auth/refresh", "");
        assertThat(refreshed.statusCode()).as(refreshed.body()).isEqualTo(200);
        assertThat(browser.cookie(AuthCookies.REFRESH)).isNotEqualTo(firstRefresh);
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(200);

        assertThat(browser.post("/api/auth/logout", "").statusCode()).isEqualTo(204);
        assertThat(browser.cookie(AuthCookies.ACCESS)).isNull();
        // Refresh đã bị thu hồi cùng phiên.
        browser.setCookie(AuthCookies.REFRESH, firstRefresh, "/api/auth");
        assertThat(browser.post("/api/auth/refresh", "").statusCode()).isEqualTo(401);
    }

    @Test
    void replayingRotatedRefreshRevokesWholeSession() throws Exception {
        Browser victim = new Browser();
        assertThat(victim.login("B26DCCN002", PASSWORD).statusCode()).isEqualTo(200);
        String stolen = victim.cookie(AuthCookies.REFRESH);
        String stolenAccess = victim.cookie(AuthCookies.ACCESS);

        assertThat(victim.post("/api/auth/refresh", "").statusCode()).isEqualTo(200);

        Browser attacker = new Browser();
        attacker.fetchCsrf();
        attacker.setCookie(AuthCookies.REFRESH, stolen, "/api/auth");
        HttpResponse<String> replay = attacker.post("/api/auth/refresh", "");
        assertThat(replay.statusCode()).isEqualTo(401);
        assertThat(replay.body()).contains("AUTH_REFRESH_INVALID");

        // Việc thu hồi đã được commit dù request trả lỗi: nạn nhân cũng mất phiên.
        assertThat(victim.get("/api/auth/me").statusCode()).isEqualTo(401);
        assertThat(victim.post("/api/auth/refresh", "").statusCode()).isEqualTo(401);

        // Phiên ở thiết bị khác không bị ảnh hưởng.
        Browser otherDevice = new Browser();
        assertThat(otherDevice.login("B26DCCN002", PASSWORD).statusCode()).isEqualTo(200);
        assertThat(otherDevice.get("/api/auth/me").statusCode()).isEqualTo(200);
        assertThat(stolenAccess).isNotNull();
    }

    @Test
    void logoutAllKillsEverySessionOfThatAccount() throws Exception {
        Browser laptop = new Browser();
        Browser phone = new Browser();
        assertThat(laptop.login("GVHN001", PASSWORD).statusCode()).isEqualTo(200);
        assertThat(phone.login("GVHN001", PASSWORD).statusCode()).isEqualTo(200);

        assertThat(laptop.post("/api/auth/logout-all", "").statusCode()).isEqualTo(204);

        assertThat(phone.get("/api/auth/me").statusCode()).isEqualTo(401);
        assertThat(phone.post("/api/auth/refresh", "").statusCode()).isEqualTo(401);
        // Đăng nhập lại vẫn được, với phiên bản mới.
        assertThat(new Browser().login("GVHN001", PASSWORD).statusCode()).isEqualTo(200);
    }

    @Test
    void inactiveAccountsCannotSignIn() throws Exception {
        assertThat(new Browser().login("B26DCCN003", PASSWORD).statusCode()).isEqualTo(401);
        assertThat(new Browser().login("B26DCCN004", PASSWORD).statusCode()).isEqualTo(401);
        assertThat(new Browser().login("khong-ton-tai", PASSWORD).statusCode()).isEqualTo(401);
    }

    /** Trình duyệt tối giản: giữ cookie, gửi CSRF header như SPA. */
    private final class Browser {

        private final CookieManager cookies = new CookieManager();
        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();

        HttpResponse<String> login(String username, String password) throws Exception {
            fetchCsrf();
            return post("/api/auth/login",
                    "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        }

        void fetchCsrf() throws Exception {
            assertThat(get("/api/auth/csrf").statusCode()).isEqualTo(200);
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String path, String json) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            String xsrf = cookie("XSRF-TOKEN");
            if (xsrf != null) {
                request.header("X-XSRF-TOKEN", xsrf);
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }

        String cookie(String name) {
            return cookies.getCookieStore().getCookies().stream()
                    .filter(c -> c.getName().equals(name) && !c.hasExpired() && !c.getValue().isEmpty())
                    .map(HttpCookie::getValue).findFirst().orElse(null);
        }

        void setCookie(String name, String value, String path) {
            HttpCookie cookie = new HttpCookie(name, value);
            cookie.setPath(path);
            cookie.setVersion(0);
            cookies.getCookieStore().add(uri("/"), cookie);
        }

        private URI uri(String path) {
            return URI.create("http://127.0.0.1:" + port + path);
        }
    }
}
