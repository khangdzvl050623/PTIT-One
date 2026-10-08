package vn.ptit.one.auth.service;

import java.time.Duration;
import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AccountContact;
import vn.ptit.one.auth.model.AccountEmail;
import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.PasswordPolicy;
import vn.ptit.one.auth.repository.AccountRepository;
import vn.ptit.one.auth.security.AttemptLimiter;
import vn.ptit.one.auth.service.CredentialWriter.Outcome;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.shared.mail.Mailer;

/**
 * Đổi mật khẩu, email + xác minh, quên/khôi phục mật khẩu (A1).
 *
 * <p>Không {@code @Transactional}: băm Argon2 và đếm tần suất ở đây, ngoài giao
 * dịch; phần ghi ở {@link CredentialWriter}.
 *
 * <p>Nguyên tắc khôi phục: mã LUÔN gửi tới email đã lưu VÀ đã xác minh. Email
 * người dùng gõ chỉ để đối chiếu — gửi tới địa chỉ họ gõ thì ai biết username
 * cũng chiếm được tài khoản. Phản hồi của "quên mật khẩu" luôn giống nhau.
 */
@Service
@Profile("central")
public class CredentialService {

    private static final Logger log = LoggerFactory.getLogger(CredentialService.class);

    static final Duration WINDOW = Duration.ofMinutes(15);
    /** Sai mật khẩu hiện tại khi đổi mật khẩu/email. */
    static final int MAX_PASSWORD_FAILURES = 10;
    /** Xin mã khôi phục: theo tài khoản và theo IP. */
    static final int MAX_RECOVERY_REQUESTS_PER_USER = 3;
    static final int MAX_RECOVERY_REQUESTS_PER_IP = 20;
    /** Nhập sai mã khôi phục theo IP — chặn dò mã trên nhiều tài khoản. */
    static final int MAX_RESET_FAILURES_PER_IP = 20;

    private final AccountRepository accounts;
    private final CredentialWriter writer;
    private final PasswordEncoder passwordEncoder;
    private final AttemptLimiter limiter;
    private final Mailer mailer;

    public CredentialService(AccountRepository accounts, CredentialWriter writer, PasswordEncoder passwordEncoder,
            AttemptLimiter limiter, Mailer mailer) {
        this.accounts = accounts;
        this.writer = writer;
        this.passwordEncoder = passwordEncoder;
        this.limiter = limiter;
        this.mailer = mailer;
    }

    public AccountEmail currentEmail(AuthenticatedUser user) {
        AccountContact contact = requireContact(user.username());
        return new AccountEmail(contact.email(), contact.emailVerified());
    }

    /** Cần mật khẩu hiện tại: phiên bị chiếm không đủ để đổi email rồi khôi phục mật khẩu. */
    public AccountEmail changeEmail(AuthenticatedUser user, String email, String currentPassword) {
        requireMail();
        AccountRecord account = requireCurrentPassword(user.username(), currentPassword);
        String normalized = AccountContact.normalize(email);
        writer.changeEmail(user.username(), account.credential().source(), normalized);
        return new AccountEmail(normalized, false);
    }

    /**
     * Admin đặt email thay cho chủ tài khoản.
     *
     * <p>Có vì tự đổi email đòi **mật khẩu hiện tại** — người đã quên mật khẩu
     * và mất hòm thư cũ thì không qua được cửa đó, và cũng không khôi phục
     * được vì mã khôi phục chỉ gửi tới địa chỉ cũ. Đây là bước một của lối
     * thoát; bước hai là {@code POST /api/accounts/{u}/password-reset}.
     *
     * <p>Email mới ở trạng thái **chưa xác minh**, cố ý. Admin bấm "đã xác
     * minh" hộ thì một lỗi gõ sai sẽ tạo ra một email được hệ thống tin tưởng
     * mà không ai sở hữu. Nó tự thành đã xác minh khi chủ tài khoản dùng được
     * mã gửi tới đó ở bước hai.
     *
     * @return email mới, luôn kèm {@code daXacMinh = false}
     */
    public AccountEmail adminChangeEmail(String username, String email) {
        AccountContact contact = requireContact(username);
        String normalized = AccountContact.normalize(email);
        writer.adminSetEmail(username, contact.source(), normalized);
        log.info("Đặt email mới cho {} (chưa xác minh)", username);
        return new AccountEmail(normalized, false);
    }

