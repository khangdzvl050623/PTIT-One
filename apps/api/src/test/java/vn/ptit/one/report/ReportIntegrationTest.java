package vn.ptit.one.report;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Thống kê trên SQL Server CENTRAL thật. Dùng học kỳ 2025-1 vì đã khoá điểm:
 * không ca test nào ghi vào đó nên số liệu cố định.
 *
 * <p>2025-1 ở HCM: INT1154 và BAS1150, mỗi lớp sức chứa 40, một sinh viên
 * (B25DCCN001), cả hai đã khoá. B25DCCN001 đạt INT1154 (8.0), trượt BAS1150 (3.2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class ReportIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";

    @LocalServerPort
    private int port;

    /**
     * Hai lượt đăng ký nhưng chỉ MỘT sinh viên. Lấp đầy là 2/80 trên tổng, không
     * phải trung bình (1/40 + 1/40)/2 — ở đây hai cách trùng số, nên kiểm thêm ở
     * ca theo môn.
     */
    @Test
    void tongQuanPhanBietLuotVaSinhVien() throws Exception {
        HttpResponse<String> response = signedIn("admin.hcm").get("/api/reports/summary?maHocKy=2025-1");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains(
                "\"maCoSo\":\"HCM\"",
                "\"soLop\":2", "\"luotDangKy\":2", "\"soSinhVien\":1",
                "\"tongSucChua\":80", "\"tongDaDangKy\":2", "\"tiLeLapDay\":0.0250",
                "\"daKhoa\":2", "\"chuaCongBo\":0", "\"capNhatLuc\":\"");
    }

    @Test
    void theoMonCoDatTruotVaPhanBoDiem() throws Exception {
        String body = signedIn("admin.hcm").get("/api/reports/courses?maHocKy=2025-1").body();

        assertThat(rowOf(body, "INT1154")).contains("\"soDat\":1", "\"soTruot\":0", "\"chuaCoKetQua\":0",
                "{\"khoang\":\"7.0–8.4\",\"soLuong\":1}");
        assertThat(rowOf(body, "BAS1150")).contains("\"soDat\":0", "\"soTruot\":1",
                "{\"khoang\":\"<4.0\",\"soLuong\":1}");
    }

    /** Lớp chưa công bố điểm: các lượt đăng ký là "chưa có kết quả", không phải trượt. */
    @Test
    void chuaCongBoLaChuaCoKetQuaKhongPhaiTruot() throws Exception {
        String body = signedIn("admin.hcm").get("/api/reports/courses?maHocKy=2026-1").body();

        assertThat(rowOf(body, "BAS1203")).contains("\"luotDangKy\":2", "\"soTruot\":0", "\"chuaCoKetQua\":2");
    }

    @Test
    void adminCoSoKhongXemDuocCoSoKhac() throws Exception {
        HttpResponse<String> response = signedIn("admin.hcm").get("/api/reports/summary?maHocKy=2025-1&maCoSo=HN");

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void adminMasterXemToanTruongVaLocTheoCoSo() throws Exception {
        Browser master = signedIn("admin.master");

        assertThat(master.get("/api/reports/summary?maHocKy=2025-1").body()).contains("\"maCoSo\":null");
        // HN không mở lớp nào ở 2025-1: tỉ lệ lấp đầy là null, không phải 0 hay chia cho 0.
        assertThat(master.get("/api/reports/summary?maHocKy=2025-1&maCoSo=HN").body())
                .contains("\"maCoSo\":\"HN\"", "\"soLop\":0", "\"tiLeLapDay\":null");
    }

    @Test
    void sinhVienVaGiangVienKhongXemThongKe() throws Exception {
        assertThat(signedIn("B26DCCN001").get("/api/reports/summary?maHocKy=2025-1").statusCode()).isEqualTo(403);
        assertThat(signedIn("GVHCM001").get("/api/reports/courses?maHocKy=2025-1").statusCode()).isEqualTo(403);
    }

    // --- Tiện ích --------------------------------------------------------

    private static String rowOf(String json, String maMonHoc) {
        int start = json.indexOf("\"maMonHoc\":\"" + maMonHoc + "\"");
        assertThat(start).as("có dòng %s", maMonHoc).isNotNegative();
        return json.substring(start, json.indexOf("]}", start) + 2);
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
            HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/login"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-XSRF-TOKEN", cookie("XSRF-TOKEN"))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        private String cookie(String name) {
            return cookies.getCookieStore().getCookies().stream()
                    .filter(c -> c.getName().equals(name) && !c.hasExpired() && !c.getValue().isEmpty())
                    .map(HttpCookie::getValue).findFirst().orElse("");
        }

        private URI uri(String path) {
            return URI.create("http://127.0.0.1:" + port + path);
        }
    }
}
