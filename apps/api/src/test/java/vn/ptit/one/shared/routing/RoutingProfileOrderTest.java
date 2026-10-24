package vn.ptit.one.shared.routing;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Thứ tự hai profile KHÔNG đổi kết quả.
 *
 * <p>Đây là một cái bẫy đáng chặn bằng test. Hai file cấu hình cùng đặt
 * {@code spring.autoconfigure.exclude}: {@code central} để trống (bật
 * DataSource tự động từ {@code spring.datasource.*}), {@code distributed} thì
 * loại trừ nó. Spring Boot nạp profile sau đè profile trước, nên nếu chỉ dựa
 * vào đó thì viết {@code distributed,central} sẽ âm thầm quay về một database
 * — ứng dụng vẫn chạy, vẫn đăng nhập được, chỉ là mọi thứ đọc nhầm CENTRAL.
 *
 * <p>Thứ tự không ảnh hưởng vì bean {@code dataSource} được khai {@code @Primary}
 * và auto-config của Spring Boot chỉ chạy khi chưa có DataSource nào. Test này
 * giữ cho điều đó đúng về sau, kể cả khi ai đó dọn dòng {@code exclude} đi.
 *
 * <p>{@link RoutingWiringTest} kiểm thứ tự thuận; lớp này kiểm thứ tự ngược.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({ "distributed", "central" })
@TestPropertySource(properties = {
        /* Flyway KHONG ton trong initialization-fail-timeout: no mo ket noi
           ngay luc dung context, nen URL gia o day lam ca test chet khi moi
           truong bat PTITONE_MIGRATE_ON_START. Mot test noi day khong co ly do
           gi chay migration — tat tuong minh thay vi phu thuoc vao .env. */
        "spring.flyway.enabled=false",
        "ptitone.sites.master.url=jdbc:sqlserver://localhost;databaseName=khong-ton-tai-master",
        "ptitone.sites.master.username=sa",
        "ptitone.sites.master.password=khong-dung",
        "ptitone.sites.nodes.HCM.url=jdbc:sqlserver://localhost;databaseName=khong-ton-tai-hcm",
        "ptitone.sites.nodes.HCM.username=sa",
        "ptitone.sites.nodes.HCM.password=khong-dung",
        "ptitone.auth.jwt-secret=dGVzdC1raG9hLWp3dC1kdS1kYWktMzItYnl0ZS1jaG8tdGVzdA==",
        "ptitone.auth.otp-secret=dGVzdC1raG9hLW90cC1kdS1kYWktMzItYnl0ZS1jaG8tdGVzdA==",
        "PTITONE_DB_URL=jdbc:sqlserver://localhost;databaseName=khong-ton-tai-central",
        "PTITONE_DB_USERNAME=sa",
        "PTITONE_DB_PASSWORD=khong-dung" })
class RoutingProfileOrderTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void vietNguocThuTuProfileVanRaBanPhanTan() {
        assertThat(dataSource).isInstanceOf(RoutingDataSource.class);
    }
}
