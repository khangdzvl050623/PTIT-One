package vn.ptit.one.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AccountContact;
import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AccountRecord.Source;
import vn.ptit.one.auth.model.VerificationCodeRecord;
import vn.ptit.one.auth.model.VerificationPurpose;
import vn.ptit.one.auth.repository.AccountRepository;
import vn.ptit.one.auth.repository.VerificationCodeRepository;
import vn.ptit.one.auth.security.AuthProperties;
import vn.ptit.one.auth.security.OtpHasher;
import vn.ptit.one.shared.mail.Mailer;

/**
 * Phần ghi của đổi/khôi phục mật khẩu và xác minh email (A1), mỗi thao tác
 * MỘT giao dịch. Băm Argon2 và giới hạn tần suất nằm ở {@link CredentialService}.
 *
 * <p>Kiểm mã trả kết quả thay vì ném lỗi, để lần nhập sai vẫn COMMIT bộ đếm sai.
 * Thứ tự khoá: {@code MaXacThuc} → phiên → token → danh bạ → tài khoản.
 */
@Service
@Profile("central")
public class CredentialWriter {

    /** Lần sai thứ 5 thu hồi mã. */
    static final int MAX_CODE_ATTEMPTS = 5;

    public enum Outcome { OK, REJECTED }

    private static final Logger log = LoggerFactory.getLogger(CredentialWriter.class);

    private final AccountRepository accounts;
    private final VerificationCodeRepository codes;
    private final SessionService sessions;
    private final OtpHasher otp;
    private final AuthProperties properties;
    private final Mailer mailer;
    private final Clock clock;

    public CredentialWriter(AccountRepository accounts, VerificationCodeRepository codes, SessionService sessions,
            OtpHasher otp, AuthProperties properties, Mailer mailer, Clock clock) {
        this.accounts = accounts;
        this.codes = codes;
        this.sessions = sessions;
        this.otp = otp;
        this.properties = properties;
        this.mailer = mailer;
        this.clock = clock;
    }

    /** Đổi hash, thu hồi MỌI phiên kể cả phiên hiện tại — người dùng đăng nhập lại. */
    @Transactional
    public void changePassword(String username, Source source, String newHash) {
        sessions.revokeAll(username, SessionService.REVOKE_PASSWORD_CHANGE);
        if (accounts.updatePassword(username, source, newHash) != 1) {
            throw new IllegalStateException("Không đổi được mật khẩu của " + username);
        }
    }

    /** Lưu email mới (chưa xác minh) và gửi mã xác minh tới CHÍNH địa chỉ đó. */
    @Transactional
    public void changeEmail(String username, Source source, String email) {
        accounts.setEmail(username, source, email);
        issue(username, VerificationPurpose.XAC_MINH_EMAIL, email);
    }

    /**
     * Admin đặt email thay cho chủ tài khoản — dùng khi người dùng mất quyền
     * vào hòm thư cũ nên không tự đổi được (tự đổi cần mật khẩu hiện tại).
     *
     * <p>Khác {@link #changeEmail}: KHÔNG gửi mã xác minh. Người vừa mất hòm
     * thư thì cũng không xác minh được, và việc này không nên đòi hỏi máy chủ
     * thư phải đang bật. Email mới nằm ở trạng thái **chưa xác minh** cho tới
     * khi chủ tài khoản dùng được mã của lần cấp lại mật khẩu gửi tới đó —
     * đúng cơ chế {@code consumeActivation} đã dùng: dùng được mã trong thư là
     * đã chứng minh sở hữu hòm thư.
     *
     * <p>Mã xác minh đang chờ cho địa chỉ CŨ bị thu hồi, nếu không nó vẫn còn
     * sống và xác minh nhầm địa chỉ không còn là của tài khoản.
     */
    @Transactional
    public void adminSetEmail(String username, Source source, String email) {
        accounts.setEmail(username, source, email);
        codes.revokeLive(username, VerificationPurpose.XAC_MINH_EMAIL, clock.instant());
    }

    /** Gửi lại mã tới email ĐANG CHỜ xác minh; mã cũ mất hiệu lực. */
    @Transactional
    public void resendEmailVerification(String username, String email) {
        issue(username, VerificationPurpose.XAC_MINH_EMAIL, email);
    }

