package vn.ptit.one.auth.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình auth. Khóa ký nằm ở biến môi trường {@code PTITONE_JWT_SECRET},
 * không vào Git, không vào {@code VITE_*}.
 *
 * @param jwtSecret    base64 của ít nhất 32 byte ngẫu nhiên; trống ở profile
 *                     mặc định thì sinh khóa tạm cho mỗi lần chạy
 * @param accessTtl    hạn access token
 * @param sessionTtl   hạn tuyệt đối của phiên tính từ lúc đăng nhập
 * @param cookieSecure bật khi chạy HTTPS
 */
@ConfigurationProperties("ptitone.auth")
public record AuthProperties(
        String jwtSecret,
        @DefaultValue("PT15M") Duration accessTtl,
        @DefaultValue("P7D") Duration sessionTtl,
        @DefaultValue("false") boolean cookieSecure) {

    public static final String ISSUER = "ptit-one-api";
    public static final String AUDIENCE = "ptit-one-web";
}
