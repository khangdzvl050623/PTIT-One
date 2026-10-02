package vn.ptit.one.grade.service;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình điểm. Giá trị mặc định lấy từ giả định trong thiết kế, chỉ dùng cho
 * demo khi chưa xác minh quy chế.
 *
 * @param nguongDat điểm tổng kết tối thiểu để đạt môn
 */
@ConfigurationProperties("ptitone.grade")
public record GradeProperties(@DefaultValue("4.0") BigDecimal nguongDat) {
}
