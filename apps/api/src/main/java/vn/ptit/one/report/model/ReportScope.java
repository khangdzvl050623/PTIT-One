package vn.ptit.one.report.model;

/**
 * Phạm vi đã áp dụng cho báo cáo — trả về để UI hiển thị đúng thứ đang xem.
 *
 * @param maCoSo {@code null} là toàn trường
 */
public record ReportScope(String maHocKy, String maCoSo, String maMonHoc) {
}
