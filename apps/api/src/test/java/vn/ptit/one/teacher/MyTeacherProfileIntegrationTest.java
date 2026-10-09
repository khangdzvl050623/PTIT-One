package vn.ptit.one.teacher;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
 * Hồ sơ của chính giảng viên: {@code /api/me/teacher-profile}.
 *
 * <p>Đối xứng với {@code MyProfileIntegrationTest} của sinh viên, kể cả cách
 * chụp–hoàn nguyên fixture: seed không điền lý lịch giảng viên, nhưng dọn bằng
 * cách ghi {@code NULL} vẫn sai nguyên tắc — test không được để lại dấu vết.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class MyTeacherProfileIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";
    private static final String GV = "GVHCM001";
    private static final String PROFILE = "/api/me/teacher-profile";
    private static final String AVATAR = PROFILE + "/avatar";

    private static final String COT_LY_LICH =
            "GioiTinh, DienThoai, SoCCCD, EmailCaNhan, NoiSinh, DanToc, TonGiao, HoKhau, AnhDaiDien";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private Map<String, Object> lyLichBanDau;
    private Map<String, Object> emailBanDau;

    @BeforeEach
    void snapshot() {
        lyLichBanDau = jdbc.queryForMap(
                "SELECT %s FROM dbo.GiangVien WHERE MaGiangVien = ?".formatted(COT_LY_LICH), GV);
        emailBanDau = jdbc.queryForMap("""
                SELECT Email, EmailDaXacMinh, ThoiDiemXacMinhEmail
                  FROM dbo.TaiKhoan WHERE TenDangNhap = ?
                """, GV);
    }

    @AfterEach
    void restore() {
        jdbc.update("""
                UPDATE dbo.TaiKhoan
                   SET Email = ?, EmailDaXacMinh = ?, ThoiDiemXacMinhEmail = ?
                 WHERE TenDangNhap = ?
                """, emailBanDau.get("Email"), emailBanDau.get("EmailDaXacMinh"),
                emailBanDau.get("ThoiDiemXacMinhEmail"), GV);
        jdbc.update("""
                UPDATE dbo.GiangVien
                   SET GioiTinh = ?, DienThoai = ?, SoCCCD = ?, EmailCaNhan = ?,
                       NoiSinh = ?, DanToc = ?, TonGiao = ?, HoKhau = ?, AnhDaiDien = ?
                 WHERE MaGiangVien = ?
                """, lyLichBanDau.get("GioiTinh"), lyLichBanDau.get("DienThoai"),
                lyLichBanDau.get("SoCCCD"), lyLichBanDau.get("EmailCaNhan"),
                lyLichBanDau.get("NoiSinh"), lyLichBanDau.get("DanToc"),
                lyLichBanDau.get("TonGiao"), lyLichBanDau.get("HoKhau"),
                lyLichBanDau.get("AnhDaiDien"), GV);
    }

    private void verifyEmail() {
        jdbc.update("""
                UPDATE dbo.TaiKhoan
                   SET Email = COALESCE(Email, 'gvhcm001.test@ptithcm.edu.vn'),
                       EmailDaXacMinh = 1,
                       ThoiDiemXacMinhEmail = COALESCE(ThoiDiemXacMinhEmail, SYSUTCDATETIME())
                 WHERE TenDangNhap = ?
                """, GV);
    }

    /** Hồ sơ gộp `GiangVien` + `Khoa` + `CoSo` + email tài khoản. */
    @Test
    void xemDuocHoSoCuaChinhMinh() throws Exception {
        HttpResponse<String> response = signedIn(GV).get(PROFILE);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("\"maGiangVien\":\"" + GV + "\"",
                "\"tenKhoa\":", "\"tenCoSo\":", "\"maCoSo\":\"HCM\"");
    }

    @Test
    void chuaXacMinhEmailThiKhongSuaDuoc() throws Exception {
        /* Tự đặt trạng thái thay vì tin seed: tài khoản này có thể đã được xác
           minh khi ai đó thử giao diện, và khi đó ca này im lặng mất tác dụng. */
        jdbc.update("UPDATE dbo.TaiKhoan SET EmailDaXacMinh = 0 WHERE TenDangNhap = ?", GV);

        HttpResponse<String> response = signedIn(GV).put(PROFILE, "{\"gioiTinh\":\"NU\"}");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(409);
        assertThat(response.body()).contains("EMAIL_NOT_VERIFIED");
        assertThat(lyLich()).isEqualTo(lyLichBanDau);
    }

    @Test
    void xacMinhEmailRoiThiSuaDuoc() throws Exception {
        verifyEmail();

        HttpResponse<String> response = signedIn(GV).put(PROFILE, """
                {"gioiTinh":"NAM","dienThoai":"0912345678","soCCCD":"079190002222",
                 "emailCaNhan":"viet.dang@gmail.com","noiSinh":"Hà Nội",
                 "danToc":"Kinh","tonGiao":"Không","hoKhau":"5 Lê Lợi, Quận 1"}
                """);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("\"gioiTinh\":\"NAM\"", "\"soCCCD\":\"079190002222\"");
        assertThat(cot("DienThoai")).isEqualTo("0912345678");
        assertThat(cot("HoKhau")).isEqualTo("5 Lê Lợi, Quận 1");
    }

    /** Học vị và khoa là dữ liệu hành chính — gửi kèm cũng không đổi được. */
    @Test
    void khongSuaDuocDuLieuHanhChinh() throws Exception {
        verifyEmail();
        // Chụp giá trị thật thay vì viết cứng: hồ sơ này do Phòng Đào tạo sửa được.
        Map<String, Object> hanhChinhCu = jdbc.queryForMap(
                "SELECT HoTen, HocVi, MaKhoa, MaCoSo FROM dbo.GiangVien WHERE MaGiangVien = ?", GV);

        HttpResponse<String> response = signedIn(GV).put(PROFILE, """
                {"gioiTinh":"NAM","hocVi":"Giáo sư","maKhoa":"CNTT1","hoTen":"Tên Giả"}
                """);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(jdbc.queryForMap(
                "SELECT HoTen, HocVi, MaKhoa, MaCoSo FROM dbo.GiangVien WHERE MaGiangVien = ?", GV))
                .as("ô hành chính gửi kèm phải bị bỏ qua")
                .isEqualTo(hanhChinhCu);
    }

    @Test
    void duLieuSaiDinhDangThiBiChan() throws Exception {
        verifyEmail();
        Browser gv = signedIn(GV);

        for (String than : new String[] {
                "{\"gioiTinh\":\"KHAC\"}", "{\"soCCCD\":\"123\"}",
                "{\"emailCaNhan\":\"khong-phai-email\"}", "{\"dienThoai\":\"abc\"}" }) {
            HttpResponse<String> response = gv.put(PROFILE, than);
            assertThat(response.statusCode()).as(than).isEqualTo(400);
            assertThat(response.body()).as(than).contains("VALIDATION_ERROR");
        }
    }

    @Test
    void khongPhaiAnhThiBiChan() throws Exception {
        verifyEmail();

        HttpResponse<String> response = signedIn(GV).upload(AVATAR, "khong phai anh".getBytes(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(response.body()).contains("IMAGE_FORMAT_INVALID");
        assertThat(cot("AnhDaiDien")).isNull();
    }

    @Test
    void goAnhDuocKeCaKhiChuaCauHinhKhoAnh() throws Exception {
        verifyEmail();
        jdbc.update("UPDATE dbo.GiangVien SET AnhDaiDien = ? WHERE MaGiangVien = ?",
                "https://res.cloudinary.com/demo/image/upload/v1/cu.jpg", GV);

        HttpResponse<String> response = signedIn(GV).delete(AVATAR);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(cot("AnhDaiDien")).isNull();
    }

    /** Hồ sơ giảng viên là của giảng viên; vai trò khác không có. */
    @Test
    void chiGiangVienCoHoSoGiangVien() throws Exception {
        for (String username : new String[] { "B26DCCN001", "admin.hcm", "admin.master" }) {
            assertThat(signedIn(username).get(PROFILE).statusCode()).as(username).isEqualTo(403);
            assertThat(signedIn(username).put(PROFILE, "{}").statusCode()).as(username).isEqualTo(403);
            assertThat(signedIn(username).delete(AVATAR).statusCode()).as(username).isEqualTo(403);
        }
    }

    // --- Tiện ích --------------------------------------------------------

    private String cot(String ten) {
        return jdbc.queryForObject(
                "SELECT %s FROM dbo.GiangVien WHERE MaGiangVien = ?".formatted(ten), String.class, GV);
    }

    private Map<String, Object> lyLich() {
        return jdbc.queryForMap(
                "SELECT %s FROM dbo.GiangVien WHERE MaGiangVien = ?".formatted(COT_LY_LICH), GV);
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

        HttpResponse<String> upload(String path, byte[] bytes) throws Exception {
            String boundary = "----ptitone" + System.nanoTime();
            var out = new java.io.ByteArrayOutputStream();
            out.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"anh.png\"\r\n"
                    + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

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
