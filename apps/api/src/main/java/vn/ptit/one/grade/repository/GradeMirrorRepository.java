package vn.ptit.one.grade.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@code BangDiemMirror} — read model của điểm liên cơ sở, đặt tại cơ sở NHÀ
 * của sinh viên. Hậu tố {@code Mirror} nói rõ đây không phải nguồn sự thật:
 * điểm gốc nằm ở cơ sở đã dạy.
 */
@Repository
@Profile("central")
public class GradeMirrorRepository {

    private final JdbcTemplate jdbc;

    public GradeMirrorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Upsert idempotent theo phiên bản.
     *
     * <p>{@code Version < @Version} là thứ chặn một sự kiện CŨ bị retry muộn
     * ghi đè điểm mới — bug rất khó tái hiện, nên chặn bằng lược đồ. Không
     * {@code MERGE}: {@code MERGE} của SQL Server không tự lấy khoá phù hợp.
     *
     * <p>Tên môn và số tín chỉ lấy tại CHỖ, từ {@code DangKyMonHoc} và
     * {@code MonHoc} của chính cơ sở nhà — sự kiện chỉ mang điểm.
     *
     * @return {@code false} nếu không tìm thấy dòng đăng ký môn tương ứng
     */
    public boolean upsert(String maSinhVien, String maLopHP, BigDecimal diemTongKet,
            long version, LocalDateTime now) {
        int updated = jdbc.update("""
                UPDATE dbo.BangDiemMirror
                   SET DiemTongKet = ?, Version = ?, LastSyncedAt = ?, SyncStatus = 'SYNCED'
                 WHERE MaSinhVien = ? AND MaLopHP = ? AND Version < ?
                """, diemTongKet, version, now, maSinhVien, maLopHP, version);
        if (updated > 0) {
            return true;
        }
        /* UPDATE 0 dòng mà bản ghi ĐÃ tồn tại → sự kiện cũ, bỏ qua (đúng ý). */
        Integer daCo = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.BangDiemMirror WITH (UPDLOCK, HOLDLOCK)
                 WHERE MaSinhVien = ? AND MaLopHP = ?
                """, Integer.class, maSinhVien, maLopHP);
        if (daCo != null && daCo > 0) {
            return true;
        }
        return jdbc.update("""
                INSERT INTO dbo.BangDiemMirror
                       (MaSinhVien, MaLopHP, MaMonHoc, TenMonHoc, SoTinChi, DiemTongKet,
                        Version, LastSyncedAt, SyncStatus)
                SELECT k.MaSinhVien, k.MaLopHP, k.MaMonHoc, m.TenMonHoc, k.SoTinChi, ?, ?, ?, 'SYNCED'
                  FROM dbo.DangKyMonHoc k
                  JOIN dbo.MonHoc m ON m.MaMonHoc = k.MaMonHoc
                 WHERE k.MaSinhVien = ? AND k.MaLopHP = ?
                """, diemTongKet, version, now, maSinhVien, maLopHP) == 1;
    }
}
