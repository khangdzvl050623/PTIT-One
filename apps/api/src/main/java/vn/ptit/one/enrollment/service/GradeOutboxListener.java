package vn.ptit.one.enrollment.service;

import java.time.Clock;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.enrollment.repository.OutboxRepository;
import vn.ptit.one.grade.model.GradesPublished;

/**
 * Lớp công bố điểm xong thì ghi sự kiện Outbox cho các sinh viên KHÁCH.
 *
 * <p>{@code @EventListener} chạy đồng bộ, nên dòng sự kiện nằm trong CÙNG giao
 * dịch với câu công bố — đúng điều kiện của Outbox. Gửi hỏng không bao giờ làm
 * mất sự kiện, vì sự kiện đã commit cùng dữ liệu.
 *
 * <p>⚠️ Chỉ ở profile {@code distributed}. {@code OutboxSuKien} là bảng của
 * SITE, không có ở CENTRAL — để nó chạy ở Phần 1 thì mỗi lần công bố điểm đều
 * hỏng với "Invalid object name" và trả 503. Phần 1 cũng không có cơ sở nào để
 * đẩy điểm về: một hộp thư đi không có worker là hộp thư chết.
 */
@Component
@Profile("distributed")
public class GradeOutboxListener {

    private final OutboxRepository outbox;
    private final ClassSectionService classes;
    private final Clock clock;

    public GradeOutboxListener(OutboxRepository outbox, ClassSectionService classes, Clock clock) {
        this.outbox = outbox;
        this.classes = classes;
        this.clock = clock;
    }

    @EventListener
    public void onGradesPublished(GradesPublished event) {
        ClassSection lop = classes.require(event.maLopHP());
        outbox.enqueueGrades(event.maLopHP(), lop.maCoSoHost(),
                OutboxRepository.utc(event.thoiDiem() == null ? clock.instant() : event.thoiDiem()));
    }
}
