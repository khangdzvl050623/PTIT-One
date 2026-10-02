package vn.ptit.one.report.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.enrollment.model.EnrollmentStatus;
import vn.ptit.one.report.model.CourseReport.GradeBucket;
import vn.ptit.one.report.model.ReportScope;

/**
 * Truy vấn tổng hợp chỉ đọc. Mọi số liệu tính trên CÙNG một tập lớp, định nghĩa
 * một lần ở CTE {@code Lop}; các con số khác nhau chỉ ở cách đếm, không ở tập lớp.
 */
@Repository
@Profile("central")
public class ReportRepository {

    /** Trạng thái giữ chỗ, nhúng làm hằng trong SQL — giá trị lấy từ mã nguồn, không từ client. */
    private static final String GIU_CHO = EnrollmentStatus.GIU_CHO.stream()
            .map(s -> "'" + s + "'").collect(Collectors.joining(", "));

    /**
     * Khoảng điểm cho phân bố. Mốc 4.0 trùng ngưỡng đạt giả định; các mốc khác
     * chỉ để vẽ biểu đồ, không mang ý nghĩa xếp loại chính thức.
     */
    private static final String[] KHOANG = { "<4.0", "4.0–5.4", "5.5–6.9", "7.0–8.4", "≥8.5" };

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SummaryRow summary(ReportScope scope) {
        Query lop = classSet(scope);
        return jdbc.queryForObject(lop.sql + """
                , GhiDanh AS (
                    SELECT d.MaLopHP, d.MaSinhVien
                      FROM dbo.DangKyHocPhan d JOIN Lop l ON l.MaLopHP = d.MaLopHP
                     WHERE d.TrangThai IN (%1$s)
                ), DiemLop AS (
                    SELECT dm.MaLopHP, COUNT(*) AS SoDong,
                           SUM(CASE WHEN dm.NgayCongBo IS NULL THEN 1 ELSE 0 END) AS SoNhap
                      FROM dbo.Diem dm JOIN Lop l ON l.MaLopHP = dm.MaLopHP
                     GROUP BY dm.MaLopHP
                )
                SELECT (SELECT COUNT(*) FROM Lop) AS SoLop,
                       (SELECT COUNT(*) FROM GhiDanh) AS LuotDangKy,
                       (SELECT COUNT(DISTINCT MaSinhVien) FROM GhiDanh) AS SoSinhVien,
                       (SELECT ISNULL(SUM(SoLuongToiDa), 0) FROM Lop) AS TongSucChua,
                       (SELECT ISNULL(SUM(SoLuongDaDangKy), 0) FROM Lop) AS TongDaDangKy,
                       (SELECT COUNT(*) FROM Lop WHERE SoLuongDaDangKy >= SoLuongToiDa) AS SoLopDay,
                       (SELECT COUNT(*) FROM Lop WHERE TrangThai = 'DA_KHOA') AS DaKhoa,
                       (SELECT COUNT(*) FROM Lop l JOIN DiemLop x ON x.MaLopHP = l.MaLopHP
                         WHERE l.TrangThai = 'MO' AND x.SoDong > 0 AND x.SoNhap = 0) AS DaCongBo
                """.formatted(GIU_CHO), (rs, rowNum) -> new SummaryRow(
                        rs.getInt("SoLop"), rs.getInt("LuotDangKy"), rs.getInt("SoSinhVien"),
                        rs.getInt("TongSucChua"), rs.getInt("TongDaDangKy"), rs.getInt("SoLopDay"),
                        rs.getInt("DaKhoa"), rs.getInt("DaCongBo")), lop.args.toArray());
    }

