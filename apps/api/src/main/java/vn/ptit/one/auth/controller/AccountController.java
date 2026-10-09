package vn.ptit.one.auth.controller;

import java.util.List;

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
import vn.ptit.one.auth.service.AccountService;
import vn.ptit.one.auth.service.CredentialService;

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

    private final AccountService accounts;
    private final CredentialService credentials;

    public AccountController(AccountService accounts, CredentialService credentials) {
        this.accounts = accounts;
        this.credentials = credentials;
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
        return accounts.reissueActivationCode(tenDangNhap, guiEmail);
    }

    @PutMapping("/{tenDangNhap}/status")
    public AccountSummary changeStatus(@PathVariable String tenDangNhap,
            @Valid @RequestBody ChangeAccountStatusRequest body) {
        return accounts.changeStatus(tenDangNhap, body.trangThai());
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
        return credentials.adminChangeEmail(tenDangNhap, body.email());
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
        return accounts.forcePasswordReset(tenDangNhap);
    }
}
