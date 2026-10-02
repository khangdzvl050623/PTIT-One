package vn.ptit.one.student.repository;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.student.model.StudentProfile;

/** Hồ sơ sinh viên. Module `student` sở hữu bảng {@code SinhVien}. */
@Repository
@Profile("central")
public class StudentRepository {

    private final JdbcTemplate jdbc;

    public StudentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<StudentProfile> findOne(String maSinhVien) {
        return jdbc.query("""
                SELECT MaSinhVien, HoTen, MaCoSoNha, MaCTDT, TrangThai
                  FROM dbo.SinhVien WHERE MaSinhVien = ?
                """, (rs, rowNum) -> new StudentProfile(
                        rs.getString("MaSinhVien"),
                        rs.getString("HoTen"),
                        rs.getString("MaCoSoNha"),
                        rs.getString("MaCTDT"),
                        rs.getString("TrangThai")), maSinhVien).stream().findFirst();
    }
}
