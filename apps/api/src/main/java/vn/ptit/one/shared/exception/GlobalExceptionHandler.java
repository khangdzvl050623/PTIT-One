package vn.ptit.one.shared.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;
import vn.ptit.one.shared.config.TraceIdFilter;

/** Đổi exception thành {@link ApiError}. Không để lộ SQL exception ra ngoài. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> api(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.status())
                .body(ApiError.of(ex.code(), ex.getMessage(), TraceIdFilter.current(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR",
                "Dữ liệu gửi lên không hợp lệ.", fields, TraceIdFilter.current(request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_ERROR",
                "Không đọc được nội dung request.", TraceIdFilter.current(request)));
    }

    /** Tham số sai kiểu, ví dụ {@code ?tuan=abc} hoặc mã thông báo không phải UUID. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR",
                "Dữ liệu gửi lên không hợp lệ.", Map.of(ex.getName(), "Sai định dạng."),
                TraceIdFilter.current(request)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> missingParameter(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR",
                "Dữ liệu gửi lên không hợp lệ.", Map.of(ex.getParameterName(), "Bắt buộc."),
                TraceIdFilter.current(request)));
    }

    /**
     * File tải lên vượt trần {@code spring.servlet.multipart.max-file-size}.
     *
     * <p>Không có nhánh này thì Spring trả {@code 500} và người dùng chỉ thấy
     * "lỗi máy chủ" cho một việc họ sửa được bằng cách chọn ảnh nhỏ hơn.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiError.of("FILE_TOO_LARGE",
                "Ảnh quá lớn. Chọn ảnh dưới 5MB.", TraceIdFilter.current(request)));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> database(DataAccessException ex, HttpServletRequest request) {
        String traceId = TraceIdFilter.current(request);
        log.error("Database error [traceId={}]", traceId, ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError.of("SERVICE_UNAVAILABLE",
                "Hệ thống tạm thời không phục vụ được. Vui lòng thử lại sau.", traceId));
    }
}
