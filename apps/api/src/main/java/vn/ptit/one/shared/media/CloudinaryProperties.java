package vn.ptit.one.shared.media;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Kho ảnh Cloudinary — chỉ dùng cho ảnh đại diện. Giá trị thật ở
 * {@code apps/api/.env}, không vào Git.
 *
 * <p><b>{@code apiSecret} không bao giờ rời server.</b> Frontend gửi file tới
 * {@code POST /api/me/avatar}; API ký và đẩy lên Cloudinary. Đưa secret xuống
 * trình duyệt là cho bất kỳ ai đẩy gì lên tài khoản Cloudinary của nhóm.
 *
 * <p>Để trống {@code cloudName} là tắt tính năng: màn hồ sơ hiện khung mặc
 * định, và gọi upload trả {@code 503} chứ không giả vờ đã lưu.
 *
 * @param folder thư mục trong tài khoản Cloudinary; tách ảnh của đồ án khỏi
 *               những thứ khác trong cùng tài khoản
 */
@ConfigurationProperties("ptitone.cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        @DefaultValue("ptitone/avatar") String folder,
        @DefaultValue("https://api.cloudinary.com/v1_1") String apiUrl) {

    public boolean enabled() {
        return hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    /**
     * Ảnh của mỗi người dùng MỘT id cố định, nên tải ảnh mới là ghi đè ảnh cũ.
     *
     * @param maThucThe mã sinh viên hoặc mã giảng viên. Hai không gian mã này
     *                  không trùng nhau nên dùng chung một thư mục là an toàn.
     */
    public String publicId(String maThucThe) {
        return folder().trim() + "/" + maThucThe;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
