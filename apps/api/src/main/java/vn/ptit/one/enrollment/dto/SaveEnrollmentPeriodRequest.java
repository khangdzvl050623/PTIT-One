package vn.ptit.one.enrollment.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Không có trường cơ sở: lấy từ JWT đã ký. Mã đợt do server sinh.
 * Khi sửa thì {@code maHocKy} lấy từ đợt sẵn có, không đổi được.
 */
public record SaveEnrollmentPeriodRequest(
        String maHocKy,
        @NotNull(message = "Nhập thời gian mở.") Instant thoiGianMo,
        @NotNull(message = "Nhập thời gian đóng.") Instant thoiGianDong,
        @NotBlank(message = "Chọn trạng thái đợt.") String trangThai) {
}
