package vn.ptit.one.grade.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một dòng trong bảng nhập điểm của lớp (F06).
 *
 * @param version    gửi lại nguyên giá trị này khi lưu; lệch nghĩa là đã có người
 *                   sửa dòng đó sau khi mình tải bảng
 * @param ngayCongBo {@code null} là điểm nháp, sinh viên chưa thấy
 */
public record GradeEntry(
        String maSinhVien,
        String hoTen,
        BigDecimal diemChuyenCan,
        BigDecimal diemGiuaKy,
        BigDecimal diemCuoiKy,
        BigDecimal diemTongKet,
        String ketQua,
        long version,
        Instant ngayCongBo) {
}
