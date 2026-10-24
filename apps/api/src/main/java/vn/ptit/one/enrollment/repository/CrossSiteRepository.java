package vn.ptit.one.enrollment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.enrollment.model.CrossSiteRequest;
import vn.ptit.one.timetable.model.ScheduleSlot;

/**
 * Phía <b>Home</b> của saga đăng ký liên cơ sở: {@code YeuCauHocLienCoSo},
 * {@code LichHocMirror} và hai bộ đếm tín chỉ trên {@code SinhVienHocKy}.
 *
 * <p>Home bảo vệ <b>ràng buộc theo sinh viên</b> — trần tín chỉ theo kỳ, trùng
 * môn, trùng lịch, tiên quyết. Ràng buộc theo <em>lớp</em> (sức chứa, đợt đăng
 * ký) là việc của Host, xem {@link HostRequestRepository}. Không gom về một chỗ
 * được vì không CSDL nào nhìn thấy đủ cả hai phía: Home không biết lớp của Host
 * còn mấy chỗ, Host không biết sinh viên đã đăng ký gì ở nơi khác.
 *
 * <p><b>{@code MaYeuCau} đi qua JDBC dưới dạng {@code String}.</b>
 * {@code uniqueidentifier} có độ ưu tiên kiểu CAO HƠN {@code nvarchar}, nên
 * SQL Server chuyển tham số chuỗi sang {@code uniqueidentifier} chứ không chuyển
 * cột sang chuỗi — phép so vẫn dùng được chỉ mục. Đổi chiều ép kiểu là mất chỉ
 * mục một cách âm thầm.
 */
@Repository
@Profile("central")
public class CrossSiteRepository {

    private final JdbcTemplate jdbc;

    public CrossSiteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- Bộ đếm tín chỉ: hai cột, chuyển qua lại -----------------------------

    /*
     * Bảng chuyển tín chỉ (thiết kế D3) — tổng chiếm KHÔNG đổi khi sinh viên vừa
     * bấm huỷ, chỉ đổi khi Host trả lời:
     *
     *   Gửi yêu cầu          DangGiuCho +c
     *   Host chấp nhận       DaDangKy   +c   DangGiuCho -c
     *   Host từ chối                         DangGiuCho -c
     *   SV huỷ (đã duyệt)    DaDangKy   -c   DangGiuCho +c
     *   Host xác nhận huỷ                    DangGiuCho -c
     *
     * Mọi câu dưới đây đặt điều kiện NGAY TRONG UPDATE rồi chỗ gọi đọc số dòng.
     * `SELECT` rồi `IF` là race condition, và ở đây còn tệ hơn: giữa hai câu đó
     * là một round-trip sang site khác.
     */

    /** @return 0 nếu giữ thêm sẽ vượt trần — điều kiện nằm trong chính câu UPDATE */
    public int holdCredits(String maSinhVien, String maHocKy, int soTinChi) {
        return jdbc.update("""
                UPDATE dbo.SinhVienHocKy SET SoTinChiDangGiuCho = SoTinChiDangGiuCho + ?
                 WHERE MaSinhVien = ? AND MaHocKy = ?
                   AND SoTinChiDaDangKy + SoTinChiDangGiuCho + ? <= TranTinChi
                """, soTinChi, maSinhVien, maHocKy, soTinChi);
    }

    /** Trả hạn mức đang giữ. Dùng khi Host từ chối hoặc xác nhận huỷ. */
    public int releaseHeld(String maSinhVien, String maHocKy, int soTinChi) {
        return jdbc.update("""
                UPDATE dbo.SinhVienHocKy SET SoTinChiDangGiuCho = SoTinChiDangGiuCho - ?
                 WHERE MaSinhVien = ? AND MaHocKy = ? AND SoTinChiDangGiuCho >= ?
                """, soTinChi, maSinhVien, maHocKy, soTinChi);
    }

