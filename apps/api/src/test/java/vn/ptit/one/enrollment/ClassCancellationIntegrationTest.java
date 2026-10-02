package vn.ptit.one.enrollment;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.enrollment.service.ClassCancellationService;
import vn.ptit.one.shared.exception.ApiException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Huỷ lớp học phần trên SQL Server CENTRAL thật.
 *
 * <p>Lớp thử {@code TEST-HUY-LOP} (môn INT1445, không tiên quyết, không lịch,
 * GVHCM001 dạy) tạo bằng SQL. B26DCCN001 và B25DCCN001 đăng ký qua API. Sau mỗi
 * ca: huỷ lớp nếu còn mở (để tín chỉ về đúng chỗ) rồi xoá mọi dòng của lớp thử.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class ClassCancellationIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";
    private static final String LOP = "TEST-HUY-LOP";
    private static final String CANCEL = "/api/classes/" + LOP + "/cancel";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ClassCancellationService cancellations;

    @BeforeEach
    void createClass() {
        dropClass();
        jdbc.update("""
                INSERT INTO dbo.LopHocPhan (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, MaGiangVien, SoLuongToiDa,
                                            TrangThai, ChoPhepLienCoSo, HinhThucHoc)
                VALUES (?, 'INT1445', '2026-1', 'HCM', 'GVHCM001', 5, 'MO', 0, 'TRUC_TIEP')
                """, LOP);
    }

    @AfterEach
    void dropClass() {
        jdbc.update("UPDATE dbo.Diem SET DiemChuyenCan = NULL WHERE MaLopHP = ?", LOP);
        jdbc.update("UPDATE dbo.LopHocPhan SET TrangThai = 'MO' WHERE MaLopHP = ? AND TrangThai = 'DA_KHOA'", LOP);
        try {
            cancellations.cancel(new AuthenticatedUser("admin.hcm", Role.ADMIN_CO_SO, null, "HCM",
                    UUID.randomUUID(), 1, Instant.now().plusSeconds(60)), LOP, null);
        } catch (ApiException ignored) {
            // Lớp chưa được tạo ở ca đầu tiên.
        }
        jdbc.update("""
                DELETE n FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao
                 WHERE t.MaLopHP = ?
                """, LOP);
        jdbc.update("DELETE FROM dbo.ThongBao WHERE MaLopHP = ?", LOP);
        jdbc.update("DELETE FROM dbo.Diem WHERE MaLopHP = ?", LOP);
        jdbc.update("DELETE FROM dbo.DangKyMonHoc WHERE MaLopHP = ?", LOP);
        jdbc.update("DELETE FROM dbo.DangKyHocPhan WHERE MaLopHP = ?", LOP);
        jdbc.update("DELETE FROM dbo.LopHocPhan WHERE MaLopHP = ?", LOP);
    }

    /** Huỷ lớp: ghi danh hai phía huỷ, tín chỉ trả, điểm rỗng xoá, sĩ số về 0, cả SV lẫn GV được báo. */
    @Test
    void huyLopTraTinChiVaBaoCaSinhVienLanGiangVien() throws Exception {
        Browser an = signedIn("B26DCCN001");
        Browser huy = signedIn("B25DCCN001");
        int tinChiAn = tinChi("B26DCCN001");
        assertThat(an.post("/api/me/enrollments", "{\"maLopHP\":\"" + LOP + "\"}").statusCode()).isEqualTo(201);
        assertThat(huy.post("/api/me/enrollments", "{\"maLopHP\":\"" + LOP + "\"}").statusCode()).isEqualTo(201);

        HttpResponse<String> response = signedIn("admin.hcm").post(CANCEL, "{\"lyDo\":\"Không đủ sinh viên\"}");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("\"soDangKyDaHuy\":2", "\"trangThai\":\"DA_HUY\"", "\"soLuongDaDangKy\":0");
        assertThat(tinChi("B26DCCN001")).isEqualTo(tinChiAn);
        assertThat(count("SELECT COUNT(*) FROM dbo.DangKyHocPhan WHERE MaLopHP = ? AND TrangThai <> 'DA_HUY'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM dbo.DangKyMonHoc WHERE MaLopHP = ? AND TrangThai <> 'DA_HUY'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM dbo.Diem WHERE MaLopHP = ?")).isZero();
        assertThat(an.get("/api/me/enrollments?maHocKy=2026-1").body()).doesNotContain(LOP);

        assertThat(an.get("/api/me/notifications?chuaDoc=true").body())
                .contains("\"suKien\":\"LOP_BI_HUY\"", "Không đủ sinh viên", "Xem lớp còn chỗ");
        assertThat(signedIn("GVHCM001").get("/api/me/notifications?chuaDoc=true").body())
                .contains("\"suKien\":\"LOP_BI_HUY\"", "bạn phụ trách");
    }

    /** Gọi lại trên lớp đã huỷ: 200, không làm gì, không báo lần hai. */
    @Test
    void huyLaiLaKhongLamGi() throws Exception {
        signedIn("B26DCCN001").post("/api/me/enrollments", "{\"maLopHP\":\"" + LOP + "\"}");
        Browser admin = signedIn("admin.hcm");

        assertThat(admin.post(CANCEL, "{}").statusCode()).isEqualTo(200);
        HttpResponse<String> again = admin.post(CANCEL, "{}");

        assertThat(again.statusCode()).isEqualTo(200);
        assertThat(again.body()).contains("\"soDangKyDaHuy\":0");
        assertThat(count("""
                SELECT COUNT(*) FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao
                 WHERE t.MaLopHP = ? AND t.SuKien = 'LOP_BI_HUY' AND n.MaNguoiNhan = 'B26DCCN001'
                """)).isEqualTo(1);
    }

    /** Đã có điểm thì không huỷ, và không có gì thay đổi. */
    @Test
    void daCoDiemThiKhongHuy() throws Exception {
        signedIn("B26DCCN001").post("/api/me/enrollments", "{\"maLopHP\":\"" + LOP + "\"}");
        jdbc.update("UPDATE dbo.Diem SET DiemChuyenCan = 9 WHERE MaLopHP = ?", LOP);

        HttpResponse<String> response = signedIn("admin.hcm").post(CANCEL, "{}");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("CLASS_HAS_GRADES", "B26DCCN001");
        assertThat(count("SELECT COUNT(*) FROM dbo.LopHocPhan WHERE MaLopHP = ? AND TrangThai = 'MO' AND SoLuongDaDangKy = 1"))
                .isEqualTo(1);
    }

    @Test
    void lopDaKhoaDiemThiKhongHuy() throws Exception {
        jdbc.update("UPDATE dbo.LopHocPhan SET TrangThai = 'DA_KHOA' WHERE MaLopHP = ?", LOP);

        HttpResponse<String> response = signedIn("admin.hcm").post(CANCEL, "{}");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("GRADE_LOCKED");
    }

    @Test
    void chiAdminCoSoCuaLopDuocHuy() throws Exception {
        for (String username : new String[] { "admin.hn", "admin.master", "GVHCM001", "B26DCCN001" }) {
            assertThat(signedIn(username).post(CANCEL, "{}").statusCode()).as(username).isEqualTo(403);
        }
    }

    /** DA_HUY chỉ đi qua thao tác huỷ lớp; lớp đã huỷ không sửa và không đăng ký được. */
    @Test
    void khongDatThangDaHuyVaLopDaHuyKhongDungDuoc() throws Exception {
        Browser admin = signedIn("admin.hcm");
        String body = "{\"soLuongToiDa\":5,\"trangThai\":\"%s\",\"hinhThucHoc\":\"TRUC_TIEP\",\"choPhepLienCoSo\":false}";

        assertThat(admin.put("/api/classes/" + LOP, body.formatted("DA_HUY")).body()).contains("CLASS_STATUS_INVALID");

        admin.post(CANCEL, "{}");
        assertThat(admin.put("/api/classes/" + LOP, body.formatted("MO")).body()).contains("CLASS_CANCELLED");
        assertThat(signedIn("B26DCCN001").post("/api/me/enrollments", "{\"maLopHP\":\"" + LOP + "\"}").body())
                .contains("CLASS_NOT_OPEN");
    }

    // --- Tiện ích --------------------------------------------------------

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class, LOP);
    }

    private int tinChi(String maSinhVien) {
        return jdbc.queryForObject(
                "SELECT SoTinChiDaDangKy FROM dbo.SinhVienHocKy WHERE MaSinhVien = ? AND MaHocKy = '2026-1'",
                Integer.class, maSinhVien);
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
