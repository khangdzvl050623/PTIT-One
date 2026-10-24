package vn.ptit.one.shared.routing;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Dọn {@link SiteContext} sau mỗi request.
 *
 * <p>Việc ĐẶT cơ sở không nằm ở đây — nó nằm ở {@code SessionJwtAuthenticationConverter},
 * vì phiên đăng nhập được kiểm bên trong chuỗi lọc bảo mật, sớm hơn mọi filter
 * thường. Filter này chỉ lo phần còn lại, và phần đó quan trọng không kém.
 *
 * <p>Chạy <b>trước</b> chuỗi bảo mật, có chủ đích. Nếu đặt sau, một request bị
 * từ chối 401 sẽ không bao giờ đi tới đây, nên {@code finally} không chạy và
 * giá trị cơ sở sót lại trên thread. Servlet container tái sử dụng thread, nên
 * request kế tiếp — có thể của người khác, ở cơ sở khác — sẽ đọc nhầm database.
 * Lỗi kiểu đó không ném exception và chỉ lộ ra khi đối soát dữ liệu.
 *
 * <p>Chưa đặt thì {@link SiteContext#current()} trả về {@code MASTER}, đúng cho
 * đăng nhập (phải tra danh bạ) và cho Admin Master (không thuộc cơ sở nào).
 */
@Component
@Profile("distributed")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SiteContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            SiteContext.clear();
            chain.doFilter(request, response);
        } finally {
            SiteContext.clear();
        }
    }
}
