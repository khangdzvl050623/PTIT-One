package vn.ptit.one.enrollment.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.enrollment.model.HostDecision;

/**
 * Phía <b>Host</b> của saga: {@code KetQuaXuLyYeuCau} và {@code DangKyHocPhan}.
 *
 * <p>Host bảo vệ <b>ràng buộc theo lớp</b> — sức chứa, ghi danh trùng, đợt đăng
 * ký, điều kiện lớp được phép nhận sinh viên liên cơ sở. Ràng buộc theo sinh
 * viên là việc của Home, xem {@link CrossSiteRepository}.
 *
 * <p>Mọi câu ở đây chạy trong {@code SiteContext.runAt(HOST, …)}, tức trên
 * DataSource của cơ sở khác. Đó là lý do chúng phải gọn và <b>nằm trong đúng một
 * giao dịch cục bộ</b>: một giao dịch không bao giờ trải hai site.
 */
@Repository
@Profile("central")
public class HostRequestRepository {

    /**
     * Khoá theo {@code MaYeuCau}, lấy TRƯỚC khi đọc {@code KetQuaXuLyYeuCau}.
     *
     * <p>⚠️ "Cùng {@code MaYeuCau}" <b>không tự tạo ra khoá</b>. Khi dòng kết quả
     * còn <em>chưa tồn tại</em> — đúng tình huống của lần gửi đầu tiên, và của
     * lệnh huỷ đến trước lệnh đăng ký — thì không có dòng nào để khoá, nên hai
     * lệnh cùng {@code MaYeuCau} sẽ cùng đi qua. Khoá phải tường minh, và mọi
     * nhánh (đăng ký · huỷ · retry) phải lấy nó cùng thứ tự.
     */
    public static final String LOCK_PREFIX = "PTITONE:YeuCau:";

    private final JdbcTemplate jdbc;

    public HostRequestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * ⚠️ Câu {@code SELECT} mồi là BẮT BUỘC, không phải thừa.
     *
     * <p>{@code sp_getapplock @LockOwner = 'Transaction'} đòi
     * {@code @@TRANCOUNT > 0}, nhưng với autocommit tắt SQL Server chỉ mở giao
     * dịch ngầm khi gặp {@code SELECT/INSERT/UPDATE/DELETE} — {@code EXEC}
     * KHÔNG nằm trong danh sách đó. Khoá này là câu ĐẦU TIÊN của giao dịch
     * Host, nên không có mồi thì {@code sp_getapplock} trả {@code -999} và mọi
     * yêu cầu đều bị báo "đang xử lý". Các chỗ khoá khác trong dự án may mắn
     * luôn có lệnh đọc chạy trước nên không lộ ra lỗi này.
     *
     * <p>Mồi phải là SELECT GÁN GIÁ TRỊ: {@code SELECT TOP 0 …} trả về một
     * result set rỗng, và {@code queryForObject} đọc đúng result set đầu tiên
     * nên ném "expected 1, actual 0".
     *
     * <p>Lời giải thích để ở ĐÂY chứ không trong chuỗi SQL: chú thích tiếng
     * Việt nằm trong câu lệnh gửi xuống driver thì dấu bị mã hoá hỏng và
     * SQL Server báo lỗi cú pháp ở giữa câu chú thích.
     */
    public boolean acquireRequestLock(String maYeuCau, int timeoutMillis) {
        Integer ketQua = jdbc.queryForObject("""
                DECLARE @moi int;
                SELECT @moi = COUNT(*) FROM sys.objects WHERE 1 = 0;
                DECLARE @ketQua int;
                EXEC @ketQua = sp_getapplock @Resource = ?, @LockMode = 'Exclusive',
                     @LockOwner = 'Transaction', @LockTimeout = ?;
                SELECT @ketQua;
                """, Integer.class, LOCK_PREFIX + maYeuCau, timeoutMillis);
        return ketQua != null && ketQua >= 0;
    }

