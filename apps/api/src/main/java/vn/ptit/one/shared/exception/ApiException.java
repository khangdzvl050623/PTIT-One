package vn.ptit.one.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi có mã ổn định để frontend rẽ nhánh. Module gọi với mã của mình;
 * message là câu hiển thị được cho người dùng.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
