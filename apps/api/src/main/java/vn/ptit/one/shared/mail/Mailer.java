package vn.ptit.one.shared.mail;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Gửi thư văn bản thuần qua Brevo: API HTTP nếu có API key, không thì SMTP relay.
 *
 * <p>Không chặn luồng HTTP của người dùng: thư đi trên một luồng nền, và nếu
 * đang trong giao dịch thì chỉ đi SAU KHI commit — rollback thì không có thư nào
 * mang mã không tồn tại. Gửi hỏng chỉ ghi log; người dùng xin mã lại.
 */
@Component
@EnableConfigurationProperties(MailProperties.class)
public class Mailer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final MailProperties properties;
    private final RestClient api;
    private final JavaMailSenderImpl smtp;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ptitone-mail");
        thread.setDaemon(true);
        return thread;
    });

    public Mailer(MailProperties properties) {
        this.properties = properties;
        boolean enabled = properties.enabled();
        this.api = enabled && properties.useApi() ? buildApi(properties) : null;
        this.smtp = enabled && !properties.useApi() ? buildSmtp(properties) : null;
        if (enabled) {
            log.info("Gửi thư bật qua {}, người gửi {}", properties.useApi() ? "Brevo API" : "SMTP " + properties.host(),
                    mask(properties.from()));
        } else {
            log.info("Gửi thư TẮT: thiếu PTITONE_MAIL_FROM hoặc cả PTITONE_BREVO_API_KEY lẫn PTITONE_MAIL_HOST.");
        }
    }

    public boolean enabled() {
        return api != null || smtp != null;
    }

    /** Gửi sau commit nếu đang trong giao dịch, ngay lập tức nếu không. */
    public void sendAfterCommit(String to, String subject, String text) {
        if (!enabled()) {
            throw new IllegalStateException("Chưa cấu hình gửi thư.");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    worker.execute(() -> send(to, subject, text));
                }
            });
        } else {
            worker.execute(() -> send(to, subject, text));
        }
    }

    private void send(String to, String subject, String text) {
        try {
            if (api != null) {
                sendViaApi(to, subject, text);
            } else {
                sendViaSmtp(to, subject, text);
            }
            log.info("Đã gửi thư \"{}\" tới {}", subject, mask(to));
        } catch (RestClientResponseException ex) {
            // Brevo trả lý do trong thân, ví dụ {"code":"unauthorized","message":"Key not found"}.
            log.warn("Gửi thư \"{}\" tới {} thất bại: Brevo {} {}", subject, mask(to), ex.getStatusCode().value(),
                    ex.getResponseBodyAsString());
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException ex) {
            // Không ghi nội dung thư: trong đó có mã.
            log.warn("Gửi thư \"{}\" tới {} thất bại: {}", subject, mask(to), ex.toString());
        }
    }

    /** POST /v3/smtp/email — "smtp" là tên API giao dịch của Brevo, không phải giao thức SMTP. */
    private void sendViaApi(String to, String subject, String text) {
        api.post()
                .body(Map.of(
                        "sender", Map.of("name", properties.fromName(), "email", properties.from().trim()),
                        "to", List.of(Map.of("email", to)),
                        "subject", subject,
                        "textContent", text))
                .retrieve()
                .toBodilessEntity();
    }

    private void sendViaSmtp(String to, String subject, String text)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = smtp.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
        helper.setFrom(properties.from().trim(), properties.fromName());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, false);
        smtp.send(message);
    }

    private static RestClient buildApi(MailProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.brevoApiUrl())
                .defaultHeader("api-key", properties.brevoApiKey().trim())
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static JavaMailSenderImpl buildSmtp(MailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.host().trim());
        sender.setPort(properties.port());
        sender.setUsername(properties.username());
        sender.setPassword(properties.password());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties smtp = sender.getJavaMailProperties();
        smtp.put("mail.smtp.auth", String.valueOf(properties.username() != null && !properties.username().isBlank()));
        smtp.put("mail.smtp.starttls.enable", "true");
        smtp.put("mail.smtp.starttls.required", "true");
        smtp.put("mail.smtp.connectiontimeout", String.valueOf(TIMEOUT.toMillis()));
        smtp.put("mail.smtp.timeout", String.valueOf(TIMEOUT.toMillis()));
        smtp.put("mail.smtp.writetimeout", String.valueOf(TIMEOUT.toMillis()));
        return sender;
    }

    /** {@code an@gmail.com} → {@code a***@gmail.com}: đủ để tra log, không lộ địa chỉ. */
    static String mask(String email) {
        if (email == null) {
            return "(trống)";
        }
        int at = email.indexOf('@');
        return at <= 1 ? "***" + email.substring(Math.max(at, 0)) : email.charAt(0) + "***" + email.substring(at);
    }

    @Override
    public void destroy() {
        worker.shutdown();
    }
}