    /** Không cần mật khẩu: chỉ gửi tới địa chỉ đã lưu, không đổi được địa chỉ qua đây. */
    public void resendEmailVerification(AuthenticatedUser user) {
        requireMail();
        AccountContact contact = requireContact(user.username());
        if (contact.email() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_NOT_SET", "Tài khoản chưa có email.");
        }
        if (contact.emailVerified()) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_VERIFIED", "Email đã được xác minh.");
        }
        if (!limiter.tryAcquire("verify:user:" + user.username().toLowerCase(Locale.ROOT),
                MAX_RECOVERY_REQUESTS_PER_USER, WINDOW)) {
            throw tooManyAttempts();
        }
        writer.resendEmailVerification(user.username(), contact.email());
    }

    public AccountEmail verifyEmail(AuthenticatedUser user, String code) {
        AccountContact contact = requireContact(user.username());
        if (writer.verifyEmail(user.username(), contact.source(), code) != Outcome.OK) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_CODE_INVALID",
                    "Mã xác minh không đúng, đã hết hạn hoặc đã được dùng.");
        }
        return currentEmail(user);
    }

    public void changePassword(AuthenticatedUser user, String currentPassword, String newPassword) {
        AccountRecord account = requireCurrentPassword(user.username(), currentPassword);
        if (newPassword.equals(currentPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_UNCHANGED",
                    "Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        requireAcceptable(user.username(), newPassword);
        writer.changePassword(user.username(), account.credential().source(), passwordEncoder.encode(newPassword));
    }

    /**
     * Luôn trả về bình thường khi chưa chạm trần — dù username không có, email
     * không khớp hay chưa xác minh — để form này không thành công cụ dò tài khoản.
     */
    public void forgotPassword(String username, String email, String clientIp) {
        requireMail();
        String tenDangNhap = username.trim();
        if (!limiter.tryAcquire("recover:ip:" + clientIp, MAX_RECOVERY_REQUESTS_PER_IP, WINDOW)
                || !limiter.tryAcquire("recover:user:" + tenDangNhap.toLowerCase(Locale.ROOT), MAX_RECOVERY_REQUESTS_PER_USER,
                        WINDOW)) {
            throw tooManyAttempts();
        }
        accounts.findContact(tenDangNhap)
                .filter(contact -> AccountRecord.ACTIVE.equals(contact.status()) && contact.hasPassword())
                .filter(contact -> contact.verifiedEmailMatches(email))
                .ifPresent(contact -> writer.issueRecovery(contact.username(), contact.email()));
    }

    public void resetPassword(String username, String code, String newPassword, String clientIp) {
        String ipKey = "reset:ip:" + clientIp;
        if (limiter.isBlocked(ipKey, MAX_RESET_FAILURES_PER_IP, WINDOW)) {
            throw tooManyAttempts();
        }
        String tenDangNhap = username.trim();
        requireAcceptable(tenDangNhap, newPassword);
        if (writer.resetPassword(tenDangNhap, code, passwordEncoder.encode(newPassword)) != Outcome.OK) {
            limiter.record(ipKey, WINDOW);
            throw new ApiException(HttpStatus.BAD_REQUEST, "RESET_CODE_INVALID",
                    "Mã khôi phục không đúng, đã hết hạn hoặc đã được dùng.");
        }
    }

    static void requireAcceptable(String username, String password) {
        if (PasswordPolicy.containsUsername(username, password)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_WEAK",
                    "Mật khẩu không được chứa tên đăng nhập.");
        }
    }

    private AccountRecord requireCurrentPassword(String username, String currentPassword) {
        String key = "password:user:" + username.toLowerCase(Locale.ROOT);
        if (limiter.isBlocked(key, MAX_PASSWORD_FAILURES, WINDOW)) {
            throw tooManyAttempts();
        }
        AccountRecord account = accounts.findByUsername(username)
                .filter(AccountRecord::credentialMatchesDirectory)
                .orElseThrow(() -> new IllegalStateException("Phiên hợp lệ nhưng tài khoản không khớp: " + username));
        if (!passwordEncoder.matches(currentPassword, account.credential().passwordHash())) {
            limiter.record(key, WINDOW);
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_INCORRECT", "Mật khẩu hiện tại không đúng.");
        }
        limiter.reset(key);
        return account;
    }

    private AccountContact requireContact(String username) {
        return accounts.findContact(username)
                .orElseThrow(() -> new IllegalStateException("Phiên hợp lệ nhưng không có tài khoản: " + username));
    }

    private void requireMail() {
        if (!mailer.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_DISABLED",
                    "Hệ thống chưa bật gửi thư nên chưa dùng được chức năng này.");
        }
    }

    static ApiException tooManyAttempts() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AUTH_TOO_MANY_ATTEMPTS",
                "Thử quá nhiều lần. Vui lòng đợi ít phút rồi thử lại.");
    }
}
