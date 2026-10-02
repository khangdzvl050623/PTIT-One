package vn.ptit.one.enrollment.model;

import java.time.Instant;

/** Một môn sinh viên đang giữ chỗ trong học kỳ. */
public record EnrolledCourse(
        String maLopHP,
        String maMonHoc,
        String tenMonHoc,
        int soTinChi,
        String trangThai,
        Instant ngayDangKy) {
}
