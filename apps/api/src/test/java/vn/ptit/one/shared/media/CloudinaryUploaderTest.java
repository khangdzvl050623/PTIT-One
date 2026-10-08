package vn.ptit.one.shared.media;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dựng một máy chủ giả đứng vào chỗ Cloudinary để xem request THẬT đi tới đâu
 * và mang theo gì.
 *
 * <p>Không cần khoá Cloudinary nên chạy được cả trên CI. Lý do phải có: đây là
 * chỗ duy nhất trong repo ghép {@code baseUrl} với {@code uri()} của
 * {@code RestClient} — {@code Mailer} gọi {@code post()} trần, nên đường ghép
 * này không được ca nào khác che.
 */
class CloudinaryUploaderTest {

    private static final Instant LUC = Instant.parse("2026-10-08T07:00:00Z");

    private HttpServer server;
    private final AtomicReference<String> duongDan = new AtomicReference<>();
    private final AtomicReference<String> than = new AtomicReference<>();

    @BeforeEach
    void dungMayChuGia() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            duongDan.set(exchange.getRequestURI().getPath());
            than.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"secure_url\":\"https://res.cloudinary.com/x/v9/anh.png\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void dongMayChuGia() {
        server.stop(0);
    }

    /** Cloud name phải nằm TRONG đường dẫn; mất nó là Cloudinary trả 404. */
    @Test
    void goiDungDuongDanCoCloudName() {
        String url = uploader().upload("B26DCCN001", new byte[] { 1, 2, 3 }, ImageKind.PNG);

        assertThat(duongDan.get()).isEqualTo("/democloud/image/upload");
        assertThat(url).isEqualTo("https://res.cloudinary.com/x/v9/anh.png");
    }

    @Test
    void guiDuThamSoDaKyVaFile() {
        uploader().upload("B26DCCN001", new byte[] { 1, 2, 3 }, ImageKind.PNG);

        assertThat(than.get())
                .contains("name=\"public_id\"", "ptitone/avatar/B26DCCN001")
                .contains("name=\"timestamp\"", String.valueOf(LUC.getEpochSecond()))
                .contains("name=\"overwrite\"", "name=\"invalidate\"")
                .contains("name=\"api_key\"", "khoa-cong-khai")
                .contains("name=\"signature\"")
                .contains("name=\"file\"", "filename=\"B26DCCN001.png\"");
    }

    /**
     * Chữ ký là SHA-1 của các tham số ĐÃ KÝ, sắp theo tên, nối {@code k=v&…},
     * cộng {@code api_secret} ở cuối. {@code file} và {@code api_key} không nằm
     * trong chuỗi ký — thêm vào là Cloudinary trả 401.
     */
    @Test
    void chuKyDungCongThucCuaCloudinary() throws Exception {
        uploader().upload("B26DCCN001", new byte[] { 1, 2, 3 }, ImageKind.PNG);

        String mong = sha1("invalidate=true&overwrite=true"
                + "&public_id=ptitone/avatar/B26DCCN001"
                + "&timestamp=" + LUC.getEpochSecond()
                + "bi-mat");
        assertThat(than.get()).contains(mong);
    }

    /** Xoá đi đường khác và không mang file. */
    @Test
    void xoaGoiDuongDestroy() {
        uploader().delete("B26DCCN001");

        assertThat(duongDan.get()).isEqualTo("/democloud/image/destroy");
        assertThat(than.get()).contains("ptitone/avatar/B26DCCN001").doesNotContain("name=\"file\"");
    }

    /**
     * Cloudinary từ chối thì thành {@code 503 UPLOAD_FAILED}, không rò chi tiết
     * ra ngoài — lý do thật nằm ở log cho người vận hành đọc.
     */
    @Test
    void cloudinaryTuChoiThiThanh503() throws IOException {
        server.removeContext("/");
        server.createContext("/", exchange -> {
            byte[] body = "{\"error\":{\"message\":\"Invalid Signature\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        assertThat(catchCode(() -> uploader().upload("B26DCCN001", new byte[] { 1 }, ImageKind.PNG)))
                .isEqualTo("UPLOAD_FAILED");
    }

    /** Chưa cấu hình thì tắt hẳn, và xoá vẫn chạy được — xem ghi chú ở `delete`. */
    @Test
    void chuaCauHinhThiTatUploadNhungVanXoaDuoc() {
        CloudinaryUploader tat = new CloudinaryUploader(
                new CloudinaryProperties("", "", "", "ptitone/avatar", "http://127.0.0.1:1"),
                Clock.fixed(LUC, ZoneOffset.UTC));

        assertThat(tat.enabled()).isFalse();
        assertThat(catchCode(() -> tat.upload("B26DCCN001", new byte[] { 1 }, ImageKind.PNG)))
                .isEqualTo("UPLOAD_DISABLED");
        tat.delete("B26DCCN001"); // không ném
    }

    private CloudinaryUploader uploader() {
        return new CloudinaryUploader(
                new CloudinaryProperties("democloud", "khoa-cong-khai", "bi-mat", "ptitone/avatar",
                        "http://127.0.0.1:" + server.getAddress().getPort()),
                Clock.fixed(LUC, ZoneOffset.UTC));
    }

    private static String catchCode(Runnable action) {
        try {
            action.run();
            return null;
        } catch (vn.ptit.one.shared.exception.ApiException ex) {
            return ex.code();
        }
    }

    private static String sha1(String raw) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));
    }
}
