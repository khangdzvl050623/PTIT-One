package vn.ptit.one.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.RefreshToken;
import vn.ptit.one.auth.repository.AccountRepository;
import vn.ptit.one.auth.security.AccessTokenIssuer;
import vn.ptit.one.auth.security.AccessTokenIssuer.IssuedAccessToken;
import vn.ptit.one.auth.service.SessionService.OpenedSession;
import vn.ptit.one.auth.service.SessionService.RefreshOutcome;
import vn.ptit.one.shared.exception.ApiException;

/** Đăng nhập bằng tài khoản trong CENTRAL. */
@Service
@Profile("central")
public class AuthenticationService {

    public record LoginResult(AuthenticatedUser user, IssuedAccessToken accessToken, RefreshToken refreshToken) {
    }

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AccountRepository accounts;
    private final SessionService sessions;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokens;
    private final JwtDecoder jwtDecoder;
    private final Clock clock;
    /* Băm giả khi username không tồn tại để thời gian phản hồi không lộ
       tài khoản nào có thật. */
    private final String dummyHash;

    public AuthenticationService(AccountRepository accounts, SessionService sessions,
            PasswordEncoder passwordEncoder, AccessTokenIssuer accessTokens, JwtDecoder jwtDecoder,
            Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.jwtDecoder = jwtDecoder;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /* Không @Transactional: băm Argon2 tốn vài chục ms, không giữ kết nối DB
       trong lúc đó. Chỉ bước ghi phiên mới cần transaction (SessionService.open). */
    public LoginResult login(String username, String password) {
        Optional<AccountRecord> found = accounts.findByUsername(username.trim());
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            throw invalidCredentials();
        }
        AccountRecord account = found.get();
        String hash = account.credential() != null ? account.credential().passwordHash() : null;
        boolean matches = passwordEncoder.matches(password, hash != null ? hash : dummyHash);
        if (hash == null || !matches) {
            throw invalidCredentials();
        }
        if (!account.canSignIn()) {
            // Mật khẩu đúng nhưng không được vào: ghi log để Admin tra, trả lỗi chung.
            log.info("Từ chối đăng nhập {}: trạng thái={}, tài khoản khớp danh bạ={}",
                    account.username(), account.status(), account.credentialMatchesDirectory());
            throw invalidCredentials();
        }

        Instant now = clock.instant();
        OpenedSession opened = sessions.open(account, now);
        IssuedAccessToken access = accessTokens.issue(opened.user(), now);
        return new LoginResult(opened.user(), access, opened.refreshToken());
    }

    /** Rotate refresh + phát access mới. Hạn phiên không đổi. */
    public LoginResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidRefresh();
        }
        // rotate() đã commit (kể cả việc thu hồi khi replay) trước khi ta ném lỗi ở đây.
        RefreshOutcome outcome = sessions.rotate(refreshToken);
        if (outcome instanceof RefreshOutcome.Rotated rotated) {
            IssuedAccessToken access = accessTokens.issue(rotated.user(), clock.instant());
            return new LoginResult(rotated.user(), access, rotated.refreshToken());
        }
        log.info("Từ chối refresh: {}", ((RefreshOutcome.Rejected) outcome).reason());
        throw invalidRefresh();
    }

    /**
     * Thu hồi phiên hiện tại. Xác định phiên qua refresh (luôn được gửi tới
     * {@code /api/auth}) hoặc access còn hạn. Không xác định được thì vẫn coi là
     * đã đăng xuất — không có gì để thu hồi.
     */
    public void logout(String refreshToken, String accessToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessions.revokeByRefreshToken(refreshToken);
            return;
        }
        sessionIdOf(accessToken).ifPresent(sessions::revokeSession);
    }

    public void logoutAll(AuthenticatedUser user) {
        sessions.revokeAllForUser(user.username());
    }

    private Optional<UUID> sessionIdOf(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            return Optional.of(UUID.fromString(jwt.getClaimAsString(AccessTokenIssuer.CLAIM_SESSION)));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    private static ApiException invalidRefresh() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_INVALID",
                "Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.");
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS",
                "Tên đăng nhập hoặc mật khẩu không đúng.");
    }
}
