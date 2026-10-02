package vn.ptit.one.teacher.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTeacherRequest(
        // Mã giảng viên cũng là tên đăng nhập, nên chỉ chữ hoa và số.
        @NotBlank(message = "Nhập mã giảng viên.")
        @Pattern(regexp = "[A-Z0-9]{4,20}", message = "Mã giảng viên 4–20 ký tự, chỉ chữ hoa và số.") String maGiangVien,
        @NotBlank(message = "Nhập họ tên.") @Size(max = 150, message = "Họ tên quá dài.") String hoTen,
        @NotBlank(message = "Chọn cơ sở.") String maCoSo,
        @NotBlank(message = "Chọn khoa.") String maKhoa,
        @Size(max = 50, message = "Học vị quá dài.") String hocVi,
        // Tuỳ chọn. Có email và đã bật gửi thư thì mã kích hoạt chỉ đi qua thư.
        @Email(message = "Email không hợp lệ.") @Size(max = 254, message = "Email quá dài.") String email) {
}
