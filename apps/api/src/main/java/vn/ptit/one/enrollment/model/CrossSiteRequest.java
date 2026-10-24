package vn.ptit.one.enrollment.model;

import java.time.LocalDateTime;
import java.util.List;

import vn.ptit.one.timetable.model.ScheduleSlot;

/**
 * Một yêu cầu học liên cơ sở, như Home nhìn thấy nó.
 *
 * <p>Bản ghi này sống ở <b>Home</b> và mang theo một <b>ảnh chụp</b> thông tin
 * lớp của Host ({@code tenMonHoc}, {@code soTinChi}, {@code lichHoc}…). Ảnh chụp
 * là có chủ đích, không phải dữ liệu trùng lặp do cẩu thả: sinh viên phải xem
 * được yêu cầu của mình <em>khi Host đang tắt</em>, và màn "đăng ký của tôi"
 * không được biến thành một lượt truy vấn chéo site cho mỗi dòng.
 *
 * @param phienBanLich phiên bản lịch lúc Home kiểm trùng giờ. Host so lại ở
 *                     bước 2 — lịch đổi giữa chừng thì phép kiểm vừa rồi vô
 *                     nghĩa, và yêu cầu phải bị từ chối thay vì đi tiếp
 * @param lyDoTuChoi   chỉ có nghĩa khi {@code trangThai} là {@code TU_CHOI};
 *                     lý do do Host sinh ra, Home chỉ lưu lại để hiển thị
 */
public record CrossSiteRequest(
        String maYeuCau,
        String maSinhVien,
        String maLopHP,
        String maCoSoHost,
        String trangThai,
        String lyDoTuChoi,
        String maHocKy,
        String maMonHoc,
        String tenMonHoc,
        int soTinChi,
        String hinhThucHoc,
        String phongHoc,
        int phienBanLich,
        List<ScheduleSlot> lichHoc,
        LocalDateTime thoiDiemDongBo) {

    /**
     * Tín chỉ của yêu cầu này còn đang bị giữ trong hạn mức của sinh viên.
     *
     * <p>⚠️ {@code DANG_HUY} nằm trong tập này. Sinh viên bấm huỷ <b>không</b>
     * phải thời điểm trả tín chỉ — hạn mức chỉ được hoàn khi Host xác nhận. Trả
     * sớm tạo cửa sổ hai bên bất đồng: Home tưởng đã huỷ, Host vẫn giữ chỗ.
     */
    public boolean dangGiuTinChi() {
        return EnrollmentStatus.DANG_XU_LY.equals(trangThai)
                || EnrollmentStatus.DANG_HUY.equals(trangThai);
    }

    /** Đã có kết luận dứt khoát; không còn chờ Host trả lời điều gì. */
    public boolean daKetThuc() {
        return EnrollmentStatus.TU_CHOI.equals(trangThai)
                || EnrollmentStatus.DA_HUY.equals(trangThai);
    }
}
