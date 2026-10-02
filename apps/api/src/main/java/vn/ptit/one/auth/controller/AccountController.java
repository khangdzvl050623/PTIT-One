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
import vn.ptit.one.auth.dto.ChangeAccountStatusRequest;
import vn.ptit.one.auth.model.AccountSummary;
import vn.ptit.one.auth.model.ActivationCode;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.service.AccountService;

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

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AccountSummary> search(@RequestParam(required = false) String maCoSo,
            @RequestParam(required = false) Role loaiNguoiDung) {
        return accounts.search(maCoSo, loaiNguoiDung);
    }

    /** Cấp lại mã khi người dùng làm mất hoặc mã hết hạn. Mã cũ bị thu hồi. */
    @PostMapping("/{tenDangNhap}/activation-code")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivationCode reissueActivationCode(@PathVariable String tenDangNhap) {
        return accounts.reissueActivationCode(tenDangNhap);
    }

    @PutMapping("/{tenDangNhap}/status")
    public AccountSummary changeStatus(@PathVariable String tenDangNhap,
            @Valid @RequestBody ChangeAccountStatusRequest body) {
        return accounts.changeStatus(tenDangNhap, body.trangThai());
    }
}
