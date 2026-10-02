package vn.ptit.one.timetable.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.timetable.model.ScheduleSlot;
import vn.ptit.one.timetable.model.TimetableEntry;

/** Lịch học. Module `timetable` sở hữu bảng `LichHoc` và `KhungGioTiet`. */
@Repository
@Profile("central")
public class ScheduleRepository {

    /**
     * Khoá tuần tự hoá việc sửa lịch trong một học kỳ.
     *
     * <p>Trùng giảng viên và trùng phòng là tính chất giữa NHIỀU lớp, nên khoá
     * theo dòng không đủ: hai admin sửa hai lớp khác nhau, mỗi bên xét riêng
     * đều sạch, ghép lại vẫn trùng. Phạm vi xung đột gói gọn trong một học kỳ
     * nên khoá theo học kỳ, không khoá toàn hệ thống.
     */
    public static final String LOCK_PREFIX = "PTITONE:LichHoc:";

    private final JdbcTemplate jdbc;

    public ScheduleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean acquireTermLock(String maHocKy, int timeoutMillis) {
        Integer result = jdbc.queryForObject("""
                DECLARE @ketQua int;
                EXEC @ketQua = sp_getapplock @Resource = ?, @LockMode = 'Exclusive',
                     @LockOwner = 'Transaction', @LockTimeout = ?;
                SELECT @ketQua;
                """, Integer.class, LOCK_PREFIX + maHocKy, timeoutMillis);
        return result != null && result >= 0;
    }

    public List<ScheduleSlot> findByClass(String maLopHP) {
        return jdbc.query("""
                SELECT Thu, TietBatDau, SoTiet, PhongHoc, TuanBatDau, TuanKetThuc
                  FROM dbo.LichHoc WHERE MaLopHP = ?
                 ORDER BY Thu, TietBatDau
                """, (rs, rowNum) -> map(rs), maLopHP);
    }

