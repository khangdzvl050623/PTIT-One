package vn.ptit.one.auth.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.ptit.one.shared.exception.ApiErrorWriter;

/** 401/403/503 của Spring Security theo hợp đồng lỗi chung, không trả trang HTML. */
final class JsonAuthErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthErrorHandler.class);

    private final ApiErrorWriter writer;

    JsonAuthErrorHandler(ApiErrorWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException ex) throws IOException {
        if (ex instanceof SessionCheckUnavailableException) {
            log.error("Không kiểm được phiên, từ chối request", ex.getCause());
            writer.write(request, response, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "Hệ thống tạm thời không phục vụ được. Vui lòng thử lại sau.");
            return;
        }
        writer.write(request, response, HttpStatus.UNAUTHORIZED, "AUTH_SESSION_INVALID",
                "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hiệu lực.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        if (ex instanceof CsrfException) {
            writer.write(request, response, HttpStatus.FORBIDDEN, "CSRF_INVALID",
                    "Thiếu hoặc sai mã CSRF. Tải lại trang rồi thử lại.");
            return;
        }
        writer.write(request, response, HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                "Bạn không có quyền thực hiện thao tác này.");
    }
}
