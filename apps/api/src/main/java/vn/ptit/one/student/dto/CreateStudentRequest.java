package vn.ptit.one.student.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
        // Mã sinh viên cũng là tên đăng nhập, nên chỉ chữ hoa và số.
        @NotBlank(message = "Nhập mã sinh viên.")
        @Pattern(regexp = "[A-Z0-9]{4,20}", message = "Mã sinh viên 4–20 ký tự, chỉ chữ hoa và số.") String maSinhVien,
        @NotBlank(message = "Nhập họ tên.") @Size(max = 150, message = "Họ tên quá dài.") String hoTen,
        @Past(message = "Ngày sinh phải ở quá khứ.") LocalDate ngaySinh,
        @NotBlank(message = "Chọn cơ sở nhà.") String maCoSoNha,
        @NotBlank(message = "Chọn chương trình đào tạo.") String maCTDT,
        // Tuỳ chọn. Có email và đã bật gửi thư thì mã kích hoạt chỉ đi qua thư.
        @Email(message = "Email không hợp lệ.") @Size(max = 254, message = "Email quá dài.") String email) {
}
