package vn.ptit.one.health.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.health.dto.MediaHealthResponse;
import vn.ptit.one.shared.media.CloudinaryProperties;
import vn.ptit.one.shared.media.CloudinaryUploader;

/**
 * Kho ảnh đã cấu hình chưa — tách khỏi liveness như {@link DatabaseHealthController}.
 *
 * <p>Có vì tải ảnh hỏng theo hai kiểu rất khác nhau mà người dùng chỉ thấy một
 * mã {@code 503}: hoặc API không thấy cấu hình ({@code UPLOAD_DISABLED}), hoặc
 * Cloudinary từ chối ({@code UPLOAD_FAILED}). Endpoint này trả lời vế đầu mà
 * không phải đọc log máy chủ.
 *
 * <p>Trả {@code 503} khi chưa cấu hình, để cùng cách đọc với
 * {@code /api/health/db}: công cụ giám sát nhìn mã HTTP là đủ.
 */
@RestController
@RequestMapping("/api/health/media")
public class MediaHealthController {

    private final CloudinaryUploader uploader;
    private final CloudinaryProperties properties;

    public MediaHealthController(CloudinaryUploader uploader, CloudinaryProperties properties) {
        this.uploader = uploader;
        this.properties = properties;
    }

    @GetMapping
    public ResponseEntity<MediaHealthResponse> media() {
        if (!uploader.enabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(MediaHealthResponse.down());
        }
        return ResponseEntity.ok(MediaHealthResponse.up(properties.cloudName().trim()));
    }
}
