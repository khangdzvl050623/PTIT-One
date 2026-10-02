package vn.ptit.one.enrollment.model;

import java.util.List;

/**
 * Các môn sinh viên đang đăng ký trong học kỳ, kèm tín chỉ đã dùng.
 *
 * @param tranTinChi {@code null} khi sinh viên chưa đăng ký gì trong kỳ
 */
public record StudentEnrollments(
        String maHocKy,
        int soTinChiDaDangKy,
        Integer tranTinChi,
        List<EnrolledCourse> dangKy) {
}
