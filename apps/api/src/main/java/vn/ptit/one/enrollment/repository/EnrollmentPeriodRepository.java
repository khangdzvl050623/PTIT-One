package vn.ptit.one.enrollment.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.enrollment.model.EnrollmentPeriod;

/** Đợt đăng ký. Module `enrollment` sở hữu bảng `DotDangKy`. */
@Repository
@Profile("central")
public class EnrollmentPeriodRepository {

    private static final String COLUMNS = """
            SELECT MaDot, MaHocKy, MaCoSo, ThoiGianMo, ThoiGianDong, TrangThai FROM dbo.DotDangKy
            """;

    private final JdbcTemplate jdbc;

    public EnrollmentPeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EnrollmentPeriod> search(String maHocKy, String maCoSo) {
        StringBuilder sql = new StringBuilder(COLUMNS).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (maHocKy != null && !maHocKy.isBlank()) {
            sql.append(" AND MaHocKy = ?");
            args.add(maHocKy.trim());
        }
        if (maCoSo != null && !maCoSo.isBlank()) {
            sql.append(" AND MaCoSo = ?");
            args.add(maCoSo.trim());
        }
        sql.append(" ORDER BY MaDot");
        return jdbc.query(sql.toString(), (rs, rowNum) -> map(rs), args.toArray());
    }

    public Optional<EnrollmentPeriod> findOne(String maDot) {
        return jdbc.query(COLUMNS + " WHERE MaDot = ?", (rs, rowNum) -> map(rs), maDot)
                .stream().findFirst();
    }

    /**
     * Học kỳ đang có đợt mở ở bất kỳ cơ sở nào.
     *
     * <p>Lọc cả hai điều kiện ngay trong SQL. Module khác hỏi qua service chứ
     * không tự join vào bảng này.
     */
    public List<String> openTerms(Instant now) {
        return jdbc.queryForList("""
                SELECT DISTINCT MaHocKy FROM dbo.DotDangKy
                 WHERE TrangThai = 'DANG_MO' AND ? BETWEEN ThoiGianMo AND ThoiGianDong
                """, String.class, toDb(now));
    }

    public int nextSequence(String prefix) {
        Integer max = jdbc.queryForObject("""
                SELECT ISNULL(MAX(TRY_CAST(RIGHT(MaDot, 2) AS int)), 0)
                  FROM dbo.DotDangKy WHERE MaDot LIKE ? + '%'
                """, Integer.class, prefix);
        return (max == null ? 0 : max) + 1;
    }

    public void insert(EnrollmentPeriod dot) {
        jdbc.update("""
                INSERT INTO dbo.DotDangKy (MaDot, MaHocKy, MaCoSo, ThoiGianMo, ThoiGianDong, TrangThai)
                VALUES (?, ?, ?, ?, ?, ?)
                """, dot.maDot(), dot.maHocKy(), dot.maCoSo(),
                toDb(dot.thoiGianMo()), toDb(dot.thoiGianDong()), dot.trangThai());
    }

    public int update(String maDot, Instant thoiGianMo, Instant thoiGianDong, String trangThai) {
        return jdbc.update("""
                UPDATE dbo.DotDangKy SET ThoiGianMo = ?, ThoiGianDong = ?, TrangThai = ?
                 WHERE MaDot = ?
                """, toDb(thoiGianMo), toDb(thoiGianDong), trangThai, maDot);
    }

    /* datetime2 không mang múi giờ; quy ước toàn hệ thống là lưu UTC. */
    private static LocalDateTime toDb(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static EnrollmentPeriod map(ResultSet rs) throws SQLException {
        return new EnrollmentPeriod(
                rs.getString("MaDot"), rs.getString("MaHocKy"), rs.getString("MaCoSo"),
                rs.getObject("ThoiGianMo", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getObject("ThoiGianDong", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getString("TrangThai"));
    }
}
