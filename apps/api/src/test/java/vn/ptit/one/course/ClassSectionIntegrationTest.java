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
 * Lớp học phần, lịch và đợt đăng ký (F04) trên SQL Server CENTRAL thật.
 *
 * <p>Chỉ chạy khi có {@code PTITONE_DB_URL}; cần DB đã migrate V3 và chạy seed.
 *
 * <p>Dữ liệu seed được dùng:
 * <ul>
 *   <li>{@code INT1445-2026-1-HCM01} — chưa phân công GV, 0 sinh viên: sân thử lịch</li>
 *   <li>{@code INT1155-2026-1-HCM01} — thứ 2 tiết 1-3 phòng A2-201: mồi để thử trùng phòng</li>
 *   <li>{@code INT1154-2026-1-HCM01} — đã có 1 sinh viên: thử luật khoá lịch</li>
 * </ul>
 *
 * <p>Test có ghi vào DB nhưng tự khôi phục lịch về trạng thái cũ. Ca tạo lớp để
 * lại một dòng được đánh {@code DA_HUY} — không xoá được vì lớp đã huỷ vẫn là
 * lịch sử, và API cố ý không mở endpoint xoá.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class ClassSectionIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";
    private static final String SANDBOX = "INT1445-2026-1-HCM01";
    private static final String LOP_DA_CO_SV = "INT1154-2026-1-HCM01";
    /** Sức chứa 3, sĩ số 2 — fixture riêng cho luật "không hạ dưới sĩ số". */
    private static final String LOP_SUC_CHUA = "BAS1203-2026-1-HCM01";
    /** Lịch gốc của sân thử, để trả về sau mỗi ca ghi. */
    private static final String LICH_GOC =
            "{\"buoiHoc\":[{\"thu\":5,\"tietBatDau\":1,\"soTiet\":3,\"tuanBatDau\":1,\"tuanKetThuc\":15}]}";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    // --- Quyền ------------------------------------------------------------

    /** B3: {@code LopHocPhan} là R/W của Admin cơ sở; Admin Master chỉ ĐỌC. */
    @Test
    void chiAdminCoSoDuocTaoLop() throws Exception {
        String body = """
                {"maMonHoc":"INT1358","maHocKy":"2026-1","soLuongToiDa":30,
                 "hinhThucHoc":"TRUC_TIEP","choPhepLienCoSo":false}
                """;
        for (String username : new String[] { "B26DCCN001", "GVHCM001", "admin.master" }) {
            HttpResponse<String> denied = signedIn(username).post("/api/classes", body);
            assertThat(denied.statusCode()).as("vai trò %s", username).isEqualTo(403);
        }
    }

    @Test
    void adminCoSoKhongSuaDuocLopCuaCoSoKhac() throws Exception {
        HttpResponse<String> response = signedIn("admin.hn")
                .put("/api/classes/" + SANDBOX + "/teacher", "{\"maGiangVien\":\"GVHN001\"}");

        assertThat(response.statusCode()).isEqualTo(403);
    }

    // --- Tạo lớp ----------------------------------------------------------

    /** Mã lớp do server sinh và nhúng cơ sở lấy từ JWT, không nhận từ client. */
    @Test
    void taoLopThiMaNhungCoSoTuJwt() throws Exception {
        Browser admin = signedIn("admin.hcm");
        HttpResponse<String> created = admin.post("/api/classes", """
                {"maMonHoc":"INT1358","maHocKy":"2026-1","soLuongToiDa":30,
                 "hinhThucHoc":"TRUC_TIEP","choPhepLienCoSo":false}
                """);

        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(created.body()).contains("\"maCoSoHost\":\"HCM\"", "INT1358-2026-1-HCM");
        // Lớp mới luôn ở DU_KIEN, chưa mở cho sinh viên thấy.
        assertThat(created.body()).contains("\"trangThai\":\"DU_KIEN\"");

        String maLopHP = extract(created.body(), "maLopHP");
        assertThat(admin.post("/api/classes/" + maLopHP + "/cancel", "{}").statusCode()).isEqualTo(200);
    }

    /** D18: chỉ lớp trực tuyến mới cho đăng ký liên cơ sở. */
    @Test
    void lienCoSoChiDanhChoLopTrucTuyen() throws Exception {
        HttpResponse<String> response = signedIn("admin.hcm").post("/api/classes", """
                {"maMonHoc":"INT1358","maHocKy":"2026-1","soLuongToiDa":30,
                 "hinhThucHoc":"TRUC_TIEP","choPhepLienCoSo":true}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("CROSS_CAMPUS_REQUIRES_ONLINE");
    }

    @Test
    void khongPhanCongGiangVienCoSoKhac() throws Exception {
        HttpResponse<String> response = signedIn("admin.hcm")
                .put("/api/classes/" + SANDBOX + "/teacher", "{\"maGiangVien\":\"GVHN001\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("TEACHER_WRONG_CAMPUS");
    }

    // --- Sức chứa ---------------------------------------------------------

    /**
     * Ma trận nghiệm thu F04 cho "không hạ sức chứa dưới sĩ số thực".
     *
     * <p>Điều kiện nằm trong chính câu UPDATE rồi đọc số dòng, không SELECT
     * trước rồi IF — nên ca hạ xuống ĐÚNG bằng sĩ số phải thành công, chỉ
     * xuống dưới mới bị chặn.
     */
    @Test
    void khongHaSucChuaXuongDuoiSiSo() throws Exception {
        Browser admin = signedIn("admin.hcm");
        try {
            HttpResponse<String> qua = admin.put("/api/classes/" + LOP_SUC_CHUA, capacity(1));
            assertThat(qua.statusCode()).isEqualTo(409);
            assertThat(qua.body()).contains("CLASS_CAPACITY_BELOW_ENROLLED");
            // Bị từ chối thì dữ liệu phải y nguyên.
            assertThat(admin.get("/api/classes/" + LOP_SUC_CHUA).body())
                    .contains("\"soLuongToiDa\":3", "\"soLuongDaDangKy\":2");

            HttpResponse<String> bang = admin.put("/api/classes/" + LOP_SUC_CHUA, capacity(2));
            assertThat(bang.statusCode()).as(bang.body()).isEqualTo(200);
            assertThat(bang.body()).contains("\"soLuongToiDa\":2");

            HttpResponse<String> tang = admin.put("/api/classes/" + LOP_SUC_CHUA, capacity(10));
            assertThat(tang.statusCode()).isEqualTo(200);
            // Tăng sức chứa không đụng tới sĩ số.
            assertThat(tang.body()).contains("\"soLuongToiDa\":10", "\"soLuongDaDangKy\":2");
        } finally {
            admin.put("/api/classes/" + LOP_SUC_CHUA, capacity(3));
        }
    }

    private static String capacity(int soLuongToiDa) {
        return """
                {"soLuongToiDa":%d,"trangThai":"MO","hinhThucHoc":"TRUC_TIEP","choPhepLienCoSo":false}
                """.formatted(soLuongToiDa);
    }

    // --- Lịch học ---------------------------------------------------------

    /**
     * BAS1150-2026-1-HCM01 (thứ 2 tiết 2-4) chồng giờ với INT1155-2026-1-HCM01
     * (thứ 2 tiết 1-3) của GVHCM001. Seed cố ý giữ hai lớp trùng giờ để F08
     * chặn SINH VIÊN đăng ký cả hai — nhưng chúng phải khác người dạy.
     */
    @Test
    void khongGanGiangVienDaBanGioKhac() throws Exception {
        Browser admin = signedIn("admin.hcm");
        HttpResponse<String> response = admin.put(
                "/api/classes/BAS1150-2026-1-HCM01/teacher", "{\"maGiangVien\":\"GVHCM001\"}");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("TEACHER_SCHEDULE_CLASH", "INT1155-2026-1-HCM01");
        // Bị từ chối thì giữ nguyên phân công cũ.
        assertThat(admin.get("/api/classes/BAS1150-2026-1-HCM01").body()).contains("GVHCM002");
    }

    @Test
    void chanTrungPhongTrongCungHocKy() throws Exception {
        Browser admin = signedIn("admin.hcm");
        try {
            // INT1155-2026-1-HCM01 đã giữ A2-201 thứ 2 tiết 1-3, tuần 1-15.
            HttpResponse<String> response = admin.put("/api/classes/" + SANDBOX + "/schedule", """
                    {"buoiHoc":[{"thu":2,"tietBatDau":1,"soTiet":3,"phongHoc":"a2-201",
                                 "tuanBatDau":1,"tuanKetThuc":15}]}
                    """);

            assertThat(response.statusCode()).isEqualTo(409);
            // Viết thường vẫn phải bắt được: so phòng không phân biệt hoa thường.
            assertThat(response.body()).contains("SCHEDULE_ROOM_CLASH", "INT1155-2026-1-HCM01");
        } finally {
            admin.put("/api/classes/" + SANDBOX + "/schedule", LICH_GOC);
        }
    }

    @Test
    void chanHaiBuoiCuaCungLopChongNhau() throws Exception {
        Browser admin = signedIn("admin.hcm");
        try {
            HttpResponse<String> response = admin.put("/api/classes/" + SANDBOX + "/schedule", """
                    {"buoiHoc":[{"thu":3,"tietBatDau":1,"soTiet":3,"tuanBatDau":1,"tuanKetThuc":15},
                                {"thu":3,"tietBatDau":2,"soTiet":2,"tuanBatDau":1,"tuanKetThuc":15}]}
                    """);

            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("SCHEDULE_SELF_OVERLAP");
        } finally {
            admin.put("/api/classes/" + SANDBOX + "/schedule", LICH_GOC);
        }
    }

    /**
     * Sân thử được gán tạm GVHCM001 (đang dạy INT1155 thứ 2 tiết 1-3). Đặt lịch
     * trùng khung giờ đó phải bị chặn dù không ghi phòng.
     */
    @Test
    void chanLichTrungGioGiangVienDangDay() throws Exception {
        Browser admin = signedIn("admin.hcm");
        jdbc.update("UPDATE dbo.LopHocPhan SET MaGiangVien = 'GVHCM001' WHERE MaLopHP = ?", SANDBOX);
        try {
            HttpResponse<String> response = admin.put("/api/classes/" + SANDBOX + "/schedule", """
                    {"buoiHoc":[{"thu":2,"tietBatDau":2,"soTiet":2,"tuanBatDau":1,"tuanKetThuc":15}]}
                    """);

            assertThat(response.statusCode()).as(response.body()).isEqualTo(409);
            assertThat(response.body()).contains("SCHEDULE_TEACHER_CLASH");
        } finally {
            jdbc.update("UPDATE dbo.LopHocPhan SET MaGiangVien = NULL WHERE MaLopHP = ?", SANDBOX);
            admin.put("/api/classes/" + SANDBOX + "/schedule", LICH_GOC);
        }
    }

    /** Một ngày có 12 tiết: bắt đầu tiết 11 mà kéo 4 tiết là tràn sang tiết 14. */
    @Test
    void chanBuoiHocVuotTiet12() throws Exception {
        Browser admin = signedIn("admin.hcm");
        try {
            HttpResponse<String> response = admin.put("/api/classes/" + SANDBOX + "/schedule", """
                    {"buoiHoc":[{"thu":4,"tietBatDau":11,"soTiet":4,"tuanBatDau":1,"tuanKetThuc":15}]}
                    """);

            assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
            assertThat(response.body()).contains("SCHEDULE_SLOT_INVALID");
        } finally {
            admin.put("/api/classes/" + SANDBOX + "/schedule", LICH_GOC);
        }
    }

    /** Danh sách rỗng là hợp lệ: xoá hết lịch của lớp. */
    @Test
    void lichRongThiXoaHetLich() throws Exception {
        Browser admin = signedIn("admin.hcm");
        try {
            HttpResponse<String> response = admin.put("/api/classes/" + SANDBOX + "/schedule", "{\"buoiHoc\":[]}");

            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            assertThat(admin.get("/api/classes/" + SANDBOX + "/schedule").body()).contains("\"buoiHoc\":[]");
        } finally {
            admin.put("/api/classes/" + SANDBOX + "/schedule", LICH_GOC);
        }
    }

    @Test
    void chanSuaLichKhiLopDaCoSinhVien() throws Exception {
        HttpResponse<String> response = signedIn("admin.hcm")
                .put("/api/classes/" + LOP_DA_CO_SV + "/schedule", LICH_GOC);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("CLASS_HAS_ENROLLMENTS");
    }

    @Test
    void thayLichRoiKhoiPhuc() throws Exception {
        Browser admin = signedIn("admin.hcm");
        try {
            HttpResponse<String> changed = admin.put("/api/classes/" + SANDBOX + "/schedule", """
                    {"buoiHoc":[{"thu":7,"tietBatDau":1,"soTiet":3,"phongHoc":"Z9-999",
                                 "tuanBatDau":1,"tuanKetThuc":15}]}
                    """);

            assertThat(changed.statusCode()).as(changed.body()).isEqualTo(200);
            assertThat(changed.body()).contains("\"thu\":7", "Z9-999");

            HttpResponse<String> read = admin.get("/api/classes/" + SANDBOX + "/schedule");
            assertThat(read.body()).contains("\"thu\":7");
        } finally {
            admin.put("/api/classes/" + SANDBOX + "/schedule", LICH_GOC);
        }
    }

    // --- Đợt đăng ký ------------------------------------------------------

    /** V3: mỗi cơ sở chỉ được MỘT đợt đang mở trong một học kỳ. */
    @Test
    void khongMoDuocDotThuHaiCungHocKy() throws Exception {
        HttpResponse<String> response = signedIn("admin.hcm").post("/api/enrollment-periods", """
                {"maHocKy":"2026-1","thoiGianMo":"2026-09-01T00:00:00Z",
                 "thoiGianDong":"2026-12-31T23:59:00Z","trangThai":"DANG_MO"}
                """);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("PERIOD_ALREADY_OPEN");
    }

    @Test
    void thoiGianMoPhaiTruocThoiGianDong() throws Exception {
        HttpResponse<String> response = signedIn("admin.hcm").post("/api/enrollment-periods", """
                {"maHocKy":"2026-1","thoiGianMo":"2026-12-31T00:00:00Z",
                 "thoiGianDong":"2026-09-01T00:00:00Z","trangThai":"CHUA_MO"}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("PERIOD_WINDOW_INVALID");
    }

    // --- Hạ tầng test -----------------------------------------------------

    private static String extract(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        return json.substring(start, json.indexOf('"', start));
    }

    private Browser signedIn(String username) throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login(username, PASSWORD).statusCode())
                .as("đăng nhập %s", username).isEqualTo(200);
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
