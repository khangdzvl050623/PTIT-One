package vn.ptit.one.auth.security;

import java.util.Set;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Đọc access token từ cookie {@link AuthCookies#ACCESS}, không nhận header
 * {@code Authorization} — một kênh duy nhất, CSRF bảo vệ được toàn bộ.
 *
 * <p>Bỏ qua cookie ở các endpoint không cần access: nếu không, access đã hết
 * hạn nằm trong trình duyệt sẽ làm chính login/refresh trả 401.
 */
final class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final Set<String> IGNORED_PATHS = Set.of(
            "/api/auth/csrf", "/api/auth/login", "/api/auth/refresh");

    @Override
    public String resolve(HttpServletRequest request) {
        if (IGNORED_PATHS.contains(request.getRequestURI())) {
            return null;
        }
        Cookie cookie = WebUtils.getCookie(request, AuthCookies.ACCESS);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }
}
