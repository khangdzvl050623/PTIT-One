package vn.ptit.one.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu học một lớp ở cơ sở khác.
 *
 * @param maCoSoHost cơ sở đang mở lớp — chỉ quyết định đọc lớp ở database nào.
 *                   Cơ sở NHÀ của sinh viên luôn lấy từ JWT đã ký
 */
public record CrossSiteRequestBody(
        @NotBlank(message = "Chọn cơ sở.") @Size(max = 10) String maCoSoHost,
        @NotBlank(message = "Chọn lớp học phần.") @Size(max = 80) String maLopHP) {
}
