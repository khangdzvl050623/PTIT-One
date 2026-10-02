package vn.ptit.one.grade.policy;

import java.math.BigDecimal;

/**
 * Quy tắc điểm thuần, không phụ thuộc Spring/JDBC.
 *
 * <p>Ngưỡng đạt là GIẢ ĐỊNH của thiết kế ({@code DiemTongKet >= 4.0}), chưa đối
 * chiếu quy chế thật — nên nhận từ cấu hình, không viết cứng.
 */
public record GradePolicy(BigDecimal nguongDat) {

    public static final String DAT = "DAT";
    public static final String KHONG_DAT = "KHONG_DAT";

    /** {@code null} nghĩa là chưa có điểm — khác với 0 điểm, và khác trượt. */
    public String ketQua(BigDecimal diemTongKet) {
        if (diemTongKet == null) {
            return null;
        }
        return diemTongKet.compareTo(nguongDat) >= 0 ? DAT : KHONG_DAT;
    }
}
