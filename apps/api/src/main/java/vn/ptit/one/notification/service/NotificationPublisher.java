package vn.ptit.one.notification.service;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import vn.ptit.one.notification.model.AutoNotification;
import vn.ptit.one.notification.model.Recipient;
import vn.ptit.one.notification.repository.NotificationRepository;

/**
 * Phát thông báo tự sinh. API công khai cho {@code enrollment} và {@code grade}.
 *
 * <p>KHÔNG mở giao dịch riêng: chạy trong giao dịch nghiệp vụ của chỗ gọi, nên
 * đăng ký rollback thì thông báo "đăng ký thành công" cũng biến mất theo — không
 * bao giờ báo thành công cho một việc chưa xảy ra. Ở Phần 1 chỉ có một DB nên
 * không cần Outbox.
 */
@Service
@Profile("central")
public class NotificationPublisher {

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationPublisher(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    /** Phát lại cùng {@code khoaSuKien} thì không làm gì. Không người nhận thì không ghi. */
    public void publish(AutoNotification event) {
        List<Recipient> nguoiNhan = List.copyOf(new LinkedHashSet<>(event.nguoiNhan()));
        if (nguoiNhan.isEmpty()) {
            return;
        }
        UUID id = UUID.randomUUID();
        if (notifications.insertAuto(id, event, clock.instant()) == 1) {
            notifications.insertRecipients(id, nguoiNhan);
        }
    }
}
