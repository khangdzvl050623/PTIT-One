package vn.ptit.one.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeAccountStatusRequest(
        @NotBlank(message = "Chọn trạng thái.")
        @Pattern(regexp = "HOAT_DONG|NGUNG", message = "Trạng thái chỉ nhận HOAT_DONG hoặc NGUNG.") String trangThai) {
}
