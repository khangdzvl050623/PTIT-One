package vn.ptit.one.report.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Thống kê chi tiết theo môn trong phạm vi được xem. */
public record CourseReport(ReportScope phamVi, List<Row> monHoc, Instant capNhatLuc) {

    /**
     * @param chuaCoKetQua lượt đăng ký chưa có điểm tổng kết đã công bố — KHÔNG
     *                     tính là trượt
     * @param phanBoDiem   chỉ gồm điểm tổng kết đã công bố
     */
    public record Row(
            String maMonHoc,
            String tenMonHoc,
            int soLop,
            int luotDangKy,
            int tongSucChua,
            int tongDaDangKy,
            BigDecimal tiLeLapDay,
            int soDat,
            int soTruot,
            int chuaCoKetQua,
            List<GradeBucket> phanBoDiem) {
    }

    /** Một khoảng điểm tổng kết, ví dụ {@code 7.0–8.4}. */
    public record GradeBucket(String khoang, int soLuong) {
    }
}
