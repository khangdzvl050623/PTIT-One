package vn.ptit.one.enrollment.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình đăng ký học phần.
 *
 * @param tranTinChi trần tín chỉ mỗi học kỳ, ghi vào {@code SinhVienHocKy} lúc tạo
 *                   dòng. 24 là GIẢ ĐỊNH của thiết kế, chưa đối chiếu quy chế
 */
@ConfigurationProperties("ptitone.enrollment")
public record EnrollmentProperties(@DefaultValue("24") int tranTinChi) {
}
