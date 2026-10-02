package vn.ptit.one.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Môn, kỳ và cơ sở không đổi được — cả ba nằm trong mã lớp. */
public record UpdateClassRequest(
        @Min(value = 1, message = "Sức chứa tối thiểu là 1.")
        @Max(value = 500, message = "Sức chứa tối đa là 500.") int soLuongToiDa,
        @NotBlank(message = "Chọn trạng thái lớp.") String trangThai,
        @NotBlank(message = "Chọn hình thức học.") String hinhThucHoc,
        boolean choPhepLienCoSo) {
}
