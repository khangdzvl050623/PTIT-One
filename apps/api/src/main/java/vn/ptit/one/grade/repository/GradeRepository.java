package vn.ptit.one.grade.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Điểm. Module `grade` sở hữu bảng {@code Diem}. */
@Repository
@Profile("central")
public class GradeRepository {

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            LocalDateTime congBo = rs.getObject("NgayCongBo", LocalDateTime.class);
            return new GradeRow(
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
                    congBo == null ? null : congBo.toInstant(ZoneOffset.UTC));
        }, args.toArray());
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
