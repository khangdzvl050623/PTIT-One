package vn.ptit.one.auth.service;

import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.service.AccountService.ActivationOutcome;
import vn.ptit.one.shared.exception.ApiException;

/**
 * Kích hoạt tài khoản (F02): người dùng tự đặt mật khẩu bằng mã một lần.
 *
 * <p>Không {@code @Transactional}: băm Argon2 trước, ngoài giao dịch; phần ghi
 * nằm ở {@link AccountService#consumeActivation}. Mọi lý do từ chối trả CÙNG
 * một lỗi, để không dò được tài khoản nào có thật hay mã nào gần đúng.
 */
@Service
@Profile("central")
public class AccountActivationService {

    private final AccountService accounts;
    private final PasswordEncoder passwordEncoder;

    public AccountActivationService(AccountService accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    public void activate(String username, String code, String newPassword) {
        String tenDangNhap = username.trim();
        if (newPassword.toLowerCase(Locale.ROOT).contains(tenDangNhap.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_WEAK",
                    "Mật khẩu không được chứa tên đăng nhập.");
        }
        String hash = passwordEncoder.encode(newPassword);
        if (accounts.consumeActivation(tenDangNhap, code, hash) != ActivationOutcome.ACTIVATED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACTIVATION_INVALID",
                    "Mã kích hoạt không đúng, đã hết hạn hoặc đã được dùng.");
        }
    }
}