    /**
     * Theo môn. Mỗi chỉ số gom ở một truy vấn con riêng rồi mới nối theo môn —
     * nối thẳng lớp × ghi danh × điểm sẽ nhân dòng và đếm sai.
     */
    public List<CourseRow> byCourse(ReportScope scope, BigDecimal nguongDat) {
        Query lop = classSet(scope);
        List<Object> args = new ArrayList<>(lop.args);
        args.add(nguongDat);
        args.add(nguongDat);
        return jdbc.query(lop.sql + """
                , GhiDanh AS (
                    SELECT l.MaMonHoc, d.MaSinhVien
                      FROM dbo.DangKyHocPhan d JOIN Lop l ON l.MaLopHP = d.MaLopHP
                     WHERE d.TrangThai IN (%1$s)
                )
                SELECT c.MaMonHoc, m.TenMonHoc, c.SoLop, c.TongSucChua, c.TongDaDangKy,
                       ISNULL(g.LuotDangKy, 0) AS LuotDangKy,
                       ISNULL(k.Dat, 0) AS Dat, ISNULL(k.Truot, 0) AS Truot,
                       ISNULL(k.K1, 0) AS K1, ISNULL(k.K2, 0) AS K2, ISNULL(k.K3, 0) AS K3,
                       ISNULL(k.K4, 0) AS K4, ISNULL(k.K5, 0) AS K5
                  FROM (SELECT MaMonHoc, COUNT(*) AS SoLop, SUM(SoLuongToiDa) AS TongSucChua,
                               SUM(SoLuongDaDangKy) AS TongDaDangKy
                          FROM Lop GROUP BY MaMonHoc) c
                  JOIN dbo.MonHoc m ON m.MaMonHoc = c.MaMonHoc
                  LEFT JOIN (SELECT MaMonHoc, COUNT(*) AS LuotDangKy
                               FROM GhiDanh GROUP BY MaMonHoc) g ON g.MaMonHoc = c.MaMonHoc
                  LEFT JOIN (SELECT l.MaMonHoc,
                                    SUM(CASE WHEN d.DiemTongKet >= ? THEN 1 ELSE 0 END) AS Dat,
                                    SUM(CASE WHEN d.DiemTongKet <  ? THEN 1 ELSE 0 END) AS Truot,
                                    SUM(CASE WHEN d.DiemTongKet < 4.0 THEN 1 ELSE 0 END) AS K1,
                                    SUM(CASE WHEN d.DiemTongKet >= 4.0 AND d.DiemTongKet < 5.5 THEN 1 ELSE 0 END) AS K2,
                                    SUM(CASE WHEN d.DiemTongKet >= 5.5 AND d.DiemTongKet < 7.0 THEN 1 ELSE 0 END) AS K3,
                                    SUM(CASE WHEN d.DiemTongKet >= 7.0 AND d.DiemTongKet < 8.5 THEN 1 ELSE 0 END) AS K4,
                                    SUM(CASE WHEN d.DiemTongKet >= 8.5 THEN 1 ELSE 0 END) AS K5
                               FROM dbo.Diem d JOIN Lop l ON l.MaLopHP = d.MaLopHP
                              WHERE d.NgayCongBo IS NOT NULL AND d.DiemTongKet IS NOT NULL
                              GROUP BY l.MaMonHoc) k ON k.MaMonHoc = c.MaMonHoc
                 ORDER BY c.MaMonHoc
                """.formatted(GIU_CHO), (rs, rowNum) -> new CourseRow(
                        rs.getString("MaMonHoc"), rs.getString("TenMonHoc"), rs.getInt("SoLop"),
                        rs.getInt("LuotDangKy"), rs.getInt("TongSucChua"), rs.getInt("TongDaDangKy"),
                        rs.getInt("Dat"), rs.getInt("Truot"),
                        List.of(new GradeBucket(KHOANG[0], rs.getInt("K1")),
                                new GradeBucket(KHOANG[1], rs.getInt("K2")),
                                new GradeBucket(KHOANG[2], rs.getInt("K3")),
                                new GradeBucket(KHOANG[3], rs.getInt("K4")),
                                new GradeBucket(KHOANG[4], rs.getInt("K5")))), args.toArray());
    }

    /** CTE {@code Lop}: tập lớp của báo cáo. Bỏ lớp dự kiến và đã huỷ. */
    private static Query classSet(ReportScope scope) {
        StringBuilder sql = new StringBuilder("""
                WITH Lop AS (
                    SELECT MaLopHP, MaMonHoc, SoLuongToiDa, SoLuongDaDangKy, TrangThai
                      FROM dbo.LopHocPhan
                     WHERE MaHocKy = ? AND TrangThai IN ('MO', 'DA_KHOA')
                """);
        List<Object> args = new ArrayList<>(List.of(scope.maHocKy()));
        if (scope.maCoSo() != null) {
            sql.append(" AND MaCoSoHost = ?");
            args.add(scope.maCoSo());
        }
        if (scope.maMonHoc() != null) {
            sql.append(" AND MaMonHoc = ?");
            args.add(scope.maMonHoc());
        }
        sql.append("\n)");
        return new Query(sql.toString(), args);
    }

    private record Query(String sql, List<Object> args) {
    }

    public record SummaryRow(int soLop, int luotDangKy, int soSinhVien, int tongSucChua,
            int tongDaDangKy, int soLopDay, int daKhoa, int daCongBo) {
    }

    public record CourseRow(String maMonHoc, String tenMonHoc, int soLop, int luotDangKy,
            int tongSucChua, int tongDaDangKy, int soDat, int soTruot, List<GradeBucket> phanBoDiem) {
    }
}
