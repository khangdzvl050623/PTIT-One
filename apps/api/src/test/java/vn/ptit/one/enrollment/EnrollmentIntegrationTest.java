package vn.ptit.one.enrollment;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import vn.ptit.one.enrollment.service.EnrollmentService;
import vn.ptit.one.shared.exception.ApiException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Đăng ký và huỷ học phần (F08) qua HTTP, trên SQL Server CENTRAL thật.
 *
 * <p>Fixture seed: B25DCCN001 đạt INT1154 (8.0), trượt BAS1150 (3.2), đang học
 * BAS1203. B26DCCN001 đang học BAS1203. INT1155 (thứ 2 tiết 1-3) cần INT1154;
 * BAS1150-2026-1 (thứ 2 tiết 2-4) trùng giờ INT1155. INT1154-2026-1-HCM01 đầy 1/1.
 *
 * <p>Hai lớp thử tạo bằng SQL và xoá sau mỗi ca. Ghi danh tạo trên lớp seed được
 * huỷ lại bằng chính {@link EnrollmentService} để bộ đếm về đúng giá trị cũ.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class EnrollmentIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";
    /** Lớp INT1154 còn chỗ — để B25DCCN001 (đã đạt INT1154) đăng ký cải thiện. */
    private static final String LOP_CAI_THIEN = "TEST-F08-INT1154";
    /** Lớp BAS1203 thứ hai — B26DCCN001 đã học lớp BAS1203 khác trong kỳ. */
    private static final String LOP_TRUNG_MON = "TEST-F08-BAS1203";
    private static final String INT1155 = "INT1155-2026-1-HCM01";
    private static final String BAS1150 = "BAS1150-2026-1-HCM01";
    private static final String BAS1203 = "BAS1203-2026-1-HCM01";
    private static final String DOT_HCM = "HCM-2026-1-01";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EnrollmentService enrollments;

    @BeforeEach
    void createClasses() {
        dropClasses();
        insertClass(LOP_CAI_THIEN, "INT1154");
        insertClass(LOP_TRUNG_MON, "BAS1203");
    }

    @AfterEach
    void restore() {
        for (String lop : List.of(INT1155, BAS1150, LOP_CAI_THIEN)) {
            try {
                enrollments.cancel(student("B25DCCN001"), lop);
            } catch (ApiException ignored) {
                // Ca đó không đăng ký lớp này.
            }
        }
        dropClasses();
    }

    // --- Đăng ký thành công ----------------------------------------------

    @Test
    void dangKyBamLaiRoiHuy() throws Exception {
        Browser sv = signedIn("B25DCCN001");

        HttpResponse<String> created = sv.post("/api/me/enrollments", body(INT1155));
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(created.body()).contains("\"loaiDangKy\":\"HOC_MOI\"", "\"trangThai\":\"DA_DANG_KY\"");

        // Bấm lại: trả kết quả cũ, không cộng sĩ số lần nữa.
        assertThat(sv.post("/api/me/enrollments", body(INT1155)).statusCode()).isEqualTo(200);
        assertThat(siSo(INT1155)).isEqualTo(1);

        assertThat(sv.get("/api/me/enrollments?maHocKy=2026-1").body()).contains(INT1155);
        assertThat(sv.get("/api/me/timetable?maHocKy=2026-1").body()).contains(INT1155);

        HttpResponse<String> cancelled = sv.delete("/api/me/enrollments/" + INT1155);
        assertThat(cancelled.statusCode()).as(cancelled.body()).isEqualTo(200);
        assertThat(cancelled.body()).doesNotContain(INT1155);
        assertThat(siSo(INT1155)).isZero();
        assertThat(sv.get("/api/me/timetable?maHocKy=2026-1").body()).doesNotContain(INT1155);
    }

    /** Môn đã trượt đăng ký lại là học lại; môn đã đạt đăng ký lại là cải thiện. */
    @Test
    void phanBietHocLaiVaCaiThien() throws Exception {
        Browser sv = signedIn("B25DCCN001");

        HttpResponse<String> hocLai = sv.post("/api/me/enrollments", body(BAS1150));
        assertThat(hocLai.statusCode()).as(hocLai.body()).isEqualTo(201);
        assertThat(hocLai.body()).contains("\"loaiDangKy\":\"HOC_LAI\"");

        HttpResponse<String> caiThien = sv.post("/api/me/enrollments", body(LOP_CAI_THIEN));
        assertThat(caiThien.statusCode()).as(caiThien.body()).isEqualTo(201);
        assertThat(caiThien.body()).contains("\"loaiDangKy\":\"CAI_THIEN\"");
    }

    // --- Từ chối ---------------------------------------------------------

    @Test
    void chuaDatTienQuyetThiBiChanVaNeuTenMon() throws Exception {
        HttpResponse<String> response = signedIn("B25DCCN001")
                .post("/api/me/enrollments", body("BAS1151-2026-1-HCM01"));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("PREREQUISITE_NOT_MET", "BAS1150");
    }

    @Test
    void trungLichThiBiChan() throws Exception {
        Browser sv = signedIn("B25DCCN001");
        assertThat(sv.post("/api/me/enrollments", body(INT1155)).statusCode()).isEqualTo(201);

        HttpResponse<String> response = sv.post("/api/me/enrollments", body(BAS1150));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("SCHEDULE_CLASH", INT1155);
    }

    /** Hai lớp khác nhau của cùng một môn trong cùng kỳ. */
    @Test
    void trungMonThiBiChan() throws Exception {
        HttpResponse<String> response = signedIn("B26DCCN001").post("/api/me/enrollments", body(LOP_TRUNG_MON));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("ENROLLMENT_DUPLICATE_COURSE", BAS1203);
    }

    /** Lớp đầy: tín chỉ đã cộng ở bước trước phải được rollback cùng. */
    @Test
    void lopDayThiKhongMatTinChi() throws Exception {
        int truoc = tinChi("B25DCCN001");

        HttpResponse<String> response = signedIn("B25DCCN001")
                .post("/api/me/enrollments", body("INT1154-2026-1-HCM01"));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("CLASS_FULL");
        assertThat(tinChi("B25DCCN001")).isEqualTo(truoc);
    }

    @Test
    void vuotTranTinChiThiBiChan() throws Exception {
        jdbc.update("UPDATE dbo.SinhVienHocKy SET TranTinChi = 3 WHERE MaSinhVien = 'B26DCCN001' AND MaHocKy = '2026-1'");
        try {
            HttpResponse<String> response = signedIn("B26DCCN001").post("/api/me/enrollments", body(BAS1150));

            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body()).contains("CREDIT_LIMIT_EXCEEDED");
            assertThat(siSo(BAS1150)).isZero();
        } finally {
            jdbc.update("UPDATE dbo.SinhVienHocKy SET TranTinChi = 24 WHERE MaSinhVien = 'B26DCCN001' AND MaHocKy = '2026-1'");
        }
    }

    @Test
    void lopCoSoKhacVaLopChuaMoThiBiChan() throws Exception {
        HttpResponse<String> lienCoSo = signedIn("B26DCCN002").post("/api/me/enrollments", body(BAS1203));
        assertThat(lienCoSo.statusCode()).isEqualTo(409);
        assertThat(lienCoSo.body()).contains("ENROLLMENT_CROSS_CAMPUS");

        HttpResponse<String> chuaMo = signedIn("B26DCCN001").post("/api/me/enrollments", body("INT1445-2026-1-HCM01"));
        assertThat(chuaMo.statusCode()).isEqualTo(409);
        assertThat(chuaMo.body()).contains("CLASS_NOT_OPEN");
    }

    /** Đợt vẫn ghi DANG_MO nhưng đã quá giờ đóng: không tính là đang mở — cả đăng ký lẫn huỷ. */
    @Test
    void dotDaQuaGioDongThiKhongDangKyKhongHuy() throws Exception {
        Map<String, Object> goc = jdbc.queryForMap(
                "SELECT ThoiGianMo, ThoiGianDong FROM dbo.DotDangKy WHERE MaDot = ?", DOT_HCM);
        jdbc.update("""
                UPDATE dbo.DotDangKy SET ThoiGianMo = '2025-12-01', ThoiGianDong = '2026-01-01'
                 WHERE MaDot = ?
                """, DOT_HCM);
        try {
            Browser sv = signedIn("B26DCCN001");

            HttpResponse<String> register = sv.post("/api/me/enrollments", body(BAS1150));
            assertThat(register.statusCode()).isEqualTo(409);
            assertThat(register.body()).contains("ENROLLMENT_PERIOD_CLOSED");

            HttpResponse<String> cancel = sv.delete("/api/me/enrollments/" + BAS1203);
            assertThat(cancel.statusCode()).isEqualTo(409);
            assertThat(cancel.body()).contains("ENROLLMENT_PERIOD_CLOSED");
        } finally {
            jdbc.update("UPDATE dbo.DotDangKy SET ThoiGianMo = ?, ThoiGianDong = ? WHERE MaDot = ?",
                    goc.get("ThoiGianMo"), goc.get("ThoiGianDong"), DOT_HCM);
        }
    }

    /** Đã có điểm thì không huỷ được, và mọi thứ giữ nguyên — không xoá điểm để huỷ. */
    @Test
    void daCoDiemThiKhongHuy() throws Exception {
        jdbc.update("UPDATE dbo.Diem SET DiemChuyenCan = 8 WHERE MaLopHP = ? AND MaSinhVien = 'B26DCCN001'", BAS1203);
        try {
            HttpResponse<String> response = signedIn("B26DCCN001").delete("/api/me/enrollments/" + BAS1203);

            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body()).contains("ENROLLMENT_HAS_GRADE");
            assertThat(siSo(BAS1203)).isEqualTo(2);
            assertThat(tinChi("B26DCCN001")).isEqualTo(3);
        } finally {
            jdbc.update("UPDATE dbo.Diem SET DiemChuyenCan = NULL WHERE MaLopHP = ? AND MaSinhVien = 'B26DCCN001'", BAS1203);
        }
    }

    @Test
    void huyLopKhongDangKyThi404() throws Exception {
        HttpResponse<String> response = signedIn("B26DCCN001").delete("/api/me/enrollments/" + INT1155);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("ENROLLMENT_NOT_FOUND");
    }

    @Test
    void chiSinhVienDangKy() throws Exception {
        assertThat(signedIn("GVHCM001").post("/api/me/enrollments", body(INT1155)).statusCode()).isEqualTo(403);
    }

    // --- Tiện ích --------------------------------------------------------

    private void insertClass(String maLopHP, String maMonHoc) {
        jdbc.update("""
                INSERT INTO dbo.LopHocPhan (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, SoLuongToiDa,
                                            TrangThai, ChoPhepLienCoSo, HinhThucHoc)
                VALUES (?, ?, '2026-1', 'HCM', 5, 'MO', 0, 'TRUC_TIEP')
                """, maLopHP, maMonHoc);
    }

    private void dropClasses() {
        for (String lop : List.of(LOP_CAI_THIEN, LOP_TRUNG_MON)) {
            jdbc.update("""
                    DELETE n FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao
                     WHERE t.MaLopHP = ?
                    """, lop);
            jdbc.update("DELETE FROM dbo.ThongBao WHERE MaLopHP = ?", lop);
            jdbc.update("DELETE FROM dbo.Diem WHERE MaLopHP = ?", lop);
            jdbc.update("DELETE FROM dbo.DangKyMonHoc WHERE MaLopHP = ?", lop);
            jdbc.update("DELETE FROM dbo.DangKyHocPhan WHERE MaLopHP = ?", lop);
            jdbc.update("DELETE FROM dbo.LopHocPhan WHERE MaLopHP = ?", lop);
        }
    }

    private int siSo(String maLopHP) {
        return jdbc.queryForObject("SELECT SoLuongDaDangKy FROM dbo.LopHocPhan WHERE MaLopHP = ?",
                Integer.class, maLopHP);
    }

    private int tinChi(String maSinhVien) {
        return jdbc.queryForObject(
                "SELECT SoTinChiDaDangKy FROM dbo.SinhVienHocKy WHERE MaSinhVien = ? AND MaHocKy = '2026-1'",
                Integer.class, maSinhVien);
    }

    private static String body(String maLopHP) {
        return "{\"maLopHP\":\"" + maLopHP + "\"}";
    }

    private static AuthenticatedUser student(String maSinhVien) {
        return new AuthenticatedUser(maSinhVien, Role.SINH_VIEN, maSinhVien, "HCM",
                UUID.randomUUID(), 1, Instant.now().plusSeconds(3600));
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
