package vn.ptit.one.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.RefreshToken;
import vn.ptit.one.auth.repository.RefreshTokenRepository;
import vn.ptit.one.auth.repository.SessionRepository;
import vn.ptit.one.auth.security.AuthProperties;

/** Mở phiên và kiểm phiên. Mỗi phiên là một token family. */
@Service
@Profile("central")
public class SessionService {

    public record OpenedSession(AuthenticatedUser user, RefreshToken refreshToken) {
    }

    private final SessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final AuthProperties properties;
    private final Clock clock;

    public SessionService(SessionRepository sessions, RefreshTokenRepository refreshTokens,
            AuthProperties properties, Clock clock) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
        this.clock = clock;
    }

    /** Phiên + refresh token đầu tiên (R0) trong cùng một transaction. */
    @Transactional
    public OpenedSession open(AccountRecord account, Instant now) {
        UUID sessionId = UUID.randomUUID();
        // Hạn tuyệt đối tính từ lúc đăng nhập; rotation về sau không kéo dài.
        Instant expiresAt = now.plus(properties.sessionTtl());
        sessions.insert(sessionId, account.username(), account.version(), now, expiresAt);

        RefreshToken refresh = RefreshToken.generate();
        refreshTokens.insert(UUID.randomUUID(), sessionId, refresh.hash(), now, expiresAt);

        AuthenticatedUser user = new AuthenticatedUser(account.username(), account.role(),
                account.entityId(), account.campus(), sessionId, account.version(), expiresAt);
        return new OpenedSession(user, refresh);
    }

    /** Gọi ở mỗi request có access token. DB lỗi thì ném, không coi là hợp lệ. */
    public Optional<AuthenticatedUser> verify(UUID sessionId, String username, int accountVersion) {
        Instant now = clock.instant();
        return sessions.findSnapshot(sessionId)
                .filter(snapshot -> snapshot.accepts(username, accountVersion, now))
                .map(snapshot -> snapshot.toUser());
    }
}
