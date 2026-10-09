package vn.ptit.one.shared.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nhận dạng ảnh theo byte đầu file.
 *
 * <p>Không cần DB nên chạy cả trên CI — đây là cổng chặn duy nhất giữa "file
 * người dùng chọn" và kho ảnh, nên nó phải được kiểm ở mọi lần build.
 */
class ImageKindTest {

    @Test
    void nhanDungNamDinhDang() {
        assertThat(ImageKind.of(bytes(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo(ImageKind.JPEG);
        assertThat(ImageKind.of(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)))
                .isEqualTo(ImageKind.PNG);
        assertThat(ImageKind.of(bytes('G', 'I', 'F', '8', '9', 'a'))).isEqualTo(ImageKind.GIF);
        assertThat(ImageKind.of(bytes('B', 'M', 0x36, 0x00))).isEqualTo(ImageKind.BMP);
        assertThat(ImageKind.of(bytes('R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P')))
                .isEqualTo(ImageKind.WEBP);
    }

    /** RIFF cũng là vỏ của WAV và AVI — thiếu chữ WEBP ở byte 8 thì không phải ảnh. */
    @Test
    void riffKhongPhaiWebpThiTuChoi() {
        assertThat(ImageKind.of(bytes('R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'A', 'V', 'E'))).isNull();
    }

    @Test
    void fileLaVaFileQuaNganThiTuChoi() {
        assertThat(ImageKind.of("<?php echo 1; ?>".getBytes())).isNull();
        assertThat(ImageKind.of(bytes(0xFF, 0xD8))).as("JPEG thiếu byte thứ ba").isNull();
        assertThat(ImageKind.of(new byte[0])).isNull();
    }

    /**
     * Đổi đuôi tên hay khai `Content-Type` không giúp gì: hàm này chỉ đọc byte,
     * nên một file thực thi đặt tên `.png` vẫn bị từ chối.
     */
    @Test
    void fileThucThiDoiTenVanBiTuChoi() {
        assertThat(ImageKind.of(bytes('M', 'Z', 0x90, 0x00))).isNull();
    }

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }
}
