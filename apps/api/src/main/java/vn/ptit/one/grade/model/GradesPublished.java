package vn.ptit.one.grade.model;

import java.time.Instant;

/**
 * Điểm của một lớp vừa được công bố.
 *
 * <p>Phát bằng {@code ApplicationEventPublisher} và nghe bằng
 * {@code @EventListener} — đồng bộ, trong CÙNG giao dịch với câu công bố. Đó
 * là điều kiện của Outbox: dòng sự kiện và thay đổi dữ liệu phải cùng commit
 * hoặc cùng rollback, không được là hai lần ghi rời nhau.
 *
 * <p>Dùng event thay vì gọi thẳng vì {@code enrollment} sở hữu
 * {@code DangKyHocPhan} (nơi biết sinh viên nào là khách) và đã phụ thuộc
 * {@code grade}; gọi ngược lại sẽ thành phụ thuộc vòng.
 */
public record GradesPublished(String maLopHP, Instant thoiDiem) {
}
