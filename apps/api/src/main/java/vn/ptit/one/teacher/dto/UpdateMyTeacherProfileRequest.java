package vn.ptit.one.teacher.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Giảng viên tự điền phần LÝ LỊCH của hồ sơ mình.
 *
 * <p>Cùng bộ ô với {@code UpdateMyProfileRequest} của sinh viên, cùng lý do cho
 * từng ràng buộc — xem ghi chú ở lớp đó, kể cả nhánh "rỗng hoặc khoảng trắng"
 * mở đầu mỗi vị từ. Hai biểu mẫu giống nhau nên giao diện dùng chung một hộp
 * thoại.
 *
 * <p>Không có {@code hoTen}, {@code hocVi}, {@code maKhoa}, {@code maCoSo}:
 * dữ liệu hành chính do Phòng Đào tạo quản. Không có {@code anhDaiDien}: ảnh
 * đặt bằng {@code POST /api/me/teacher-profile/avatar}.
 */
public record UpdateMyTeacherProfileRequest(
        @Pattern(regexp = "\\s*|NAM|NU", message = "Giới tính chỉ nhận NAM hoặc NU.") String gioiTinh,
        @Pattern(regexp = "\\s*|[0-9 +().-]{8,20}", message = "Số điện thoại không hợp lệ.")
        String dienThoai,
        @Pattern(regexp = "\\s*|[0-9]{9}|[0-9]{12}", message = "Số CCCD phải là 9 hoặc 12 chữ số.")
        String soCCCD,
        @Pattern(regexp = "\\s*|[^\\s@]+@[^\\s@]+\\.[^\\s@]+", message = "Email cá nhân không hợp lệ.")
        @Size(max = 254, message = "Email cá nhân quá dài.") String emailCaNhan,
        @Size(max = 150, message = "Nơi sinh quá dài.") String noiSinh,
        @Size(max = 50, message = "Dân tộc quá dài.") String danToc,
        @Size(max = 50, message = "Tôn giáo quá dài.") String tonGiao,
        @Size(max = 255, message = "Hộ khẩu quá dài.") String hoKhau) {
}
