package vn.ptit.one.teacher.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.teacher.model.Teacher;

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

    private static Teacher map(ResultSet rs) throws SQLException {
        return new Teacher(
                rs.getString("MaGiangVien"), rs.getString("HoTen"),
                rs.getString("MaCoSo"), rs.getString("MaKhoa"), rs.getString("HocVi"));
    }
}
