package vn.ptit.one;

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
 * Các màn chỉ đọc của Phần 1 trên SQL Server CENTRAL thật: GV xem lớp và danh
 * sách (F05), SV xem điểm (F07), SV xem thời khoá biểu (F09), CTĐT (F03).
 *
 * <p>Chỉ chạy khi có {@code PTITONE_DB_URL}; cần DB đã chạy seed hiện tại (có
 * dòng {@code Diem} rỗng cho ghi danh 2026-1). Không ghi gì vào DB.
 *
 * <p>Fixture: {@code GVHCM001} dạy INT1154/INT1155 2026-1 nhưng KHÔNG dạy
 * {@code BAS1203-2026-1-HCM01} (của GVHCM002, có B25DCCN001 và B26DCCN001).
 * B25DCCN001 đạt INT1154 (8.0) và trượt BAS1150 (3.3) ở 2025-1.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class ReadFeaturesIntegrationTest {

    private static final String PASSWORD = "PtitOne@2026";
    private static final String LOP_CUA_GV_KHAC = "BAS1203-2026-1-HCM01";
    private static final String LOP_CUA_GVHCM001 = "INT1154-2026-1-HCM01";

    @LocalServerPort
    private int port;

    // --- F05 Giảng viên --------------------------------------------------

    @Test
    void giangVienChiThayLopMinhPhuTrach() throws Exception {
        HttpResponse<String> response = signedIn("GVHCM001").get("/api/me/teaching-classes?maHocKy=2026-1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("INT1155-2026-1-HCM01", LOP_CUA_GVHCM001)
                .doesNotContain(LOP_CUA_GV_KHAC);
    }

    @Test
    void sinhVienKhongCoDanhSachLopPhuTrach() throws Exception {
        assertThat(signedIn("B26DCCN001").get("/api/me/teaching-classes").statusCode()).isEqualTo(403);
    }

    /** Đổi mã lớp trên URL thành lớp của giảng viên khác vẫn bị chặn. */
    @Test
    void giangVienKhongXemDuocDanhSachLopNguoiKhac() throws Exception {
        HttpResponse<String> response = signedIn("GVHCM001").get("/api/classes/" + LOP_CUA_GV_KHAC + "/students");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("AUTH_FORBIDDEN").doesNotContain("B26DCCN001");
    }

    @Test
    void giangVienXemDanhSachVaSiSoLopMinh() throws Exception {
        HttpResponse<String> response = signedIn("GVHCM001").get("/api/classes/" + LOP_CUA_GVHCM001 + "/students");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("\"soLuongDaDangKy\":1", "\"maSinhVien\":\"B26DCCN003\"");
    }

    @Test
    void adminCoSoXemDuocLopCoSoMinhNhungKhongXemCoSoKhac() throws Exception {
        HttpResponse<String> own = signedIn("admin.hcm").get("/api/classes/" + LOP_CUA_GV_KHAC + "/students");
        assertThat(own.statusCode()).isEqualTo(200);
        // Bộ đếm và các dòng ghi danh trả cùng nhau để đối soát.
        assertThat(own.body()).contains("\"soLuongDaDangKy\":2", "B25DCCN001", "B26DCCN001");

        assertThat(signedIn("admin.hn").get("/api/classes/" + LOP_CUA_GV_KHAC + "/students").statusCode())
                .isEqualTo(403);
    }

    /** Danh sách lớp chứa thông tin người khác — sinh viên cùng lớp cũng không xem được. */
    @Test
    void sinhVienKhongXemDuocDanhSachLop() throws Exception {
        assertThat(signedIn("B26DCCN001").get("/api/classes/" + LOP_CUA_GV_KHAC + "/students").statusCode())
                .isEqualTo(403);
    }

    // --- F07 Bảng điểm ---------------------------------------------------

    @Test
    void bangDiemPhanBietDatTruotVaChuaCoDiem() throws Exception {
        HttpResponse<String> response = signedIn("B25DCCN001").get("/api/me/grades");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        String body = response.body();
        assertThat(rowOf(body, "INT1154-2025-1-HCM01"))
                .contains("\"diemTongKet\":8.0", "\"ketQua\":\"DAT\"", "\"daCongBo\":true");
        assertThat(rowOf(body, "BAS1150-2025-1-HCM01")).contains("\"ketQua\":\"KHONG_DAT\"");
        // Môn đang học: có dòng nhưng chưa có điểm — null chứ không phải 0.
        assertThat(rowOf(body, LOP_CUA_GV_KHAC))
                .contains("\"diemTongKet\":null", "\"ketQua\":null", "\"daCongBo\":false");
    }

    @Test
    void bangDiemLocTheoHocKy() throws Exception {
        HttpResponse<String> response = signedIn("B25DCCN001").get("/api/me/grades?maHocKy=2025-1");

        assertThat(response.body()).contains("INT1154-2025-1-HCM01").doesNotContain(LOP_CUA_GV_KHAC);
    }

    @Test
    void chiSinhVienCoBangDiem() throws Exception {
        assertThat(signedIn("GVHCM001").get("/api/me/grades").statusCode()).isEqualTo(403);
    }

    // --- F09 Thời khoá biểu ----------------------------------------------

    @Test
    void thoiKhoaBieuCoLichCuaLopDaDangKyVaGioThat() throws Exception {
        HttpResponse<String> response = signedIn("B26DCCN001").get("/api/me/timetable?maHocKy=2026-1");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("\"ngayBatDau\":\"2026-09-07\"", LOP_CUA_GV_KHAC,
                "\"thu\":4", "\"phongHoc\":\"A2-401\"", "\"gioBatDau\":\"");
        // Không lẫn lịch của lớp không đăng ký.
        assertThat(response.body()).doesNotContain("INT1155-2026-1-HCM01");
    }

    @Test
    void thoiKhoaBieuLocTheoTuan() throws Exception {
        Browser sv = signedIn("B26DCCN001");

        assertThat(sv.get("/api/me/timetable?maHocKy=2026-1&tuan=15").body()).contains(LOP_CUA_GV_KHAC);
        // Lớp học tuần 1-15, tuần 16 phải trống.
        assertThat(sv.get("/api/me/timetable?maHocKy=2026-1&tuan=16").body()).contains("\"buoiHoc\":[]");
    }

    @Test
    void thoiKhoaBieuHocKyKhongTonTai() throws Exception {
        HttpResponse<String> response = signedIn("B26DCCN001").get("/api/me/timetable?maHocKy=1999-9");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("TERM_NOT_FOUND");
    }

    // --- F03 Chương trình đào tạo ----------------------------------------

    @Test
    void chuongTrinhDaoTaoCoDanhSachMon() throws Exception {
        Browser sv = signedIn("B26DCCN001");

        HttpResponse<String> detail = sv.get("/api/programs/CN-CNTT-2022");
        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(detail.body()).contains("\"maMonHoc\":\"BAS1150\"", "\"batBuoc\":false");

        HttpResponse<String> missing = sv.get("/api/programs/KHONG-CO");
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.body()).contains("PROGRAM_NOT_FOUND");
    }

    // --- Tiện ích --------------------------------------------------------

    /** Đoạn JSON của một dòng bảng điểm, từ mã lớp tới hết object. */
    private static String rowOf(String json, String maLopHP) {
        int start = json.indexOf("\"maLopHP\":\"" + maLopHP + "\"");
        assertThat(start).as("có dòng %s", maLopHP).isNotNegative();
        return json.substring(start, json.indexOf('}', start));
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
