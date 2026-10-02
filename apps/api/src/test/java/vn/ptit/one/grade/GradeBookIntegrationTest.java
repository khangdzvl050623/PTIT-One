package vn.ptit.one.grade;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nhập, công bố, khoá điểm (F06) và lịch dạy giảng viên trên SQL Server CENTRAL thật.
 *
 * <p>Sân thử: {@code INT1154-2026-1-HCM01} do GVHCM001 dạy, có đúng một sinh viên
 * B26DCCN003 với dòng {@code Diem} rỗng từ seed. Công bố và khoá không có đường
 * lui qua API, nên sau mỗi ca test tự đưa dòng điểm và trạng thái lớp về như cũ
 * bằng SQL trực tiếp.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class GradeBookIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";
    private static final String LOP = "INT1154-2026-1-HCM01";
    private static final String SV = "B26DCCN003";
    private static final String GRADES = "/api/classes/" + LOP + "/grades";
    private static final Pattern VERSION = Pattern.compile("\"version\":(\\d+)");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void restoreSandbox() {
        jdbc.update("""
                UPDATE dbo.Diem
                   SET DiemChuyenCan = NULL, DiemGiuaKy = NULL, DiemCuoiKy = NULL,
                       DiemTongKet = NULL, NgayCongBo = NULL
                 WHERE MaLopHP = ? AND MaSinhVien = ?
                """, LOP, SV);
        jdbc.update("UPDATE dbo.LopHocPhan SET TrangThai = 'MO' WHERE MaLopHP = ?", LOP);
    }

    // --- Nhập điểm -------------------------------------------------------

    /** Tổng kết do server tính: 0.9 + 2.25 + 4.8 = 7.95 → 8.0. Lưu xong vẫn là nháp. */
    @Test
    void giangVienLuuNhapThiServerTinhTongKet() throws Exception {
        Browser gv = signedIn("GVHCM001");

        HttpResponse<String> saved = gv.put(GRADES, row(SV, "9.0", "7.5", "8.0", version(gv)));

        assertThat(saved.statusCode()).as(saved.body()).isEqualTo(200);
        assertThat(saved.body()).contains("\"diemTongKet\":8.0", "\"ketQua\":\"DAT\"",
                "\"trangThai\":\"NHAP\"", "\"ngayCongBo\":null");
    }

    /** Hai người cùng sửa từ một phiên bản: người sau bị chặn, không ghi đè âm thầm. */
    @Test
    void haiLanSuaCungPhienBanThiLanSauBiChan() throws Exception {
        Browser gv = signedIn("GVHCM001");
        long cu = version(gv);

        assertThat(gv.put(GRADES, row(SV, "9.0", "7.5", "8.0", cu)).statusCode()).isEqualTo(200);
        HttpResponse<String> stale = gv.put(GRADES, row(SV, "1.0", "1.0", "1.0", cu));

        assertThat(stale.statusCode()).isEqualTo(409);
        assertThat(stale.body()).contains("GRADE_VERSION_CONFLICT");
        assertThat(gv.get(GRADES).body()).contains("\"diemTongKet\":8.0");
    }

    /** Một dòng lỗi thì cả loạt rollback — dòng hợp lệ đứng trước cũng không được ghi. */
    @Test
    void sinhVienNgoaiLopLamHongCaLoat() throws Exception {
        Browser gv = signedIn("GVHCM001");
        String body = """
                {"diem":[
                  {"maSinhVien":"%s","diemChuyenCan":9.0,"diemGiuaKy":7.5,"diemCuoiKy":8.0,"version":%d},
                  {"maSinhVien":"B25DCCN001","diemChuyenCan":5.0,"diemGiuaKy":5.0,"diemCuoiKy":5.0,"version":1}
                ]}
                """.formatted(SV, version(gv));

        HttpResponse<String> response = gv.put(GRADES, body);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("GRADE_STUDENT_NOT_ENROLLED");
        assertThat(gv.get(GRADES).body()).contains("\"diemTongKet\":null");
    }

    @Test
    void diemNgoaiKhoangBiTuChoi() throws Exception {
        Browser gv = signedIn("GVHCM001");

        HttpResponse<String> response = gv.put(GRADES, row(SV, "10.5", "7.5", "8.0", version(gv)));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("VALIDATION_ERROR");
    }

    // --- Quyền -----------------------------------------------------------

    @Test
    void chiGiangVienPhuTrachDuocNhapVaChiAdminCoSoDuocKhoa() throws Exception {
        long v = version(signedIn("GVHCM001"));

        // Admin không nhập thay giảng viên.
        assertThat(signedIn("admin.hcm").put(GRADES, row(SV, "9.0", "7.5", "8.0", v)).statusCode())
                .isEqualTo(403);
        // Giảng viên cơ sở khác không xem được bảng điểm.
        assertThat(signedIn("GVHN001").get(GRADES).statusCode()).isEqualTo(403);
        // Giảng viên không tự khoá điểm.
        assertThat(signedIn("GVHCM001").post(GRADES + "/lock").statusCode()).isEqualTo(403);
        // Admin cơ sở khác không khoá được.
        assertThat(signedIn("admin.hn").post(GRADES + "/lock").statusCode()).isEqualTo(403);
    }

    // --- Công bố và khoá -------------------------------------------------

    @Test
    void khongCongBoKhiConThieuDiem() throws Exception {
        HttpResponse<String> response = signedIn("GVHCM001").post(GRADES + "/publish");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("GRADE_INCOMPLETE", SV);
    }

    @Test
    void khongKhoaKhiChuaCongBo() throws Exception {
        Browser gv = signedIn("GVHCM001");
        gv.put(GRADES, row(SV, "9.0", "7.5", "8.0", version(gv)));

        HttpResponse<String> response = signedIn("admin.hcm").post(GRADES + "/lock");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("GRADE_NOT_PUBLISHED");
    }

    /**
     * Luồng đầy đủ: nhập → công bố → sửa sau công bố vẫn được → khoá → hết sửa,
     * kể cả đi vòng qua API lớp học phần.
     */
    @Test
    void congBoRoiKhoaThiKhongAiSuaDuocNua() throws Exception {
        Browser gv = signedIn("GVHCM001");
        Browser admin = signedIn("admin.hcm");
        gv.put(GRADES, row(SV, "9.0", "7.5", "8.0", version(gv)));

        HttpResponse<String> published = gv.post(GRADES + "/publish");
        assertThat(published.statusCode()).as(published.body()).isEqualTo(200);
        assertThat(published.body()).contains("\"trangThai\":\"DA_CONG_BO\"");

        // Đã công bố vẫn sửa được cho tới khi khoá.
        HttpResponse<String> corrected = gv.put(GRADES, row(SV, "9.0", "7.5", "9.0", version(gv)));
        assertThat(corrected.statusCode()).isEqualTo(200);
        assertThat(corrected.body()).contains("\"diemTongKet\":8.6", "\"trangThai\":\"DA_CONG_BO\"");

        HttpResponse<String> locked = admin.post(GRADES + "/lock");
        assertThat(locked.statusCode()).as(locked.body()).isEqualTo(200);
        assertThat(locked.body()).contains("\"trangThai\":\"DA_KHOA\"");
        // Khoá lại lần nữa là không làm gì.
        assertThat(admin.post(GRADES + "/lock").statusCode()).isEqualTo(200);

        HttpResponse<String> afterLock = gv.put(GRADES, row(SV, "1.0", "1.0", "1.0", version(gv)));
        assertThat(afterLock.statusCode()).isEqualTo(409);
        assertThat(afterLock.body()).contains("GRADE_LOCKED");

        // Không mở khoá được bằng cách sửa trạng thái lớp.
        HttpResponse<String> reopen = admin.put("/api/classes/" + LOP, """
                {"soLuongToiDa":1,"trangThai":"MO","hinhThucHoc":"TRUC_TIEP","choPhepLienCoSo":false}
                """);
        assertThat(reopen.statusCode()).isEqualTo(409);
        assertThat(reopen.body()).contains("GRADE_LOCKED");
    }

    /** DA_KHOA chỉ đi vào qua luồng khoá điểm, không đặt thẳng qua API lớp. */
    @Test
    void khongDatThangTrangThaiDaKhoa() throws Exception {
        HttpResponse<String> response = signedIn("admin.hcm").put("/api/classes/" + LOP, """
                {"soLuongToiDa":1,"trangThai":"DA_KHOA","hinhThucHoc":"TRUC_TIEP","choPhepLienCoSo":false}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("CLASS_STATUS_INVALID");
    }

    // --- Lịch dạy giảng viên ---------------------------------------------

    @Test
    void lichDayGomMoiLopGiangVienPhuTrach() throws Exception {
        HttpResponse<String> response = signedIn("GVHCM001").get("/api/me/teaching-schedule?maHocKy=2026-1");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("INT1155-2026-1-HCM01", LOP, "\"ngayBatDau\":\"2026-09-07\"")
                .doesNotContain("BAS1203-2026-1-HCM01");

        assertThat(signedIn("B26DCCN001").get("/api/me/teaching-schedule?maHocKy=2026-1").statusCode())
                .isEqualTo(403);
    }

    // --- Tiện ích --------------------------------------------------------

    private long version(Browser browser) throws Exception {
        Matcher m = VERSION.matcher(browser.get(GRADES).body());
        assertThat(m.find()).as("bảng điểm có version").isTrue();
        return Long.parseLong(m.group(1));
    }

    private static String row(String maSinhVien, String cc, String gk, String ck, long version) {
        return """
                {"diem":[{"maSinhVien":"%s","diemChuyenCan":%s,"diemGiuaKy":%s,"diemCuoiKy":%s,"version":%d}]}
                """.formatted(maSinhVien, cc, gk, ck, version);
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

        HttpResponse<String> post(String path) throws Exception {
            return send("POST", path, "");
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
