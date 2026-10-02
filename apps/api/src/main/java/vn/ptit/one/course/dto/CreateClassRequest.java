package vn.ptit.one.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Không có trường cơ sở: cơ sở lấy từ JWT đã ký. Nhận {@code maCoSo} của client
 * là mở đường cho tài khoản cơ sở này mở lớp mang mã cơ sở khác.
 *
 * <p>Cũng không có {@code maLopHP}: server sinh từ môn + kỳ + cơ sở.
 */
public record CreateClassRequest(
        @NotBlank(message = "Chọn môn học.") String maMonHoc,
        @NotBlank(message = "Chọn học kỳ.") String maHocKy,
        @Min(value = 1, message = "Sức chứa tối thiểu là 1.")
        @Max(value = 500, message = "Sức chứa tối đa là 500.") int soLuongToiDa,
        @NotBlank(message = "Chọn hình thức học.") String hinhThucHoc,
        boolean choPhepLienCoSo,
        /** Bỏ trống thì tạo lớp chưa phân công, gán sau bằng endpoint riêng. */
        String maGiangVien) {
}
