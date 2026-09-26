package vn.ptit.one.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank(message = "Nhập mã môn học.")
        @Size(max = 20, message = "Mã môn học tối đa 20 ký tự.") String maMonHoc,
        @NotBlank(message = "Nhập tên môn học.")
        @Size(max = 200, message = "Tên môn học quá dài.") String tenMonHoc,
        // Trùng CHECK trong V2: báo lỗi có tên trường thay vì để constraint nổ.
        @Min(value = 1, message = "Số tín chỉ tối thiểu là 1.")
        @Max(value = 15, message = "Số tín chỉ tối đa là 15.") int soTinChi,
        @NotBlank(message = "Chọn khoa.") String maKhoa) {
}