    /**
     * Buổi học của các lớp cho trước, kèm môn, giảng viên và giờ thật.
     *
     * <p>Giờ ra lấy theo tiết cuối {@code TietBatDau + SoTiet - 1}; CHECK của
     * dịch vụ đã chặn buổi vượt tiết 12 nên tiết đó luôn có trong khung giờ.
     */
    public List<TimetableEntry> entriesFor(Collection<String> maLopHP) {
        if (maLopHP.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", Collections.nCopies(maLopHP.size(), "?"));
        return jdbc.query("""
                SELECT h.MaLopHP, l.MaMonHoc, m.TenMonHoc, g.HoTen AS TenGiangVien, l.HinhThucHoc,
                       h.Thu, h.TietBatDau, h.SoTiet, h.PhongHoc, h.TuanBatDau, h.TuanKetThuc,
                       vao.GioBatDau, ra.GioKetThuc
                  FROM dbo.LichHoc h
                  JOIN dbo.LopHocPhan l ON l.MaLopHP = h.MaLopHP
                  JOIN dbo.MonHoc m     ON m.MaMonHoc = l.MaMonHoc
                  LEFT JOIN dbo.GiangVien g     ON g.MaGiangVien = l.MaGiangVien
                  LEFT JOIN dbo.KhungGioTiet vao ON vao.SoTiet = h.TietBatDau
                  LEFT JOIN dbo.KhungGioTiet ra  ON ra.SoTiet = h.TietBatDau + h.SoTiet - 1
                 WHERE h.MaLopHP IN (%s)
                 ORDER BY h.Thu, h.TietBatDau, h.MaLopHP
                """.formatted(placeholders), (rs, rowNum) -> new TimetableEntry(
                        rs.getString("MaLopHP"),
                        rs.getString("MaMonHoc"),
                        rs.getString("TenMonHoc"),
                        rs.getString("TenGiangVien"),
                        rs.getString("HinhThucHoc"),
                        rs.getInt("Thu"),
                        rs.getInt("TietBatDau"),
                        rs.getInt("SoTiet"),
                        rs.getString("PhongHoc"),
                        rs.getInt("TuanBatDau"),
                        rs.getInt("TuanKetThuc"),
                        rs.getObject("GioBatDau", LocalTime.class),
                        rs.getObject("GioKetThuc", LocalTime.class)),
                maLopHP.toArray());
    }

    /**
     * Buổi học của MỌI lớp khác trong cùng học kỳ do một giảng viên dạy.
     *
     * <p>Lấy về rồi so trong Java thay vì viết điều kiện chồng lấn trong SQL:
     * cùng một phép so được dùng lại cho cả xung đột nội bộ của chính lớp đang
     * sửa, và lỗi trả về nêu được đúng lớp nào trùng.
     */
    public List<ClashRow> teacherSlots(String maHocKy, String maGiangVien, String excludeLopHP) {
        return jdbc.query("""
                SELECT l.MaLopHP, h.Thu, h.TietBatDau, h.SoTiet, h.PhongHoc, h.TuanBatDau, h.TuanKetThuc
                  FROM dbo.LichHoc h
                  JOIN dbo.LopHocPhan l ON l.MaLopHP = h.MaLopHP
                 WHERE l.MaHocKy = ? AND l.MaGiangVien = ?
                   AND l.MaLopHP <> ? AND l.TrangThai <> 'DA_HUY'
                """, (rs, rowNum) -> new ClashRow(rs.getString("MaLopHP"), map(rs)),
                maHocKy, maGiangVien, excludeLopHP);
    }

    /** Buổi học của các lớp cho trước, kèm lớp giữ buổi đó. */
    public List<ClashRow> slotsOf(Collection<String> maLopHP) {
        if (maLopHP.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", Collections.nCopies(maLopHP.size(), "?"));
        return jdbc.query("""
                SELECT MaLopHP, Thu, TietBatDau, SoTiet, PhongHoc, TuanBatDau, TuanKetThuc
                  FROM dbo.LichHoc WHERE MaLopHP IN (%s)
                """.formatted(placeholders), (rs, rowNum) -> new ClashRow(rs.getString("MaLopHP"), map(rs)),
                maLopHP.toArray());
    }

    /** Buổi học của mọi lớp khác trong cùng học kỳ dùng cùng phòng (so không phân biệt hoa thường). */
    public List<ClashRow> roomSlots(String maHocKy, String phongHoc, String excludeLopHP) {
        return jdbc.query("""
                SELECT l.MaLopHP, h.Thu, h.TietBatDau, h.SoTiet, h.PhongHoc, h.TuanBatDau, h.TuanKetThuc
                  FROM dbo.LichHoc h
                  JOIN dbo.LopHocPhan l ON l.MaLopHP = h.MaLopHP
                 WHERE l.MaHocKy = ? AND UPPER(LTRIM(RTRIM(h.PhongHoc))) = ?
                   AND l.MaLopHP <> ? AND l.TrangThai <> 'DA_HUY'
                """, (rs, rowNum) -> new ClashRow(rs.getString("MaLopHP"), map(rs)),
                maHocKy, phongHoc, excludeLopHP);
    }

    public void deleteByClass(String maLopHP) {
        jdbc.update("DELETE FROM dbo.LichHoc WHERE MaLopHP = ?", maLopHP);
    }

    public void insert(String maLopHP, ScheduleSlot slot) {
        jdbc.update("""
                INSERT INTO dbo.LichHoc
                       (MaLopHP, Thu, TietBatDau, SoTiet, PhongHoc, TuanBatDau, TuanKetThuc)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, maLopHP, slot.thu(), slot.tietBatDau(), slot.soTiet(),
                slot.phongHoc(), slot.tuanBatDau(), slot.tuanKetThuc());
    }

    /** Bộ đếm phiên bản lịch do ứng dụng sở hữu, không trigger nào chạm vào. */
    public void bumpScheduleVersion(String maLopHP) {
        jdbc.update("UPDATE dbo.LopHocPhan SET PhienBanLich = PhienBanLich + 1 WHERE MaLopHP = ?",
                maLopHP);
    }

    /** Một buổi học kèm lớp đang giữ nó — đủ để báo đúng lớp nào trùng. */
    public record ClashRow(String maLopHP, ScheduleSlot slot) {
    }

    private static ScheduleSlot map(ResultSet rs) throws SQLException {
        return new ScheduleSlot(
                rs.getInt("Thu"), rs.getInt("TietBatDau"), rs.getInt("SoTiet"),
                rs.getString("PhongHoc"), rs.getInt("TuanBatDau"), rs.getInt("TuanKetThuc"));
    }
}
