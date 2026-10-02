package vn.ptit.one.notification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.notification.dto.SaveNotificationRequest;
import vn.ptit.one.notification.model.AuthoredNotification;
import vn.ptit.one.notification.model.RecipientCount;
import vn.ptit.one.notification.service.NotificationService;

/** Soạn, xem trước, gửi thông báo. Mỗi người chỉ thấy bản do chính mình soạn. */
@RestController
@RequestMapping("/api/notifications")
@Profile("central")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<AuthoredNotification> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return notifications.mine(user);
    }

    /** Số người nhận nếu gửi ngay bây giờ — không lưu gì. */
    @PostMapping("/preview")
    public RecipientCount preview(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SaveNotificationRequest body) {
        return notifications.preview(user, body);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthoredNotification create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SaveNotificationRequest body) {
        return notifications.createDraft(user, body);
    }

    @GetMapping("/{maThongBao}")
    public AuthoredNotification detail(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID maThongBao) {
        return notifications.detail(user, maThongBao);
    }

    @PutMapping("/{maThongBao}")
    public AuthoredNotification update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID maThongBao, @Valid @RequestBody SaveNotificationRequest body) {
        return notifications.updateDraft(user, maThongBao, body);
    }

    @DeleteMapping("/{maThongBao}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID maThongBao) {
        notifications.deleteDraft(user, maThongBao);
    }

    @PostMapping("/{maThongBao}/send")
    public AuthoredNotification send(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID maThongBao) {
        return notifications.send(user, maThongBao);
    }
}
