package vn.ptit.one.auth.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Hai cookie HttpOnly, SameSite=Lax, host-only. Access đi mọi {@code /api};
 * refresh chỉ đi {@code /api/auth} để API nghiệp vụ không bao giờ thấy nó.
 * Không lưu token trong localStorage.
 */
@Component
public class AuthCookies {

    public static final String ACCESS = "PTITONE_AT";
    public static final String REFRESH = "PTITONE_RT";
    static final String ACCESS_PATH = "/api";
    static final String REFRESH_PATH = "/api/auth";

    private final boolean secure;

    public AuthCookies(AuthProperties properties) {
        this.secure = properties.cookieSecure();
    }

    public void writeAccess(HttpServletResponse response, String token, Instant expiresAt, Instant now) {
        add(response, ACCESS, token, ACCESS_PATH, Duration.between(now, expiresAt));
    }

    public void writeRefresh(HttpServletResponse response, String token, Instant expiresAt, Instant now) {
        add(response, REFRESH, token, REFRESH_PATH, Duration.between(now, expiresAt));
    }

    public void clear(HttpServletResponse response) {
        add(response, ACCESS, "", ACCESS_PATH, Duration.ZERO);
        add(response, REFRESH, "", REFRESH_PATH, Duration.ZERO);
    }

    private void add(HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
