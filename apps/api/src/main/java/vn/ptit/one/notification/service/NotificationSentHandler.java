package vn.ptit.one.notification.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import vn.ptit.one.notification.model.NotificationSent;
import vn.ptit.one.shared.outbox.MasterOutboxHandler;
import vn.ptit.one.shared.outbox.OutboxStore;
import vn.ptit.one.shared.outbox.OutboxStore.PendingEvent;
import vn.ptit.one.shared.routing.SiteContext;
import vn.ptit.one.shared.routing.SiteProperties;

/**
 * {@code ThongBaoDaGui}: chép thông báo của Admin Master về MỌI cơ sở TRƯỚC,
 * đánh dấu {@code SENT} tại Master SAU. Một cơ sở tắt thì các cơ sở còn lại vẫn
 * nhận ngay, sự kiện thử lại cho tới khi đủ — bước chép idempotent nên cơ sở đã
 * nhận không bị ghi trùng.
 */
@Component
@Profile("distributed")
public class NotificationSentHandler implements MasterOutboxHandler {

    private final NotificationService notifications;
    private final OutboxStore outbox;
    private final SiteProperties sites;
    private final ObjectMapper json;

    public NotificationSentHandler(NotificationService notifications, OutboxStore outbox, SiteProperties sites,
            ObjectMapper json) {
        this.notifications = notifications;
        this.outbox = outbox;
        this.sites = sites;
        this.json = json;
    }

    @Override
    public String loaiSuKien() {
        return NotificationSent.LOAI;
    }

    @Override
    public void deliver(PendingEvent e) {
        NotificationSent ev = json.readValue(e.noiDung(), NotificationSent.class);
        RuntimeException loi = null;
        for (String site : sites.nodes().keySet()) {
            try {
                SiteContext.runAt(site, () -> notifications.deliverHere(ev));
            } catch (RuntimeException ex) {
                // Giao tiếp cho cơ sở sau: một cơ sở tắt không được giữ thông báo của cơ sở khác.
                loi = ex;
            }
        }
        if (loi != null) {
            throw loi;
        }
        SiteContext.runAt(SiteContext.MASTER,
                () -> outbox.markSent(e.eventId(), LocalDateTime.now(ZoneOffset.UTC)));
    }
}
