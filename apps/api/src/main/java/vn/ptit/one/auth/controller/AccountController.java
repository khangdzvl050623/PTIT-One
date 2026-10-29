package vn.ptit.one.auth.controller;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.dto.AdminChangeEmailRequest;
import vn.ptit.one.auth.dto.ChangeAccountStatusRequest;
import vn.ptit.one.auth.model.AccountEmail;
import vn.ptit.one.auth.model.AccountSummary;
import vn.ptit.one.auth.model.ActivationCode;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.security.LoginSiteResolver;
import vn.ptit.one.auth.service.AccountService;
import vn.ptit.one.auth.service.CredentialService;
import vn.ptit.one.auth.service.SessionService;
import vn.ptit.one.shared.routing.SiteContext;

/**
 * Quản trị tài khoản (F02). Chỉ {@code ADMIN_MASTER}: danh bạ là bảng Master
 * sở hữu, B3 cho Admin cơ sở chỉ đọc. Tạo tài khoản đi kèm tạo hồ sơ, ở
 * {@code POST /api/students} và {@code POST /api/teachers}.
 */
@RestController
@RequestMapping("/api/accounts")
@Profile("central")
@PreAuthorize("hasRole('ADMIN_MASTER')")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accounts;
    private final CredentialService credentials;
    private final SessionService sessions;
    private final ObjectProvider<LoginSiteResolver> siteResolver;

    public AccountController(AccountService accounts, CredentialService credentials, SessionService sessions,
            ObjectProvider<LoginSiteResolver> siteResolver) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.sessions = sessions;
        this.siteResolver = siteResolver;
    }

    /**
     * Phần 2: Admin Master đứng ở Master, còn {@code TaiKhoan}/{@code MaKichHoat}
     * của SV/GV nằm ở cơ sở nhà. Đổi nút ở ĐÂY vì các phương thức service là
     * {@code @Transactional}, mà không được đổi nút trong giao dịch.
     */
    private <T> T atAccountSite(String username, Supplier<T> body) {
        LoginSiteResolver resolver = siteResolver.getIfAvailable();
        return resolver == null ? body.get() : SiteContext.runAt(resolver.resolve(username), body);
    }

    @GetMapping
    public List<AccountSummary> search(@RequestParam(required = false) String maCoSo,
            @RequestParam(required = false) Role loaiNguoiDung) {
        return accounts.search(maCoSo, loaiNguoiDung);
    }

    /**
     * Cấp lại mã khi người dùng làm mất hoặc mã hết hạn. Mã cũ bị thu hồi.
     * {@code guiEmail=false}: Admin nhận mã trao tay dù tài khoản có email —
     * dùng khi thư không tới được.
     */
    @PostMapping("/{tenDangNhap}/activation-code")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivationCode reissueActivationCode(@PathVariable String tenDangNhap,
            @RequestParam(defaultValue = "true") boolean guiEmail) {
        return atAccountSite(tenDangNhap, () -> accounts.reissueActivationCode(tenDangNhap, guiEmail));
    }

    @PutMapping("/{tenDangNhap}/status")
    public AccountSummary changeStatus(@PathVariable String tenDangNhap,
            @Valid @RequestBody ChangeAccountStatusRequest body) {
        AccountSummary ketQua = accounts.changeStatus(tenDangNhap, body.trangThai());
        LoginSiteResolver resolver = siteResolver.getIfAvailable();
        if (resolver != null && AccountService.NGUNG.equals(ketQua.trangThai())) {
            /* Phần 2: phiên SV/GV nằm ở cơ sở nhà, Master không thu hồi được. Không
               chờ nhân bản danh bạ — Distribution Agent dừng thì người bị khoá vẫn
               dùng được mãi. Bước riêng, giao dịch riêng (không trải hai nút). */
            String site = resolver.resolve(tenDangNhap);
            if (!SiteContext.MASTER.equals(site)) {
                try {
                    SiteContext.runAt(site, () -> sessions.revokeAllForAccountChange(tenDangNhap));
                } catch (RuntimeException ex) {
                    // Master đã khoá; cơ sở bật lại thì danh bạ nhân bản chặn tiếp.
                    log.warn("Khoá {}: chưa thu hồi được phiên ở cơ sở {} — {}", tenDangNhap, site, ex.toString());
                }
            }
        }
        return ketQua;
    }

    /**
     * Đặt email mới cho người dùng mất quyền vào hòm thư cũ.
     *
     * <p>Tách khỏi cấp lại mật khẩu CỐ Ý: hai việc để lại hai dấu vết khác
     * nhau, nên nhật ký phân biệt được "sửa email gõ nhầm" với "chiếm tài
     * khoản". Cấp lại mã mà không phải đụng email lần nữa cũng nhờ vậy.
     *
     * <p>Email mới **chưa xác minh**; nó tự thành đã xác minh khi chủ tài
     * khoản dùng được mã gửi tới đó.
     */
    @PutMapping("/{tenDangNhap}/email")
    public AccountEmail changeEmail(@PathVariable String tenDangNhap,
            @Valid @RequestBody AdminChangeEmailRequest body) {
        return atAccountSite(tenDangNhap, () -> credentials.adminChangeEmail(tenDangNhap, body.email()));
    }

    /**
     * Cấp lại mật khẩu: thu hồi mọi phiên, xoá mật khẩu, cấp mã dùng một lần.
     *
     * <p>Admin **không** đặt mật khẩu hộ — chủ tài khoản tự đặt ở màn kích
     * hoạt bằng mã này. Có email thì mã CHỈ đi qua thư và Admin không thấy;
     * không có email (hoặc chưa bật gửi thư) thì mã hiện một lần để trao tay.
     *
     * <p>Sau thao tác này tài khoản **không đăng nhập được** cho tới khi đặt
     * mật khẩu mới — đó là ý nghĩa của "cấp lại", không phải tác dụng phụ.
     */
    @PostMapping("/{tenDangNhap}/password-reset")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivationCode forcePasswordReset(@PathVariable String tenDangNhap) {
        return atAccountSite(tenDangNhap, () -> accounts.forcePasswordReset(tenDangNhap));
    }
}
