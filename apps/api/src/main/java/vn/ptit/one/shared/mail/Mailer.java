package vn.ptit.one.shared.mail;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Gửi thư văn bản thuần qua SMTP.
 *
 * <p>Không chặn luồng HTTP chờ SMTP: thư đi trên một luồng nền, và nếu đang
 * trong giao dịch thì chỉ đi SAU KHI commit — rollback thì không có thư nào mang
 * mã không tồn tại. Gửi hỏng chỉ ghi log; người dùng xin mã lại.
 */
@Component
@EnableConfigurationProperties(MailProperties.class)
public class Mailer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);

    private final MailProperties properties;
    private final JavaMailSenderImpl sender;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ptitone-mail");
        thread.setDaemon(true);
        return thread;
    });

    public Mailer(MailProperties properties) {
        this.properties = properties;
        this.sender = properties.enabled() ? build(properties) : null;
    }

    public boolean enabled() {
        return sender != null;
    }

    /** Gửi sau commit nếu đang trong giao dịch, ngay lập tức nếu không. */
    public void sendAfterCommit(String to, String subject, String text) {
        if (!enabled()) {
            throw new IllegalStateException("Chưa cấu hình gửi thư (PTITONE_MAIL_HOST/PTITONE_MAIL_FROM).");
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
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from(), properties.fromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            sender.send(message);
            log.info("Đã gửi thư \"{}\" tới {}", subject, mask(to));
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException ex) {
            // Không ghi nội dung thư: trong đó có mã.
            log.warn("Gửi thư \"{}\" tới {} thất bại: {}", subject, mask(to), ex.toString());
        }
    }

    private static JavaMailSenderImpl build(MailProperties properties) {
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
        smtp.put("mail.smtp.connectiontimeout", "10000");
        smtp.put("mail.smtp.timeout", "10000");
        smtp.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    /** {@code an@gmail.com} → {@code a***@gmail.com}: đủ để tra log, không lộ địa chỉ. */
    static String mask(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" + email.substring(Math.max(at, 0)) : email.charAt(0) + "***" + email.substring(at);
    }

    @Override
    public void destroy() {
        worker.shutdown();
    }
}
