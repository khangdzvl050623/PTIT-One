package vn.ptit.one.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.security.AccessTokenIssuer;
import vn.ptit.one.auth.security.AuthCookies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chính sách route ở profile mặc định (không DB). Không thay được kiểm thử
 * luồng thật trên SQL Server — xem AuthFlowIntegrationTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityRoutesTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AccessTokenIssuer issuer;

    @Test
    void loginWithoutCsrfIsRejectedBeforeReachingController() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"a\",\"password\":\"b\"}")));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"CSRF_INVALID\"");
    }

    @Test
    void validSignatureAloneIsNotEnoughWithoutLiveSession() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser("B26DCCN001", Role.ADMIN_MASTER, null, null,
                UUID.randomUUID(), 1, Instant.now().plus(Duration.ofDays(1)));
        String token = issuer.issue(user, Instant.now()).value();

        assertThat(getWithAccessCookie("/api/auth/me", token).statusCode()).isEqualTo(401);
        assertThat(getWithAccessCookie("/api/health/db", token).statusCode()).isEqualTo(401);
    }

    @Test
    void forgedOrMalformedTokenIsUnauthorized() throws Exception {
        String forged = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbi5tYXN0ZXIifQ.c2lnbmF0dXJl";
        HttpResponse<String> response = getWithAccessCookie("/api/auth/me", forged);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"AUTH_SESSION_INVALID\"");
    }

    @Test
    void staleAccessCookieDoesNotBlockCsrfEndpoint() throws Exception {
        // Profile mặc định không có AuthController nên 404, nhưng KHÔNG được là 401.
        assertThat(getWithAccessCookie("/api/auth/csrf", "het-han").statusCode()).isNotEqualTo(401);
    }

    private HttpResponse<String> getWithAccessCookie(String path, String token) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).header("Cookie", AuthCookies.ACCESS + "=" + token).GET());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            return client.send(request.timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
