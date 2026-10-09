package vn.ptit.one.shared.media;

import java.util.Arrays;

/**
 * Định dạng ảnh nhận được, nhận dạng bằng **byte đầu file**.
 *
 * <p>Không tin {@code Content-Type} hay đuôi tên file: cả hai do client đặt,
 * nên đổi tên {@code virus.exe} thành {@code anh.png} là qua. Byte đầu file là
 * thứ duy nhất nói về nội dung thật.
 *
 * <p>Năm định dạng dưới đây là những gì trình duyệt hiện nào cũng hiện được.
 * HEIC của iPhone cố ý không nhận: Safari hiện được nhưng Chrome và Firefox
 * thì không, nên ảnh sẽ vỡ ở phần lớn máy xem.
 */
public enum ImageKind {

    JPEG("image/jpeg", new int[] { 0xFF, 0xD8, 0xFF }),
    PNG("image/png", new int[] { 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }),
    GIF("image/gif", new int[] { 0x47, 0x49, 0x46, 0x38 }),
    /* RIFF....WEBP: bốn byte giữa là độ dài file nên phải bỏ qua, xem match(). */
    WEBP("image/webp", new int[] { 0x52, 0x49, 0x46, 0x46 }),
    BMP("image/bmp", new int[] { 0x42, 0x4D });

    private final String mediaType;
    private final int[] magic;

    ImageKind(String mediaType, int[] magic) {
        this.mediaType = mediaType;
        this.magic = magic;
    }

    public String mediaType() {
        return mediaType;
    }

    /** {@code null} khi không phải ảnh thuộc danh sách trên. */
    public static ImageKind of(byte[] bytes) {
        return Arrays.stream(values()).filter(kind -> kind.match(bytes)).findFirst().orElse(null);
    }

    private boolean match(byte[] bytes) {
        if (bytes.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if ((bytes[i] & 0xFF) != magic[i]) {
                return false;
            }
        }
        // RIFF còn là vỏ của WAV và AVI — chữ WEBP ở byte 8..11 mới chốt.
        return this != WEBP || (bytes.length >= 12
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P');
    }
}
