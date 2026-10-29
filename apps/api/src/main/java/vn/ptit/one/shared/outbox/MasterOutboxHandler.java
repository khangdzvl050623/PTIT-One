package vn.ptit.one.shared.outbox;

import vn.ptit.one.shared.outbox.OutboxStore.PendingEvent;

/**
 * Xử lý một loại sự kiện trong hộp thư đi của Master. Interface vì có nhiều
 * loại do các module khác nhau sở hữu (sinh viên, giảng viên), còn
 * {@code shared} không được import module nghiệp vụ.
 *
 * <p>{@link #deliver} tự làm hết: ghi nơi nhận TRƯỚC, rồi đánh dấu {@code SENT}
 * tại Master SAU. Ném lỗi thì worker ghi nhận lần thử hỏng và thử lại sau.
 */
public interface MasterOutboxHandler {

    String loaiSuKien();

    void deliver(PendingEvent event);
}
