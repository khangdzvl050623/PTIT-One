package vn.ptit.one.auth.model;

/**
 * Cơ sở, ở mức tra cứu — đủ cho ô chọn cơ sở trên giao diện.
 *
 * <p>Bảng {@code CoSo} do {@code V1} (lát cắt auth) tạo nên module {@code auth}
 * sở hữu. Các module khác đọc tên cơ sở bằng JOIN trong câu đọc của mình, không
 * gọi sang đây.
 */
public record Campus(String maCoSo, String tenCoSo, String thanhPho) {
}
