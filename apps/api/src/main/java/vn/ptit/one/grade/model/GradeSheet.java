package vn.ptit.one.grade.model;

import java.util.List;

import vn.ptit.one.course.model.ClassSection;

/**
 * Bảng điểm của một lớp (F06).
 *
 * @param trangThai {@code NHAP} khi còn dòng chưa công bố, {@code DA_CONG_BO} khi
 *                  mọi dòng đã công bố, {@code DA_KHOA} khi lớp đã khoá điểm
 */
public record GradeSheet(ClassSection lop, String trangThai, List<GradeEntry> diem) {

    public static final String NHAP = "NHAP";
    public static final String DA_CONG_BO = "DA_CONG_BO";
    public static final String DA_KHOA = "DA_KHOA";
}
