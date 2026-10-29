package vn.ptit.one.auth.service;

import java.util.Locale;
import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.security.AttemptLimiter;
import vn.ptit.one.auth.security.LoginSiteResolver;
import vn.ptit.one.auth.service.AccountService.ActivationOutcome;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.shared.mail.Mailer;

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
    private final AttemptLimiter limiter;
    private final Mailer mailer;
    private final ObjectProvider<LoginSiteResolver> siteResolver;

    public AccountActivationService(AccountService accounts, PasswordEncoder passwordEncoder, AttemptLimiter limiter,
            Mailer mailer, ObjectProvider<LoginSiteResolver> siteResolver) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.limiter = limiter;
        this.mailer = mailer;
        this.siteResolver = siteResolver;
    }

    /** Phần 1 không có bean phân giải: một database, gọi thẳng. */
    private <T> T atUserSite(String username, Supplier<T> body, T ifNoSite) {
        LoginSiteResolver resolver = siteResolver.getIfAvailable();
        return resolver == null ? body.get() : resolver.atUserSite(username, body, ifNoSite);
    }

    /**
     * "Không nhận được mã? Gửi lại". Mã đi tới email Admin đã lưu, không bao giờ
     * tới địa chỉ người dùng gõ. Luôn trả bình thường khi chưa chạm trần, để
     * không dò được tài khoản nào có thật hay đã kích hoạt chưa.
     */
    public void resend(String username, String clientIp) {
        if (!mailer.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_DISABLED",
                    "Hệ thống chưa bật gửi thư. Liên hệ Phòng Đào tạo để được cấp lại mã.");
        }
        String tenDangNhap = username.trim();
        if (!limiter.tryAcquire("activation:ip:" + clientIp, CredentialService.MAX_RECOVERY_REQUESTS_PER_IP,
                CredentialService.WINDOW)
                || !limiter.tryAcquire("activation:user:" + tenDangNhap.toLowerCase(Locale.ROOT),
                        CredentialService.MAX_RECOVERY_REQUESTS_PER_USER, CredentialService.WINDOW)) {
            throw CredentialService.tooManyAttempts();
        }
        atUserSite(tenDangNhap, () -> {
            accounts.resendActivationToStoredEmail(tenDangNhap);
            return null;
        }, null);
    }

    public void activate(String username, String code, String newPassword) {
        String tenDangNhap = username.trim();
        CredentialService.requireAcceptable(tenDangNhap, newPassword);
        String hash = passwordEncoder.encode(newPassword);
        if (atUserSite(tenDangNhap, () -> accounts.consumeActivation(tenDangNhap, code, hash),
                ActivationOutcome.REJECTED) != ActivationOutcome.ACTIVATED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACTIVATION_INVALID",
                    "Mã kích hoạt không đúng, đã hết hạn hoặc đã được dùng.");
        }
    }
}
