package vn.ptit.one.shared.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Profile {@code central,distributed} khởi động được, và DataSource lộ ra cho
 * phần còn lại của ứng dụng đúng là bản định tuyến bốn nút.
 *
 * <p>Không cần SQL Server: mỗi pool đặt {@code initializationFailTimeout = -1}
 * nên Hikari không nối lúc boot. Vì vậy test này bắt được <b>lỗi cấu hình</b>
 * — thiếu biến, sai tên property, bean trùng, hai profile đá nhau — mà không
 * cần tới bốn máy. Đúng loại lỗi làm mất cả buổi nếu chỉ phát hiện lúc mở
 * trình duyệt.
 *
 * <p>Nó KHÔNG chứng minh kết nối tới các cơ sở là thông; việc đó thuộc
 * {@code db/tests/} và các phép thử chạy trên máy thật.
 */
/* MOCK chu khong phai NONE: SecurityConfig can HttpSecurity, von chi co
   trong ung dung web. MOCK dung servlet context ma khong mo cong nao. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({ "central", "distributed" })
@TestPropertySource(properties = {
        /* URL giả, cố tình không trỏ tới đâu cả. */
        "ptitone.sites.master.url=jdbc:sqlserver://localhost;databaseName=khong-ton-tai-master",
        "ptitone.sites.master.username=sa",
        "ptitone.sites.master.password=khong-dung",
        "ptitone.sites.nodes.HCM.url=jdbc:sqlserver://localhost;databaseName=khong-ton-tai-hcm",
        "ptitone.sites.nodes.HCM.username=sa",
        "ptitone.sites.nodes.HCM.password=khong-dung",
        "ptitone.sites.nodes.HN.url=jdbc:sqlserver://localhost;databaseName=khong-ton-tai-hn",
        "ptitone.sites.nodes.HN.username=sa",
        "ptitone.sites.nodes.HN.password=khong-dung",
        /* Profile central doi hai khoa nay phai co; gia tri chi can du dai. */
        "ptitone.auth.jwt-secret=dGVzdC1raG9hLWp3dC1kdS1kYWktMzItYnl0ZS1jaG8tdGVzdA==",
        "ptitone.auth.otp-secret=dGVzdC1raG9hLW90cC1kdS1kYWktMzItYnl0ZS1jaG8tdGVzdA==",
        /* Profile central tro vao bien nay. Test khong dung toi, nhung
           placeholder van phai phan giai duoc. */
        "PTITONE_DB_URL=jdbc:sqlserver://localhost;databaseName=khong-ton-tai-central",
        "PTITONE_DB_USERNAME=sa",
        "PTITONE_DB_PASSWORD=khong-dung" })
class RoutingWiringTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void profilePhanTanDungRoutingDataSource() {
        assertThat(dataSource).isInstanceOf(RoutingDataSource.class);
    }

    /**
     * Chưa ai đặt cơ sở thì rơi về Master. Đây là điều kiện để đăng nhập chạy
     * được: lúc nhận tên đăng nhập chưa biết cơ sở nào, nên phải tra danh bạ ở
     * Master trước.
     */
    @Test
    void chuaDatThiRoiVeMaster() {
        SiteContext.clear();
        assertThat(SiteContext.current()).isEqualTo(SiteContext.MASTER);
    }

    /**
     * Đổi cơ sở giữa một giao dịch phải NỔ, không được im lặng.
     *
     * <p>{@code AbstractRoutingDataSource} phân giải khoá một lần lúc mượn
     * connection, nên đổi giữa chừng không đổi được nơi ghi — mà cũng không
     * báo lỗi gì. Đây là ca kiểm cho đúng cái bẫy đó.
     */
    @Test
    void doiCoSoGiuaGiaoDichThiNemLoi() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            Throwable thrown = catchThrowable(() -> SiteContext.runAt("HN", () -> "khong toi day"));
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Mot giao dich = mot site");
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            SiteContext.clear();
        }
    }
}
