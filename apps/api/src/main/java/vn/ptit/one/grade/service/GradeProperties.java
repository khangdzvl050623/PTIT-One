package vn.ptit.one.grade.service;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình điểm. Giá trị mặc định lấy từ giả định trong thiết kế, chỉ dùng cho
 * demo khi chưa xác minh quy chế.
 *
 * @param trongSoChuyenCan trọng số điểm chuyên cần
 * @param trongSoGiuaKy    trọng số điểm giữa kỳ
 * @param trongSoCuoiKy    trọng số điểm cuối kỳ; ba trọng số phải cộng lại bằng 1
 * @param nguongDat        điểm tổng kết tối thiểu để đạt môn
 */
@ConfigurationProperties("ptitone.grade")
public record GradeProperties(
        @DefaultValue("0.1") BigDecimal trongSoChuyenCan,
        @DefaultValue("0.3") BigDecimal trongSoGiuaKy,
        @DefaultValue("0.6") BigDecimal trongSoCuoiKy,
        @DefaultValue("4.0") BigDecimal nguongDat) {
}
