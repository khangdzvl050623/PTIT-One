package vn.ptit.one.notification.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Thông báo do chính người dùng soạn.
 *
 * @param nguoiNhan với bản nháp là số DỰ KIẾN tính lại mỗi lần xem; với bản đã
 *                  gửi là danh sách đã chốt lúc gửi
 * @param soDaDoc   {@code 0} với bản nháp
 */
public record AuthoredNotification(
        UUID maThongBao,
        String trangThai,
        String mucDo,
        String tieuDe,
        String noiDung,
        String lienKet,
        String phamVi,
        String maCoSo,
        String maLopHP,
        String doiTuong,
        Instant ngayTao,
        Instant ngayGui,
        RecipientCount nguoiNhan,
        int soDaDoc) {
}
