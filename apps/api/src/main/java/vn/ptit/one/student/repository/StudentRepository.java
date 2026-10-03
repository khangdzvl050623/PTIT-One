package vn.ptit.one.student.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.student.model.StudentDetail;
import vn.ptit.one.student.model.StudentProfile;

/** Hồ sơ sinh viên. Module `student` sở hữu bảng {@code SinhVien}. */
@Repository
@Profile("central")
public class StudentRepository {

    private final JdbcTemplate jdbc;

    public StudentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mã sinh viên còn thuộc trường (đang học hoặc bảo lưu); {@code maCoSo} null là toàn trường. */
    public List<String> currentStudentIds(String maCoSo) {
        if (maCoSo == null) {
            return jdbc.queryForList("""
                    SELECT MaSinhVien FROM dbo.SinhVien WHERE TrangThai IN ('DANG_HOC', 'BAO_LUU')
                    """, String.class);
        }
        return jdbc.queryForList("""
                SELECT MaSinhVien FROM dbo.SinhVien
                 WHERE MaCoSoNha = ? AND TrangThai IN ('DANG_HOC', 'BAO_LUU')
                """, String.class, maCoSo);
    }

    /**
     * Ghi kèm điều kiện chương trình đào tạo có thật, để báo lỗi rõ thay vì
     * dựa vào FK.
     *
     * @return 0 nếu không có chương trình {@code maCTDT}
     */
    public int insert(String maSinhVien, String hoTen, LocalDate ngaySinh, String maCoSoNha, String maCTDT) {
        return jdbc.update("""
                INSERT INTO dbo.SinhVien (MaSinhVien, HoTen, NgaySinh, MaCoSoNha, MaCTDT)
                SELECT ?, ?, ?, ?, c.MaCTDT FROM dbo.ChuongTrinhDaoTao c WHERE c.MaCTDT = ?
                """, maSinhVien, hoTen, ngaySinh, maCoSoNha, maCTDT);
    }

    public boolean exists(String maSinhVien) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SinhVien WHERE MaSinhVien = ?",
                Integer.class, maSinhVien);
        return count != null && count > 0;
    }

    /**
     * Hồ sơ đầy đủ để hiển thị. JOIN sang bảng của module khác chỉ để lấy TÊN
     * hiển thị — cùng cách {@code ClassSectionRepository} lấy tên giảng viên.
     * Quy tắc "không truy cập repository của module khác" nói về class Java,
     * không cấm JOIN trong một câu đọc.
     *
     * <p>{@code LEFT JOIN TaiKhoan}: sinh viên có hồ sơ nhưng chưa có tài khoản
     * thì vẫn phải xem được hồ sơ, chỉ là không có email.
     */
    public Optional<StudentDetail> findDetail(String maSinhVien) {
        return jdbc.query("""
                SELECT s.MaSinhVien, s.HoTen, s.NgaySinh, t.Email,
                       s.GioiTinh, s.DienThoai, s.SoCCCD, s.EmailCaNhan,
                       s.NoiSinh, s.DanToc, s.TonGiao, s.HoKhau, s.AnhDaiDien,
                       s.MaCoSoNha, cs.TenCoSo, s.TrangThai, s.SoTinChiTichLuy,
                       s.MaCTDT, ct.TenCTDT, ct.TongTinChi, k.TenKhoa
                  FROM dbo.SinhVien s
                  JOIN dbo.CoSo cs             ON cs.MaCoSo = s.MaCoSoNha
                  JOIN dbo.ChuongTrinhDaoTao ct ON ct.MaCTDT = s.MaCTDT
                  JOIN dbo.Khoa k              ON k.MaKhoa = ct.MaKhoa
                  LEFT JOIN dbo.TaiKhoan t     ON t.MaThucThe = s.MaSinhVien
                 WHERE s.MaSinhVien = ?
                """, (rs, rowNum) -> new StudentDetail(
                        rs.getString("MaSinhVien"),
                        rs.getString("HoTen"),
                        rs.getObject("NgaySinh", LocalDate.class),
                        rs.getString("Email"),
                        rs.getString("GioiTinh"),
                        rs.getString("DienThoai"),
                        rs.getString("SoCCCD"),
                        rs.getString("EmailCaNhan"),
                        rs.getString("NoiSinh"),
                        rs.getString("DanToc"),
                        rs.getString("TonGiao"),
                        rs.getString("HoKhau"),
                        rs.getString("AnhDaiDien"),
                        rs.getString("MaCoSoNha"),
                        rs.getString("TenCoSo"),
                        rs.getString("TrangThai"),
                        rs.getString("MaCTDT"),
                        rs.getString("TenCTDT"),
                        rs.getString("TenKhoa"),
                        rs.getInt("TongTinChi"),
                        rs.getInt("SoTinChiTichLuy")), maSinhVien).stream().findFirst();
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
