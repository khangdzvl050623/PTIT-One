package vn.ptit.one.auth;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * F02 trên SQL Server CENTRAL thật: Admin Master cấp hồ sơ + tài khoản, người
 * dùng kích hoạt bằng mã một lần, Admin khoá/mở tài khoản.
 *
 * <p>Hồ sơ thử {@code B99TEST001} / {@code GV99TEST01} tạo qua API, xoá sạch sau mỗi ca.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class AccountProvisioningIntegrationTest {

    private static final String SEED_PASSWORD = "PtitOne@2026";
    private static final String NEW_PASSWORD = "MatKhauMoi#2026";
    private static final String SV = "B99TEST001";
    private static final String GV = "GV99TEST01";
    private static final String SV_BODY = """
            {"maSinhVien":"%s","hoTen":"Sinh Viên Thử","ngaySinh":"2008-05-05",
             "maCoSoNha":"HCM","maCTDT":"CN-CNTT-2022"}""".formatted(SV);
    private static final Pattern CODE = Pattern.compile("\"maKichHoat\":\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        for (String user : new String[] { SV, GV }) {
            jdbc.update("""
                    DELETE t FROM dbo.TokenLamMoi t JOIN dbo.PhienDangNhap p ON p.MaPhien = t.MaPhien
                     WHERE p.TenDangNhap = ?
                    """, user);
            jdbc.update("DELETE FROM dbo.PhienDangNhap WHERE TenDangNhap = ?", user);
            jdbc.update("DELETE FROM dbo.MaKichHoat WHERE TenDangNhap = ?", user);
            jdbc.update("DELETE FROM dbo.TaiKhoan WHERE TenDangNhap = ?", user);
            jdbc.update("DELETE FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = ?", user);
        }
        jdbc.update("DELETE FROM dbo.SinhVien WHERE MaSinhVien = ?", SV);
        jdbc.update("DELETE FROM dbo.GiangVien WHERE MaGiangVien = ?", GV);
    }

    /** Collation không phân biệt hoa thường: "hcm" phải được lưu thành mã chuẩn "HCM" ở cả ba bảng. */
    @Test
    void maCoSoGoThuongDuocLuuThanhMaChuan() throws Exception {
        HttpResponse<String> created = signedIn("admin.master", SEED_PASSWORD)
                .post("/api/students", SV_BODY.replace("\"HCM\"", "\"hcm\""));

        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(created.body()).contains("\"maCoSoNha\":\"HCM\"");
        assertThat(jdbc.queryForObject("""
                SELECT s.MaCoSoNha + '|' + d.MaCoSo + '|' + t.MaCoSo
                  FROM dbo.SinhVien s
                  JOIN dbo.DanhBaNguoiDung d ON d.TenDangNhap = s.MaSinhVien
                  JOIN dbo.TaiKhoan t ON t.TenDangNhap = s.MaSinhVien
                 WHERE s.MaSinhVien = ?
                """, String.class, SV)).isEqualTo("HCM|HCM|HCM");
    }

    /** Luồng chính: cấp → chưa đăng nhập được → kích hoạt → đăng nhập → mã không dùng lại được. */
    @Test
    void capKichHoatRoiDangNhap() throws Exception {
        HttpResponse<String> created = signedIn("admin.master", SEED_PASSWORD).post("/api/students", SV_BODY);

        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(created.body()).contains("\"maSinhVien\":\"" + SV + "\"", "\"tenDangNhap\":\"" + SV + "\"");
        String code = codeOf(created);
        assertThat(code).matches("[0-9A-Z]{4}(-[0-9A-Z]{4}){3}");
        assertThat(jdbc.queryForObject("SELECT TrangThai FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = ?",
                String.class, SV)).isEqualTo("HOAT_DONG");

        assertThat(new Browser().login(SV, NEW_PASSWORD).statusCode()).isEqualTo(401);

        // Gõ thường, không gạch vẫn khớp.
        String typed = code.replace("-", "").toLowerCase();
        assertThat(activate(SV, typed, NEW_PASSWORD).statusCode()).isEqualTo(204);
        Browser sv = new Browser();
        assertThat(sv.login(SV, NEW_PASSWORD).statusCode()).isEqualTo(200);
        assertThat(sv.get("/api/auth/me").body()).contains("\"role\":\"SINH_VIEN\"");

        HttpResponse<String> again = activate(SV, code, "MotMatKhauKhac#1");
        assertThat(again.statusCode()).isEqualTo(400);
        assertThat(again.body()).contains("ACTIVATION_INVALID");
        assertThat(new Browser().login(SV, NEW_PASSWORD).statusCode()).isEqualTo(200);
    }

    @Test
    void capGiangVienCungKichHoatDuoc() throws Exception {
        HttpResponse<String> created = signedIn("admin.master", SEED_PASSWORD).post("/api/teachers", """
                {"maGiangVien":"%s","hoTen":"Giảng Viên Thử","maCoSo":"HN","maKhoa":"CNTT","hocVi":"Thạc sĩ"}
                """.formatted(GV));

        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(activate(GV, codeOf(created), NEW_PASSWORD).statusCode()).isEqualTo(204);
        assertThat(new Browser().login(GV, NEW_PASSWORD).statusCode()).isEqualTo(200);
    }

    /** Cấp tài khoản chỉ ở Master: Admin cơ sở, GV, SV đều 403 và không ghi gì. */
    @Test
    void chiAdminMasterDuocCap() throws Exception {
        for (String user : new String[] { "admin.hcm", "GVHCM001", "B26DCCN001" }) {
            assertThat(signedIn(user, SEED_PASSWORD).post("/api/students", SV_BODY).statusCode())
                    .as(user).isEqualTo(403);
        }
        assertThat(signedIn("admin.hcm", SEED_PASSWORD).get("/api/accounts").statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SinhVien WHERE MaSinhVien = ?",
                Integer.class, SV)).isZero();
    }

    @Test
    void trungMaHoacSaiDanhMucThiKhongDeLaiGi() throws Exception {
        Browser admin = signedIn("admin.master", SEED_PASSWORD);

        assertThat(admin.post("/api/students", SV_BODY.replace(SV, "B26DCCN001")).body()).contains("ACCOUNT_EXISTS");
        assertThat(admin.post("/api/students", SV_BODY.replace("CN-CNTT-2022", "KHONG-CO")).body())
                .contains("PROGRAM_NOT_FOUND");
        assertThat(admin.post("/api/students", SV_BODY.replace("\"HCM\"", "\"XYZ\"")).body())
                .contains("CAMPUS_NOT_FOUND");
        // CTDT sai làm rollback cả giao dịch: không có hồ sơ, danh bạ hay tài khoản mồ côi.
        assertThat(jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM dbo.SinhVien WHERE MaSinhVien = ?)
                     + (SELECT COUNT(*) FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = ?)
                """, Integer.class, SV, SV)).isZero();

        assertThat(admin.post("/api/students", SV_BODY).statusCode()).isEqualTo(201);
        assertThat(admin.post("/api/students", SV_BODY).body()).contains("ACCOUNT_EXISTS");
    }

    /** Sai 5 lần thì mã bị thu hồi, kể cả khi lần sau nhập đúng; cấp lại thì mã cũ hết hiệu lực. */
    @Test
    void saiNamLanThiThuHoiMaVaCapLaiDuoc() throws Exception {
        Browser admin = signedIn("admin.master", SEED_PASSWORD);
        String code = codeOf(admin.post("/api/students", SV_BODY));

        for (int i = 0; i < 5; i++) {
            assertThat(activate(SV, "0000-0000-0000-0000", NEW_PASSWORD).statusCode()).isEqualTo(400);
        }
        assertThat(activate(SV, code, NEW_PASSWORD).body()).contains("ACTIVATION_INVALID");

        HttpResponse<String> reissued = admin.post("/api/accounts/" + SV + "/activation-code", "");
        assertThat(reissued.statusCode()).isEqualTo(201);
        String replaced = codeOf(reissued);
        String latest = codeOf(admin.post("/api/accounts/" + SV + "/activation-code", ""));
        assertThat(activate(SV, replaced, NEW_PASSWORD).statusCode()).isEqualTo(400);
        assertThat(activate(SV, latest, NEW_PASSWORD).statusCode()).isEqualTo(204);

        assertThat(admin.post("/api/accounts/" + SV + "/activation-code", "").body())
                .contains("ACCOUNT_ALREADY_ACTIVATED");
    }

    @Test
    void matKhauChuaTenDangNhapBiTuChoi() throws Exception {
        String code = codeOf(signedIn("admin.master", SEED_PASSWORD).post("/api/students", SV_BODY));

        HttpResponse<String> response = activate(SV, code, SV.toLowerCase() + "!x");

        assertThat(response.body()).contains("PASSWORD_TOO_WEAK");
        // Mã chưa bị đốt: nhập lại với mật khẩu hợp lệ vẫn được.
        assertThat(activate(SV, code, NEW_PASSWORD).statusCode()).isEqualTo(204);
    }

    /** Khoá: phiên đang mở bị từ chối ngay, đăng nhập lại không được; mở lại thì vào được. */
    @Test
    void khoaThuHoiPhienVaMoLaiDuoc() throws Exception {
        Browser admin = signedIn("admin.master", SEED_PASSWORD);
        activate(SV, codeOf(admin.post("/api/students", SV_BODY)), NEW_PASSWORD);
        Browser sv = signedIn(SV, NEW_PASSWORD);

        HttpResponse<String> locked = admin.put("/api/accounts/" + SV + "/status", "{\"trangThai\":\"NGUNG\"}");

        assertThat(locked.statusCode()).as(locked.body()).isEqualTo(200);
        assertThat(locked.body()).contains("\"trangThai\":\"NGUNG\"", "\"daKichHoat\":true");
        assertThat(sv.get("/api/auth/me").statusCode()).isEqualTo(401);
        assertThat(new Browser().login(SV, NEW_PASSWORD).statusCode()).isEqualTo(401);

        assertThat(admin.put("/api/accounts/" + SV + "/status", "{\"trangThai\":\"HOAT_DONG\"}").statusCode())
                .isEqualTo(200);
        assertThat(new Browser().login(SV, NEW_PASSWORD).statusCode()).isEqualTo(200);

        assertThat(admin.put("/api/accounts/admin.master/status", "{\"trangThai\":\"NGUNG\"}").body())
                .contains("ACCOUNT_NOT_MANAGEABLE");
        assertThat(admin.get("/api/accounts?maCoSo=HCM&loaiNguoiDung=SINH_VIEN").body())
                .contains("\"tenDangNhap\":\"" + SV + "\"").doesNotContain("admin.hcm");
    }

    // --- Tiện ích --------------------------------------------------------

    private HttpResponse<String> activate(String user, String code, String password) throws Exception {
        Browser browser = new Browser();
        browser.get("/api/auth/csrf");
        return browser.post("/api/auth/activate", """
                {"tenDangNhap":"%s","maKichHoat":"%s","matKhauMoi":"%s"}""".formatted(user, code, password));
    }

    private static String codeOf(HttpResponse<String> response) {
        Matcher matcher = CODE.matcher(response.body());
        assertThat(matcher.find()).as(response.body()).isTrue();
        return matcher.group(1);
    }

    private Browser signedIn(String username, String password) throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login(username, password).statusCode()).as("đăng nhập %s", username).isEqualTo(200);
        return browser;
    }

    /** Trình duyệt tối giản: giữ cookie, gửi CSRF header như SPA. */
    private final class Browser {

        private final CookieManager cookies = new CookieManager();
        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();

        HttpResponse<String> login(String username, String password) throws Exception {
            assertThat(get("/api/auth/csrf").statusCode()).isEqualTo(200);
            return send("POST", "/api/auth/login",
                    "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String path, String json) throws Exception {
            return send("POST", path, json);
        }

        HttpResponse<String> put(String path, String json) throws Exception {
            return send("PUT", path, json);
        }

        private HttpResponse<String> send(String method, String path, String json) throws Exception {
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
