package vn.ptit.one.grade.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Quy tắc điểm thuần, không phụ thuộc Spring/JDBC.
 *
 * <p>Trọng số và ngưỡng đạt là GIẢ ĐỊNH của thiết kế ({@code 0.1·CC + 0.3·GK +
 * 0.6·CK}, đạt khi {@code >= 4.0}), chưa đối chiếu quy chế thật — nên nhận từ
 * cấu hình, không viết cứng.
 */
public record GradePolicy(
        BigDecimal trongSoChuyenCan,
        BigDecimal trongSoGiuaKy,
        BigDecimal trongSoCuoiKy,
        BigDecimal nguongDat) {

    public static final String DAT = "DAT";
    public static final String KHONG_DAT = "KHONG_DAT";

    public GradePolicy {
        BigDecimal tong = trongSoChuyenCan.add(trongSoGiuaKy).add(trongSoCuoiKy);
        if (tong.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Tổng trọng số điểm phải bằng 1, đang là " + tong);
        }
    }

    /**
     * Điểm tổng kết làm tròn 1 chữ số. Thiếu bất kỳ điểm thành phần nào thì
     * {@code null} — không coi điểm thiếu là 0.
     */
    public BigDecimal tongKet(BigDecimal chuyenCan, BigDecimal giuaKy, BigDecimal cuoiKy) {
        if (chuyenCan == null || giuaKy == null || cuoiKy == null) {
            return null;
        }
        return chuyenCan.multiply(trongSoChuyenCan)
                .add(giuaKy.multiply(trongSoGiuaKy))
                .add(cuoiKy.multiply(trongSoCuoiKy))
                .setScale(1, RoundingMode.HALF_UP);
    }

    /** {@code null} nghĩa là chưa có điểm — khác với 0 điểm, và khác trượt. */
    public String ketQua(BigDecimal diemTongKet) {
        if (diemTongKet == null) {
            return null;
        }
        return diemTongKet.compareTo(nguongDat) >= 0 ? DAT : KHONG_DAT;
    }
}
