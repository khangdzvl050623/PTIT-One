package vn.ptit.one.shared.media;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import vn.ptit.one.shared.exception.ApiException;

/**
 * Đẩy ảnh đại diện lên Cloudinary và xoá khi cần.
 *
 * <p>Cùng khuôn với {@code Mailer}: có {@link #enabled()} để tính năng tự tắt
 * khi chưa cấu hình, và trả {@code 503} chứ không giả vờ đã lưu.
 *
 * <p><b>Mỗi sinh viên một {@code public_id} cố định.</b> Tải ảnh mới là ghi đè
 * ảnh cũ, nên tài khoản Cloudinary không tích ảnh mồ côi và DB không cần thêm
 * cột để nhớ id. URL trả về vẫn khác nhau sau mỗi lần tải vì Cloudinary chèn
 * số phiên bản (`/v1728…/`) — nhờ đó trình duyệt không hiện ảnh cũ trong cache.
 *
 * <p>Không dùng SDK: ký một yêu cầu Cloudinary là sắp xếp tham số rồi băm SHA-1,
 * ít hơn hẳn chi phí thêm một thư viện vào đồ án.
 */
@Component
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryUploader {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryUploader.class);
    private static final int TIMEOUT = 15_000;

    private final CloudinaryProperties properties;
    private final Clock clock;
    private final RestClient api;

    public CloudinaryUploader(CloudinaryProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.api = properties.enabled() ? buildApi(properties) : null;
        if (!properties.enabled()) {
            log.info("Chưa cấu hình Cloudinary, tải ảnh đại diện bị tắt (xem .env.example)");
        }
    }

    public boolean enabled() {
        return api != null;
    }

    /**
     * @param maThucThe quyết định {@code public_id}, nên ảnh của người này
     *                   không bao giờ ghi đè ảnh của người khác
     * @return {@code secure_url} có số phiên bản, lưu thẳng vào {@code AnhDaiDien}
     */
    public String upload(String maThucThe, byte[] bytes, ImageKind kind) {
        requireEnabled();
        Map<String, String> signed = new TreeMap<>(Map.of(
                "public_id", properties.publicId(maThucThe),
                "overwrite", "true",
                /* Ghi đè cùng public_id thì CDN vẫn giữ bản cũ ở biên; không có
                   cờ này, ảnh mới có thể cả tiếng sau mới thấy. */
                "invalidate", "true",
                "timestamp", String.valueOf(clock.instant().getEpochSecond())));

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        signed.forEach(form::add);
        form.add("api_key", properties.apiKey().trim());
        form.add("signature", sign(signed));
        form.add("file", filePart(maThucThe, bytes, kind));

        Map<?, ?> response = send("/image/upload", form);
        Object url = response.get("secure_url");
        if (url == null) {
            log.warn("Cloudinary không trả secure_url, khoá nhận được: {}", response.keySet());
            throw unavailable();
        }
        return url.toString();
    }

    /**
     * Xoá ảnh trên Cloudinary.
     *
     * <p>Chưa cấu hình kho ảnh thì **bỏ qua trong im lặng** thay vì ném: kết
     * quả mong muốn là "không còn ảnh", mà kho chưa bật thì điều đó đã đúng
     * sẵn. Ném ở đây sẽ khiến sinh viên không gỡ nổi ảnh cũ khi nhóm tắt
     * Cloudinary — ngược hẳn với {@link #upload}, nơi không bật kho thì thật
     * sự không lưu được gì.
     */
    public void delete(String maThucThe) {
        if (!enabled()) {
            return;
        }
        Map<String, String> signed = new TreeMap<>(Map.of(
                "public_id", properties.publicId(maThucThe),
                "invalidate", "true",
                "timestamp", String.valueOf(clock.instant().getEpochSecond())));

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        signed.forEach(form::add);
        form.add("api_key", properties.apiKey().trim());
        form.add("signature", sign(signed));

        send("/image/destroy", form);
    }

    private Map<?, ?> send(String path, MultiValueMap<String, Object> form) {
        try {
            Map<?, ?> body = api.post().uri(path)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            return body == null ? Map.of() : body;
        } catch (RestClientResponseException ex) {
            /* Thân phản hồi của Cloudinary mới nói lý do thật ("Invalid Signature",
               "Invalid cloud_name", "Stale request"...). Thiếu nó thì log chỉ còn
               một con số và người đọc không biết sửa gì. */
            log.warn("Cloudinary từ chối {} ({}): {}", path, ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            throw unavailable();
        } catch (RestClientException ex) {
            log.warn("Không gọi được Cloudinary {}: {}", path, ex.toString());
            throw unavailable();
        }
    }

    /**
     * Chữ ký Cloudinary: các tham số đã ký, sắp theo tên, nối {@code k=v&…},
     * ghép {@code api_secret} vào cuối rồi băm SHA-1.
     *
     * <p>{@code file}, {@code api_key} và {@code cloud_name} KHÔNG nằm trong
     * chuỗi ký — đó là quy định của Cloudinary, thêm vào là chữ ký sai.
     */
    private String sign(Map<String, String> signed) {
        StringBuilder raw = new StringBuilder();
        signed.forEach((key, value) -> {
            if (!raw.isEmpty()) {
                raw.append('&');
            }
            raw.append(key).append('=').append(value);
        });
        raw.append(properties.apiSecret().trim());
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(sha1.digest(raw.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM không có SHA-1", ex);
        }
    }

    /** Tên file phải có để Spring gửi phần này dạng file chứ không dạng trường chữ. */
    private static ByteArrayResource filePart(String maThucThe, byte[] bytes, ImageKind kind) {
        String name = maThucThe + "." + kind.name().toLowerCase();
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }

    private void requireEnabled() {
        if (!enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "UPLOAD_DISABLED",
                    "Chưa cấu hình kho ảnh. Liên hệ quản trị để bật tính năng ảnh đại diện.");
        }
    }

    private ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "UPLOAD_FAILED",
                "Không lưu được ảnh lúc này. Thử lại sau.");
    }

    private static RestClient buildApi(CloudinaryProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.apiUrl().trim() + "/" + properties.cloudName().trim())
                .build();
    }
}
