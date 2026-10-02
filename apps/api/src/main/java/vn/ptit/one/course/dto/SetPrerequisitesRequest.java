package vn.ptit.one.course.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

/**
 * Thay TOÀN BỘ tập tiên quyết. Danh sách rỗng nghĩa là gỡ hết — đó là thao tác
 * hợp lệ, không phải thiếu dữ liệu.
 *
 * <p>Nhiều tiên quyết nghĩa là phải đạt TẤT CẢ. Điều kiện "hoặc" nằm ngoài bản đầu.
 */
public record SetPrerequisitesRequest(
        @Size(max = 20, message = "Tối đa 20 môn tiên quyết.") List<String> tienQuyet) {
}
