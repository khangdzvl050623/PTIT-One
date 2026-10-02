package vn.ptit.one.grade.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Điểm. Module `grade` sở hữu bảng {@code Diem}. */
@Repository
@Profile("central")
public class GradeRepository {

    private final JdbcTemplate jdbc;

    /**
     * Khoá tuần tự hoá mọi thao tác ghi trên bảng điểm của MỘT lớp: lưu, công
     * bố, khoá. Không có nó, admin khoá lớp xen giữa lúc giảng viên đã kiểm
     * "lớp chưa khoá" và lúc ghi — điểm vẫn lọt vào lớp đã khoá.
     */
    public static final String LOCK_PREFIX = "PTITONE:Diem:";

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean acquireClassLock(String maLopHP, int timeoutMillis) {
        Integer result = jdbc.queryForObject("""
                DECLARE @ketQua int;
                EXEC @ketQua = sp_getapplock @Resource = ?, @LockMode = 'Exclusive',
                     @LockOwner = 'Transaction', @LockTimeout = ?;
                SELECT @ketQua;
                """, Integer.class, LOCK_PREFIX + maLopHP, timeoutMillis);
        return result != null && result >= 0;
    }

    // --- Bảng điểm của lớp (F06) -----------------------------------------

    /**
     * Mọi dòng điểm của lớp. Họ tên chỉ để hiển thị nên LEFT JOIN: thiếu hồ sơ
     * thì vẫn nhập điểm được, không mất dòng.
     */
    public List<ClassGradeRow> findByClass(String maLopHP) {
        return jdbc.query("""
                SELECT d.MaSinhVien, s.HoTen, d.DiemChuyenCan, d.DiemGiuaKy, d.DiemCuoiKy,
                       d.DiemTongKet, d.Version, d.NgayCongBo
                  FROM dbo.Diem d
                  LEFT JOIN dbo.SinhVien s ON s.MaSinhVien = d.MaSinhVien
                 WHERE d.MaLopHP = ?
                 ORDER BY d.MaSinhVien
                """, (rs, rowNum) -> new ClassGradeRow(
                        rs.getString("MaSinhVien"),
                        rs.getString("HoTen"),
                        rs.getBigDecimal("DiemChuyenCan"),
                        rs.getBigDecimal("DiemGiuaKy"),
                        rs.getBigDecimal("DiemCuoiKy"),
                        rs.getBigDecimal("DiemTongKet"),
                        rs.getLong("Version"),
                        toInstant(rs.getObject("NgayCongBo", LocalDateTime.class))), maLopHP);
    }

    /**
     * Ghi điểm có kiểm phiên bản trong chính câu UPDATE.
     *
     * @return 0 nếu dòng không tồn tại HOẶC phiên bản đã đổi; chỗ gọi phân biệt
     */
    public int updateScores(String maLopHP, String maSinhVien, BigDecimal chuyenCan,
            BigDecimal giuaKy, BigDecimal cuoiKy, BigDecimal tongKet, long expectedVersion) {
        return jdbc.update("""
                UPDATE dbo.Diem
                   SET DiemChuyenCan = ?, DiemGiuaKy = ?, DiemCuoiKy = ?, DiemTongKet = ?,
                       Version = Version + 1
                 WHERE MaLopHP = ? AND MaSinhVien = ? AND Version = ?
                """, chuyenCan, giuaKy, cuoiKy, tongKet, maLopHP, maSinhVien, expectedVersion);
    }

