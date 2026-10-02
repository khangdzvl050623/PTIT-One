package vn.ptit.one.notification.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.notification.model.Inbox;
import vn.ptit.one.notification.model.NotificationTerms;
import vn.ptit.one.notification.repository.NotificationRepository;
import vn.ptit.one.shared.exception.ApiException;

/**
 * Hộp thư của sinh viên và giảng viên. Người nhận lấy từ principal — không có
 * tham số nào để đọc hộp thư của người khác.
 *
 * <p>Mở hộp thư KHÔNG tự đánh dấu đã đọc; chỉ đọc từng cái hoặc "đánh dấu tất cả".
 */
@Service
@Profile("central")
public class InboxService {

    public static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notifications;
    private final Clock clock;

    public InboxService(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    public Inbox inbox(AuthenticatedUser user, boolean chiChuaDoc, int trang, int kichThuoc) {
        Owner me = owner(user);
        if (trang < 0 || kichThuoc < 1 || kichThuoc > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Trang bắt đầu từ 0, kích thước từ 1 đến %d.".formatted(MAX_PAGE_SIZE));
        }
        return new Inbox(notifications.unreadCount(me.loai(), me.ma()), trang, kichThuoc,
                notifications.inbox(me.loai(), me.ma(), chiChuaDoc, trang * kichThuoc, kichThuoc));
    }

    public int unreadCount(AuthenticatedUser user) {
        Owner me = owner(user);
        return notifications.unreadCount(me.loai(), me.ma());
    }

    /** Đọc lại thông báo đã đọc là không làm gì; thông báo không phải của mình là 404. */
    @Transactional
    public int markRead(AuthenticatedUser user, UUID id) {
        Owner me = owner(user);
        if (notifications.markRead(me.loai(), me.ma(), id, clock.instant()) == 0
                && !notifications.isRecipient(me.loai(), me.ma(), id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo.");
        }
        return notifications.unreadCount(me.loai(), me.ma());
    }

    @Transactional
    public int markAllRead(AuthenticatedUser user) {
        Owner me = owner(user);
        return notifications.markAllRead(me.loai(), me.ma(), clock.instant());
    }

    private static Owner owner(AuthenticatedUser user) {
        String loai = switch (user.role()) {
            case SINH_VIEN -> NotificationTerms.SINH_VIEN;
            case GIANG_VIEN -> NotificationTerms.GIANG_VIEN;
            default -> throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Hộp thư thông báo dành cho sinh viên và giảng viên.");
        };
        if (user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "Tài khoản chưa gắn hồ sơ.");
        }
        return new Owner(loai, user.entityId());
    }

    private record Owner(String loai, String ma) {
    }
}
