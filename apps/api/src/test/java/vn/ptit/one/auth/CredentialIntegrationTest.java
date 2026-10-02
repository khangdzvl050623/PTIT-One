package vn.ptit.one.auth;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import vn.ptit.one.auth.security.AttemptLimiter;
import vn.ptit.one.shared.mail.Mailer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * A1 trên SQL Server CENTRAL thật: mã kích hoạt qua thư, email + xác minh, đổi
 * mật khẩu, quên/khôi phục mật khẩu, giới hạn tần suất.
 *
 * <p>{@link Mailer} được thay bằng mock: test đọc mã từ thư "đã gửi" thay vì
 * gửi thật qua Brevo. Tài khoản thử {@code B99TEST002} tạo qua API, xoá sau mỗi ca.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class CredentialIntegrationTest {

    private static final String SEED_PASSWORD = "PtitOne@2026";
    private static final String PASSWORD = "MatKhauMoi#2026";
    private static final String SV = "B99TEST002";
    private static final String EMAIL = "b99test002@example.com";
    private static final Pattern ACTIVATION = Pattern.compile("[0-9A-Z]{4}(-[0-9A-Z]{4}){3}");
    private static final Pattern OTP = Pattern.compile("\\b\\d{6}\\b");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AttemptLimiter limiter;

    @MockitoBean
    private Mailer mailer;

    @BeforeEach
    void setUp() {
        cleanUp();
        when(mailer.enabled()).thenReturn(true);
        // Bộ đếm sống cùng context Spring: ca trước không được làm ca sau bị 429.
        for (String key : new String[] { "login:user:b99test002", "login:ip:127.0.0.1", "password:user:b99test002",
                "recover:user:b99test002", "recover:ip:127.0.0.1", "reset:ip:127.0.0.1" }) {
            limiter.reset(key);
        }
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("""
                DELETE t FROM dbo.TokenLamMoi t JOIN dbo.PhienDangNhap p ON p.MaPhien = t.MaPhien
                 WHERE p.TenDangNhap = ?
                """, SV);
        jdbc.update("DELETE FROM dbo.PhienDangNhap WHERE TenDangNhap = ?", SV);
        jdbc.update("DELETE FROM dbo.MaXacThuc WHERE TenDangNhap = ?", SV);
        jdbc.update("DELETE FROM dbo.MaKichHoat WHERE TenDangNhap = ?", SV);
        jdbc.update("DELETE FROM dbo.TaiKhoan WHERE TenDangNhap = ?", SV);
        jdbc.update("DELETE FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = ?", SV);
        jdbc.update("DELETE FROM dbo.SinhVien WHERE MaSinhVien = ?", SV);
    }

    /** Có email → mã chỉ đi qua thư, Admin không thấy; kích hoạt được thì email coi như đã xác minh. */
    @Test
    void maKichHoatQuaThuVaKichHoatXacMinhEmail() throws Exception {
        HttpResponse<String> created = provision(EMAIL);

        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(created.body()).contains("\"maKichHoat\":null", "\"guiToiEmail\":\"" + EMAIL + "\"");
        assertThat(activate(find(ACTIVATION, lastMailTo(EMAIL))).statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT EmailDaXacMinh FROM dbo.TaiKhoan WHERE TenDangNhap = ?",
                Boolean.class, SV)).isTrue();
        assertThat(signedIn(PASSWORD).get("/api/auth/email").body())
                .contains("\"email\":\"" + EMAIL + "\"", "\"daXacMinh\":true");
    }

    /** Gửi lại với guiEmail=false: Admin nhận mã trao tay, và kích hoạt KHÔNG xác minh email. */
    @Test
    void capLaiMaTraoTayKhongXacMinhEmail() throws Exception {
        provision(EMAIL);
        HttpResponse<String> reissued = admin().post("/api/accounts/" + SV + "/activation-code?guiEmail=false", "");

        assertThat(activate(find(ACTIVATION, reissued.body())).statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT EmailDaXacMinh FROM dbo.TaiKhoan WHERE TenDangNhap = ?",
                Boolean.class, SV)).isFalse();
    }

    /** Mã khôi phục chỉ đi tới email ĐÃ LƯU và đã xác minh; email gõ vào chỉ để đối chiếu. */
    @Test
    void quenMatKhauGuiToiEmailDaLuuRoiDatLai() throws Exception {
        activatedWithVerifiedEmail();
        Browser oldSession = signedIn(PASSWORD);

        assertThat(forgot("nguoi-khac@example.com").statusCode()).isEqualTo(202);
        assertThat(mailsTo("nguoi-khac@example.com")).isEmpty();
        assertThat(forgot(EMAIL.toUpperCase()).statusCode()).isEqualTo(202);
        String code = find(OTP, lastMailTo(EMAIL));

        HttpResponse<String> wrong = reset(code.equals("000000") ? "111111" : "000000", "KhongPhaiDau#1");
        assertThat(wrong.statusCode()).isEqualTo(400);
        assertThat(wrong.body()).contains("RESET_CODE_INVALID");
        assertThat(reset(code, "DatLaiMoi#2026").statusCode()).isEqualTo(204);

        assertThat(oldSession.get("/api/auth/me").statusCode()).isEqualTo(401);
        assertThat(new Browser().login(SV, PASSWORD).statusCode()).isEqualTo(401);
        assertThat(new Browser().login(SV, "DatLaiMoi#2026").statusCode()).isEqualTo(200);
        assertThat(reset(code, "LanThuHai#2026").statusCode()).isEqualTo(400);
    }

    @Test
    void emailChuaXacMinhThiKhongGuiMaKhoiPhuc() throws Exception {
        provision(null);
        String code = find(ACTIVATION, admin().post("/api/accounts/" + SV + "/activation-code", "").body());
        activate(code);
        Browser sv = signedIn(PASSWORD);
        sv.put("/api/auth/email", "{\"email\":\"" + EMAIL + "\",\"matKhauHienTai\":\"" + PASSWORD + "\"}");
        int before = mailsTo(EMAIL).size();

        assertThat(forgot(EMAIL).statusCode()).isEqualTo(202);
        assertThat(mailsTo(EMAIL)).hasSize(before);
    }

    /** Đổi email cần mật khẩu hiện tại; mã gửi tới địa chỉ MỚI và xác minh nó. */
    @Test
    void doiEmailCanMatKhauVaXacMinhBangMa() throws Exception {
        activatedWithVerifiedEmail();
        Browser sv = signedIn(PASSWORD);
        String moi = "dia-chi-moi@example.com";

        assertThat(sv.put("/api/auth/email", "{\"email\":\"" + moi + "\",\"matKhauHienTai\":\"sai-mat-khau\"}").body())
                .contains("PASSWORD_INCORRECT");
        HttpResponse<String> changed = sv.put("/api/auth/email",
                "{\"email\":\"" + moi + "\",\"matKhauHienTai\":\"" + PASSWORD + "\"}");
        assertThat(changed.body()).contains("\"daXacMinh\":false");

        String code = find(OTP, lastMailTo(moi));
        assertThat(sv.post("/api/auth/email/verify", "{\"maXacThuc\":\"" + (code.equals("000000") ? "111111" : "000000") + "\"}")
                .body()).contains("EMAIL_CODE_INVALID");
        assertThat(sv.post("/api/auth/email/verify", "{\"maXacThuc\":\"" + code + "\"}").body())
                .contains("\"email\":\"" + moi + "\"", "\"daXacMinh\":true");
    }

    /** Đổi mật khẩu thu hồi mọi phiên kể cả phiên đang dùng. */
    @Test
    void doiMatKhauThuHoiMoiPhien() throws Exception {
        activatedWithVerifiedEmail();
        Browser sv = signedIn(PASSWORD);
        Browser other = signedIn(PASSWORD);

        assertThat(sv.post("/api/auth/change-password",
                "{\"matKhauHienTai\":\"sai\",\"matKhauMoi\":\"DoiMoi#2026\"}").body()).contains("PASSWORD_INCORRECT");
        assertThat(sv.post("/api/auth/change-password",
                "{\"matKhauHienTai\":\"" + PASSWORD + "\",\"matKhauMoi\":\"" + PASSWORD + "\"}").body())
                .contains("PASSWORD_UNCHANGED");
        assertThat(sv.post("/api/auth/change-password",
                "{\"matKhauHienTai\":\"" + PASSWORD + "\",\"matKhauMoi\":\"DoiMoi#2026\"}").statusCode()).isEqualTo(204);

        assertThat(other.get("/api/auth/me").statusCode()).isEqualTo(401);
        assertThat(new Browser().login(SV, "DoiMoi#2026").statusCode()).isEqualTo(200);
    }

    /** Xin mã khôi phục lần thứ 4 trong 15 phút → 429; đăng nhập sai 10 lần → 429 kể cả mật khẩu đúng. */
    @Test
    void gioiHanTanSuat() throws Exception {
        activatedWithVerifiedEmail();

        for (int i = 0; i < 3; i++) {
            assertThat(forgot(EMAIL).statusCode()).isEqualTo(202);
        }
        HttpResponse<String> blocked = forgot(EMAIL);
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(blocked.body()).contains("AUTH_TOO_MANY_ATTEMPTS");

        for (int i = 0; i < 10; i++) {
            assertThat(new Browser().login(SV, "sai-mat-khau").statusCode()).isEqualTo(401);
        }
        assertThat(new Browser().login(SV, PASSWORD).statusCode()).isEqualTo(429);
    }

    // --- Tiện ích --------------------------------------------------------

    private void activatedWithVerifiedEmail() throws Exception {
        provision(EMAIL);
        assertThat(activate(find(ACTIVATION, lastMailTo(EMAIL))).statusCode()).isEqualTo(204);
    }

    private HttpResponse<String> provision(String email) throws Exception {
        String emailJson = email == null ? "" : ",\"email\":\"" + email + "\"";
        return admin().post("/api/students", """
                {"maSinhVien":"%s","hoTen":"Sinh Viên Thử","maCoSoNha":"HCM","maCTDT":"CN-CNTT-2022"%s}
                """.formatted(SV, emailJson));
    }

    private HttpResponse<String> activate(String code) throws Exception {
        return anonymous().post("/api/auth/activate",
                "{\"tenDangNhap\":\"%s\",\"maKichHoat\":\"%s\",\"matKhauMoi\":\"%s\"}".formatted(SV, code, PASSWORD));
    }

    private HttpResponse<String> forgot(String email) throws Exception {
        return anonymous().post("/api/auth/forgot-password",
                "{\"tenDangNhap\":\"%s\",\"email\":\"%s\"}".formatted(SV, email));
    }

    private HttpResponse<String> reset(String code, String password) throws Exception {
        return anonymous().post("/api/auth/reset-password",
                "{\"tenDangNhap\":\"%s\",\"maXacThuc\":\"%s\",\"matKhauMoi\":\"%s\"}".formatted(SV, code, password));
    }

    /** Nội dung các thư "đã gửi" tới một địa chỉ, theo thứ tự gửi. */
    private List<String> mailsTo(String to) {
        return Mockito.mockingDetails(mailer).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("sendAfterCommit"))
                .map(Invocation::getArguments)
                .filter(args -> to.equals(args[0]))
                .map(args -> (String) args[2])
                .toList();
    }

    private String lastMailTo(String to) {
        List<String> mails = mailsTo(to);
        assertThat(mails).as("thư tới %s", to).isNotEmpty();
        return mails.getLast();
    }

    private static String find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        assertThat(matcher.find()).as(text).isTrue();
        return matcher.group();
    }

    private Browser admin() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login("admin.master", SEED_PASSWORD).statusCode()).isEqualTo(200);
        return browser;
    }

    private Browser signedIn(String password) throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login(SV, password).statusCode()).as("đăng nhập %s", SV).isEqualTo(200);
        return browser;
    }

    private Browser anonymous() throws Exception {
        Browser browser = new Browser();
        browser.get("/api/auth/csrf");
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
