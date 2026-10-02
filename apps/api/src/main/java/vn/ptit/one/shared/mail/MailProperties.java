package vn.ptit.one.shared.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Gửi thư qua Brevo. Giá trị thật ở {@code apps/api/.env}, không vào Git.
 *
 * <p>Hai đường, ưu tiên API: có {@code brevoApiKey} ({@code xkeysib-...}) thì
 * gọi API HTTP của Brevo; không thì dùng SMTP relay với {@code host} và SMTP key
 * ({@code xsmtpsib-...}). Hai loại key KHÔNG dùng thay nhau được.
 *
 * @param brevoApiKey API key Brevo, mục SMTP &amp; API → API Keys
 * @param host        SMTP host; cùng {@code brevoApiKey} trống thì tắt gửi thư —
 *                    tính năng cần mail báo "chưa bật", không giả vờ đã gửi
 * @param password    SMTP key, không phải mật khẩu tài khoản Brevo
 * @param from        địa chỉ gửi, phải đã xác minh trong Brevo (Senders)
 */
@ConfigurationProperties("ptitone.mail")
public record MailProperties(
        String brevoApiKey,
        @DefaultValue("https://api.brevo.com/v3/smtp/email") String brevoApiUrl,
        String host,
        @DefaultValue("587") int port,
        String username,
        String password,
        String from,
        @DefaultValue("PTIT One") String fromName) {

    public boolean useApi() {
        return hasText(brevoApiKey);
    }

    public boolean enabled() {
        return hasText(from) && (useApi() || hasText(host));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
