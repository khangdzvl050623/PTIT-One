package vn.ptit.one.teacher.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.teacher.model.Teacher;
import vn.ptit.one.teacher.model.TeacherDetail;

/** Hồ sơ giảng viên. Module `teacher` sở hữu bảng `GiangVien`. */
@Repository
@Profile("central")
public class TeacherRepository {

    private static final String COLUMNS = """
            SELECT MaGiangVien, HoTen, MaCoSo, MaKhoa, HocVi FROM dbo.GiangVien
            """;

    private final JdbcTemplate jdbc;

    public TeacherRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** `maCoSo` null nghĩa là mọi cơ sở — chỉ Admin Master được gọi như vậy. */
    public List<Teacher> search(String maCoSo, String maKhoa) {
        StringBuilder sql = new StringBuilder(COLUMNS).append(" WHERE 1 = 1");
        List<Object> args = new java.util.ArrayList<>();
        if (maCoSo != null && !maCoSo.isBlank()) {
            sql.append(" AND MaCoSo = ?");
            args.add(maCoSo);
        }
        if (maKhoa != null && !maKhoa.isBlank()) {
            sql.append(" AND MaKhoa = ?");
            args.add(maKhoa);
        }
        sql.append(" ORDER BY MaGiangVien");
        return jdbc.query(sql.toString(), (rs, rowNum) -> map(rs), args.toArray());
    }

    /** @return 0 nếu không có khoa {@code maKhoa} — báo lỗi rõ thay vì dựa vào FK */
    public int insert(String maGiangVien, String hoTen, String maCoSo, String maKhoa, String hocVi) {
        return jdbc.update("""
                INSERT INTO dbo.GiangVien (MaGiangVien, HoTen, MaCoSo, MaKhoa, HocVi)
                SELECT ?, ?, ?, k.MaKhoa, ? FROM dbo.Khoa k WHERE k.MaKhoa = ?
                """, maGiangVien, hoTen, maCoSo, hocVi, maKhoa);
    }

    public Optional<Teacher> findOne(String maGiangVien) {
        return jdbc.query(COLUMNS + " WHERE MaGiangVien = ?", (rs, rowNum) -> map(rs), maGiangVien)
                .stream().findFirst();
    }

    public boolean exists(String maGiangVien) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.GiangVien WHERE MaGiangVien = ?",
                Integer.class, maGiangVien);
        return count != null && count > 0;
    }

    /**
     * Hồ sơ đầy đủ để hiển thị. JOIN sang bảng của module khác chỉ để lấy TÊN
     * hiển thị — cùng cách {@code StudentRepository.findDetail} làm.
     *
     * <p>{@code LEFT JOIN TaiKhoan}: giảng viên có hồ sơ nhưng chưa có tài
     * khoản thì vẫn phải xem được hồ sơ, chỉ là không có email.
     */
    public Optional<TeacherDetail> findDetail(String maGiangVien) {
        return jdbc.query("""
                SELECT g.MaGiangVien, g.HoTen, g.HocVi, g.MaKhoa, k.TenKhoa,
                       g.MaCoSo, cs.TenCoSo, t.Email,
                       g.GioiTinh, g.DienThoai, g.SoCCCD, g.EmailCaNhan,
                       g.NoiSinh, g.DanToc, g.TonGiao, g.HoKhau, g.AnhDaiDien
                  FROM dbo.GiangVien g
                  JOIN dbo.Khoa k          ON k.MaKhoa = g.MaKhoa
                  JOIN dbo.CoSo cs         ON cs.MaCoSo = g.MaCoSo
                  LEFT JOIN dbo.TaiKhoan t ON t.MaThucThe = g.MaGiangVien
                 WHERE g.MaGiangVien = ?
                """, (rs, rowNum) -> new TeacherDetail(
                        rs.getString("MaGiangVien"),
                        rs.getString("HoTen"),
                        rs.getString("HocVi"),
                        rs.getString("MaKhoa"),
                        rs.getString("TenKhoa"),
                        rs.getString("MaCoSo"),
                        rs.getString("TenCoSo"),
                        rs.getString("Email"),
                        rs.getString("GioiTinh"),
                        rs.getString("DienThoai"),
                        rs.getString("SoCCCD"),
                        rs.getString("EmailCaNhan"),
                        rs.getString("NoiSinh"),
                        rs.getString("DanToc"),
                        rs.getString("TonGiao"),
                        rs.getString("HoKhau"),
                        rs.getString("AnhDaiDien")), maGiangVien).stream().findFirst();
    }

    /**
     * Thay TOÀN BỘ phần lý lịch. Tham số {@code null} ghi {@code NULL} — để
     * trống một ô là xoá giá trị cũ. KHÔNG đụng {@code AnhDaiDien}: ảnh có
     * đường ghi riêng ({@link #updateAvatar}).
     *
     * @return 0 nếu không có giảng viên đó
     */
    public int updateProfile(String maGiangVien, String gioiTinh, String dienThoai, String soCCCD,
            String emailCaNhan, String noiSinh, String danToc, String tonGiao, String hoKhau) {
        return jdbc.update("""
                UPDATE dbo.GiangVien
                   SET GioiTinh = ?, DienThoai = ?, SoCCCD = ?, EmailCaNhan = ?,
                       NoiSinh = ?, DanToc = ?, TonGiao = ?, HoKhau = ?
                 WHERE MaGiangVien = ?
                """, gioiTinh, dienThoai, soCCCD, emailCaNhan, noiSinh, danToc, tonGiao, hoKhau,
                maGiangVien);
    }

    /** Đặt hoặc xoá ({@code null}) URL ảnh đại diện. @return 0 nếu không có giảng viên đó */
    public int updateAvatar(String maGiangVien, String anhDaiDien) {
        return jdbc.update("UPDATE dbo.GiangVien SET AnhDaiDien = ? WHERE MaGiangVien = ?",
                anhDaiDien, maGiangVien);
    }

    private static Teacher map(ResultSet rs) throws SQLException {
        return new Teacher(
                rs.getString("MaGiangVien"), rs.getString("HoTen"),
                rs.getString("MaCoSo"), rs.getString("MaKhoa"), rs.getString("HocVi"));
    }
}
