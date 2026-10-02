package vn.ptit.one.grade.policy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GradePolicyTest {

    private final GradePolicy policy = new GradePolicy(new BigDecimal("4.0"));

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
}
