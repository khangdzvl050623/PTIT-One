package vn.ptit.one.enrollment.model;

import java.time.Instant;

/**
 * Đợt đăng ký của một cơ sở trong một học kỳ.
 *
 * <p>"Đang mở" cần CẢ HAI: {@code trangThai = DANG_MO} và thời điểm hiện tại
 * nằm trong [{@code thoiGianMo}, {@code thoiGianDong}] — quyết định nhóm
 * 02/10/2026. Chỉ xem trạng thái là bỏ sót đợt đã hết giờ mà quên đóng.
 */
public record EnrollmentPeriod(
        String maDot,
        String maHocKy,
        String maCoSo,
        Instant thoiGianMo,
        Instant thoiGianDong,
        String trangThai) {

    public static final String CHUA_MO = "CHUA_MO";
    public static final String DANG_MO = "DANG_MO";
    public static final String DA_DONG = "DA_DONG";

    public boolean dangMo(Instant now) {
        return DANG_MO.equals(trangThai)
                && !now.isBefore(thoiGianMo)
                && !now.isAfter(thoiGianDong);
    }
}
