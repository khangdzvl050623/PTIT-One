package vn.ptit.one.enrollment.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@code OutboxSuKien} — hộp thư đi của một cơ sở.
 *
 * <p>Tồn tại ở MỌI site; worker xử lý đối xứng theo mọi chiều bằng cùng một
 * đoạn code.
 */
@Repository
@Profile("central")
public class OutboxRepository {

    public static final String DIEM_DA_CHOT = "DiemDaChot";

    private final JdbcTemplate jdbc;

    public OutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Ghi một sự kiện cho mỗi sinh viên KHÁCH của lớp vừa công bố điểm.
     *
     * <p>⭐ Sinh viên có cơ sở nhà trùng cơ sở của lớp thì điểm đã nằm đúng chỗ
     * — không mirror, không event. Với tỉ lệ liên cơ sở ~2%, điều này cắt ~98%
     * lượng sự kiện.
     *
     * <p>{@code NOT EXISTS} chống ghi lặp khi công bố lại: đã có sự kiện
     * {@code PENDING} cho đúng (lớp, sinh viên, phiên bản) thì thôi.
     *
     * @return số sự kiện đã ghi
     */
    public int enqueueGrades(String maLopHP, String maCoSoHost, LocalDateTime now) {
        return jdbc.update("""
                INSERT INTO dbo.OutboxSuKien (EventId, LoaiSuKien, KhoaThucThe, NoiDung, Version, ThoiDiemTao)
                SELECT NEWID(), ?, d.MaLopHP + ':' + d.MaSinhVien,
                       CONCAT('{"maLopHP":"', d.MaLopHP,
                              '","maSinhVien":"', d.MaSinhVien,
                              '","maCoSoNhaSV":"', k.MaCoSoNhaSV,
                              '","diemTongKet":', CONVERT(varchar(10), d.DiemTongKet),
                              ',"version":', CONVERT(varchar(20), d.Version), '}'),
                       d.Version, ?
                  FROM dbo.Diem d
                  JOIN dbo.DangKyHocPhan k ON k.MaLopHP = d.MaLopHP AND k.MaSinhVien = d.MaSinhVien
                 WHERE d.MaLopHP = ?
                   AND d.NgayCongBo IS NOT NULL
                   AND d.DiemTongKet IS NOT NULL
                   AND k.MaCoSoNhaSV <> ?
                   AND NOT EXISTS (SELECT 1 FROM dbo.OutboxSuKien o
                                    WHERE o.KhoaThucThe = d.MaLopHP + ':' + d.MaSinhVien
                                      AND o.Version = d.Version AND o.TrangThai = 'PENDING')
                """, DIEM_DA_CHOT, now, maLopHP, maCoSoHost);
    }

    public static LocalDateTime utc(java.time.Instant now) {
        return LocalDateTime.ofInstant(now, ZoneOffset.UTC);
    }

    /** Nội dung sự kiện {@code DiemDaChot} sau khi đọc JSON. */
    public record GradeEvent(String maLopHP, String maSinhVien, String maCoSoNhaSV,
            BigDecimal diemTongKet, long version) {
    }
}
