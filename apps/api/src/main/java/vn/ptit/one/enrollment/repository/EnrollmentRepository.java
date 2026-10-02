package vn.ptit.one.enrollment.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.enrollment.model.EnrolledCourse;
import vn.ptit.one.enrollment.model.EnrollmentStatus;
import vn.ptit.one.enrollment.model.RosterEntry;

/**
 * Ghi danh. Module `enrollment` sở hữu {@code DangKyHocPhan} (phía lớp),
 * {@code DangKyMonHoc} và {@code SinhVienHocKy} (phía sinh viên).
 *
 * <p>Mọi bộ đếm và điều kiện "còn chỗ / còn tín chỉ" nằm trong chính câu UPDATE
 * rồi chỗ gọi đọc số dòng. Không SELECT rồi IF, không MERGE.
 *
 * <p><b>Upsert ở đây KHÔNG dùng {@code HOLDLOCK}.</b> Cả ba bảng được ghi đều có
 * {@code MaSinhVien} trong khoá chính, và mọi ghi của một sinh viên trong một
 * học kỳ đã tuần tự hoá bằng {@link #acquireStudentTermLock} — nên không có hai giao
 * dịch nào cùng "UPDATE ra 0 dòng rồi INSERT" trên CÙNG một khoá. {@code HOLDLOCK}
 * lại khoá cả KHOẢNG giữa các khoá khi dòng chưa tồn tại: hai sinh viên KHÁC
 * nhau rơi vào cùng khoảng, cùng giữ RangeS-U, rồi cùng chờ nhau để INSERT —
 * deadlock. Test 30 chỗ/100 sinh viên đã bắt được đúng lỗi này.
 */
@Repository
@Profile("central")
public class EnrollmentRepository {

    /**
     * Khoá tuần tự hoá mọi thao tác đăng ký/huỷ của MỘT sinh viên trong MỘT
     * học kỳ. Phải lấy TRƯỚC mọi phép kiểm tín chỉ/trùng lịch/trùng môn: khoá
     * lúc cộng bộ đếm là quá muộn — hai request đã cùng vượt qua bước kiểm.
     */
    public static final String LOCK_PREFIX = "PTITONE:DangKy:";

    /** {@code IN (?, ?, ?)} cho {@link EnrollmentStatus#GIU_CHO}, tham số hoá. */
    private static final String GIU_CHO_IN =
            String.join(", ", Collections.nCopies(EnrollmentStatus.GIU_CHO.size(), "?"));

    private final JdbcTemplate jdbc;

    public EnrollmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean acquireStudentTermLock(String maSinhVien, String maHocKy, int timeoutMillis) {
        Integer result = jdbc.queryForObject("""
                DECLARE @ketQua int;
                EXEC @ketQua = sp_getapplock @Resource = ?, @LockMode = 'Exclusive',
                     @LockOwner = 'Transaction', @LockTimeout = ?;
                SELECT @ketQua;
                """, Integer.class, LOCK_PREFIX + maSinhVien + ":" + maHocKy, timeoutMillis);
        return result != null && result >= 0;
    }

    // --- Đọc --------------------------------------------------------------

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

    /** Lớp đang giữ chỗ của một môn trong học kỳ, nếu có. */
    public Optional<String> activeClassOfCourse(String maSinhVien, String maHocKy, String maMonHoc) {
        Object[] args = withActiveStatuses(maSinhVien, maHocKy, maMonHoc);
        return jdbc.queryForList("""
                SELECT MaLopHP FROM dbo.DangKyMonHoc
                 WHERE MaSinhVien = ? AND MaHocKy = ? AND MaMonHoc = ? AND TrangThai IN (%s)
                """.formatted(GIU_CHO_IN), String.class, args).stream().findFirst();
    }

