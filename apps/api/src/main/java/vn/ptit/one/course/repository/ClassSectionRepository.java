package vn.ptit.one.course.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.course.model.ClassSection;

/** Lớp học phần. Module `course` sở hữu bảng `LopHocPhan`. */
@Repository
@Profile("central")
public class ClassSectionRepository {

    private static final String COLUMNS = """
            SELECT l.MaLopHP, l.MaMonHoc, m.TenMonHoc, m.SoTinChi, l.MaHocKy, l.MaCoSoHost,
                   l.MaGiangVien, g.HoTen AS TenGiangVien, l.SoLuongToiDa, l.SoLuongDaDangKy,
                   l.TrangThai, l.ChoPhepLienCoSo, l.HinhThucHoc, l.PhienBanLich
              FROM dbo.LopHocPhan l
              JOIN dbo.MonHoc m    ON m.MaMonHoc = l.MaMonHoc
              LEFT JOIN dbo.GiangVien g ON g.MaGiangVien = l.MaGiangVien
            """;

    private final JdbcTemplate jdbc;

    public ClassSectionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ClassSection> search(String maHocKy, String maMonHoc, String maCoSo, String maGiangVien) {
        StringBuilder sql = new StringBuilder(COLUMNS).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        appendFilter(sql, args, " AND l.MaHocKy = ?", maHocKy);
        appendFilter(sql, args, " AND l.MaMonHoc = ?", maMonHoc);
        appendFilter(sql, args, " AND l.MaCoSoHost = ?", maCoSo);
        appendFilter(sql, args, " AND l.MaGiangVien = ?", maGiangVien);
        sql.append(" ORDER BY l.MaLopHP");
        return jdbc.query(sql.toString(), (rs, rowNum) -> map(rs), args.toArray());
    }

    public Optional<ClassSection> findOne(String maLopHP) {
        return jdbc.query(COLUMNS + " WHERE l.MaLopHP = ?", (rs, rowNum) -> map(rs), maLopHP)
                .stream().findFirst();
    }

    /**
     * Số thứ tự lớp kế tiếp của một (môn, kỳ, cơ sở).
     *
     * <p>Không dùng IDENTITY (C9): mã lớp mang ngữ nghĩa và phải nhúng mã cơ sở.
     * Có thể đua nhau giữa hai request; khoá chính sẽ bắt, chỗ gọi thử lại.
     */
    public int nextSequence(String prefix) {
        Integer max = jdbc.queryForObject("""
                SELECT ISNULL(MAX(TRY_CAST(RIGHT(MaLopHP, 2) AS int)), 0)
                  FROM dbo.LopHocPhan WHERE MaLopHP LIKE ? + '%'
                """, Integer.class, prefix);
        return (max == null ? 0 : max) + 1;
    }

    public void insert(ClassSection lop) {
        jdbc.update("""
                INSERT INTO dbo.LopHocPhan
                       (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, MaGiangVien,
                        SoLuongToiDa, TrangThai, ChoPhepLienCoSo, HinhThucHoc)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                lop.maLopHP(), lop.maMonHoc(), lop.maHocKy(), lop.maCoSoHost(), lop.maGiangVien(),
                lop.soLuongToiDa(), lop.trangThai(), lop.choPhepLienCoSo(), lop.hinhThucHoc());
    }

    /**
     * Đổi thuộc tính lớp. KHÔNG đổi môn/kỳ/cơ sở — ba thứ đó nằm trong mã lớp.
     *
     * <p>Điều kiện {@code SoLuongToiDa >= SoLuongDaDangKy} nằm ngay trong câu
     * UPDATE rồi đọc số dòng, thay vì SELECT trước rồi IF — sĩ số có thể đổi
     * giữa hai câu lệnh.
     *
     * @return số dòng đổi; 0 nghĩa là không có lớp hoặc hạ sức chứa dưới sĩ số
     */
    public int update(String maLopHP, int soLuongToiDa, String trangThai,
            boolean choPhepLienCoSo, String hinhThucHoc) {
        return jdbc.update("""
                UPDATE dbo.LopHocPhan
                   SET SoLuongToiDa = ?, TrangThai = ?, ChoPhepLienCoSo = ?, HinhThucHoc = ?
                 WHERE MaLopHP = ? AND ? >= SoLuongDaDangKy
                """, soLuongToiDa, trangThai, choPhepLienCoSo, hinhThucHoc, maLopHP, soLuongToiDa);
    }

    public int assignTeacher(String maLopHP, String maGiangVien) {
        return jdbc.update("UPDATE dbo.LopHocPhan SET MaGiangVien = ? WHERE MaLopHP = ?",
                maGiangVien, maLopHP);
    }

    /** Giảng viên đã dạy lớp nào khác trùng khung giờ chưa. Dùng khi phân công. */
    public List<String> teacherClashes(String maGiangVien, String maLopHP) {
        return jdbc.queryForList("""
                SELECT DISTINCT khac.MaLopHP
                  FROM dbo.LichHoc moi
                  JOIN dbo.LopHocPhan lopMoi  ON lopMoi.MaLopHP = moi.MaLopHP
                  JOIN dbo.LopHocPhan lopKhac ON lopKhac.MaHocKy = lopMoi.MaHocKy
                                             AND lopKhac.MaGiangVien = ?
                                             AND lopKhac.MaLopHP <> moi.MaLopHP
                                             AND lopKhac.TrangThai <> 'DA_HUY'
                  JOIN dbo.LichHoc khac ON khac.MaLopHP = lopKhac.MaLopHP
                                       AND khac.Thu = moi.Thu
                                       AND khac.TietBatDau < moi.TietBatDau + moi.SoTiet
                                       AND moi.TietBatDau < khac.TietBatDau + khac.SoTiet
                                       AND khac.TuanBatDau <= moi.TuanKetThuc
                                       AND moi.TuanBatDau <= khac.TuanKetThuc
                 WHERE moi.MaLopHP = ?
                """, String.class, maGiangVien, maLopHP);
    }

    private static void appendFilter(StringBuilder sql, List<Object> args, String clause, String value) {
        if (value != null && !value.isBlank()) {
            sql.append(clause);
            args.add(value.trim());
        }
    }

    private static ClassSection map(ResultSet rs) throws SQLException {
        return new ClassSection(
                rs.getString("MaLopHP"), rs.getString("MaMonHoc"), rs.getString("TenMonHoc"),
                rs.getInt("SoTinChi"), rs.getString("MaHocKy"), rs.getString("MaCoSoHost"),
                rs.getString("MaGiangVien"), rs.getString("TenGiangVien"),
                rs.getInt("SoLuongToiDa"), rs.getInt("SoLuongDaDangKy"),
                rs.getString("TrangThai"), rs.getBoolean("ChoPhepLienCoSo"),
                rs.getString("HinhThucHoc"), rs.getInt("PhienBanLich"));
    }
}
