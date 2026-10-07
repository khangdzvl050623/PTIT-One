package vn.ptit.one.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lý do gỡ ghi danh — BẮT BUỘC.
 *
 * <p>Đây là thao tác admin làm trên dữ liệu của người khác, nên phải có vết.
 * Lý do đi thẳng vào thông báo gửi sinh viên, nên viết cho sinh viên đọc.
 */
public record RemoveEnrollmentRequest(
        @NotBlank(message = "Nhập lý do gỡ ghi danh.")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự.") String lyDo) {
}
