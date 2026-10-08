package vn.ptit.one.auth;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lối thoát khi người dùng quên mật khẩu VÀ mất hòm thư: Admin đặt email mới,
 * rồi cấp lại mật khẩu.
 *
 * <p>Hai thao tác TÁCH RIÊNG có chủ ý, nên test cũng kiểm riêng: mỗi cái phải
 * tự đứng được, và nhật ký phải phân biệt được "sửa email gõ nhầm" với "chiếm
 * tài khoản".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class AdminRecoveryIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";
    /** Sinh viên fixture; mọi thay đổi đều hoàn nguyên ở `@AfterEach`. */
    private static final String SV = "B25DCCN001";
    private static final String MASTER = "admin.master";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void restore() {
        // Trả mật khẩu seed và xoá dấu vết email/mã mà test vừa tạo.
        jdbc.update("""
                UPDATE dbo.TaiKhoan
                   SET MatKhauHash = ?, Email = NULL, EmailDaXacMinh = 0,
                       ThoiDiemXacMinhEmail = NULL
                 WHERE TenDangNhap = ?
                """, passwordEncoder.encode(PASSWORD), SV);
        jdbc.update("DELETE FROM dbo.MaKichHoat WHERE TenDangNhap = ?", SV);
        jdbc.update("DELETE FROM dbo.MaXacThuc WHERE TenDangNhap = ?", SV);
    }

    // --- Đổi email ------------------------------------------------------

    /** Email mới phải ở trạng thái CHƯA xác minh: admin không xác minh hộ được. */
    @Test
    void adminDatEmailMoiNhungChuaXacMinh() throws Exception {
        jdbc.update("""
                UPDATE dbo.TaiKhoan SET Email = 'cu@mat-roi.com', EmailDaXacMinh = 1,
                       ThoiDiemXacMinhEmail = SYSUTCDATETIME()
                 WHERE TenDangNhap = ?
                """, SV);

        HttpResponse<String> response = signedIn(MASTER)
                .put("/api/accounts/" + SV + "/email", "{\"email\":\"moi@gmail.com\"}");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("\"email\":\"moi@gmail.com\"", "\"daXacMinh\":false");

        Map<String, Object> row = taiKhoan();
        assertThat(row.get("Email")).isEqualTo("moi@gmail.com");
        assertThat(row.get("EmailDaXacMinh")).isEqualTo(false);
        assertThat(row.get("ThoiDiemXacMinhEmail")).isNull();
    }

    /** Đổi email KHÔNG đụng tới mật khẩu — hai thao tác tách riêng. */
    @Test
    void doiEmailKhongDungToiMatKhau() throws Exception {
        signedIn(MASTER).put("/api/accounts/" + SV + "/email", "{\"email\":\"moi@gmail.com\"}");

        assertThat(taiKhoan().get("MatKhauHash")).as("vẫn đăng nhập được").isNotNull();
        assertThat(new Browser().login(SV).statusCode()).isEqualTo(200);
    }

    @Test
    void emailSaiDinhDangThiBiChan() throws Exception {
        HttpResponse<String> response = signedIn(MASTER)
                .put("/api/accounts/" + SV + "/email", "{\"email\":\"khong-phai-email\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("VALIDATION_ERROR");
    }

    // --- Cấp lại mật khẩu -----------------------------------------------

    /**
     * Không có email thì mã hiện cho Admin trao tay — đúng ca "mất hòm thư",
     * và tài khoản KHÔNG đăng nhập được cho tới khi đặt mật khẩu mới.
     */
    @Test
    void capLaiMatKhauThiTraMaVaKhoaDangNhap() throws Exception {
        HttpResponse<String> response = signedIn(MASTER)
                .post("/api/accounts/" + SV + "/password-reset");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        assertThat(response.body()).contains("\"maKichHoat\":\"", "\"hetHan\":");
        assertThat(taiKhoan().get("MatKhauHash")).as("mật khẩu cũ đã bị xoá").isNull();
        assertThat(new Browser().login(SV).statusCode())
                .as("chưa đặt mật khẩu mới thì chưa vào được").isEqualTo(401);
    }

    /** Toàn bộ vòng: cấp lại → dùng mã đặt mật khẩu mới → đăng nhập được. */
    @Test
    void dungMaDatMatKhauMoiRoiDangNhapDuoc() throws Exception {
        String body = signedIn(MASTER).post("/api/accounts/" + SV + "/password-reset").body();
        String ma = giuaHaiDauNhay(body, "\"maKichHoat\":\"");
        String matKhauMoi = "MatKhauMoi#2026";

        HttpResponse<String> kichHoat = new Browser().post("/api/auth/activate", """
                {"tenDangNhap":"%s","maKichHoat":"%s","matKhauMoi":"%s"}
                """.formatted(SV, ma, matKhauMoi));

        assertThat(kichHoat.statusCode()).as(kichHoat.body()).isEqualTo(204);
        assertThat(new Browser().login(SV, matKhauMoi).statusCode()).isEqualTo(200);
        assertThat(new Browser().login(SV, PASSWORD).statusCode())
                .as("mật khẩu cũ phải chết").isEqualTo(401);
    }

    /** Ghi lại AI đã cấp — đây là công cụ chiếm tài khoản, phải truy được. */
    @Test
    void ghiLaiNguoiCapMa() throws Exception {
        signedIn(MASTER).post("/api/accounts/" + SV + "/password-reset");

        assertThat(jdbc.queryForObject("""
                SELECT NguoiCap FROM dbo.MaKichHoat
                 WHERE TenDangNhap = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
                """, String.class, SV)).isEqualTo(MASTER);
    }

    /** Mã cũ phải chết khi cấp mã mới — `UQ_MaKichHoat_ConSong` chỉ cho một mã sống. */
    @Test
    void capLanHaiThiMaLanMotChet() throws Exception {
        Browser master = signedIn(MASTER);
        String ma1 = giuaHaiDauNhay(
                master.post("/api/accounts/" + SV + "/password-reset").body(), "\"maKichHoat\":\"");
        master.post("/api/accounts/" + SV + "/password-reset");

        HttpResponse<String> response = new Browser().post("/api/auth/activate", """
                {"tenDangNhap":"%s","maKichHoat":"%s","matKhauMoi":"MatKhauMoi#2026"}
                """.formatted(SV, ma1));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("ACTIVATION_INVALID");
    }

    @Test
    void khongCapLaiChoAdminMaster() throws Exception {
        HttpResponse<String> response = signedIn(MASTER)
                .post("/api/accounts/" + MASTER + "/password-reset");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("ACCOUNT_NOT_MANAGEABLE");
    }

    /** Hai đường đều là quyền của Admin Master. */
    @Test
    void chiAdminMasterDuocDung() throws Exception {
        for (String username : new String[] { "admin.hcm", "GVHCM001", "B26DCCN001" }) {
            assertThat(signedIn(username).post("/api/accounts/" + SV + "/password-reset").statusCode())
                    .as(username).isEqualTo(403);
            assertThat(signedIn(username)
                    .put("/api/accounts/" + SV + "/email", "{\"email\":\"x@gmail.com\"}").statusCode())
                    .as(username).isEqualTo(403);
        }
    }

    // --- Tiện ích --------------------------------------------------------

    private Map<String, Object> taiKhoan() {
        return jdbc.queryForMap("""
                SELECT MatKhauHash, Email, EmailDaXacMinh, ThoiDiemXacMinhEmail
                  FROM dbo.TaiKhoan WHERE TenDangNhap = ?
                """, SV);
    }

    private static String giuaHaiDauNhay(String json, String sau) {
        int from = json.indexOf(sau) + sau.length();
        return json.substring(from, json.indexOf('"', from));
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
            return login(username, PASSWORD);
        }

        HttpResponse<String> login(String username, String password) throws Exception {
            return send("POST", "/api/auth/login",
                    "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> put(String path, String json) throws Exception {
            return send("PUT", path, json);
        }

        HttpResponse<String> post(String path) throws Exception {
            return post(path, "");
        }

        HttpResponse<String> post(String path, String json) throws Exception {
            return send("POST", path, json);
        }

        private HttpResponse<String> send(String method, String path, String json) throws Exception {
            /* Lấy token trước MỌI request ghi nếu chưa có: `/api/auth/activate`
               gọi được khi chưa đăng nhập, nên trình duyệt này có thể chưa đi
               qua `login()` — mà CSRF thì vẫn bắt buộc. */
            if (cookie("XSRF-TOKEN") == null) {
                assertThat(get("/api/auth/csrf").statusCode()).isEqualTo(200);
            }
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json));
            String xsrf = cookie("XSRF-TOKEN");
            if (xsrf != null) {
                request.header("X-XSRF-TOKEN", xsrf);
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
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
