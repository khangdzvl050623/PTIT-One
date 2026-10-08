package vn.ptit.one.student;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hồ sơ của chính sinh viên: {@code GET} và {@code PUT /api/me/profile}.
 *
 * <p>Seed KHÔNG đặt email nên mọi tài khoản bắt đầu ở trạng thái chưa xác minh —
 * ca "sửa được" phải tự bật cờ, và nhờ vậy ca "chưa xác minh thì bị chặn" là
 * trạng thái mặc định chứ không phải dựng riêng.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class MyProfileIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";
    private static final String SV = "B26DCCN001";
    private static final String PROFILE = "/api/me/profile";
    private static final String AVATAR = PROFILE + "/avatar";

    /** PNG 1×1 hợp lệ — đủ để qua bước nhận dạng byte đầu file. */
    private static final byte[] PNG_1PX = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private static final String COT_LY_LICH = """
            GioiTinh, DienThoai, SoCCCD, EmailCaNhan, NoiSinh, DanToc, TonGiao, HoKhau, AnhDaiDien
            """;

    private Map<String, Object> lyLichBanDau;
    private Map<String, Object> emailBanDau;

    /**
     * Chụp trạng thái trước mỗi ca để hoàn lại NGUYÊN VẸN sau đó.
     *
     * <p>Không ghi `NULL` để "dọn": seed CÓ điền lý lịch cho {@code B26DCCN001}
     * (xem `20-hoc-vu-seed.sql`, khối V8) nên dọn kiểu đó là xoá fixture mà các
     * màn hình khác đang dùng, và seed chạy lại cũng không bù vì câu `UPDATE`
     * của nó có điều kiện `GioiTinh IS NULL`.
     */
    @BeforeEach
    void snapshot() {
        lyLichBanDau = jdbc.queryForMap(
                "SELECT %s FROM dbo.SinhVien WHERE MaSinhVien = ?".formatted(COT_LY_LICH.trim()), SV);
        emailBanDau = jdbc.queryForMap("""
                SELECT Email, EmailDaXacMinh, ThoiDiemXacMinhEmail
                  FROM dbo.TaiKhoan WHERE TenDangNhap = ?
                """, SV);
    }

    @AfterEach
    void restore() {
        // Một câu cho cả ba cột email: CK_TaiKhoan_EmailXacMinh soi chúng cùng lúc.
        jdbc.update("""
                UPDATE dbo.TaiKhoan
                   SET Email = ?, EmailDaXacMinh = ?, ThoiDiemXacMinhEmail = ?
                 WHERE TenDangNhap = ?
                """, emailBanDau.get("Email"), emailBanDau.get("EmailDaXacMinh"),
                emailBanDau.get("ThoiDiemXacMinhEmail"), SV);
        jdbc.update("""
                UPDATE dbo.SinhVien
                   SET GioiTinh = ?, DienThoai = ?, SoCCCD = ?, EmailCaNhan = ?,
                       NoiSinh = ?, DanToc = ?, TonGiao = ?, HoKhau = ?, AnhDaiDien = ?
                 WHERE MaSinhVien = ?
                """, lyLichBanDau.get("GioiTinh"), lyLichBanDau.get("DienThoai"),
                lyLichBanDau.get("SoCCCD"), lyLichBanDau.get("EmailCaNhan"),
                lyLichBanDau.get("NoiSinh"), lyLichBanDau.get("DanToc"),
                lyLichBanDau.get("TonGiao"), lyLichBanDau.get("HoKhau"),
                lyLichBanDau.get("AnhDaiDien"), SV);
    }

    /**
     * Giữ email đang có nếu tài khoản đã có một địa chỉ — chỉ bật cờ xác minh.
     * Ghi đè bằng địa chỉ cố định sẽ phá email thật mà người dùng đặt tay khi
     * thử Swagger.
     */
    private void verifyEmail() {
        jdbc.update("""
                UPDATE dbo.TaiKhoan
                   SET Email = COALESCE(Email, 'b26dccn001.test@stu.ptit.edu.vn'),
                       EmailDaXacMinh = 1,
                       ThoiDiemXacMinhEmail = COALESCE(ThoiDiemXacMinhEmail, SYSUTCDATETIME())
                 WHERE TenDangNhap = ?
                """, SV);
    }

    /** Hồ sơ gộp từ nhiều bảng: `SinhVien` + `CoSo` + `ChuongTrinhDaoTao` + `Khoa` + email. */
    @Test
    void xemDuocHoSoCuaChinhMinh() throws Exception {
        HttpResponse<String> response = signedIn(SV).get(PROFILE);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("\"maSinhVien\":\"" + SV + "\"", "\"maCoSoNha\":\"HCM\"",
                "\"tenCoSo\":", "\"tenCTDT\":", "\"tenKhoa\":", "\"tongTinChiCTDT\":");
    }

    /** Chưa xác minh email thì KHÔNG sửa được — cổng chặn chính của endpoint. */
    @Test
    void chuaXacMinhEmailThiKhongSuaDuoc() throws Exception {
        HttpResponse<String> response = signedIn(SV).put(PROFILE, "{\"gioiTinh\":\"NU\"}");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(409);
        assertThat(response.body()).contains("EMAIL_NOT_VERIFIED");
        // Chặn nghĩa là KHÔNG ghi gì, kể cả những ô request bỏ trống.
        assertThat(lyLich()).isEqualTo(lyLichBanDau);
    }

    @Test
    void xacMinhEmailRoiThiSuaDuoc() throws Exception {
        verifyEmail();

        HttpResponse<String> response = signedIn(SV).put(PROFILE, """
                {"gioiTinh":"NU","dienThoai":"0988777666","soCCCD":"001303004455",
                 "emailCaNhan":"mai.tran@gmail.com","noiSinh":"Nghệ An",
                 "danToc":"Tày","tonGiao":"Phật giáo","hoKhau":"12 Nguyễn Trãi, Vinh"}
                """);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("\"gioiTinh\":\"NU\"", "\"soCCCD\":\"001303004455\"");
        assertThat(cot("DienThoai")).isEqualTo("0988777666");
        assertThat(cot("HoKhau")).isEqualTo("12 Nguyễn Trãi, Vinh");
        assertThat(cot("TonGiao")).isEqualTo("Phật giáo");
    }

    /**
     * Ảnh đại diện KHÔNG nằm trong biểu mẫu lý lịch.
     *
     * <p>Hai lý do: sinh viên không trỏ được ảnh sang URL bất kỳ trên internet,
     * và lưu lý lịch không vô tình xoá mất ảnh đã tải lên.
     */
    @Test
    void bieuMauLyLichKhongDungToiAnh() throws Exception {
        verifyEmail();
        jdbc.update("UPDATE dbo.SinhVien SET AnhDaiDien = ? WHERE MaSinhVien = ?",
                "https://res.cloudinary.com/demo/image/upload/v1/ptitone/avatar/" + SV + ".jpg", SV);

        HttpResponse<String> response = signedIn(SV).put(PROFILE, """
                {"gioiTinh":"NAM","anhDaiDien":"https://ke-xau.example/anh.jpg"}
                """);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        // Ô lạ bị bỏ qua, và ảnh đang có vẫn nguyên.
        assertThat(cot("AnhDaiDien")).contains("res.cloudinary.com").doesNotContain("ke-xau");
    }

    /** Thay TOÀN BỘ phần lý lịch: ô bỏ trống là XOÁ giá trị cũ, không phải giữ nguyên. */
    @Test
    void oBoTrongLaXoaGiaTriCu() throws Exception {
        verifyEmail();
        Browser sv = signedIn(SV);
        sv.put(PROFILE, "{\"gioiTinh\":\"NU\",\"danToc\":\"Tày\"}");
        assertThat(cot("DanToc")).isEqualTo("Tày");

        assertThat(sv.put(PROFILE, "{\"gioiTinh\":\"NU\"}").statusCode()).isEqualTo(200);

        assertThat(cot("DanToc")).isNull();
        assertThat(cot("GioiTinh")).isEqualTo("NU");
    }

    /** Chuỗi toàn khoảng trắng lưu thành NULL, không lưu '' — để ô trống chỉ có một dạng. */
    @Test
    void khoangTrangLuuThanhNull() throws Exception {
        verifyEmail();

        assertThat(signedIn(SV).put(PROFILE, "{\"noiSinh\":\"   \"}").statusCode()).isEqualTo(200);

        assertThat(cot("NoiSinh")).isNull();
    }

    /**
     * Biểu mẫu web gửi ô trống thành {@code ""}, không phải {@code null}. Nếu
     * những ô có định dạng không nhận chuỗi rỗng thì không bao giờ xoá được
     * giá trị cũ qua giao diện — ca này giữ cửa đó.
     */
    @Test
    void oTrongGuiChuoiRongVanXoaDuoc() throws Exception {
        verifyEmail();
        Browser sv = signedIn(SV);
        sv.put(PROFILE, """
                {"gioiTinh":"NAM","dienThoai":"0901234567","soCCCD":"079208001234",
                 "emailCaNhan":"an.nguyen@gmail.com"}
                """);
        assertThat(cot("SoCCCD")).isEqualTo("079208001234");

        HttpResponse<String> response = sv.put(PROFILE, """
                {"gioiTinh":"","dienThoai":"","soCCCD":"","emailCaNhan":"",
                 "noiSinh":"","danToc":"","tonGiao":"","hoKhau":""}
                """);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        for (String ten : new String[] { "GioiTinh", "DienThoai", "SoCCCD", "EmailCaNhan",
                "NoiSinh", "DanToc", "TonGiao", "HoKhau" }) {
            assertThat(cot(ten)).as(ten).isNull();
        }
    }

    @Test
    void duLieuSaiDinhDangThiBiChan() throws Exception {
        verifyEmail();
        Browser sv = signedIn(SV);

        record Ca(String than, String truong) {
        }
        for (Ca ca : new Ca[] {
                new Ca("{\"gioiTinh\":\"KHAC\"}", "gioiTinh"),
                new Ca("{\"soCCCD\":\"123\"}", "soCCCD"),
                new Ca("{\"emailCaNhan\":\"khong-phai-email\"}", "emailCaNhan"),
                new Ca("{\"dienThoai\":\"abc\"}", "dienThoai"),
        }) {
            HttpResponse<String> response = sv.put(PROFILE, ca.than());
            assertThat(response.statusCode()).as(ca.than()).isEqualTo(400);
            assertThat(response.body()).as(ca.than()).contains("VALIDATION_ERROR", ca.truong());
        }
    }

    /** Hồ sơ sinh viên là của sinh viên; vai trò khác không có. */
    @Test
    void chiSinhVienCoHoSo() throws Exception {
        for (String username : new String[] { "GVHCM001", "admin.hcm", "admin.master" }) {
            assertThat(signedIn(username).get(PROFILE).statusCode()).as(username).isEqualTo(403);
            assertThat(signedIn(username).put(PROFILE, "{}").statusCode()).as(username).isEqualTo(403);
            assertThat(signedIn(username).upload(AVATAR, PNG_1PX).statusCode())
                    .as(username).isEqualTo(403);
            assertThat(signedIn(username).delete(AVATAR).statusCode()).as(username).isEqualTo(403);
        }
    }

    // --- Ảnh đại diện ----------------------------------------------------

    /**
     * Định dạng xét theo BYTE ĐẦU FILE.
     *
     * <p>Gửi chữ thường nhưng khai {@code image/png} và đặt tên {@code .png} —
     * nếu server tin `Content-Type` hay đuôi tên thì ca này lọt, và đó là
     * đường đưa file bất kỳ lên kho ảnh.
     */
    @Test
    void khongPhaiAnhThiBiChan() throws Exception {
        verifyEmail();

        HttpResponse<String> response = signedIn(SV)
                .upload(AVATAR, "<?php echo 1; ?>".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(response.body()).contains("IMAGE_FORMAT_INVALID");
        assertThat(cot("AnhDaiDien")).isNull();
    }

    /** Cùng cổng chặn với sửa lý lịch: chưa xác minh email thì chưa đổi được ảnh. */
    @Test
    void chuaXacMinhEmailThiKhongDoiDuocAnh() throws Exception {
        HttpResponse<String> tai = signedIn(SV).upload(AVATAR, PNG_1PX);
        assertThat(tai.statusCode()).as(tai.body()).isEqualTo(409);
        assertThat(tai.body()).contains("EMAIL_NOT_VERIFIED");

        HttpResponse<String> go = signedIn(SV).delete(AVATAR);
        assertThat(go.statusCode()).as(go.body()).isEqualTo(409);
        assertThat(go.body()).contains("EMAIL_NOT_VERIFIED");
    }

    /**
     * Gỡ ảnh chạy được cả khi chưa cấu hình Cloudinary.
     *
     * <p>Kết quả mong muốn là "không còn ảnh"; kho ảnh tắt thì điều đó vốn đã
     * đúng, nên chặn ở đây sẽ khoá sinh viên lại với một tấm ảnh cũ.
     */
    @Test
    void goAnhDuocKeCaKhiChuaCauHinhKhoAnh() throws Exception {
        verifyEmail();
        jdbc.update("UPDATE dbo.SinhVien SET AnhDaiDien = ? WHERE MaSinhVien = ?",
                "https://res.cloudinary.com/demo/image/upload/v1/cu.jpg", SV);

        HttpResponse<String> response = signedIn(SV).delete(AVATAR);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(cot("AnhDaiDien")).isNull();
    }

    // --- Tiện ích --------------------------------------------------------

    private String cot(String ten) {
        return jdbc.queryForObject(
                "SELECT %s FROM dbo.SinhVien WHERE MaSinhVien = ?".formatted(ten), String.class, SV);
    }

    private Map<String, Object> lyLich() {
        return jdbc.queryForMap(
                "SELECT %s FROM dbo.SinhVien WHERE MaSinhVien = ?".formatted(COT_LY_LICH.trim()), SV);
    }

    private Browser signedIn(String username) throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login(username).statusCode()).as("đăng nhập %s", username).isEqualTo(200);
        return browser;
    }

    /** Trình duyệt tối giản: giữ cookie, gửi CSRF header như SPA. */
    private final class Browser {

        private final CookieManager cookies = new CookieManager();
        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();

        HttpResponse<String> login(String username) throws Exception {
            assertThat(get("/api/auth/csrf").statusCode()).isEqualTo(200);
            return send("POST", "/api/auth/login",
                    "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}");
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> put(String path, String json) throws Exception {
            return send("PUT", path, json);
        }

        HttpResponse<String> delete(String path) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                    .timeout(Duration.ofSeconds(10)).DELETE();
            csrf(request);
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }

        /**
         * Gửi file như biểu mẫu web: {@code multipart/form-data}, trường
         * {@code file}, kèm tên file và {@code Content-Type} TỰ KHAI.
         *
         * <p>Khai `image/png` cho mọi nội dung là có chủ ý — ca kiểm định dạng
         * dựa vào đó để chứng minh server không tin hai thứ này.
         */
        HttpResponse<String> upload(String path, byte[] bytes) throws Exception {
            String boundary = "----ptitone" + System.nanoTime();
            var out = new java.io.ByteArrayOutputStream();
            out.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"anh.png\"\r\n"
                    + "Content-Type: image/png\r\n\r\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.write(bytes);
            out.write(("\r\n--" + boundary + "--\r\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));

            HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()));
            csrf(request);
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> send(String method, String path, String json) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json));
            csrf(request);
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }

        /** Header CSRF như SPA gửi; thiếu nó thì mọi request ghi bị chặn trước controller. */
        private void csrf(HttpRequest.Builder request) {
            String xsrf = cookie("XSRF-TOKEN");
            if (xsrf != null) {
                request.header("X-XSRF-TOKEN", xsrf);
            }
        }

        private String cookie(String name) {
            return cookies.getCookieStore().getCookies().stream()
                    .filter(c -> c.getName().equals(name) && !c.hasExpired() && !c.getValue().isEmpty())
                    .map(HttpCookie::getValue).findFirst().orElse(null);
        }

        private URI uri(String path) {
            return URI.create("http://127.0.0.1:" + port + path);
        }
    }
}
