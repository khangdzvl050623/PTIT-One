package vn.ptit.one.health.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Kho ảnh ngoài đã cấu hình chưa. Trả lời đúng một câu hỏi: API có đẩy ảnh đại
 * diện lên được không.
 *
 * <p>{@code cloudName} KHÔNG phải bí mật — nó nằm trong mọi URL ảnh
 * ({@code res.cloudinary.com/<cloudName>/…}). Để nó ở đây để đối chiếu giá trị
 * API thật sự nhận được với giá trị đã điền trong {@code .env}. API key và
 * secret thì không bao giờ ra khỏi server.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MediaHealthResponse(String status, String cloudName, String hint) {

    public static MediaHealthResponse up(String cloudName) {
        return new MediaHealthResponse("UP", cloudName, null);
    }

    public static MediaHealthResponse down() {
        return new MediaHealthResponse("DOWN", null,
                "Thiếu PTITONE_CLOUDINARY_CLOUD_NAME, _API_KEY hoặc _API_SECRET trong môi trường "
                        + "của tiến trình API. Sửa .env xong phải KHỞI ĐỘNG LẠI API.");
    }
}
