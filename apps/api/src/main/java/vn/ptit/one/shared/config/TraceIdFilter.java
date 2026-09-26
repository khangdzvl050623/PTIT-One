package vn.ptit.one.shared.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gắn mã truy vết cho mỗi request: trả qua header {@code X-Trace-Id}, đưa vào
 * log (MDC) và vào body lỗi. Chạy trước Spring Security để cả lỗi 401/403
 * cũng có mã.
 *
 * <p>Luôn tự sinh, không nhận mã client gửi lên — tránh bị chèn chuỗi tùy ý vào log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";
    private static final String ATTRIBUTE = TraceIdFilter.class.getName();
    private static final String MDC_KEY = "traceId";

    public static String current(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);
        return value != null ? value.toString() : null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, traceId);
        response.setHeader(HEADER, traceId);
        MDC.put(MDC_KEY, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
