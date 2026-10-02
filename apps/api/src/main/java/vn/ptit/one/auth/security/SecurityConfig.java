package vn.ptit.one.auth.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.service.SessionService;
import vn.ptit.one.shared.exception.ApiErrorWriter;

/**
 * Chính sách route. Mặc định mọi thứ cần đăng nhập; chỉ mở đúng những gì
 * phải public. Module khác kiểm quyền theo bản ghi trong service của mình.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    public CsrfTokenRepository csrfTokenRepository(AuthProperties properties) {
        /* Double-submit: cookie XSRF-TOKEN đọc được bằng JS, gửi lại qua header
           X-XSRF-TOKEN. Cần vì trình duyệt tự đính cookie access vào mọi request. */
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax").secure(properties.cookieSecure()));
        return repository;
    }

    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
            ObjectProvider<SessionService> sessions, ApiErrorWriter errorWriter) throws Exception {
        JsonAuthErrorHandler errors = new JsonAuthErrorHandler(errorWriter);
        /* Handler không che (XOR) token: SPA đọc thẳng giá trị cookie, và
           token trả ở GET /api/auth/csrf trùng với cookie. */
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfHandler)
                /* Mặc định Spring xóa cookie CSRF mỗi lần "xác thực thành công" —
                   với JWT stateless là MỌI request, nên request ghi kế tiếp luôn 403.
                   Token CSRF được đổi thủ công đúng một lần lúc login (AuthController). */
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                // Ba endpoint này tự xác định phiên qua cookie, không đòi access còn hạn.
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh",
                        "/api/auth/logout").permitAll()
                // Người chưa kích hoạt chưa có mật khẩu nên chưa đăng nhập được.
                .requestMatchers(HttpMethod.POST, "/api/auth/activate").permitAll()
                .requestMatchers("/error").permitAll()
                /* Swagger UI phải tải được TRƯỚC khi đăng nhập, nếu không thì
                   không ai dùng được form login ngay trong đó. Spec chỉ lộ hình
                   dạng API — thứ frontend vốn đã biết — không lộ dữ liệu.
                   Tắt hẳn ngoài máy dev bằng springdoc.api-docs.enabled=false. */
                .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**",
                        "/swagger-ui.html", "/swagger-ui/**").permitAll()
                // Lộ tên DB và login SQL — chỉ quản trị được xem.
                .requestMatchers("/api/health/db").hasAnyRole(
                        Role.ADMIN_CO_SO.name(), Role.ADMIN_MASTER.name())
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(new CookieBearerTokenResolver())
                .authenticationEntryPoint(errors)
                .accessDeniedHandler(errors)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new SessionJwtAuthenticationConverter(sessions))))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(errors)
                .accessDeniedHandler(errors));
        return http.build();
    }
}