    public boolean exists(String maLopHP, String maSinhVien) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.Diem WHERE MaLopHP = ? AND MaSinhVien = ?",
                Integer.class, maLopHP, maSinhVien);
        return count != null && count > 0;
    }

    /** Công bố các dòng còn nháp. Dòng đã công bố giữ nguyên thời điểm công bố đầu. */
    public int publish(String maLopHP, Instant now) {
        return jdbc.update("""
                UPDATE dbo.Diem SET NgayCongBo = ?, Version = Version + 1
                 WHERE MaLopHP = ? AND NgayCongBo IS NULL
                """, LocalDateTime.ofInstant(now, ZoneOffset.UTC), maLopHP);
    }

    /**
     * Mọi dòng điểm của một sinh viên, kể cả nháp. Lọc điểm nháp là quy tắc
     * nghiệp vụ nên làm ở service, không giấu trong SQL.
     */
    public List<GradeRow> findByStudent(String maSinhVien, String maHocKy) {
        StringBuilder sql = new StringBuilder("""
                SELECT l.MaHocKy, h.TenHocKy, d.MaLopHP, l.MaMonHoc, m.TenMonHoc, m.SoTinChi,
                       d.DiemChuyenCan, d.DiemGiuaKy, d.DiemCuoiKy, d.DiemTongKet, d.NgayCongBo
                  FROM dbo.Diem d
                  JOIN dbo.LopHocPhan l ON l.MaLopHP = d.MaLopHP
                  JOIN dbo.MonHoc m     ON m.MaMonHoc = l.MaMonHoc
                  JOIN dbo.HocKy h      ON h.MaHocKy = l.MaHocKy
                 WHERE d.MaSinhVien = ?
                """);
        List<Object> args = new ArrayList<>(List.of(maSinhVien));
        if (maHocKy != null && !maHocKy.isBlank()) {
            sql.append(" AND l.MaHocKy = ?");
            args.add(maHocKy.trim());
        }
        sql.append(" ORDER BY h.NgayBatDau DESC, l.MaMonHoc");
        return jdbc.query(sql.toString(), (rs, rowNum) -> new GradeRow(
                        rs.getString("MaHocKy"),
                        rs.getString("TenHocKy"),
                        rs.getString("MaLopHP"),
                        rs.getString("MaMonHoc"),
                        rs.getString("TenMonHoc"),
                        rs.getInt("SoTinChi"),
                        rs.getBigDecimal("DiemChuyenCan"),
                        rs.getBigDecimal("DiemGiuaKy"),
                        rs.getBigDecimal("DiemCuoiKy"),
                        rs.getBigDecimal("DiemTongKet"),
                        toInstant(rs.getObject("NgayCongBo", LocalDateTime.class))), args.toArray());
    }

    // --- API cho đăng ký học phần (F08) ----------------------------------

    /**
     * Điểm tổng kết CAO NHẤT của mỗi môn sinh viên đã học, chỉ tính điểm đã công
     * bố — điểm nháp không được coi là đã đạt tiên quyết.
     */
    public Map<String, BigDecimal> bestPublishedByCourse(String maSinhVien) {
        Map<String, BigDecimal> best = new HashMap<>();
        jdbc.query("""
                SELECT l.MaMonHoc, MAX(d.DiemTongKet) AS CaoNhat
                  FROM dbo.Diem d
                  JOIN dbo.LopHocPhan l ON l.MaLopHP = d.MaLopHP
                 WHERE d.MaSinhVien = ? AND d.NgayCongBo IS NOT NULL AND d.DiemTongKet IS NOT NULL
                 GROUP BY l.MaMonHoc
                """, rs -> {
            best.put(rs.getString("MaMonHoc"), rs.getBigDecimal("CaoNhat"));
        }, maSinhVien);
        return best;
    }

    /** Dòng điểm rỗng cho ghi danh mới. Chỗ gọi đã giữ khoá theo sinh viên. */
    public void insertEmpty(String maLopHP, String maSinhVien) {
        jdbc.update("""
                INSERT INTO dbo.Diem (MaLopHP, MaSinhVien)
                SELECT ?, ?
                 WHERE NOT EXISTS (SELECT 1 FROM dbo.Diem WHERE MaLopHP = ? AND MaSinhVien = ?)
                """, maLopHP, maSinhVien, maLopHP, maSinhVien);
    }

    /**
     * Xoá dòng điểm CHỈ KHI chưa có điểm nào và chưa công bố. Điều kiện nằm trong
     * câu DELETE: giảng viên vừa nhập điểm xen giữa thì xoá 0 dòng.
     */
    public int deleteIfEmpty(String maLopHP, String maSinhVien) {
        return jdbc.update("""
                DELETE FROM dbo.Diem
                 WHERE MaLopHP = ? AND MaSinhVien = ? AND NgayCongBo IS NULL
                   AND DiemChuyenCan IS NULL AND DiemGiuaKy IS NULL
                   AND DiemCuoiKy IS NULL AND DiemTongKet IS NULL
                """, maLopHP, maSinhVien);
    }

    private static Instant toInstant(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }

    /** Dòng điểm của lớp, thô từ DB. */
    public record ClassGradeRow(
            String maSinhVien,
            String hoTen,
            BigDecimal diemChuyenCan,
            BigDecimal diemGiuaKy,
            BigDecimal diemCuoiKy,
            BigDecimal diemTongKet,
            long version,
            Instant ngayCongBo) {
    }

    /** Dòng thô từ DB, chưa áp quy tắc che điểm nháp. */
    public record GradeRow(
            String maHocKy,
            String tenHocKy,
            String maLopHP,
            String maMonHoc,
            String tenMonHoc,
            int soTinChi,
            BigDecimal diemChuyenCan,
            BigDecimal diemGiuaKy,
            BigDecimal diemCuoiKy,
            BigDecimal diemTongKet,
            Instant ngayCongBo) {
    }
}
