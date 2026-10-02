/**
 * Hồ sơ giảng viên.
 *
 * <p>Sở hữu bảng {@code GiangVien}. Tách khỏi {@code student} vì đó là hồ sơ
 * sinh viên — hai thực thể khác nhau, quyền khác nhau trong B3, dù ở Phần 2
 * cả hai đều được phân mảnh ngang theo cơ sở.
 *
 * <p>Module khác lấy dữ liệu qua {@code TeacherDirectory}, không đụng repository.
 *
 * <p>Đã có: tra cứu giảng viên cho việc phân công lớp (F04); Admin Master cấp
 * hồ sơ kèm tài khoản chưa kích hoạt (F02). Chưa có: sửa hồ sơ.
 */
package vn.ptit.one.teacher;
