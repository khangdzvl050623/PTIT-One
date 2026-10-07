package vn.ptit.one.course;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Danh mục môn học và đồ thị tiên quyết (F03) trên SQL Server CENTRAL thật.
 *
 * <p>Chỉ chạy khi có {@code PTITONE_DB_URL}; cần DB đã migrate V2 và chạy seed
 * {@code 10-auth-seed.sql} + {@code 20-hoc-vu-seed.sql}.
 *
 * <p>Dựa vào chuỗi tiên quyết có sẵn trong seed:
 * {@code INT1154 → INT1155 → INT1306 → INT1332 → INT1339}.
 *
 * <p>Test có ghi vào DB nhưng <b>tự khôi phục</b>: chỉ đổi tập tiên quyết của
 * {@code INT1313} rồi trả về trạng thái cũ. Các ca còn lại đều bị từ chối nên
 * rollback, không để lại gì.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class CourseCatalogIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void moiVaiTroDeuDocDuocDanhMuc() throws Exception {
        Browser sv = signedIn("B26DCCN001");

        HttpResponse<String> list = sv.get("/api/courses?maKhoa=CNTT");
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains("INT1154").doesNotContain("ATT1234");

        HttpResponse<String> detail = sv.get("/api/courses/INT1155");
        assertThat(detail.statusCode()).isEqualTo(200);
        // Cả hai chiều của quan hệ: môn phải đạt trước, và môn đang phụ thuộc vào nó.
        assertThat(detail.body()).contains("\"tienQuyet\"", "INT1154", "\"monPhuThuoc\"", "INT1306");

        assertThat(sv.get("/api/faculties").statusCode()).isEqualTo(200);
        assertThat(sv.get("/api/terms").statusCode()).isEqualTo(200);
    }

    @Test
    void chuaDangNhapThiKhongDocDuoc() throws Exception {
        assertThat(new Browser().get("/api/courses").statusCode()).isEqualTo(401);
    }

    /** B3: `MonHoc` là R/W chỉ tại Master. Ba vai trò còn lại chỉ được đọc. */
    @Test
    void chiAdminMasterDuocSuaDanhMuc() throws Exception {
        String body = "{\"tienQuyet\":[]}";
        for (String username : new String[] { "B26DCCN001", "GVHCM001", "admin.hcm" }) {
            HttpResponse<String> denied = signedIn(username).put("/api/courses/INT1313/prerequisites", body);
            assertThat(denied.statusCode()).as("vai trò %s", username).isEqualTo(403);
        }
    }

    @Test
    void chanMonTuLamTienQuyetCuaChinhNo() throws Exception {
        HttpResponse<String> response = signedIn("admin.master")
                .put("/api/courses/INT1313/prerequisites", "{\"tienQuyet\":[\"INT1313\"]}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("PREREQUISITE_SELF");
    }

    @Test
    void chanTienQuyetKhongTonTai() throws Exception {
        HttpResponse<String> response = signedIn("admin.master")
                .put("/api/courses/INT1313/prerequisites", "{\"tienQuyet\":[\"KHONG-CO\"]}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("PREREQUISITE_UNKNOWN");
    }

    /**
     * Chu trình DÀI, không phải môn tự trỏ chính nó.
     *
     * <p>Seed đã có {@code INT1306 → INT1332 → INT1339}. Đặt thêm
     * {@code INT1339} làm tiên quyết của {@code INT1306} là khép vòng ba tầng.
     * Đồng thời kiểm luôn việc rollback giữ nguyên tập cũ.
     */
    @Test
    void chanChuTrinhDaiVaGiuNguyenTapCu() throws Exception {
        Browser admin = signedIn("admin.master");

        HttpResponse<String> response = admin
                .put("/api/courses/INT1306/prerequisites", "{\"tienQuyet\":[\"INT1339\"]}");
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("PREREQUISITE_CYCLE");

        /* Cài đặt xoá tập cũ TRƯỚC khi xét chu trình, nên đây là phép thử thật
           cho việc rollback: INT1155 phải còn nguyên. */
        assertThat(admin.get("/api/courses/INT1306").body()).contains("INT1155");
    }

    /** Không cho đổi tiên quyết khi môn có lớp trong kỳ đang mở đợt đăng ký. */
    @Test
    void chanSuaTienQuyetKhiDangMoDotDangKy() throws Exception {
        HttpResponse<String> response = signedIn("admin.master")
                .put("/api/courses/INT1154/prerequisites", "{\"tienQuyet\":[]}");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("COURSE_REGISTRATION_OPEN");
    }

    @Test
    void thayTapTienQuyetRoiKhoiPhuc() throws Exception {
        Browser admin = signedIn("admin.master");
        try {
            HttpResponse<String> changed = admin
                    .put("/api/courses/INT1313/prerequisites", "{\"tienQuyet\":[\"INT1306\"]}");
            assertThat(changed.statusCode()).as(changed.body()).isEqualTo(200);
            assertThat(changed.body()).contains("INT1306");

            // Đọc lại ngay sau commit phải thấy tập mới, không chờ đồng bộ gì.
            assertThat(admin.get("/api/courses/INT1313").body()).contains("INT1306");

            // Danh sách rỗng là thao tác hợp lệ: gỡ hết tiên quyết.
            assertThat(admin.put("/api/courses/INT1313/prerequisites", "{\"tienQuyet\":[]}").statusCode())
                    .isEqualTo(200);
            assertThat(admin.get("/api/courses/INT1313").body()).doesNotContain("INT1306");
        } finally {
            admin.put("/api/courses/INT1313/prerequisites", "{\"tienQuyet\":[\"INT1155\"]}");
        }
    }

    // --- Xoá môn học (quyết định nhóm 07/10/2026) ------------------------

    private static final String MON_THU = "TEST-XOA-01";

    /** Môn chưa dùng ở đâu thì xoá được, và biến mất khỏi danh mục. */
    @Test
    void monChuaDungODauThiXoaDuoc() throws Exception {
        Browser master = signedIn("admin.master");
        try {
            assertThat(master.post("/api/courses", newCourse(MON_THU)).statusCode()).isEqualTo(201);

            assertThat(master.delete("/api/courses/" + MON_THU).statusCode()).isEqualTo(204);
            assertThat(master.get("/api/courses").body()).doesNotContain(MON_THU);
            assertThat(master.get("/api/courses/" + MON_THU).statusCode()).isEqualTo(404);
        } finally {
            jdbc.update("DELETE FROM dbo.MonHocTienQuyet WHERE MaMonHoc = ? OR MaMonTienQuyet = ?",
                    MON_THU, MON_THU);
            jdbc.update("DELETE FROM dbo.MonHoc WHERE MaMonHoc = ?", MON_THU);
        }
    }

    /** Tiên quyết CỦA CHÍNH môn đó xoá kèm — không môn nào khác phụ thuộc vào chúng. */
    @Test
    void xoaKemTienQuyetCuaChinhNo() throws Exception {
        Browser master = signedIn("admin.master");
        try {
            master.post("/api/courses", newCourse(MON_THU));
            master.put("/api/courses/" + MON_THU + "/prerequisites", "{\"tienQuyet\":[\"BAS1150\"]}");
            assertThat(count("SELECT COUNT(*) FROM dbo.MonHocTienQuyet WHERE MaMonHoc = ?", MON_THU))
                    .isEqualTo(1);

            assertThat(master.delete("/api/courses/" + MON_THU).statusCode()).isEqualTo(204);
            assertThat(count("SELECT COUNT(*) FROM dbo.MonHocTienQuyet WHERE MaMonHoc = ?", MON_THU))
                    .isZero();
        } finally {
            jdbc.update("DELETE FROM dbo.MonHocTienQuyet WHERE MaMonHoc = ? OR MaMonTienQuyet = ?",
                    MON_THU, MON_THU);
            jdbc.update("DELETE FROM dbo.MonHoc WHERE MaMonHoc = ?", MON_THU);
        }
    }

    /**
     * Ba cổng chặn, mỗi cổng một mã riêng. Fixture seed:
     * INT1154 là tiên quyết của INT1155 · BAS1203 đã có lớp · INT1339 trong CTĐT
     * nhưng chưa mở lớp và không là tiên quyết của ai.
     */
    @Test
    void khongXoaDuocMonDangDuocDung() throws Exception {
        Browser master = signedIn("admin.master");

        HttpResponse<String> tienQuyet = master.delete("/api/courses/INT1154");
        assertThat(tienQuyet.statusCode()).as(tienQuyet.body()).isEqualTo(409);
        assertThat(tienQuyet.body()).contains("COURSE_IS_PREREQUISITE");

        HttpResponse<String> coLop = master.delete("/api/courses/BAS1203");
        assertThat(coLop.statusCode()).as(coLop.body()).isEqualTo(409);
        assertThat(coLop.body()).contains("COURSE_HAS_CLASSES");

        HttpResponse<String> trongCtdt = master.delete("/api/courses/INT1339");
        assertThat(trongCtdt.statusCode()).as(trongCtdt.body()).isEqualTo(409);
        assertThat(trongCtdt.body()).contains("COURSE_IN_PROGRAM");

        // Không cổng nào được phép xoá mất gì.
        for (String ma : new String[] { "INT1154", "BAS1203", "INT1339" }) {
            assertThat(count("SELECT COUNT(*) FROM dbo.MonHoc WHERE MaMonHoc = ?", ma)).as(ma).isEqualTo(1);
        }
    }

    @Test
    void xoaMonKhongTonTaiThiBao404() throws Exception {
        assertThat(signedIn("admin.master").delete("/api/courses/KHONG-CO").statusCode()).isEqualTo(404);
    }

    @Test
    void chiAdminMasterDuocXoaMon() throws Exception {
        for (String username : new String[] { "admin.hcm", "GVHCM001", "B26DCCN001" }) {
            assertThat(signedIn(username).delete("/api/courses/INT1339").statusCode())
                    .as(username).isEqualTo(403);
        }
    }

    private static String newCourse(String maMonHoc) {
        return ("{\"maMonHoc\":\"%s\",\"tenMonHoc\":\"Môn thử để xoá\",\"soTinChi\":3,"
                + "\"maKhoa\":\"CNTT\"}").formatted(maMonHoc);
    }

    private int count(String sql, String arg) {
        return jdbc.queryForObject(sql, Integer.class, arg);
    }

    private Browser signedIn(String username) throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login(username, PASSWORD).statusCode()).as("đăng nhập %s", username).isEqualTo(200);
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

        HttpResponse<String> put(String path, String json) throws Exception {
            return send("PUT", path, json);
        }

        HttpResponse<String> post(String path, String json) throws Exception {
            return send("POST", path, json);
        }

        HttpResponse<String> delete(String path) throws Exception {
            return send("DELETE", path, "");
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
