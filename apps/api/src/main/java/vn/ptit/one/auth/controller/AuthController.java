package vn.ptit.one.auth.controller;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.annotation.Profile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import vn.ptit.one.auth.dto.CsrfResponse;
import vn.ptit.one.auth.dto.LoginRequest;
import vn.ptit.one.auth.dto.SessionUserResponse;
import vn.ptit.one.auth.security.AuthCookies;
import vn.ptit.one.auth.security.AuthenticatedUserToken;
import vn.ptit.one.auth.service.AuthenticationService;
import vn.ptit.one.auth.service.AuthenticationService.LoginResult;

@RestController
@RequestMapping("/api/auth")
@Profile("central")
public class AuthController {

    private final AuthenticationService authentication;
    private final AuthCookies cookies;
    private final CsrfTokenRepository csrfTokens;
    private final Clock clock;

    public AuthController(AuthenticationService authentication, AuthCookies cookies,
            CsrfTokenRepository csrfTokens, Clock clock) {
        this.authentication = authentication;
        this.cookies = cookies;
        this.csrfTokens = csrfTokens;
        this.clock = clock;
    }

    /** Gọi trước request ghi đầu tiên; đồng thời đặt cookie {@code XSRF-TOKEN}. */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/login")
    public SessionUserResponse login(@Valid @RequestBody LoginRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        LoginResult result = authentication.login(body.username(), body.password());
        Instant now = clock.instant();
        cookies.writeAccess(response, result.accessToken().value(), result.accessToken().expiresAt(), now);
        cookies.writeRefresh(response, result.refreshToken().value(), result.user().sessionExpiresAt(), now);
        // Đổi CSRF token khi danh tính đổi, như CsrfAuthenticationStrategy của Spring.
        csrfTokens.saveToken(csrfTokens.generateToken(request), request, response);
        return SessionUserResponse.of(result.user(), result.accessToken().expiresAt());
    }

    @GetMapping("/me")
    public SessionUserResponse me(AuthenticatedUserToken authentication) {
        return SessionUserResponse.of(authentication.getPrincipal(), authentication.getCredentials().getExpiresAt());
    }
}
