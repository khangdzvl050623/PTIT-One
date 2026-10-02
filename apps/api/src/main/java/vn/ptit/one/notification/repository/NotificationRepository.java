package vn.ptit.one.notification.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.notification.model.AutoNotification;
import vn.ptit.one.notification.model.InboxItem;
import vn.ptit.one.notification.model.NotificationTerms;
import vn.ptit.one.notification.model.Recipient;
import vn.ptit.one.notification.model.RecipientCount;

/** Module `notification` sở hữu {@code ThongBao} và {@code ThongBaoNguoiNhan}. */
@Repository
@Profile("central")
public class NotificationRepository {

    private static final String AUTHORED_COLUMNS = """
            SELECT MaThongBao, TrangThai, MucDo, TieuDe, NoiDung, LienKet, PhamVi, MaCoSo,
                   MaLopHP, DoiTuong, NguoiTao, NgayTao, NgayGui
              FROM dbo.ThongBao
            """;

    private final JdbcTemplate jdbc;

    public NotificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- Tự sinh ----------------------------------------------------------

    /**
     * Ghi thông báo tự sinh nếu khoá sự kiện chưa có. Điều kiện nằm trong câu
     * INSERT nên không cần bắt lỗi trùng khoá giữa giao dịch nghiệp vụ.
     *
     * @return 0 nếu sự kiện này đã có thông báo
     */
    public int insertAuto(UUID id, AutoNotification event, Instant now) {
        LocalDateTime ngay = toDb(now);
        return jdbc.update("""
                INSERT INTO dbo.ThongBao (MaThongBao, Loai, TrangThai, MucDo, TieuDe, NoiDung, LienKet,
                                          PhamVi, MaLopHP, DoiTuong, SuKien, KhoaSuKien, NgayTao, NgayGui)
                SELECT ?, 'TU_DONG', 'DA_GUI', ?, ?, ?, ?, ?, ?, 'SINH_VIEN', ?, ?, ?, ?
                 WHERE NOT EXISTS (SELECT 1 FROM dbo.ThongBao WHERE KhoaSuKien = ?)
                """, id.toString(), event.mucDo(), event.tieuDe(), event.noiDung(), event.lienKet(),
                event.nguoiNhan().size() == 1 ? NotificationTerms.CA_NHAN : NotificationTerms.LOP_HOC_PHAN,
                event.maLopHP(), event.suKien(), event.khoaSuKien(), ngay, ngay, event.khoaSuKien());
    }

    // --- Soạn tay ---------------------------------------------------------

