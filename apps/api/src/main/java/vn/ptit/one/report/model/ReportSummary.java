package vn.ptit.one.report.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Thống kê tổng quan của một học kỳ trong phạm vi được xem. */
public record ReportSummary(
        ReportScope phamVi,
        Registrations dangKy,
        Capacity sucChua,
        GradeProgress tienDoDiem,
        Instant capNhatLuc) {

    /**
     * @param luotDangKy số dòng ghi danh còn giữ chỗ
     * @param soSinhVien số sinh viên khác nhau trong các lượt đó
     */
    public record Registrations(int soLop, int luotDangKy, int soSinhVien) {
    }

    /**
     * @param tiLeLapDay {@code tongDaDangKy / tongSucChua}, từ 0 đến 1; {@code null}
     *                   khi không có lớp nào
     */
    public record Capacity(int tongSucChua, int tongDaDangKy, BigDecimal tiLeLapDay,
            int soLopDay, int soLopConCho) {
    }

    /**
     * Theo lớp. {@code chuaCongBo} gồm cả lớp chưa có sinh viên — chưa có gì để
     * công bố cũng là chưa công bố.
     */
    public record GradeProgress(int chuaCongBo, int daCongBo, int daKhoa) {
    }
}
