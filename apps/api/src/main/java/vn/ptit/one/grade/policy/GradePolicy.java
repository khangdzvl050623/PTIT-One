package vn.ptit.one.grade.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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

    /**
     * Bậc quy đổi: từ {@code tu} điểm thang 10 trở lên thì được {@code diemChu}
     * và {@code diemHe4}. Xét từ trên xuống nên phải sắp giảm dần theo {@code tu}.
     */
    public record Bac(BigDecimal tu, String diemChu, BigDecimal diemHe4) {
    }

    /** Mức xếp loại: từ {@code tu} điểm trung bình hệ 4 trở lên thì mang {@code ten}. */
    public record Muc(BigDecimal tu, String ten) {
    }

    /**
     * ⚠️ GIẢ ĐỊNH như trọng số và ngưỡng đạt — theo thang tín chỉ thường dùng,
     * CHƯA đối chiếu quy chế PTIT. Đổi quy chế thì sửa đúng hai bảng này; đây là
     * chỗ duy nhất quy đổi điểm, giao diện không được tự quy đổi lần nữa.
     */
    public static final List<Bac> THANG_CHU = List.of(
            new Bac(new BigDecimal("9.0"), "A+", new BigDecimal("4.0")),
            new Bac(new BigDecimal("8.5"), "A", new BigDecimal("3.7")),
            new Bac(new BigDecimal("8.0"), "B+", new BigDecimal("3.5")),
            new Bac(new BigDecimal("7.0"), "B", new BigDecimal("3.0")),
            new Bac(new BigDecimal("6.5"), "C+", new BigDecimal("2.5")),
            new Bac(new BigDecimal("5.5"), "C", new BigDecimal("2.0")),
            new Bac(new BigDecimal("5.0"), "D+", new BigDecimal("1.5")),
            new Bac(new BigDecimal("4.0"), "D", new BigDecimal("1.0")),
            new Bac(BigDecimal.ZERO, "F", BigDecimal.ZERO));

    public static final List<Muc> XEP_LOAI = List.of(
            new Muc(new BigDecimal("3.6"), "Xuất sắc"),
            new Muc(new BigDecimal("3.2"), "Giỏi"),
            new Muc(new BigDecimal("2.5"), "Khá"),
            new Muc(new BigDecimal("2.0"), "Trung bình"),
            new Muc(new BigDecimal("1.0"), "Yếu"),
            new Muc(BigDecimal.ZERO, "Kém"));

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

    public boolean dat(BigDecimal diemTongKet) {
        return diemTongKet != null && diemTongKet.compareTo(nguongDat) >= 0;
    }

    private static Bac bac(BigDecimal diem10) {
        return THANG_CHU.stream()
                .filter(b -> diem10.compareTo(b.tu()) >= 0)
                .findFirst()
                .orElse(THANG_CHU.get(THANG_CHU.size() - 1));
    }

    /** Điểm chữ (A+ … F); {@code null} khi chưa có điểm. */
    public String diemChu(BigDecimal diem10) {
        return diem10 == null ? null : bac(diem10).diemChu();
    }

    /** Điểm thang 4; {@code null} khi chưa có điểm. */
    public BigDecimal diemHe4(BigDecimal diem10) {
        return diem10 == null ? null : bac(diem10).diemHe4();
    }

    /** Xếp loại theo điểm trung bình hệ 4; {@code null} khi chưa có điểm nào. */
    public String xepLoai(BigDecimal trungBinhHe4) {
        if (trungBinhHe4 == null) {
            return null;
        }
        return XEP_LOAI.stream()
                .filter(m -> trungBinhHe4.compareTo(m.tu()) >= 0)
                .findFirst()
                .orElse(XEP_LOAI.get(XEP_LOAI.size() - 1))
                .ten();
    }

    /**
     * Trung bình có trọng số theo tín chỉ, làm tròn 2 chữ số.
     *
     * <p>{@code null} khi tổng tín chỉ bằng 0 — KHÔNG trả 0, vì "chưa có môn nào"
     * khác "trung bình 0 điểm".
     *
     * @param diemTheoTinChi từng cặp (điểm, số tín chỉ)
     */
    public BigDecimal trungBinh(List<TinChiDiem> diemTheoTinChi) {
        int tongTinChi = diemTheoTinChi.stream().mapToInt(TinChiDiem::soTinChi).sum();
        if (tongTinChi == 0) {
            return null;
        }
        BigDecimal tong = diemTheoTinChi.stream()
                .map(d -> d.diem().multiply(BigDecimal.valueOf(d.soTinChi())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return tong.divide(BigDecimal.valueOf(tongTinChi), 2, RoundingMode.HALF_UP);
    }

    /** Một môn đã có điểm, dùng để tính trung bình. */
    public record TinChiDiem(BigDecimal diem, int soTinChi) {
    }
}
