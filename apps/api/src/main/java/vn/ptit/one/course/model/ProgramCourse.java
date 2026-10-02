package vn.ptit.one.course.model;

/**
 * Một môn trong chương trình đào tạo.
 *
 * @param hocKyGoiY học kỳ thứ mấy trong lộ trình; {@code null} nếu không gợi ý
 * @param batBuoc   {@code false} là môn tự chọn
 */
public record ProgramCourse(
        String maMonHoc,
        String tenMonHoc,
        int soTinChi,
        Integer hocKyGoiY,
        boolean batBuoc) {
}
