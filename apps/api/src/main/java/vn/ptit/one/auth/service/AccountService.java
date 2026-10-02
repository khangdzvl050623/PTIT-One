package vn.ptit.one.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AccountContact;
import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AccountSummary;
import vn.ptit.one.auth.model.ActivationCode;
import vn.ptit.one.auth.model.ActivationCodeRecord;
import vn.ptit.one.auth.model.ActivationSecret;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.repository.AccountRepository;
import vn.ptit.one.auth.repository.ActivationCodeRepository;
import vn.ptit.one.auth.security.AuthProperties;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.shared.mail.Mailer;

/**
 * Cấp, kích hoạt và khoá tài khoản (F02). API công khai của module auth cho
 * việc cấp hồ sơ: module {@code student}/{@code teacher} gọi
 * {@link #requireAvailable} rồi {@link #provision} trong giao dịch của chúng.
 *
 * <p>Chỉ Admin Master cấp tài khoản (chốt 02/10/2026): danh bạ là bảng Master
 * sở hữu, B3 cho Admin cơ sở chỉ đọc. Phần 2 tách thành ghi danh bạ + Outbox ở
 * Master rồi worker dựng hồ sơ và tài khoản ở site nhà — "Vòng đời sinh viên".
 */
@Service
@Profile("central")
public class AccountService {

    public static final String HOAT_DONG = AccountRecord.ACTIVE;
    public static final String NGUNG = "NGUNG";

    /** Lần sai thứ 5 thu hồi mã. */
    static final int MAX_ACTIVATION_ATTEMPTS = 5;

    public enum ActivationOutcome { ACTIVATED, REJECTED }

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accounts;
    private final ActivationCodeRepository codes;
    private final SessionService sessions;
    private final AuthProperties properties;
    private final Mailer mailer;
    private final Clock clock;

    public AccountService(AccountRepository accounts, ActivationCodeRepository codes, SessionService sessions,
            AuthProperties properties, Mailer mailer, Clock clock) {
        this.accounts = accounts;
        this.codes = codes;
        this.sessions = sessions;
        this.properties = properties;
        this.mailer = mailer;
        this.clock = clock;
    }

    /**
     * Kiểm TRƯỚC khi module gọi ghi hồ sơ: cơ sở có thật và tên đăng nhập /
     * mã thực thể chưa có trong danh bạ. Không dựa vào lỗi FK/UNIQUE để báo.
     */
    public void requireAvailable(String username, String campus) {
        if (!accounts.campusExists(campus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CAMPUS_NOT_FOUND",
                    "Không có cơ sở %s.".formatted(campus));
        }
        if (accounts.directoryTaken(username, username)) {
            throw accountExists(username);
        }
    }

    /**
     * Danh bạ + tài khoản chưa có mật khẩu + mã kích hoạt, trong giao dịch của
     * người gọi. Tên đăng nhập của SV/GV chính là mã thực thể.
     *
     * @param email tuỳ chọn. Có email và đã bật gửi thư thì mã CHỈ đi qua thư
     *              (thư gửi sau commit); không thì mã trả cho Admin trao tay
     */
    @Transactional
    public ActivationCode provision(String entityId, Role role, String campus, String email) {
        if (role != Role.SINH_VIEN && role != Role.GIANG_VIEN) {
            throw new IllegalArgumentException("Chỉ cấp tài khoản sinh viên/giảng viên qua hồ sơ: " + role);
        }
        try {
            // Phần 1 dựng xong mọi thứ trong cùng giao dịch, nên không có trạng thái chờ đồng bộ.
            accounts.insertDirectory(entityId, campus, role, entityId, HOAT_DONG);
            accounts.insertSiteAccount(entityId, role, entityId, campus, normalizeEmail(email));
        } catch (DuplicateKeyException ex) {
            // Hai Admin cùng cấp một mã: người sau thua ở UNIQUE, cả giao dịch rollback.
            throw accountExists(entityId);
        }
        return issueCode(entityId, normalizeEmail(email), true, clock.instant());
    }

