package vn.ptit.one.grade.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Lưu một loạt dòng điểm. Cả loạt là MỘT giao dịch: một dòng lỗi thì không
 * dòng nào được ghi. Gửi {@code null} cho một điểm thành phần là xoá điểm đó.
 */
public record SaveGradesRequest(@NotEmpty List<@Valid @NotNull Row> diem) {

    public record Row(
            @NotBlank String maSinhVien,
            @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 1) BigDecimal diemChuyenCan,
            @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 1) BigDecimal diemGiuaKy,
            @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 1) BigDecimal diemCuoiKy,
            @NotNull Long version) {
    }
}
