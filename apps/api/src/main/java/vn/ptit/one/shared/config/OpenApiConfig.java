package vn.ptit.one.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Mô tả OpenAPI. Spec được SINH từ controller và DTO, không viết tay — thêm
 * endpoint là spec tự có, nên không thể lệch với code.
 *
 * <p>Hai thứ springdoc KHÔNG suy ra được: quyền theo vai trò (phần lớn kiểm
 * trong service, không phải {@code @PreAuthorize}) và danh sách mã lỗi (ném
 * từ service qua {@code ApiException}). Cả hai nằm ở
 * {@code docs/PTIT-One-API-Contract.md} — Swagger UI không thay được tài liệu đó.
 */
@Configuration
public class OpenApiConfig {

    /** Khớp cookie thật do {@code AuthController} đặt. */
    private static final String ACCESS_COOKIE = "PTITONE_AT";

    @Bean
    public OpenAPI ptitOneOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PTIT One API")
                        .version("v1")
                        .description("""
                                Đăng nhập bằng `POST /api/auth/login` ngay trong trang này: \
                                cookie phiên được trình duyệt giữ, các lệnh sau tự dùng. \
                                Không có `Authorization` header.

                                Bảng quyền theo vai trò, 36 mã lỗi và các quy tắc nghiệp vụ \
                                nằm ở `docs/PTIT-One-API-Contract.md`; trang này chỉ mô tả \
                                hình dạng request/response.

                                Phạm vi cơ sở LUÔN lấy từ JWT đã ký — không endpoint nào \
                                nhận tham số `maCoSo`."""))
                .components(new Components()
                        .addSecuritySchemes(ACCESS_COOKIE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(ACCESS_COOKIE)
                                .description("""
                                        Cookie HttpOnly do server đặt khi đăng nhập. \
                                        Khai báo ở đây để spec nói rõ API dùng cookie; \
                                        không nhập tay được và cũng không cần.""")));
    }
}