    /**
     * Mã cũ (nếu còn) bị thu hồi. Chỉ cấp cho tài khoản chưa kích hoạt.
     *
     * @param sendByEmail {@code false} để Admin nhận mã trao tay dù tài khoản có
     *                    email — dùng khi thư không tới được
     */
    @Transactional
    public ActivationCode reissueActivationCode(String username, boolean sendByEmail) {
        AccountSummary account = requireSummary(username);
        if (account.loaiNguoiDung() == Role.ADMIN_MASTER) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_NOT_MANAGEABLE",
                    "Tài khoản Admin Master không kích hoạt bằng mã.");
        }
        if (account.daKichHoat()) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_ACTIVATED",
                    "Tài khoản %s đã kích hoạt.".formatted(username));
        }
        Instant now = clock.instant();
        codes.revokeLive(username, now);
        String email = accounts.findContact(username).map(AccountContact::email).orElse(null);
        return issueCode(username, email, sendByEmail, now);
    }

    /**
     * Khoá ({@code NGUNG}) hoặc mở lại ({@code HOAT_DONG}). Khoá thì thu hồi mọi
     * phiên và tăng phiên bản trong CÙNG giao dịch — access token cũ bị từ chối
     * ở request kế tiếp.
     */
    @Transactional
    public AccountSummary changeStatus(String username, String status) {
        if (!HOAT_DONG.equals(status) && !NGUNG.equals(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACCOUNT_STATUS_INVALID",
                    "Trạng thái chỉ nhận HOAT_DONG hoặc NGUNG.");
        }
        AccountSummary account = requireSummary(username);
        if (status.equals(account.trangThai())) {
            return account;
        }
        if (NGUNG.equals(status)) {
            // Thứ tự phiên → token → danh bạ như logout-all, để không deadlock với refresh.
            sessions.revokeAllForAccountChange(username);
        }
        if (accounts.updateStatus(username, status) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_NOT_MANAGEABLE",
                    "Không đổi được trạng thái tài khoản %s (%s, %s)."
                            .formatted(username, account.loaiNguoiDung(), account.trangThai()));
        }
        return requireSummary(username);
    }

    /**
     * Kiểm mã và đặt mật khẩu đầu tiên. Trả kết quả thay vì ném lỗi, để lần nhập
     * sai vẫn COMMIT bộ đếm sai — ném ra thì rollback mất bộ đếm.
     *
     * @param passwordHash đã băm sẵn ở ngoài giao dịch (Argon2 chậm)
     */
    @Transactional
    public ActivationOutcome consumeActivation(String username, String presentedCode, String passwordHash) {
        Instant now = clock.instant();
        ActivationCodeRecord code = codes.lockLive(username).orElse(null);
        if (code == null) {
            return reject(username, "không có mã còn sống");
        }
        if (code.isExpired(now)) {
            return reject(username, "mã hết hạn");
        }
        if (!ActivationSecret.matches(presentedCode, code.hash())) {
            codes.recordFailure(code.codeId(), MAX_ACTIVATION_ATTEMPTS, now);
            return reject(username, "sai mã, lần " + (code.failedAttempts() + 1));
        }
        AccountRecord account = accounts.findByUsername(username).orElse(null);
        if (account == null || !HOAT_DONG.equals(account.status())) {
            return reject(username, "tài khoản không hoạt động");
        }
        if (accounts.setInitialPassword(username, passwordHash) != 1) {
            return reject(username, "đã có mật khẩu");
        }
        codes.markUsed(code.codeId(), now);
        if (code.emailRecipient() != null) {
            // Mã chỉ đi qua thư, Admin không thấy → kích hoạt được là đã chứng minh sở hữu email.
            accounts.markEmailVerified(username, AccountRecord.Source.SITE, code.emailRecipient(), now);
        }
        log.info("Tài khoản {} đã kích hoạt", username);
        return ActivationOutcome.ACTIVATED;
    }

    public List<AccountSummary> search(String maCoSo, Role role) {
        return accounts.search(blankToNull(maCoSo), role);
    }

    private ActivationCode issueCode(String username, String email, boolean sendByEmail, Instant now) {
        ActivationSecret secret = ActivationSecret.generate();
        Instant expiresAt = now.plus(properties.activationTtl());
        String recipient = sendByEmail && email != null && mailer.enabled() ? email : null;
        codes.insert(UUID.randomUUID(), username, secret.hash(), now, expiresAt, recipient);
        if (recipient == null) {
            return new ActivationCode(username, secret.value(), expiresAt, null);
        }
        mailer.sendAfterCommit(recipient, "Kích hoạt tài khoản PTIT One", """
                Chào bạn,

                Nhà trường đã cấp tài khoản PTIT One cho bạn.

                Tên đăng nhập: %s
                Mã kích hoạt:  %s
                Hạn dùng:      %s (giờ UTC)

                Mở PTIT One, chọn "Kích hoạt tài khoản", nhập tên đăng nhập, mã trên và
                mật khẩu bạn tự chọn. Mã chỉ dùng được một lần.

                Nếu bạn không phải người nhận thư này, hãy bỏ qua nó.
                """.formatted(username, secret.value(), expiresAt));
        return new ActivationCode(username, null, expiresAt, recipient);
    }

    static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : AccountContact.normalize(email);
    }

    private AccountSummary requireSummary(String username) {
        return accounts.findSummary(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Không có tài khoản %s.".formatted(username)));
    }

    private static ActivationOutcome reject(String username, String reason) {
        log.info("Từ chối kích hoạt {}: {}", username, reason);
        return ActivationOutcome.REJECTED;
    }

    private static ApiException accountExists(String username) {
        return new ApiException(HttpStatus.CONFLICT, "ACCOUNT_EXISTS",
                "Tên đăng nhập hoặc mã %s đã có tài khoản.".formatted(username));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