    @Transactional
    public Outcome verifyEmail(String username, Source source, String presented) {
        Instant now = clock.instant();
        VerificationCodeRecord code = liveCode(username, VerificationPurpose.XAC_MINH_EMAIL, presented, now);
        if (code == null) {
            return Outcome.REJECTED;
        }
        // Đổi email sau khi xin mã thì mã cũ không xác minh được địa chỉ mới.
        if (accounts.markEmailVerified(username, source, code.email(), now) != 1) {
            return reject(username, "email đã đổi sau khi gửi mã");
        }
        codes.markUsed(code.codeId(), now);
        return Outcome.OK;
    }

    /** @param email email ĐÃ LƯU và đã xác minh — không bao giờ là email người dùng gõ */
    @Transactional
    public void issueRecovery(String username, String email) {
        issue(username, VerificationPurpose.KHOI_PHUC_MAT_KHAU, email);
    }

    @Transactional
    public Outcome resetPassword(String username, String presented, String newHash) {
        Instant now = clock.instant();
        VerificationCodeRecord code = liveCode(username, VerificationPurpose.KHOI_PHUC_MAT_KHAU, presented, now);
        if (code == null) {
            return Outcome.REJECTED;
        }
        AccountContact account = accounts.findContact(username).orElse(null);
        if (account == null || !AccountRecord.ACTIVE.equals(account.status()) || !account.hasPassword()) {
            return reject(username, "tài khoản không khôi phục được");
        }
        sessions.revokeAll(username, SessionService.REVOKE_PASSWORD_RESET);
        if (accounts.updatePassword(username, account.source(), newHash) != 1) {
            throw new IllegalStateException("Không đặt lại được mật khẩu của " + username);
        }
        codes.markUsed(code.codeId(), now);
        log.info("Tài khoản {} đã khôi phục mật khẩu", username);
        return Outcome.OK;
    }

    /** Mã còn sống, chưa hết hạn và KHỚP; sai thì tính một lần sai. {@code null} = từ chối. */
    private VerificationCodeRecord liveCode(String username, VerificationPurpose purpose, String presented,
            Instant now) {
        VerificationCodeRecord code = codes.lockLive(username, purpose).orElse(null);
        if (code == null || code.isExpired(now)) {
            reject(username, purpose + ": không có mã còn hạn");
            return null;
        }
        if (!otp.matches(purpose.name(), username, presented, code.hash())) {
            codes.recordFailure(code.codeId(), MAX_CODE_ATTEMPTS, now);
            reject(username, purpose + ": sai mã, lần " + (code.failedAttempts() + 1));
            return null;
        }
        return code;
    }

    private void issue(String username, VerificationPurpose purpose, String email) {
        Instant now = clock.instant();
        codes.revokeLive(username, purpose, now);
        String code = otp.generate();
        Instant expiresAt = now.plus(properties.otpTtl());
        codes.insert(UUID.randomUUID(), username, purpose, email, otp.hash(purpose.name(), username, code),
                now, expiresAt);
        long minutes = properties.otpTtl().toMinutes();
        if (purpose == VerificationPurpose.XAC_MINH_EMAIL) {
            mailer.sendAfterCommit(email, "Mã xác minh email PTIT One: " + code, """
                    Mã xác minh email cho tài khoản %s là: %s

                    Mã có hiệu lực %d phút và chỉ dùng được một lần.
                    Nếu bạn không yêu cầu, hãy bỏ qua thư này.
                    """.formatted(username, code, minutes));
        } else {
            mailer.sendAfterCommit(email, "Mã khôi phục mật khẩu PTIT One: " + code, """
                    Có yêu cầu đặt lại mật khẩu cho tài khoản %s.

                    Mã khôi phục: %s
                    Mã có hiệu lực %d phút và chỉ dùng được một lần.

                    Nếu bạn không yêu cầu, hãy bỏ qua thư này — mật khẩu của bạn không đổi.
                    """.formatted(username, code, minutes));
        }
    }

    private static Outcome reject(String username, String reason) {
        log.info("Từ chối mã của {}: {}", username, reason);
        return Outcome.REJECTED;
    }
}
