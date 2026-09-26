package vn.ptit.one.auth.security;

import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import vn.ptit.one.auth.model.AuthenticatedUser;

/** Authentication sau khi JWT hợp lệ VÀ phiên trong DB còn sống. */
public class AuthenticatedUserToken extends AbstractAuthenticationToken {

    private final AuthenticatedUser user;
    private final transient Jwt jwt;

    public AuthenticatedUserToken(AuthenticatedUser user, Jwt jwt) {
        super(List.of(new SimpleGrantedAuthority(user.role().authority())));
        this.user = user;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return user;
    }

    @Override
    public Jwt getCredentials() {
        return jwt;
    }

    @Override
    public String getName() {
        return user.username();
    }
}
