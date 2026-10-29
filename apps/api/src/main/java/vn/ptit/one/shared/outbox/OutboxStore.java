package vn.ptit.one.shared.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Phần kỹ thuật của {@code OutboxSuKien}: ghi, đọc sự kiện chờ gửi, đánh dấu.
 * Bảng có ở mọi nút — mỗi cơ sở (điểm) và Master (vòng đời người dùng).
 * Nội dung sự kiện là việc của module phát sinh nó.
 */
@Repository
@Profile("central")
public class OutboxStore {

    private static final String PENDING = "PENDING";
    private static final String SENT = "SENT";

    private final JdbcTemplate jdbc;

    public OutboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Gọi TRONG giao dịch ghi dữ liệu — đó là toàn bộ ý nghĩa của Outbox. */
    public void enqueue(UUID eventId, String loai, String khoaThucThe, String noiDung, long version,
            LocalDateTime now) {
        jdbc.update("""
                INSERT INTO dbo.OutboxSuKien (EventId, LoaiSuKien, KhoaThucThe, NoiDung, Version, ThoiDiemTao)
                VALUES (?, ?, ?, ?, ?, ?)
                """, eventId.toString(), loai, khoaThucThe, noiDung, version, now);
    }

    /**
     * Sự kiện còn chờ gửi: ít lần thử trước, rồi cũ trước — sự kiện hỏng mãi
     * không chiếm hết lô và chặn sự kiện mới.
     */
    public List<PendingEvent> pending(String loai, int limit) {
        return jdbc.query("""
                SELECT TOP (?) EventId, KhoaThucThe, NoiDung, Version, SoLanThu
                  FROM dbo.OutboxSuKien
                 WHERE TrangThai = ? AND LoaiSuKien = ?
                 ORDER BY SoLanThu, ThoiDiemTao
                """, (rs, rowNum) -> new PendingEvent(
                        UUID.fromString(rs.getString("EventId")),
                        rs.getString("KhoaThucThe"),
                        rs.getString("NoiDung"),
                        rs.getLong("Version"),
                        rs.getInt("SoLanThu")),
                limit, PENDING, loai);
    }

    /** Đánh dấu đã gửi. Làm SAU khi nơi nhận ghi xong — đảo thứ tự là mất sự kiện. */
    public int markSent(UUID eventId, LocalDateTime now) {
        return jdbc.update("""
                UPDATE dbo.OutboxSuKien SET TrangThai = ?, ThoiDiemXuLy = ?
                 WHERE EventId = ? AND TrangThai = ?
                """, SENT, now, eventId.toString(), PENDING);
    }

    public void recordFailure(UUID eventId) {
        jdbc.update("UPDATE dbo.OutboxSuKien SET SoLanThu = SoLanThu + 1 WHERE EventId = ?",
                eventId.toString());
    }

    /** @param noiDung JSON thô; module xử lý tự đọc ra. */
    public record PendingEvent(UUID eventId, String khoaThucThe, String noiDung, long version,
            int soLanThu) {
    }
}
