package vn.ptit.one.auth.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Không kiểm được phiên vì DB lỗi. Từ chối request (503), KHÔNG chấp nhận
 * JWT chỉ vì chữ ký hợp lệ.
 */
class SessionCheckUnavailableException extends AuthenticationException {

    SessionCheckUnavailableException(Throwable cause) {
        super("Không kiểm được phiên đăng nhập", cause);
    }
}