    public void insertDraft(UUID id, AuthoredRow row, String vaiTro, Instant now) {
        jdbc.update("""
                INSERT INTO dbo.ThongBao (MaThongBao, Loai, TrangThai, MucDo, TieuDe, NoiDung, LienKet,
                                          PhamVi, MaCoSo, MaLopHP, DoiTuong, NguoiTao, VaiTroNguoiTao, NgayTao)
                VALUES (?, 'SOAN', 'NHAP', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id.toString(), row.mucDo(), row.tieuDe(), row.noiDung(), row.lienKet(), row.phamVi(),
                row.maCoSo(), row.maLopHP(), row.doiTuong(), row.nguoiTao(), vaiTro, toDb(now));
    }

    /** @return 0 nếu không còn là nháp của người này */
    public int updateDraft(UUID id, AuthoredRow row) {
        return jdbc.update("""
                UPDATE dbo.ThongBao
                   SET MucDo = ?, TieuDe = ?, NoiDung = ?, LienKet = ?, PhamVi = ?, MaCoSo = ?,
                       MaLopHP = ?, DoiTuong = ?
                 WHERE MaThongBao = ? AND NguoiTao = ? AND TrangThai = 'NHAP'
                """, row.mucDo(), row.tieuDe(), row.noiDung(), row.lienKet(), row.phamVi(), row.maCoSo(),
                row.maLopHP(), row.doiTuong(), id.toString(), row.nguoiTao());
    }

    public int deleteDraft(UUID id, String nguoiTao) {
        return jdbc.update("""
                DELETE FROM dbo.ThongBao WHERE MaThongBao = ? AND NguoiTao = ? AND TrangThai = 'NHAP'
                """, id.toString(), nguoiTao);
    }

    /** Chuyển nháp sang đã gửi; điều kiện trong câu UPDATE chặn gửi hai lần. */
    public int markSent(UUID id, Instant now) {
        return jdbc.update("""
                UPDATE dbo.ThongBao SET TrangThai = 'DA_GUI', NgayGui = ?
                 WHERE MaThongBao = ? AND TrangThai = 'NHAP'
                """, toDb(now), id.toString());
    }

    public Optional<AuthoredRow> findAuthored(UUID id) {
        return jdbc.query(AUTHORED_COLUMNS + " WHERE MaThongBao = ? AND Loai = 'SOAN'",
                (rs, rowNum) -> mapAuthored(rs), id.toString()).stream().findFirst();
    }

    public List<AuthoredRow> findByAuthor(String nguoiTao) {
        return jdbc.query(AUTHORED_COLUMNS + " WHERE NguoiTao = ? ORDER BY NgayTao DESC",
                (rs, rowNum) -> mapAuthored(rs), nguoiTao);
    }

    public boolean campusExists(String maCoSo) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.CoSo WHERE MaCoSo = ?",
                Integer.class, maCoSo);
        return count != null && count > 0;
    }

    // --- Người nhận -------------------------------------------------------

    public void insertRecipients(UUID id, List<Recipient> recipients) {
        jdbc.batchUpdate("""
                INSERT INTO dbo.ThongBaoNguoiNhan (LoaiNguoiNhan, MaNguoiNhan, MaThongBao)
                VALUES (?, ?, ?)
                """, recipients, 500, (ps, r) -> {
                    ps.setString(1, r.loai());
                    ps.setString(2, r.ma());
                    ps.setString(3, id.toString());
                });
    }

    public RecipientCount recipientCount(UUID id) {
        return jdbc.queryForObject("""
                SELECT SUM(CASE WHEN LoaiNguoiNhan = 'SINH_VIEN' THEN 1 ELSE 0 END) AS SoSinhVien,
                       SUM(CASE WHEN LoaiNguoiNhan = 'GIANG_VIEN' THEN 1 ELSE 0 END) AS SoGiangVien
                  FROM dbo.ThongBaoNguoiNhan WHERE MaThongBao = ?
                """, (rs, rowNum) -> new RecipientCount(rs.getInt("SoSinhVien"), rs.getInt("SoGiangVien")), id.toString());
    }

    public int readCount(UUID id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.ThongBaoNguoiNhan WHERE MaThongBao = ? AND NgayDoc IS NOT NULL",
                Integer.class, id.toString());
        return count == null ? 0 : count;
    }

    // --- Hộp thư ----------------------------------------------------------

    public List<InboxItem> inbox(String loai, String ma, boolean chiChuaDoc, int offset, int size) {
        List<Object> args = new ArrayList<>(List.of(loai, ma));
        String unreadOnly = chiChuaDoc ? " AND n.NgayDoc IS NULL" : "";
        args.add(offset);
        args.add(size);
        return jdbc.query("""
                SELECT t.MaThongBao, t.Loai, t.SuKien, t.MucDo, t.TieuDe, t.NoiDung, t.LienKet,
                       t.VaiTroNguoiTao, t.NgayGui, n.NgayDoc
                  FROM dbo.ThongBaoNguoiNhan n
                  JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao
                 WHERE n.LoaiNguoiNhan = ? AND n.MaNguoiNhan = ?%s
                 ORDER BY t.NgayGui DESC, t.MaThongBao
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """.formatted(unreadOnly), (rs, rowNum) -> {
                    Instant ngayDoc = toInstant(rs.getObject("NgayDoc", LocalDateTime.class));
                    return new InboxItem(
                            UUID.fromString(rs.getString("MaThongBao")),
                            rs.getString("Loai"),
                            rs.getString("SuKien"),
                            rs.getString("MucDo"),
                            rs.getString("TieuDe"),
                            rs.getString("NoiDung"),
                            rs.getString("LienKet"),
                            rs.getString("VaiTroNguoiTao"),
                            toInstant(rs.getObject("NgayGui", LocalDateTime.class)),
                            ngayDoc != null,
                            ngayDoc);
                }, args.toArray());
    }

    public int unreadCount(String loai, String ma) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.ThongBaoNguoiNhan
                 WHERE LoaiNguoiNhan = ? AND MaNguoiNhan = ? AND NgayDoc IS NULL
                """, Integer.class, loai, ma);
        return count == null ? 0 : count;
    }

    public boolean isRecipient(String loai, String ma, UUID id) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.ThongBaoNguoiNhan
                 WHERE LoaiNguoiNhan = ? AND MaNguoiNhan = ? AND MaThongBao = ?
                """, Integer.class, loai, ma, id.toString());
        return count != null && count > 0;
    }

    /** Chỉ ghi thời điểm đọc ĐẦU TIÊN; đọc lại không đổi gì. */
    public int markRead(String loai, String ma, UUID id, Instant now) {
        return jdbc.update("""
                UPDATE dbo.ThongBaoNguoiNhan SET NgayDoc = ?
                 WHERE LoaiNguoiNhan = ? AND MaNguoiNhan = ? AND MaThongBao = ? AND NgayDoc IS NULL
                """, toDb(now), loai, ma, id.toString());
    }

    public int markAllRead(String loai, String ma, Instant now) {
        return jdbc.update("""
                UPDATE dbo.ThongBaoNguoiNhan SET NgayDoc = ?
                 WHERE LoaiNguoiNhan = ? AND MaNguoiNhan = ? AND NgayDoc IS NULL
                """, toDb(now), loai, ma);
    }

    // --- Ánh xạ -----------------------------------------------------------

    private static AuthoredRow mapAuthored(ResultSet rs) throws SQLException {
        return new AuthoredRow(
                UUID.fromString(rs.getString("MaThongBao")),
                rs.getString("TrangThai"),
                rs.getString("MucDo"),
                rs.getString("TieuDe"),
                rs.getString("NoiDung"),
                rs.getString("LienKet"),
                rs.getString("PhamVi"),
                rs.getString("MaCoSo"),
                rs.getString("MaLopHP"),
                rs.getString("DoiTuong"),
                rs.getString("NguoiTao"),
                toInstant(rs.getObject("NgayTao", LocalDateTime.class)),
                toInstant(rs.getObject("NgayGui", LocalDateTime.class)));
    }

    private static LocalDateTime toDb(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }

    /** Thông báo soạn tay, thô từ DB. {@code maThongBao}/{@code trangThai}/ngày bỏ qua khi ghi. */
    public record AuthoredRow(
            UUID maThongBao,
            String trangThai,
            String mucDo,
            String tieuDe,
            String noiDung,
            String lienKet,
            String phamVi,
            String maCoSo,
            String maLopHP,
            String doiTuong,
            String nguoiTao,
            Instant ngayTao,
            Instant ngayGui) {
    }
}
