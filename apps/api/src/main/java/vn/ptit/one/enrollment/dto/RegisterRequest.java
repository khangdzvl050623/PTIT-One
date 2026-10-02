package vn.ptit.one.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Chỉ gửi mã lớp. Mã sinh viên lấy từ phiên; điều kiện đủ do server tự kiểm. */
public record RegisterRequest(@NotBlank @Size(max = 80) String maLopHP) {
}
