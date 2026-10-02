package vn.ptit.one.shared.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * SMTP gửi thư (Brevo relay). Giá trị thật ở {@code apps/api/.env}, không vào Git.
 *
 * @param host     trống thì tắt gửi thư — tính năng cần mail báo "chưa bật",
 *                 không giả vờ đã gửi
 * @param password SMTP key của Brevo, không phải mật khẩu tài khoản Brevo
 * @param from     địa chỉ gửi, phải đã xác minh trong Brevo
 */
@ConfigurationProperties("ptitone.mail")
public record MailProperties(
        String host,
        @DefaultValue("587") int port,
        String username,
        String password,
        String from,
        @DefaultValue("PTIT One") String fromName) {

    public boolean enabled() {
        return host != null && !host.isBlank() && from != null && !from.isBlank();
    }
}
