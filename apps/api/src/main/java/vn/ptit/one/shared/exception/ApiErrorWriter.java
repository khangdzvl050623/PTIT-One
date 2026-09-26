package vn.ptit.one.shared.exception;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import vn.ptit.one.shared.config.TraceIdFilter;

/**
 * Ghi {@link ApiError} thẳng ra response cho những chỗ nằm ngoài
 * {@code @RestControllerAdvice}, như entry point của Spring Security.
 */
@Component
public class ApiErrorWriter {

    private final ObjectMapper json;

    public ApiErrorWriter(ObjectMapper json) {
        this.json = json;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(),
                ApiError.of(code, message, TraceIdFilter.current(request)));
    }
}