    /** Host chấp nhận: tín chỉ chuyển từ "đang giữ" sang "đã dùng", tổng không đổi. */
    public int heldToUsed(String maSinhVien, String maHocKy, int soTinChi) {
        return jdbc.update("""
                UPDATE dbo.SinhVienHocKy
                   SET SoTinChiDaDangKy   = SoTinChiDaDangKy + ?,
                       SoTinChiDangGiuCho = SoTinChiDangGiuCho - ?
                 WHERE MaSinhVien = ? AND MaHocKy = ? AND SoTinChiDangGiuCho >= ?
                """, soTinChi, soTinChi, maSinhVien, maHocKy, soTinChi);
    }

    /** Sinh viên huỷ một đăng ký ĐÃ duyệt: quay về "đang giữ" cho tới khi Host xác nhận. */
    public int usedToHeld(String maSinhVien, String maHocKy, int soTinChi) {
        return jdbc.update("""
                UPDATE dbo.SinhVienHocKy
                   SET SoTinChiDaDangKy   = SoTinChiDaDangKy - ?,
                       SoTinChiDangGiuCho = SoTinChiDangGiuCho + ?
                 WHERE MaSinhVien = ? AND MaHocKy = ? AND SoTinChiDaDangKy >= ?
                """, soTinChi, soTinChi, maSinhVien, maHocKy, soTinChi);
    }

    // --- YeuCauHocLienCoSo --------------------------------------------------

