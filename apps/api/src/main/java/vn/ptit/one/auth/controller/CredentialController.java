package vn.ptit.one.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import vn.ptit.one.auth.dto.ChangeEmailRequest;
import vn.ptit.one.auth.dto.ChangePasswordRequest;
import vn.ptit.one.auth.dto.ForgotPasswordRequest;
import vn.ptit.one.auth.dto.ResetPasswordRequest;
import vn.ptit.one.auth.dto.VerifyEmailRequest;
import vn.ptit.one.auth.model.AccountEmail;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.security.AuthCookies;
import vn.ptit.one.auth.service.CredentialService;

/**
 * Mật khẩu và email của chính mình (A1). {@code forgot-password} và
 * {@code reset-password} gọi được khi chưa đăng nhập; các đường còn lại cần
 * phiên. Mọi đường ghi vẫn cần CSRF.
 *
 * <p>IP lấy từ kết nối TCP, không đọc {@code X-Forwarded-For}: Phần 1 không có
 * reverse proxy, tin header đó thì ai cũng tự đổi IP để lách giới hạn.
 */
@RestController
@RequestMapping("/api/auth")
@Profile("central")
public class CredentialController {

    private final CredentialService credentials;
    private final AuthCookies cookies;

    public CredentialController(CredentialService credentials, AuthCookies cookies) {
        this.credentials = credentials;
        this.cookies = cookies;
    }

    @GetMapping("/email")
    public AccountEmail email(@AuthenticationPrincipal AuthenticatedUser user) {
        return credentials.currentEmail(user);
    }

    /** Lưu email chưa xác minh và gửi mã 6 số tới chính địa chỉ đó. */
    @PutMapping("/email")
    public AccountEmail changeEmail(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangeEmailRequest body) {
        return credentials.changeEmail(user, body.email(), body.matKhauHienTai());
    }

    @PostMapping("/email/verify")
    public AccountEmail verifyEmail(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody VerifyEmailRequest body) {
        return credentials.verifyEmail(user, body.maXacThuc());
    }

    /** Thu hồi mọi phiên, kể cả phiên này: xoá cookie, người dùng đăng nhập lại. */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest body, HttpServletResponse response) {
        credentials.changePassword(user, body.matKhauHienTai(), body.matKhauMoi());
        cookies.clear(response);
        return ResponseEntity.noContent().build();
    }

    /** Luôn {@code 202} khi chưa chạm trần — không lộ tài khoản hay email nào có thật. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body,
            HttpServletRequest request) {
        credentials.forgotPassword(body.tenDangNhap(), body.email(), request.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest body,
            HttpServletRequest request) {
        credentials.resetPassword(body.tenDangNhap(), body.maXacThuc(), body.matKhauMoi(), request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
