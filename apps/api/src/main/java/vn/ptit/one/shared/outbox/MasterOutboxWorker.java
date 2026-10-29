package vn.ptit.one.shared.outbox;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import vn.ptit.one.shared.outbox.OutboxStore.PendingEvent;
import vn.ptit.one.shared.routing.SiteContext;

/**
 * Rút hộp thư đi của Master. Không {@code @Transactional}: mỗi sự kiện chạm
 * hai nút, và một giao dịch không bao giờ trải hai nút.
 */
@Service
@Profile("distributed")
public class MasterOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(MasterOutboxWorker.class);
    /*
     * KHÔNG có trần số lần thử: 10 lần × 10 giây chỉ là ~100 giây, ngắn hơn
     * kịch bản demo tắt một cơ sở — sự kiện sẽ bị bỏ vĩnh viễn, sinh viên mắc ở
     * CHO_KICH_HOAT. SoLanThu vẫn tăng để người vận hành nhìn thấy.
     */
    private static final int BATCH = 50;

    private final OutboxStore outbox;
    private final List<MasterOutboxHandler> handlers;

    public MasterOutboxWorker(OutboxStore outbox, List<MasterOutboxHandler> handlers) {
        this.outbox = outbox;
        this.handlers = handlers;
    }

    @Scheduled(fixedDelayString = "${ptitone.outbox.interval-ms:10000}")
    public void drain() {
        for (MasterOutboxHandler h : handlers) {
            List<PendingEvent> cho;
            try {
                cho = SiteContext.runAt(SiteContext.MASTER, () -> outbox.pending(h.loaiSuKien(), BATCH));
            } catch (RuntimeException ex) {
                log.warn("Outbox MASTER: {}", ex.toString());
                return;
            }
            for (PendingEvent e : cho) {
                try {
                    h.deliver(e);
                } catch (RuntimeException ex) {
                    /* Cơ sở nhà tắt máy không được chặn sự kiện của cơ sở khác. */
                    log.warn("Outbox MASTER {} {}: {}", h.loaiSuKien(), e.khoaThucThe(), ex.toString());
                    SiteContext.runAt(SiteContext.MASTER, () -> outbox.recordFailure(e.eventId()));
                }
            }
        }
    }
}