    public void insertRequest(CrossSiteRequest yeuCau, String lichHocJson, LocalDateTime now) {
        jdbc.update("""
                INSERT INTO dbo.YeuCauHocLienCoSo
                       (MaYeuCau, MaSinhVien, MaLopHP, MaCoSoHost, TrangThai, MaMonHoc,
                        TenMonHoc, SoTinChi, HinhThucHoc, LichHocJson, PhongHoc, ThoiDiemDongBo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, yeuCau.maYeuCau(), yeuCau.maSinhVien(), yeuCau.maLopHP(), yeuCau.maCoSoHost(),
                yeuCau.trangThai(), yeuCau.maMonHoc(), yeuCau.tenMonHoc(), yeuCau.soTinChi(),
                yeuCau.hinhThucHoc(), lichHocJson, yeuCau.phongHoc(), now);
    }

    /**
     * Chuyển trạng thái yêu cầu, <b>chỉ khi</b> đang ở đúng trạng thái nguồn.
     *
     * <p>⚠️ Mệnh đề {@code AND TrangThai = <nguồn>} là thứ chống phản hồi đến
     * muộn ghi đè: câu trả lời "đã chấp nhận" có thể về SAU khi sinh viên đã bấm
     * huỷ, và nó không được phép ghi đè {@code DANG_HUY}. Nó cũng là thứ canh để
     * bộ đếm tín chỉ chỉ bị đụng <b>đúng một lần</b> — chỉ luồng thắng cuộc thấy
     * {@code @@ROWCOUNT = 1}.
     *
     * @return 1 nếu chuyển được, 0 nếu trạng thái đã khác (xử lý lặp, bỏ qua)
     */
    public int moveRequest(String maYeuCau, String tuTrangThai, String denTrangThai, String lyDo) {
        return jdbc.update("""
                UPDATE dbo.YeuCauHocLienCoSo
                   SET TrangThai = ?, LyDoTuChoi = ?, SoLanThu = SoLanThu + 1
                 WHERE MaYeuCau = ? AND TrangThai = ?
                """, denTrangThai, lyDo, maYeuCau, tuTrangThai);
    }

    public Optional<CrossSiteRequest> findRequest(String maYeuCau) {
        return jdbc.query(SELECT_REQUEST + " WHERE y.MaYeuCau = ?", CrossSiteRepository::mapRequest,
                maYeuCau).stream().findFirst();
    }

    /** Yêu cầu của một sinh viên trong một học kỳ, mới nhất trước. */
    public List<CrossSiteRequest> listRequests(String maSinhVien, String maHocKy) {
        return jdbc.query(SELECT_REQUEST + """
                 WHERE y.MaSinhVien = ? AND k.MaHocKy = ?
                 ORDER BY y.ThoiDiemDongBo DESC
                """, CrossSiteRepository::mapRequest, maSinhVien, maHocKy);
    }

    /*
     * Học kỳ và phiên bản lịch đọc từ `DangKyMonHoc` chứ không nhân bản thêm vào
     * `YeuCauHocLienCoSo`: hai bảng được ghi trong CÙNG một giao dịch Home, nên
     * join là rẻ và không có cửa sổ lệch. `LEFT JOIN` vì dòng môn học có thể đã
     * bị xoá trong tương lai mà yêu cầu vẫn còn làm lịch sử.
     */
    private static final String SELECT_REQUEST = """
            SELECT y.MaYeuCau, y.MaSinhVien, y.MaLopHP, y.MaCoSoHost, y.TrangThai,
                   y.LyDoTuChoi, y.MaMonHoc, y.TenMonHoc, y.SoTinChi, y.HinhThucHoc,
                   y.PhongHoc, y.LichHocJson, y.ThoiDiemDongBo,
                   k.MaHocKy, k.PhienBanLich
              FROM dbo.YeuCauHocLienCoSo y
              LEFT JOIN dbo.DangKyMonHoc k ON k.MaYeuCau = y.MaYeuCau
            """;

    // --- DangKyMonHoc mang MaYeuCau -----------------------------------------

    /**
     * Giữ quyền đăng ký môn ngay khi ghi nhận yêu cầu, ở trạng thái
     * {@code DANG_XU_LY}.
     *
     * <p>⚠️ Dòng này là thứ làm unique filtered index trên
     * {@code (MaSinhVien, MaHocKy, MaMonHoc)} chặn được "hai lớp khác nhau của
     * cùng một môn". Đợi Host duyệt rồi mới ghi thì hai yêu cầu song song cho
     * hai lớp của cùng môn sẽ cùng lọt qua.
     */
    public void insertPendingCourse(String maSinhVien, String maHocKy, String maMonHoc,
            String maLopHP, String maCoSoHost, int soTinChi, int phienBanLich, String maYeuCau,
            String trangThai) {
        int updated = jdbc.update("""
                UPDATE dbo.DangKyMonHoc
                   SET MaLopHP = ?, MaCoSoHost = ?, TrangThai = ?, MaYeuCau = ?,
                       SoTinChi = ?, PhienBanLich = ?
                 WHERE MaSinhVien = ? AND MaHocKy = ? AND MaMonHoc = ?
                   AND TrangThai IN ('DA_HUY', 'TU_CHOI')
                """, maLopHP, maCoSoHost, trangThai, maYeuCau, soTinChi, phienBanLich,
                maSinhVien, maHocKy, maMonHoc);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO dbo.DangKyMonHoc
                           (MaSinhVien, MaHocKy, MaMonHoc, MaLopHP, MaCoSoHost, TrangThai,
                            MaYeuCau, SoTinChi, PhienBanLich)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, maSinhVien, maHocKy, maMonHoc, maLopHP, maCoSoHost, trangThai,
                    maYeuCau, soTinChi, phienBanLich);
        }
    }

    public int moveCourse(String maYeuCau, String tuTrangThai, String denTrangThai) {
        return jdbc.update("""
                UPDATE dbo.DangKyMonHoc SET TrangThai = ?
                 WHERE MaYeuCau = ? AND TrangThai = ?
                """, denTrangThai, maYeuCau, tuTrangThai);
    }

    public int bumpCrossSiteCount(String maSinhVien, int delta) {
        return jdbc.update("""
                UPDATE dbo.SinhVien SET SoMonLienCoSo = SoMonLienCoSo + ?
                 WHERE MaSinhVien = ? AND SoMonLienCoSo + ? >= 0
                """, delta, maSinhVien, delta);
    }

    // --- LichHocMirror ------------------------------------------------------

    /**
     * Lịch lớp của Host, lưu tại Home <b>ngay khi ghi nhận yêu cầu</b> — không
     * đợi Host duyệt.
     *
     * <p>⚠️ Đợi duyệt mới lưu là một lỗ thật: hai yêu cầu {@code DANG_XU_LY}
     * cùng khung giờ sẽ cùng lọt, vì lúc kiểm trùng lịch cho yêu cầu thứ hai
     * chưa có gì ở Home nói rằng khung giờ đó đã bị yêu cầu thứ nhất chiếm.
     */
    public void insertMirrorSlot(String maSinhVien, String maHocKy, String maMonHoc,
            ScheduleSlot slot, int phienBanLich) {
        jdbc.update("""
                INSERT INTO dbo.LichHocMirror
                       (MaSinhVien, MaHocKy, MaMonHoc, Thu, TietBatDau, SoTiet, PhongHoc,
                        TuanBatDau, TuanKetThuc, PhienBanLich, SyncStatus)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCED')
                """, maSinhVien, maHocKy, maMonHoc, slot.thu(), slot.tietBatDau(), slot.soTiet(),
                slot.phongHoc(), slot.tuanBatDau(), slot.tuanKetThuc(), phienBanLich);
    }

    public int deleteMirrorSlots(String maSinhVien, String maHocKy, String maMonHoc) {
        return jdbc.update("""
                DELETE FROM dbo.LichHocMirror
                 WHERE MaSinhVien = ? AND MaHocKy = ? AND MaMonHoc = ?
                """, maSinhVien, maHocKy, maMonHoc);
    }

    /**
     * Lịch các lớp liên cơ sở sinh viên đang giữ trong kỳ.
     *
     * <p>Không trùng với lịch lớp cùng cơ sở: mirror CHỈ có dòng cho lớp của site
     * khác, còn lớp tại chỗ đọc thẳng từ {@code LichHoc}. Hai nguồn bù nhau,
     * không chồng nhau — nên gộp hai danh sách rồi so là đủ, không phải lọc trùng.
     */
    public List<ScheduleSlot> mirrorSlots(String maSinhVien, String maHocKy) {
        return jdbc.query("""
                SELECT Thu, TietBatDau, SoTiet, PhongHoc, TuanBatDau, TuanKetThuc
                  FROM dbo.LichHocMirror
                 WHERE MaSinhVien = ? AND MaHocKy = ?
                """, (rs, rowNum) -> new ScheduleSlot(
                        rs.getInt("Thu"), rs.getInt("TietBatDau"), rs.getInt("SoTiet"),
                        rs.getString("PhongHoc"), rs.getInt("TuanBatDau"), rs.getInt("TuanKetThuc")),
                maSinhVien, maHocKy);
    }

    private static CrossSiteRequest mapRequest(java.sql.ResultSet rs, int rowNum)
            throws java.sql.SQLException {
        return new CrossSiteRequest(
                rs.getString("MaYeuCau"),
                rs.getString("MaSinhVien"),
                rs.getString("MaLopHP"),
                rs.getString("MaCoSoHost"),
                rs.getString("TrangThai"),
                rs.getString("LyDoTuChoi"),
                rs.getString("MaHocKy"),
                rs.getString("MaMonHoc"),
                rs.getString("TenMonHoc"),
                rs.getInt("SoTinChi"),
                rs.getString("HinhThucHoc"),
                rs.getString("PhongHoc"),
                rs.getInt("PhienBanLich"),
                List.of(),
                rs.getObject("ThoiDiemDongBo", LocalDateTime.class));
    }

    /** Chuỗi JSON lịch học thô của một yêu cầu — service tự đọc ra {@link ScheduleSlot}. */
    public Optional<String> scheduleJson(String maYeuCau) {
        return jdbc.queryForList("SELECT LichHocJson FROM dbo.YeuCauHocLienCoSo WHERE MaYeuCau = ?",
                String.class, maYeuCau).stream().findFirst();
    }
}
