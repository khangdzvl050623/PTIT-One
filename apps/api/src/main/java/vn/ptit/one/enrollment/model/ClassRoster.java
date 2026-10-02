package vn.ptit.one.enrollment.model;

import java.util.List;

import vn.ptit.one.course.model.ClassSection;

/**
 * Danh sách sinh viên của lớp (F05).
 *
 * <p>{@code lop.soLuongDaDangKy} là bộ đếm; {@code sinhVien.size()} là số dòng
 * ghi danh còn giữ chỗ. Hai số này phải bằng nhau — trả cả hai để UI và người
 * nghiệm thu đối soát được, thay vì chỉ tin bộ đếm.
 */
public record ClassRoster(ClassSection lop, List<RosterEntry> sinhVien) {
}
