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
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.RefreshToken;
import vn.ptit.one.auth.repository.AccountRepository;
import vn.ptit.one.auth.security.AccessTokenIssuer;
import vn.ptit.one.auth.security.AccessTokenIssuer.IssuedAccessToken;
import vn.ptit.one.auth.service.SessionService.OpenedSession;
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
    private final Clock clock;
    /* Băm giả khi username không tồn tại để thời gian phản hồi không lộ
       tài khoản nào có thật. */
    private final String dummyHash;

    public AuthenticationService(AccountRepository accounts, SessionService sessions,
            PasswordEncoder passwordEncoder, AccessTokenIssuer accessTokens, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
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

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS",
                "Tên đăng nhập hoặc mật khẩu không đúng.");
    }
}
