package vn.ptit.one.auth.security;

import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.service.SessionService;

/**
 * Chạy sau khi chữ ký/issuer/audience/hạn đã hợp lệ: đối chiếu {@code sid} +
 * {@code ver} với CENTRAL ở MỖI request. Principal dựng từ DB, không từ claim.
 */
final class SessionJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /* ObjectProvider: profile mặc định không có DB nên không có SessionService.
       Khi đó không token nào được chấp nhận — đúng, vì cũng không có đăng nhập. */
    private final ObjectProvider<SessionService> sessions;

    SessionJwtAuthenticationConverter(ObjectProvider<SessionService> sessions) {
        this.sessions = sessions;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        SessionService service = sessions.getIfAvailable();
        if (service == null) {
            throw new InvalidBearerTokenException("Chưa bật kho phiên");
        }
        UUID sessionId = sessionIdOf(jwt);
        Integer version = versionOf(jwt);
        AuthenticatedUser user;
        try {
            user = service.verify(sessionId, jwt.getSubject(), version)
                    .orElseThrow(() -> new InvalidBearerTokenException("Phiên không còn hiệu lực"));
        } catch (DataAccessException ex) {
            throw new SessionCheckUnavailableException(ex);
        }
        return new AuthenticatedUserToken(user, jwt);
    }

    private static UUID sessionIdOf(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getClaimAsString(AccessTokenIssuer.CLAIM_SESSION));
        } catch (RuntimeException ex) {
            throw new InvalidBearerTokenException("Thiếu sid");
        }
    }

    private static Integer versionOf(Jwt jwt) {
        Object value = jwt.getClaim(AccessTokenIssuer.CLAIM_VERSION);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new InvalidBearerTokenException("Thiếu ver");
    }
}
