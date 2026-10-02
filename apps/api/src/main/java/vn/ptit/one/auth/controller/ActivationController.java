package vn.ptit.one.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.dto.ActivateAccountRequest;
import vn.ptit.one.auth.service.AccountActivationService;

/**
 * Kích hoạt tài khoản, gọi được khi CHƯA đăng nhập. Vẫn cần CSRF như login.
 * Thành công không tự đăng nhập — người dùng đăng nhập bằng mật khẩu vừa đặt.
 */
@RestController
@RequestMapping("/api/auth")
@Profile("central")
public class ActivationController {

    private final AccountActivationService activation;

    public ActivationController(AccountActivationService activation) {
        this.activation = activation;
    }

    @PostMapping("/activate")
    public ResponseEntity<Void> activate(@Valid @RequestBody ActivateAccountRequest body) {
        activation.activate(body.tenDangNhap(), body.maKichHoat(), body.matKhauMoi());
        return ResponseEntity.noContent().build();
    }
}
