package vn.ptit.one.course.model;

/** Một môn học ở mức danh sách. Kèm tên khoa để không phải gọi thêm lượt nữa. */
public record CourseSummary(
        String maMonHoc,
        String tenMonHoc,
        int soTinChi,
        String maKhoa,
        String tenKhoa) {
}
