package vn.ptit.one.report.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Thống kê chi tiết theo môn trong phạm vi được xem. */
public record CourseReport(ReportScope phamVi, List<CourseStat> monHoc, Instant capNhatLuc) {

    /**
     * Tên {@code CourseStat} chứ KHÔNG phải {@code Row}: springdoc đặt tên
     * schema theo tên record lồng, nên trùng tên với {@code SaveGradesRequest.Row}
     * sẽ làm hai hình dạng khác nhau dùng CÙNG một schema trong `openapi.json`
     * — một bên ghi đè bên kia và hợp đồng công bố ra sai.
     *
     * @param chuaCoKetQua lượt đăng ký chưa có điểm tổng kết đã công bố — KHÔNG
     *                     tính là trượt
     * @param phanBoDiem   chỉ gồm điểm tổng kết đã công bố
     */
    public record CourseStat(
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
