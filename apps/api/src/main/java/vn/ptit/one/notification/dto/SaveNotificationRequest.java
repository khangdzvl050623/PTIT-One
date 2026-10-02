package vn.ptit.one.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Soạn hoặc sửa bản nháp.
 *
 * @param phamVi   {@code TOAN_TRUONG} · {@code CO_SO} · {@code LOP_HOC_PHAN}
 * @param maCoSo   bắt buộc với {@code CO_SO} của Admin Master; Admin cơ sở bỏ trống
 *                 thì lấy cơ sở của mình
 * @param maLopHP  bắt buộc với {@code LOP_HOC_PHAN}
 * @param doiTuong {@code SINH_VIEN} · {@code GIANG_VIEN} · {@code TAT_CA}
 * @param lienKet  đường dẫn NỘI BỘ của web, ví dụ {@code /sinh-vien/dang-ky}. Không
 *                 nhận URL ngoài: thông báo chính thức không được thành công cụ dẫn
 *                 người đọc sang trang lạ
 */
public record SaveNotificationRequest(
        @NotBlank @Size(max = 200) String tieuDe,
        @NotBlank @Size(max = 4000) String noiDung,
        @NotBlank String mucDo,
        @NotBlank String phamVi,
        @Size(max = 10) String maCoSo,
        @Size(max = 80) String maLopHP,
        @NotBlank String doiTuong,
        @Size(max = 300) @Pattern(regexp = "^/(?!/)\\S*$", message = "Chỉ nhận đường dẫn nội bộ bắt đầu bằng /")
        String lienKet) {
}
