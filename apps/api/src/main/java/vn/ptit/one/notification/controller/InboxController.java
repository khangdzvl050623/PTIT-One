package vn.ptit.one.notification.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.notification.model.Inbox;
import vn.ptit.one.notification.service.InboxService;

/** Hộp thư thông báo của sinh viên/giảng viên đang đăng nhập. */
@RestController
@RequestMapping("/api/me/notifications")
@Profile("central")
public class InboxController {

    private final InboxService inbox;

    public InboxController(InboxService inbox) {
        this.inbox = inbox;
    }

    @GetMapping
    public Inbox list(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "false") boolean chuaDoc,
            @RequestParam(defaultValue = "0") int trang,
            @RequestParam(defaultValue = "20") int kichThuoc) {
        return inbox.inbox(user, chuaDoc, trang, kichThuoc);
    }

    /** Cho chuông: rẻ hơn tải cả trang. */
    @GetMapping("/unread-count")
    public Map<String, Integer> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("soChuaDoc", inbox.unreadCount(user));
    }

    @PostMapping("/{maThongBao}/read")
    public Map<String, Integer> markRead(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID maThongBao) {
        return Map.of("soChuaDoc", inbox.markRead(user, maThongBao));
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("soDaDanhDau", inbox.markAllRead(user), "soChuaDoc", 0);
    }
}