    /** Ghi danh còn giữ chỗ của sinh viên trong học kỳ, kèm môn và ngày đăng ký. */
    public List<EnrolledCourse> enrolledCourses(String maSinhVien, String maHocKy) {
        Object[] args = withActiveStatuses(maSinhVien, maHocKy);
        return jdbc.query("""
                SELECT k.MaLopHP, k.MaMonHoc, m.TenMonHoc, k.SoTinChi, k.TrangThai, h.NgayDangKy
                  FROM dbo.DangKyMonHoc k
                  JOIN dbo.MonHoc m ON m.MaMonHoc = k.MaMonHoc
                  LEFT JOIN dbo.DangKyHocPhan h ON h.MaLopHP = k.MaLopHP AND h.MaSinhVien = k.MaSinhVien
                 WHERE k.MaSinhVien = ? AND k.MaHocKy = ? AND k.TrangThai IN (%s)
                 ORDER BY k.MaMonHoc
                """.formatted(GIU_CHO_IN), (rs, rowNum) -> {
                    LocalDateTime ngay = rs.getObject("NgayDangKy", LocalDateTime.class);
                    return new EnrolledCourse(
                            rs.getString("MaLopHP"),
                            rs.getString("MaMonHoc"),
                            rs.getString("TenMonHoc"),
                            rs.getInt("SoTinChi"),
                            rs.getString("TrangThai"),
                            ngay == null ? null : ngay.toInstant(ZoneOffset.UTC));
                }, args);
    }

    /** Số tín chỉ đã dùng của học kỳ, theo môn đang giữ chỗ — dùng khi huỷ. */
    public Optional<Integer> creditsOfClass(String maSinhVien, String maLopHP) {
        Object[] args = withActiveStatuses(maSinhVien, maLopHP);
        return jdbc.queryForList("""
                SELECT SoTinChi FROM dbo.DangKyMonHoc
                 WHERE MaSinhVien = ? AND MaLopHP = ? AND TrangThai IN (%s)
                """.formatted(GIU_CHO_IN), Integer.class, args).stream().findFirst();
    }

    /** Rỗng nếu sinh viên chưa đăng ký gì trong kỳ. */
    public Optional<TermCredits> termCredits(String maSinhVien, String maHocKy) {
        return jdbc.query("""
                SELECT SoTinChiDaDangKy, TranTinChi FROM dbo.SinhVienHocKy
                 WHERE MaSinhVien = ? AND MaHocKy = ?
                """, (rs, rowNum) -> new TermCredits(rs.getInt("SoTinChiDaDangKy"), rs.getInt("TranTinChi")),
                maSinhVien, maHocKy).stream().findFirst();
    }

    // --- Tín chỉ (Home) ---------------------------------------------------

    /** Tạo dòng học kỳ nếu chưa có. Trần truyền rõ, không dựa vào DEFAULT. */
    public void ensureTermRow(String maSinhVien, String maHocKy, int tranTinChi) {
        jdbc.update("""
                INSERT INTO dbo.SinhVienHocKy (MaSinhVien, MaHocKy, TranTinChi)
                SELECT ?, ?, ?
                 WHERE NOT EXISTS (SELECT 1 FROM dbo.SinhVienHocKy
                                    WHERE MaSinhVien = ? AND MaHocKy = ?)
                """, maSinhVien, maHocKy, tranTinChi, maSinhVien, maHocKy);
    }

    /** @return 0 nếu cộng thêm sẽ vượt trần — điều kiện nằm trong câu UPDATE */
    public int addCredits(String maSinhVien, String maHocKy, int soTinChi) {
        return jdbc.update("""
                UPDATE dbo.SinhVienHocKy SET SoTinChiDaDangKy = SoTinChiDaDangKy + ?
                 WHERE MaSinhVien = ? AND MaHocKy = ?
                   AND SoTinChiDaDangKy + SoTinChiDangGiuCho + ? <= TranTinChi
                """, soTinChi, maSinhVien, maHocKy, soTinChi);
    }

    public int subtractCredits(String maSinhVien, String maHocKy, int soTinChi) {
        return jdbc.update("""
                UPDATE dbo.SinhVienHocKy SET SoTinChiDaDangKy = SoTinChiDaDangKy - ?
                 WHERE MaSinhVien = ? AND MaHocKy = ? AND SoTinChiDaDangKy >= ?
                """, soTinChi, maSinhVien, maHocKy, soTinChi);
    }

    // --- Ghi danh phía lớp (Host) ------------------------------------------

