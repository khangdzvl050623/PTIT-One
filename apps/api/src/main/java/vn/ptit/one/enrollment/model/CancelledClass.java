package vn.ptit.one.enrollment.model;

import vn.ptit.one.course.model.ClassSection;

/**
 * Kết quả huỷ lớp.
 *
 * @param soDangKyDaHuy số ghi danh vừa huỷ; {@code 0} khi lớp đã huỷ từ trước
 */
public record CancelledClass(ClassSection lop, int soDangKyDaHuy) {
}
