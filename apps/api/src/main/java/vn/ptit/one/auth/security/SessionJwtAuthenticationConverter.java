package vn.ptit.one.auth.security;

import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.shared.routing.SiteContext;
import vn.ptit.one.auth.service.SessionService;

/**
 * Chạy sau khi chữ ký/issuer/audience/hạn đã hợp lệ: đối chiếu {@code sid} +
 * {@code ver} với CENTRAL ở MỖI request. Principal dựng từ DB, không từ claim.
 */
final class SessionJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /* ObjectProvider: profile mặc định không có DB nên không có SessionService.
       Khi đó không token nào được chấp nhận — đúng, vì cũng không có đăng nhập. */
    private final ObjectProvider<SessionService> sessions;

    /* ⚠️ Chỉ đụng tới SiteContext khi CÓ định tuyến. `SiteContextFilter` —
       thứ DỌN context — mang @Profile("distributed"); nếu chỗ ĐẶT không bị
       chặn bởi cùng điều kiện thì ở profile `central` context được đặt mà
       không ai dọn, rò sang request sau trên cùng luồng. Nặng hơn: khi
       `explicit()` khác null, `AccountRepository` chuyển sang câu truy vấn
       Phần 2 và che một trong hai bảng tài khoản bằng bảng rỗng — trên một
       database Phần 1 có đủ cả hai, đó là kết quả sai âm thầm.

       Dùng chính `LoginSiteResolver` làm tín hiệu, giống `AuthenticationService`:
       đặt và dọn khi đó cùng một điều kiện, không thể lệch nhau. */
    private final ObjectProvider<LoginSiteResolver> siteResolver;

    SessionJwtAuthenticationConverter(ObjectProvider<SessionService> sessions,
            ObjectProvider<LoginSiteResolver> siteResolver) {
        this.sessions = sessions;
        this.siteResolver = siteResolver;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        SessionService service = sessions.getIfAvailable();
        if (service == null) {
            throw new InvalidBearerTokenException("Chưa bật kho phiên");
        }
        UUID sessionId = sessionIdOf(jwt);
        Integer version = versionOf(jwt);

        /* ⭐ Phần 2: phiên nằm ở database của CƠ SỞ người dùng, không ở Master.
           Phải đặt cơ sở TRƯỚC khi hỏi, nếu không câu kiểm phiên chạy nhầm
           database và mọi request đều bị từ chối.

           Đặt ở đây chứ không ở SiteContextFilter vì filter đó chạy SAU chuỗi
           lọc bảo mật — tới lúc nó chạy thì việc kiểm phiên đã xong từ lâu.

           Claim `campus` dùng được vì tới bước này chữ ký JWT đã được xác
           minh; nó không phải dữ liệu client tự khai. Dù vậy nó cũng chỉ
           quyết định HỎI Ở ĐÂU: phiên có hợp lệ hay không vẫn do DB trả lời. */
        boolean dinhTuyen = siteResolver.getIfAvailable() != null;
        String campus = jwt.getClaimAsString(AccessTokenIssuer.CLAIM_CAMPUS);
        /* Không có claim nghĩa là Admin Master — họ không thuộc cơ sở nào, và
           tài khoản của họ nằm ở Master. Đặt TƯỜNG MINH chứ không để trống:
           tầng dưới phân biệt "ở Master" với "chưa biết cơ sở", vì hai trạng
           thái đó cần hai câu truy vấn khác nhau. */
        if (dinhTuyen) {
            SiteContext.set(campus != null && !campus.isBlank() ? campus : SiteContext.MASTER);
        }

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
