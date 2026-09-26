package vn.ptit.one.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.RefreshToken;
import vn.ptit.one.auth.model.RefreshTokenRecord;
import vn.ptit.one.auth.model.SessionSnapshot;
import vn.ptit.one.auth.repository.AccountRepository;
import vn.ptit.one.auth.repository.RefreshTokenRepository;
import vn.ptit.one.auth.repository.SessionRepository;
import vn.ptit.one.auth.security.AuthProperties;

/**
 * Vòng đời phiên: mở, kiểm, rotate refresh, thu hồi. Mỗi phiên là một token
 * family, nên "thu hồi family" và "thu hồi phiên" là cùng một thao tác.
 */
@Service
@Profile("central")
public class SessionService {

    public record OpenedSession(AuthenticatedUser user, RefreshToken refreshToken) {
    }

    /** Kết quả rotate. Không ném exception trong transaction để việc thu hồi khi replay vẫn được commit. */
    public sealed interface RefreshOutcome {
        record Rotated(AuthenticatedUser user, RefreshToken refreshToken) implements RefreshOutcome {
        }

        record Rejected(String reason) implements RefreshOutcome {
        }
    }

    static final String REVOKE_LOGOUT = "DANG_XUAT";
    static final String REVOKE_LOGOUT_ALL = "DANG_XUAT_TAT_CA";
    static final String REVOKE_REPLAY = "PHAT_HIEN_DUNG_LAI";

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final AccountRepository accounts;
    private final AuthProperties properties;
    private final Clock clock;

    public SessionService(SessionRepository sessions, RefreshTokenRepository refreshTokens,
            AccountRepository accounts, AuthProperties properties, Clock clock) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.accounts = accounts;
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
                .map(SessionSnapshot::toUser);
    }

    /**
     * Rotate {@code Rn → Rn+1} trong một transaction. Token đã dùng mà được trình
     * lại = replay → thu hồi cả phiên và COMMIT việc thu hồi, rồi mới báo từ chối.
     */
    @Transactional
    public RefreshOutcome rotate(String presentedToken) {
        Instant now = clock.instant();
        byte[] hash = RefreshToken.hashOf(presentedToken);
        Optional<RefreshTokenRecord> found = refreshTokens.findByHash(hash);
        if (found.isEmpty()) {
            return new RefreshOutcome.Rejected("không có token");
        }

        // Khóa dòng phiên trước: hai refresh cùng phiên xếp hàng ở đây.
        Optional<SessionSnapshot> locked = sessions.lockSnapshot(found.get().sessionId());
        if (locked.isEmpty()) {
            return new RefreshOutcome.Rejected("không có phiên");
        }
        SessionSnapshot session = locked.get();
        // Đọc lại token SAU khi đã giữ khóa phiên, để thấy kết quả của refresh chạy trước.
        RefreshTokenRecord token = refreshTokens.findByHash(hash).orElseThrow();

        if (token.isReplay()) {
            revokeFamily(session.sessionId(), now, REVOKE_REPLAY);
            log.warn("Phát hiện dùng lại refresh token đã rotate: thu hồi phiên {} của {}",
                    session.sessionId(), session.username());
            return new RefreshOutcome.Rejected("replay");
        }
        if (!token.isUsable(now) || !session.canRefresh(now)) {
            return new RefreshOutcome.Rejected("phiên hoặc token hết hiệu lực");
        }
        if (refreshTokens.markUsed(token.tokenId(), now) != 1) {
            // Không thể xảy ra khi đã giữ khóa phiên; nếu có thì coi như replay.
            revokeFamily(session.sessionId(), now, REVOKE_REPLAY);
            return new RefreshOutcome.Rejected("mất cuộc đua rotate");
        }

        RefreshToken next = RefreshToken.generate();
        // Hạn token mới = hạn phiên: rotation không kéo dài 7 ngày.
        refreshTokens.insert(UUID.randomUUID(), session.sessionId(), next.hash(), now, session.expiresAt());
        return new RefreshOutcome.Rotated(session.toUser(), next);
    }

    /** Logout theo refresh token; không tìm thấy thì coi như đã đăng xuất. */
    @Transactional
    public void revokeByRefreshToken(String presentedToken) {
        refreshTokens.findByHash(RefreshToken.hashOf(presentedToken))
                .ifPresent(token -> revokeFamily(token.sessionId(), clock.instant(), REVOKE_LOGOUT));
    }

    @Transactional
    public void revokeSession(UUID sessionId) {
        revokeFamily(sessionId, clock.instant(), REVOKE_LOGOUT);
    }

    /**
     * Logout-all: thu hồi mọi phiên rồi tăng phiên bản, trong một transaction.
     * Thứ tự phiên → token → danh bạ khớp với refresh (xem SessionRepository).
     */
    @Transactional
    public void revokeAllForUser(String username) {
        Instant now = clock.instant();
        sessions.revokeAllForUser(username, now, REVOKE_LOGOUT_ALL);
        refreshTokens.revokeAllForUser(username, now);
        accounts.bumpVersion(username);
    }

    private void revokeFamily(UUID sessionId, Instant now, String reason) {
        sessions.revoke(sessionId, now, reason);
        refreshTokens.revokeAllForSession(sessionId, now);
    }
}