    /**
     * Quyết định đã ghi cho {@code MaYeuCau}, nếu có.
     *
     * <p>★ Đây là câu đầu tiên của mọi nhánh. Có bản ghi thì <b>trả về nguyên
     * kết quả cũ</b>, không đánh giá lại — kể cả khi kết quả cũ là từ chối.
     */
    public Optional<HostDecision> findDecision(String maYeuCau) {
        return jdbc.query("""
                SELECT KetQua, MaLoi, LyDo FROM dbo.KetQuaXuLyYeuCau WHERE MaYeuCau = ?
                """, (rs, rowNum) -> new HostDecision(
                        rs.getString("KetQua"), rs.getString("MaLoi"), rs.getString("LyDo")),
                maYeuCau).stream().findFirst();
    }

    /** Ghi quyết định — LUÔN LUÔN, cả khi từ chối. Xem {@link HostDecision}. */
    public void insertDecision(String maYeuCau, HostDecision quyetDinh, String maLopHP,
            String maSinhVien, LocalDateTime now) {
        jdbc.update("""
                INSERT INTO dbo.KetQuaXuLyYeuCau
                       (MaYeuCau, KetQua, MaLoi, LyDo, MaLopHP, MaSinhVien, ThoiDiemXuLy)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, maYeuCau, quyetDinh.ketQua(), quyetDinh.maLoi(), quyetDinh.lyDo(),
                maLopHP, maSinhVien, now);
    }

    /**
     * Chuyển quyết định sang {@code DA_HUY}, chỉ từ đúng trạng thái nguồn.
     *
     * @return 1 nếu luồng này THẮNG — chỉ nó được trả chỗ và xoá ghi danh
     */
    public int moveDecision(String maYeuCau, String tuKetQua, String denKetQua,
            String maThaoTacHuy) {
        return jdbc.update("""
                UPDATE dbo.KetQuaXuLyYeuCau SET KetQua = ?, MaThaoTacHuy = ?
                 WHERE MaYeuCau = ? AND KetQua = ?
                """, denKetQua, maThaoTacHuy, maYeuCau, tuKetQua);
    }

    // --- DangKyHocPhan tại Host ---------------------------------------------

    /**
     * Ghi danh sinh viên khách vào lớp, mang theo {@code MaYeuCau} để đường huỷ
     * tìm lại đúng dòng này.
     *
     * <p>PK là (lớp, sinh viên) nên đăng ký lại một lớp đã huỷ phải dùng lại dòng
     * cũ: UPDATE trước, INSERT sau. Không {@code MERGE} — {@code MERGE} của SQL
     * Server không tự lấy khoá phù hợp.
     */
    public void upsertEnrollment(String maLopHP, String maSinhVien, String maCoSoNhaSV,
            String hoTen, String maYeuCau, LocalDateTime now) {
        int updated = jdbc.update("""
                UPDATE dbo.DangKyHocPhan
                   SET TrangThai = 'DA_DANG_KY', NgayDangKy = ?, MaCoSoNhaSV = ?,
                       HoTenSinhVien = ?, MaYeuCau = ?
                 WHERE MaLopHP = ? AND MaSinhVien = ? AND TrangThai IN ('DA_HUY', 'TU_CHOI')
                """, now, maCoSoNhaSV, hoTen, maYeuCau, maLopHP, maSinhVien);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO dbo.DangKyHocPhan
                           (MaLopHP, MaSinhVien, MaCoSoNhaSV, HoTenSinhVien, NgayDangKy,
                            TrangThai, MaYeuCau)
                    VALUES (?, ?, ?, ?, ?, 'DA_DANG_KY', ?)
                    """, maLopHP, maSinhVien, maCoSoNhaSV, hoTen, now, maYeuCau);
        }
    }

    public int cancelEnrollment(String maYeuCau) {
        return jdbc.update("""
                UPDATE dbo.DangKyHocPhan SET TrangThai = 'DA_HUY'
                 WHERE MaYeuCau = ? AND TrangThai = 'DA_DANG_KY'
                """, maYeuCau);
    }
}
