package vn.ptit.one.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mã môn lấy từ đường dẫn, không cho đổi — nó là khoá và đã bị tham chiếu. */
public record UpdateCourseRequest(
        @NotBlank(message = "Nhập tên môn học.")
        @Size(max = 200, message = "Tên môn học quá dài.") String tenMonHoc,
        @Min(value = 1, message = "Số tín chỉ tối thiểu là 1.")
        @Max(value = 15, message = "Số tín chỉ tối đa là 15.") int soTinChi,
        @NotBlank(message = "Chọn khoa.") String maKhoa) {
}
