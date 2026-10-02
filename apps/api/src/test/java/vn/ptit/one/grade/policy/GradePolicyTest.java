package vn.ptit.one.grade.policy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GradePolicyTest {

    private final GradePolicy policy = new GradePolicy(
            new BigDecimal("0.1"), new BigDecimal("0.3"), new BigDecimal("0.6"), new BigDecimal("4.0"));

    /** Ngưỡng là "lớn hơn hoặc bằng": đúng 4.0 là đạt. */
    @Test
    void dungNguongLaDat() {
        assertThat(policy.ketQua(new BigDecimal("4.0"))).isEqualTo(GradePolicy.DAT);
        assertThat(policy.ketQua(new BigDecimal("4.00"))).isEqualTo(GradePolicy.DAT);
        assertThat(policy.ketQua(new BigDecimal("3.9"))).isEqualTo(GradePolicy.KHONG_DAT);
    }

    /** Chưa có điểm khác 0 điểm: 0 là trượt, null là chưa có kết quả. */
    @Test
    void chuaCoDiemKhacKhongDiem() {
        assertThat(policy.ketQua(null)).isNull();
        assertThat(policy.ketQua(BigDecimal.ZERO)).isEqualTo(GradePolicy.KHONG_DAT);
    }

    /** 0.9 + 2.25 + 4.8 = 7.95 → làm tròn nửa lên thành 8.0. */
    @Test
    void tongKetLamTronMotChuSo() {
        assertThat(policy.tongKet(new BigDecimal("9.0"), new BigDecimal("7.5"), new BigDecimal("8.0")))
                .isEqualByComparingTo("8.0");
        assertThat(policy.tongKet(new BigDecimal("5.0"), new BigDecimal("3.0"), new BigDecimal("3.0")))
                .isEqualByComparingTo("3.2");
    }

    /** Thiếu một điểm thành phần thì chưa có tổng kết — không coi điểm thiếu là 0. */
    @Test
    void thieuDiemThanhPhanThiChuaCoTongKet() {
        assertThat(policy.tongKet(new BigDecimal("9.0"), null, new BigDecimal("8.0"))).isNull();
    }

    @Test
    void trongSoPhaiCongLaiBangMot() {
        assertThatThrownBy(() -> new GradePolicy(
                new BigDecimal("0.2"), new BigDecimal("0.3"), new BigDecimal("0.6"), new BigDecimal("4.0")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
