package vn.ptit.one.enrollment.repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.enrollment.model.EnrollmentStatus;
import vn.ptit.one.enrollment.model.RosterEntry;

/**
 * Ghi danh. Module `enrollment` sở hữu {@code DangKyHocPhan} (phía lớp) và
 * {@code DangKyMonHoc} (phía sinh viên).
 */
@Repository
@Profile("central")
public class EnrollmentRepository {

    /** {@code IN (?, ?, ?)} cho {@link EnrollmentStatus#GIU_CHO}, tham số hoá. */
    private static final String GIU_CHO_IN =
            String.join(", ", Collections.nCopies(EnrollmentStatus.GIU_CHO.size(), "?"));

    private final JdbcTemplate jdbc;

    public EnrollmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Sinh viên còn giữ chỗ trong lớp. Họ tên lấy từ bản sao trên
     * {@code DangKyHocPhan}: ở Phần 2 lớp không có hồ sơ của sinh viên khách.
     */
    public List<RosterEntry> roster(String maLopHP) {
        Object[] args = withActiveStatuses(maLopHP);
        return jdbc.query("""
                SELECT MaSinhVien, HoTenSinhVien, MaCoSoNhaSV, NgayDangKy, TrangThai
                  FROM dbo.DangKyHocPhan
                 WHERE MaLopHP = ? AND TrangThai IN (%s)
                 ORDER BY MaSinhVien
                """.formatted(GIU_CHO_IN), (rs, rowNum) -> new RosterEntry(
                        rs.getString("MaSinhVien"),
                        rs.getString("HoTenSinhVien"),
                        rs.getString("MaCoSoNhaSV"),
                        rs.getObject("NgayDangKy", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        rs.getString("TrangThai")), args);
    }

    /** Lớp sinh viên còn giữ chỗ trong một học kỳ — một lớp cho mỗi môn. */
    public List<String> activeClasses(String maSinhVien, String maHocKy) {
        Object[] args = withActiveStatuses(maSinhVien, maHocKy);
        return jdbc.queryForList("""
                SELECT MaLopHP FROM dbo.DangKyMonHoc
                 WHERE MaSinhVien = ? AND MaHocKy = ? AND TrangThai IN (%s)
                """.formatted(GIU_CHO_IN), String.class, args);
    }

    /** Tham số đứng trước, rồi tới các trạng thái giữ chỗ cho mệnh đề {@code IN}. */
    private static Object[] withActiveStatuses(Object... leading) {
        List<Object> args = new ArrayList<>(List.of(leading));
        args.addAll(EnrollmentStatus.GIU_CHO);
        return args.toArray();
    }
}