    /**
     * Ghi danh vào lớp. PK là (lớp, sinh viên) nên đăng ký lại đúng lớp đã huỷ
     * phải dùng lại dòng cũ: UPDATE trước, INSERT sau. Không ai chen được vào
     * giữa hai câu vì khoá theo sinh viên — xem ghi chú đầu lớp.
     */
    public void upsertClassEnrollment(String maLopHP, String maSinhVien, String maCoSoNha,
            String hoTen, Instant now) {
        LocalDateTime ngay = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        int updated = jdbc.update("""
                UPDATE dbo.DangKyHocPhan
                   SET TrangThai = 'DA_DANG_KY', NgayDangKy = ?, MaCoSoNhaSV = ?,
                       HoTenSinhVien = ?, MaYeuCau = NULL
                 WHERE MaLopHP = ? AND MaSinhVien = ? AND TrangThai IN ('DA_HUY', 'TU_CHOI')
                """, ngay, maCoSoNha, hoTen, maLopHP, maSinhVien);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO dbo.DangKyHocPhan
                           (MaLopHP, MaSinhVien, MaCoSoNhaSV, HoTenSinhVien, NgayDangKy, TrangThai)
                    VALUES (?, ?, ?, ?, ?, 'DA_DANG_KY')
                    """, maLopHP, maSinhVien, maCoSoNha, hoTen, ngay);
        }
    }

    public int cancelClassEnrollment(String maLopHP, String maSinhVien) {
        Object[] args = withActiveStatuses(maLopHP, maSinhVien);
        return jdbc.update("""
                UPDATE dbo.DangKyHocPhan SET TrangThai = 'DA_HUY'
                 WHERE MaLopHP = ? AND MaSinhVien = ? AND TrangThai IN (%s)
                """.formatted(GIU_CHO_IN), args);
    }

    // --- Ghi danh phía sinh viên (Home) ------------------------------------

    /**
     * PK là (sinh viên, kỳ, môn) nên huỷ rồi đăng ký lại — kể cả sang lớp khác
     * của cùng môn — phải dùng lại dòng đã huỷ.
     */
    public void upsertCourseEnrollment(String maSinhVien, String maHocKy, String maMonHoc,
            String maLopHP, String maCoSoHost, int soTinChi, int phienBanLich) {
        int updated = jdbc.update("""
                UPDATE dbo.DangKyMonHoc
                   SET MaLopHP = ?, MaCoSoHost = ?, TrangThai = 'DA_DANG_KY', MaYeuCau = NULL,
                       SoTinChi = ?, PhienBanLich = ?
                 WHERE MaSinhVien = ? AND MaHocKy = ? AND MaMonHoc = ?
                   AND TrangThai IN ('DA_HUY', 'TU_CHOI')
                """, maLopHP, maCoSoHost, soTinChi, phienBanLich, maSinhVien, maHocKy, maMonHoc);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO dbo.DangKyMonHoc
                           (MaSinhVien, MaHocKy, MaMonHoc, MaLopHP, MaCoSoHost, TrangThai,
                            SoTinChi, PhienBanLich)
                    VALUES (?, ?, ?, ?, ?, 'DA_DANG_KY', ?, ?)
                    """, maSinhVien, maHocKy, maMonHoc, maLopHP, maCoSoHost, soTinChi, phienBanLich);
        }
    }

    public int cancelCourseEnrollment(String maSinhVien, String maLopHP) {
        Object[] args = withActiveStatuses(maSinhVien, maLopHP);
        return jdbc.update("""
                UPDATE dbo.DangKyMonHoc SET TrangThai = 'DA_HUY'
                 WHERE MaSinhVien = ? AND MaLopHP = ? AND TrangThai IN (%s)
                """.formatted(GIU_CHO_IN), args);
    }

    public record TermCredits(int daDangKy, int tranTinChi) {
    }

    /** Tham số đứng trước, rồi tới các trạng thái giữ chỗ cho mệnh đề {@code IN}. */
    private static Object[] withActiveStatuses(Object... leading) {
        List<Object> args = new ArrayList<>(List.of(leading));
        args.addAll(EnrollmentStatus.GIU_CHO);
        return args.toArray();
    }
}
