package vn.ptit.one.enrollment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import vn.ptit.one.enrollment.repository.OutboxRepository;
import vn.ptit.one.enrollment.repository.OutboxRepository.GradeEvent;
import vn.ptit.one.enrollment.repository.OutboxRepository.PendingEvent;
import vn.ptit.one.grade.service.GradeRecords;
import vn.ptit.one.shared.routing.SiteContext;
import vn.ptit.one.shared.routing.SiteProperties;

/**
 * Đẩy điểm liên cơ sở từ hộp thư đi của mỗi cơ sở về cơ sở nhà của sinh viên.
 *
 * <p>Class cụ thể, không interface — chỉ có một cách chạy worker này.
 *
 * <p><b>Thứ tự hai bước là điều làm cho nó đúng:</b> upsert mirror TRƯỚC, đánh
 * dấu {@code SENT} SAU. Chết giữa hai bước thì lần sau gửi lại, upsert
 * idempotent nên vô hại. Đảo thứ tự là mất sự kiện vĩnh viễn.
 *
 * <p>Không {@code @Transactional}: mỗi bước chạm một site khác nhau, và một
 * giao dịch không bao giờ trải hai site.
 */
@Service
@Profile("distributed")
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final int BATCH = 50;
    /** Thôi thử lại sau ngần này lần; để lại PENDING cho người xem. */
    private static final int MAX_RETRY = 10;

    private final OutboxRepository outbox;
    private final GradeRecords grades;
    private final SiteProperties sites;
    private final ObjectMapper json;
    private final Clock clock;

    public OutboxWorker(OutboxRepository outbox, GradeRecords grades, SiteProperties sites,
            ObjectMapper json, Clock clock) {
        this.outbox = outbox;
        this.grades = grades;
        this.sites = sites;
        this.json = json;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${ptitone.outbox.interval-ms:10000}")
    public void drain() {
        for (String site : sites.nodes().keySet()) {
            try {
                drainSite(site);
            } catch (RuntimeException ex) {
                /* Một cơ sở tắt máy không được chặn các cơ sở còn lại. */
                log.warn("Outbox {}: {}", site, ex.toString());
            }
        }
    }

    private void drainSite(String site) {
        List<PendingEvent> cho = SiteContext.runAt(site, () -> outbox.pending(BATCH));
        for (PendingEvent e : cho) {
            GradeEvent g = parse(e);
            if (g == null || e.soLanThu() >= MAX_RETRY) {
                SiteContext.runAt(site, () -> outbox.recordFailure(e.eventId()));
                continue;
            }
            LocalDateTime now = OutboxRepository.utc(clock.instant());
            boolean ok;
            try {
                // 1. Ghi mirror tại cơ sở NHÀ — LÀM TRƯỚC.
                ok = SiteContext.runAt(g.maCoSoNhaSV(), () -> grades.mirrorGrade(
                        g.maSinhVien(), g.maLopHP(), g.diemTongKet(), g.version(), now));
            } catch (RuntimeException ex) {
                log.warn("Outbox {} → {}: {}", site, g.maCoSoNhaSV(), ex.toString());
                SiteContext.runAt(site, () -> outbox.recordFailure(e.eventId()));
                continue;
            }
            if (ok) {
                // 2. Đánh dấu đã gửi — LÀM SAU.
                SiteContext.runAt(site, () -> outbox.markSent(e.eventId(), now));
            } else {
                SiteContext.runAt(site, () -> outbox.recordFailure(e.eventId()));
            }
        }
    }

    private GradeEvent parse(PendingEvent e) {
        try {
            return json.readValue(e.noiDung(), GradeEvent.class);
        } catch (JacksonException ex) {
            log.warn("Outbox {}: nội dung không đọc được, bỏ qua", e.eventId());
            return null;
        }
    }
}
