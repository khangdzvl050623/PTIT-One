package vn.ptit.one.notification.model;

import java.util.List;

/**
 * Thông báo tự sinh từ một sự kiện nghiệp vụ.
 *
 * @param khoaSuKien khoá chống trùng: phát lại cùng khoá thì không tạo bản thứ hai.
 *                   Huỷ rồi đăng ký lại phải ra khoá MỚI (khoá gồm thời điểm)
 * @param lienKet    đường dẫn nội bộ của web, bắt đầu bằng {@code /}
 * @param maLopHP    lớp liên quan, nếu có
 */
public record AutoNotification(
        String suKien,
        String khoaSuKien,
        String mucDo,
        String tieuDe,
        String noiDung,
        String lienKet,
        String maLopHP,
        List<Recipient> nguoiNhan) {
}
