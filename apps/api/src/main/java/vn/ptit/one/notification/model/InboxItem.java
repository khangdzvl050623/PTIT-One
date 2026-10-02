package vn.ptit.one.notification.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Một thông báo trong hộp thư của người nhận.
 *
 * @param suKien         {@code null} với thông báo soạn tay
 * @param vaiTroNguoiGui vai trò người soạn; {@code null} là hệ thống tự sinh
 * @param daDoc          "Chưa đọc" là của RIÊNG người này — khác nhãn "Mới" theo thời gian
 */
public record InboxItem(
        UUID maThongBao,
        String loai,
        String suKien,
        String mucDo,
        String tieuDe,
        String noiDung,
        String lienKet,
        String vaiTroNguoiGui,
        Instant ngayGui,
        boolean daDoc,
        Instant ngayDoc) {
}
