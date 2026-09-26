package vn.ptit.one.auth.security;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import vn.ptit.one.auth.model.AuthenticatedUser;

/**
 * Phát access JWT. {@code sid} + {@code ver} là hai claim quyết định; vai trò
 * và cơ sở có mặt cho định tuyến Phần 2 nhưng quyền vẫn lấy từ DB lúc kiểm phiên.
 */
@Component
public class AccessTokenIssuer {

    public static final String CLAIM_SESSION = "sid";
    public static final String CLAIM_VERSION = "ver";

    public record IssuedAccessToken(String value, Instant expiresAt) {
    }

    private final JwtEncoder encoder;
    private final AuthProperties properties;

    public AccessTokenIssuer(JwtEncoder encoder, AuthProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedAccessToken issue(AuthenticatedUser user, Instant now) {
        // Access không được sống quá hạn phiên.
        Instant expiresAt = min(now.plus(properties.accessTtl()), user.sessionExpiresAt());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(AuthProperties.ISSUER)
                .audience(List.of(AuthProperties.AUDIENCE))
                .subject(user.username())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(CLAIM_SESSION, user.sessionId().toString())
                .claim(CLAIM_VERSION, user.accountVersion())
                .claim("role", user.role().name());
        if (user.homeCampus() != null) {
            claims.claim("campus", user.homeCampus());
        }
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        return new IssuedAccessToken(token, expiresAt);
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
