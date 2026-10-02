package vn.ptit.one.enrollment.model;

/**
 * Kết quả một lần đăng ký.
 *
 * @param loaiDangKy {@code HOC_MOI} · {@code HOC_LAI} (đã trượt) · {@code CAI_THIEN}
 *                   (đã đạt, học để nâng điểm — tính điểm cao nhất)
 */
public record RegistrationResult(EnrolledCourse dangKy, String loaiDangKy) {

    public static final String HOC_MOI = "HOC_MOI";
    public static final String HOC_LAI = "HOC_LAI";
    public static final String CAI_THIEN = "CAI_THIEN";
}
