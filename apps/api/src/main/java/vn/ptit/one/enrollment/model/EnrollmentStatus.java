package vn.ptit.one.enrollment.model;

import java.util.List;

/**
 * Từ vựng trạng thái ghi danh — phải khớp từng ký tự với CHECK và filtered
 * index trong DB; ghi sai tên là ràng buộc im lặng không áp dụng.
 */
public final class EnrollmentStatus {

    public static final String DANG_XU_LY = "DANG_XU_LY";
    public static final String DA_DANG_KY = "DA_DANG_KY";
    public static final String TU_CHOI = "TU_CHOI";
    public static final String DANG_HUY = "DANG_HUY";
    public static final String DA_HUY = "DA_HUY";

    /**
     * Trạng thái còn giữ chỗ trong lớp — cùng tập với bộ đếm
     * {@code SoLuongDaDangKy} và với {@code UQ_DangKyMonHoc_SV_Ky_Mon}.
     */
    public static final List<String> GIU_CHO = List.of(DANG_XU_LY, DA_DANG_KY, DANG_HUY);

    private EnrollmentStatus() {
    }
}
