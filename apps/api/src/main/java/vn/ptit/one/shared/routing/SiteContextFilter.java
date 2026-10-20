package vn.ptit.one.shared.routing;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.ptit.one.auth.model.AuthenticatedUser;

/**
 * Đặt {@link SiteContext} cho mỗi request, lấy từ phiên đã xác thực.
 *
 * <p>Chạy SAU chuỗi lọc của Spring Security (đăng ký ở độ ưu tiên thấp nhất)
 * nên lúc này principal đã có. Cơ sở đọc từ {@code AuthenticatedUser.homeCampus()},
 * vốn lấy từ DB lúc kiểm phiên — <b>không</b> từ tham số client.
 *
 * <p>Ba trường hợp rơi về {@link SiteContext#MASTER}:
 * <ul>
 *   <li>chưa đăng nhập — đăng nhập phải tra danh bạ ở Master mới biết cơ sở;</li>
 *   <li>Admin Master — không thuộc cơ sở nào, và việc của họ là danh mục;</li>
 *   <li>tài khoản không có cơ sở vì lý do nào khác.</li>
 * </ul>
 *
 * <p>Xoá trong {@code finally} là bắt buộc: thread được tái sử dụng cho request
 * sau, để sót giá trị là request của người khác đọc nhầm database.
 */
@Component
@Profile("distributed")
@Order(Ordered.LOWEST_PRECEDENCE)
public class SiteContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            SiteContext.set(resolveSite());
            chain.doFilter(request, response);
        } finally {
            SiteContext.clear();
        }
    }

    private static String resolveSite() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                && user.homeCampus() != null) {
            return user.homeCampus();
        }
        return SiteContext.MASTER;
    }
}
