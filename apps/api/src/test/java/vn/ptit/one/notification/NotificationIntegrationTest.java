package vn.ptit.one.notification;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
 * Thông báo trên SQL Server CENTRAL thật (cần V4).
 *
 * <p>Người nhận theo seed ở HCM: sinh viên còn học B26DCCN001, B26DCCN003,
 * B25DCCN001 (B26DCCN004 đã thôi học, không nhận); giảng viên GVHCM001, GVHCM002.
 *
 * <p>Mỗi ca xoá các thông báo sinh ra từ lúc ca bắt đầu, huỷ ghi danh và đưa
 * bảng điểm thử về như cũ.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class NotificationIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";
    private static final String INT1155 = "INT1155-2026-1-HCM01";
    private static final String LOP_DIEM = "INT1154-2026-1-HCM01";
    private static final Pattern ID = Pattern.compile("\"maThongBao\":\"([0-9a-fA-F-]{36})\"");
    private static final Pattern VERSION = Pattern.compile("\"version\":(\\d+)");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private Timestamp batDau;

    @BeforeEach
    void markStart() {
        // datetime2(0) làm tròn giây: lùi một giây để không sót thông báo của chính ca này.
        batDau = Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now().minusSeconds(1), ZoneOffset.UTC));
    }

    @AfterEach
    void cleanUp() throws Exception {
        Browser sv = signedIn("B25DCCN001");
        sv.delete("/api/me/enrollments/" + INT1155);
        jdbc.update("""
                UPDATE dbo.Diem SET DiemChuyenCan = NULL, DiemGiuaKy = NULL, DiemCuoiKy = NULL,
                                    DiemTongKet = NULL, NgayCongBo = NULL
                 WHERE MaLopHP = ? AND MaSinhVien = 'B26DCCN003'
                """, LOP_DIEM);
        jdbc.update("""
                DELETE n FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao
                 WHERE t.NgayTao >= ?
                """, batDau);
        jdbc.update("DELETE FROM dbo.ThongBao WHERE NgayTao >= ?", batDau);
    }

    // --- Tự sinh: đăng ký / huỷ -------------------------------------------

    /**
     * Đăng ký → một thông báo; bấm lại → không thêm; huỷ → thông báo huỷ; đăng ký
     * lại → thông báo MỚI (không bị khoá chống trùng chặn nhầm).
     */
    @Test
    void dangKyHuyVaDangKyLaiSinhDungSoThongBao() throws Exception {
        Browser sv = signedIn("B25DCCN001");
        int truoc = unread(sv);

        assertThat(sv.post("/api/me/enrollments", "{\"maLopHP\":\"" + INT1155 + "\"}").statusCode()).isEqualTo(201);
        assertThat(sv.post("/api/me/enrollments", "{\"maLopHP\":\"" + INT1155 + "\"}").statusCode()).isEqualTo(200);
        assertThat(unread(sv)).isEqualTo(truoc + 1);

        assertThat(sv.delete("/api/me/enrollments/" + INT1155).statusCode()).isEqualTo(200);
        Thread.sleep(5);
        assertThat(sv.post("/api/me/enrollments", "{\"maLopHP\":\"" + INT1155 + "\"}").statusCode()).isEqualTo(201);

        assertThat(unread(sv)).isEqualTo(truoc + 3);
        String inbox = sv.get("/api/me/notifications?chuaDoc=true").body();
        assertThat(inbox).contains("\"suKien\":\"DANG_KY\"", "\"suKien\":\"HUY_DANG_KY\"",
                "\"lienKet\":\"/sinh-vien/dang-ky?maHocKy=2026-1\"", "\"vaiTroNguoiGui\":null");
    }

    /** Đăng ký bị từ chối thì không có thông báo "thành công" nào lọt ra. */
    @Test
    void dangKyThatBaiKhongSinhThongBao() throws Exception {
        Browser sv = signedIn("B25DCCN001");
        int truoc = unread(sv);

        assertThat(sv.post("/api/me/enrollments", "{\"maLopHP\":\"INT1154-2026-1-HCM01\"}").statusCode())
                .isEqualTo(409);

        assertThat(unread(sv)).isEqualTo(truoc);
    }

    // --- Tự sinh: công bố / sửa điểm --------------------------------------

    @Test
    void congBoVaSuaDiemDaCongBoThiSinhVienDuocBao() throws Exception {
        Browser gv = signedIn("GVHCM001");
        String grades = "/api/classes/" + LOP_DIEM + "/grades";
        gv.put(grades, gradeRow("8.0", version(gv, grades)));
        assertThat(gv.post(grades + "/publish").statusCode()).isEqualTo(200);
        assertThat(eventsFor("B26DCCN003", "CONG_BO_DIEM")).isEqualTo(1);

        // Lưu lại đúng điểm cũ: không đổi gì, không báo.
        gv.put(grades, gradeRow("8.0", version(gv, grades)));
        assertThat(eventsFor("B26DCCN003", "SUA_DIEM")).isZero();

        gv.put(grades, gradeRow("9.0", version(gv, grades)));
        assertThat(eventsFor("B26DCCN003", "SUA_DIEM")).isEqualTo(1);

        // Công bố lại khi không còn nháp: không phát thêm.
        gv.post(grades + "/publish");
        assertThat(eventsFor("B26DCCN003", "CONG_BO_DIEM")).isEqualTo(1);
    }

    // --- Soạn tay ---------------------------------------------------------

    /** Cơ sở HCM: 3 SV còn học (bỏ người đã thôi học) + 2 GV. */
    @Test
    void soanXemTruocGuiRoiDocVaDanhDauTatCa() throws Exception {
        Browser admin = signedIn("admin.hcm");
        String body = draft("CO_SO", null, null, "TAT_CA");

        assertThat(admin.post("/api/notifications/preview", body).body())
                .contains("\"soSinhVien\":3", "\"soGiangVien\":2");

        HttpResponse<String> created = admin.post("/api/notifications", body);
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(created.body()).contains("\"trangThai\":\"NHAP\"", "\"maCoSo\":\"HCM\"");
        String id = id(created.body());

        HttpResponse<String> sent = admin.post("/api/notifications/" + id + "/send");
        assertThat(sent.statusCode()).as(sent.body()).isEqualTo(200);
        assertThat(sent.body()).contains("\"trangThai\":\"DA_GUI\"", "\"soSinhVien\":3", "\"soGiangVien\":2");

        // Gửi hai lần không ra hai bộ người nhận; bản đã gửi không sửa được.
        assertThat(admin.post("/api/notifications/" + id + "/send").body()).contains("NOTIFICATION_ALREADY_SENT");
        assertThat(admin.put("/api/notifications/" + id, body).statusCode()).isEqualTo(409);

        Browser sv = signedIn("B26DCCN001");
        int chuaDoc = unread(sv);
        assertThat(sv.get("/api/me/notifications").body()).contains("Lịch thi giữa kỳ",
                "\"vaiTroNguoiGui\":\"ADMIN_CO_SO\"", "\"mucDo\":\"QUAN_TRONG\"", "\"daDoc\":false");
        // Mở hộp thư không tự đánh dấu đã đọc.
        assertThat(unread(sv)).isEqualTo(chuaDoc);

        assertThat(sv.post("/api/me/notifications/" + id + "/read").body())
                .contains("\"soChuaDoc\":" + (chuaDoc - 1));
        // Đọc lại: không đổi gì.
        assertThat(sv.post("/api/me/notifications/" + id + "/read").statusCode()).isEqualTo(200);
        assertThat(admin.get("/api/notifications/" + id).body()).contains("\"soDaDoc\":1");

        sv.post("/api/me/notifications/read-all");
        assertThat(unread(sv)).isZero();
    }

    /** Giảng viên gửi cho lớp mình: chỉ sinh viên của lớp, không gửi cho chính mình. */
    @Test
    void giangVienGuiLopMinhKhongTuNhan() throws Exception {
        HttpResponse<String> preview = signedIn("GVHCM001")
                .post("/api/notifications/preview", draft("LOP_HOC_PHAN", null, LOP_DIEM, "TAT_CA"));

        assertThat(preview.statusCode()).as(preview.body()).isEqualTo(200);
        assertThat(preview.body()).contains("\"soSinhVien\":1", "\"soGiangVien\":0");
    }

    @Test
    void phamViVuotQuyenBiChan() throws Exception {
        Browser adminHcm = signedIn("admin.hcm");
        assertThat(adminHcm.post("/api/notifications", draft("TOAN_TRUONG", null, null, "TAT_CA")).statusCode())
                .isEqualTo(403);
        assertThat(adminHcm.post("/api/notifications", draft("CO_SO", "HN", null, "TAT_CA")).statusCode())
                .isEqualTo(403);
        // Lớp của GVHCM002, không phải của GVHCM001.
        assertThat(signedIn("GVHCM001").post("/api/notifications",
                draft("LOP_HOC_PHAN", null, "BAS1203-2026-1-HCM01", "SINH_VIEN")).statusCode()).isEqualTo(403);
        assertThat(signedIn("B26DCCN001").post("/api/notifications", draft("CO_SO", null, null, "TAT_CA"))
                .statusCode()).isEqualTo(403);
    }

    /** Bản nháp của người khác: 404, không lộ là có tồn tại. */
    @Test
    void khongXemDuocBanSoanCuaNguoiKhac() throws Exception {
        String id = id(signedIn("admin.hcm").post("/api/notifications", draft("CO_SO", null, null, "SINH_VIEN")).body());

        assertThat(signedIn("admin.master").get("/api/notifications/" + id).statusCode()).isEqualTo(404);
        // Sinh viên không nhận thông báo đó thì đánh dấu đọc cũng là 404.
        assertThat(signedIn("B26DCCN001").post("/api/me/notifications/" + id + "/read").statusCode())
                .isEqualTo(404);
    }

    @Test
    void khongNhanLienKetNgoaiVaMaSaiDinhDang() throws Exception {
        Browser admin = signedIn("admin.hcm");
        String ngoai = draft("CO_SO", null, null, "TAT_CA").replace("/sinh-vien/lich-hoc", "https://la.example");
        String doiGach = draft("CO_SO", null, null, "TAT_CA").replace("/sinh-vien/lich-hoc", "//la.example");

        assertThat(admin.post("/api/notifications", ngoai).statusCode()).isEqualTo(400);
        assertThat(admin.post("/api/notifications", doiGach).statusCode()).isEqualTo(400);
        assertThat(admin.get("/api/notifications/khong-phai-uuid").body()).contains("VALIDATION_ERROR");
    }

    @Test
    void hopThuChiDanhChoSinhVienVaGiangVien() throws Exception {
        assertThat(signedIn("GVHCM001").get("/api/me/notifications").statusCode()).isEqualTo(200);
        assertThat(signedIn("admin.hcm").get("/api/me/notifications").statusCode()).isEqualTo(403);
    }

    // --- Tiện ích --------------------------------------------------------

    private static String draft(String phamVi, String maCoSo, String maLopHP, String doiTuong) {
        return """
                {"tieuDe":"Lịch thi giữa kỳ","noiDung":"Xem lịch thi chi tiết trên trang lịch học.",
                 "mucDo":"QUAN_TRONG","phamVi":"%s","maCoSo":%s,"maLopHP":%s,"doiTuong":"%s",
                 "lienKet":"/sinh-vien/lich-hoc"}
                """.formatted(phamVi, quote(maCoSo), quote(maLopHP), doiTuong);
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String gradeRow(String cuoiKy, long version) {
        return """
                {"diem":[{"maSinhVien":"B26DCCN003","diemChuyenCan":9.0,"diemGiuaKy":7.5,"diemCuoiKy":%s,"version":%d}]}
                """.formatted(cuoiKy, version);
    }

    private int eventsFor(String maSinhVien, String suKien) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao
                 WHERE n.LoaiNguoiNhan = 'SINH_VIEN' AND n.MaNguoiNhan = ? AND t.SuKien = ? AND t.NgayTao >= ?
                """, Integer.class, maSinhVien, suKien, batDau);
    }

    private static long version(Browser gv, String path) throws Exception {
        Matcher m = VERSION.matcher(gv.get(path).body());
        assertThat(m.find()).isTrue();
        return Long.parseLong(m.group(1));
    }

    private static int unread(Browser browser) throws Exception {
        String body = browser.get("/api/me/notifications/unread-count").body();
        return Integer.parseInt(body.replaceAll("\\D", ""));
    }

    private static String id(String json) {
        Matcher m = ID.matcher(json);
        assertThat(m.find()).as(json).isTrue();
        return m.group(1);
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

        HttpResponse<String> post(String path, String json) throws Exception {
            return send("POST", path, json);
        }

        HttpResponse<String> put(String path, String json) throws Exception {
            return send("PUT", path, json);
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
