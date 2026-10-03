package vn.ptit.one.grade.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một dòng trong bảng điểm của sinh viên (F07).
 *
 * <p>Điểm nháp (chưa công bố) xuất hiện như một môn "Chưa có điểm": mọi cột
 * điểm và {@code ketQua} là {@code null}, {@code daCongBo = false}. Không bao
 * giờ đổi {@code null} thành 0.
 *
 * @param ketQua {@code DAT}, {@code KHONG_DAT}, hoặc {@code null} khi chưa có điểm
 */
public record StudentGrade(
        String maHocKy,
        String tenHocKy,
        String maLopHP,
        String maMonHoc,
        String tenMonHoc,
        int soTinChi,
        BigDecimal diemChuyenCan,
        BigDecimal diemGiuaKy,
        BigDecimal diemCuoiKy,
        BigDecimal diemTongKet,
        /** Điểm chữ quy đổi từ {@code diemTongKet}; {@code null} khi chưa có điểm. */
        String diemChu,
        /** Điểm thang 4 quy đổi từ {@code diemTongKet}; {@code null} khi chưa có điểm. */
        BigDecimal diemHe4,
        String ketQua,
        boolean daCongBo,
        Instant ngayCongBo) {
}
