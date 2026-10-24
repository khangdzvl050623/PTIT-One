package vn.ptit.one.shared.routing;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Dựng một pool kết nối cho mỗi nút, rồi gộp lại sau {@link RoutingDataSource}.
 *
 * <p>Chỉ bật ở profile {@code distributed}. Profile {@code central} của Phần 1
 * vẫn dùng một {@code spring.datasource} như cũ, nên hai chế độ không đụng nhau
 * và bộ test hiện có không bị ảnh hưởng.
 */
@Configuration
@Profile("distributed")
/* Bat lich cho OutboxWorker. Chi o profile phan tan: Phan 1 mot database
   thi khong co gi de day di dau ca. */
@EnableScheduling
@EnableConfigurationProperties(SiteProperties.class)
public class RoutingDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(RoutingDataSourceConfig.class);

    /**
     * Một DataSource duy nhất lộ ra cho phần còn lại của ứng dụng. Spring Boot
     * tự nối {@code JdbcTemplate} vào đây, nên repository không phải sửa gì.
     */
    @Bean
    @Primary
    public DataSource dataSource(SiteProperties properties) {
        if (properties.master() == null || properties.master().url() == null) {
            throw new IllegalStateException(
                    "Thieu ptitone.sites.master.url. Profile 'distributed' can dia chi cua PTITONE_MASTER.");
        }

        DataSource master = pool(SiteContext.MASTER, properties.master(), properties.poolSize());

        Map<Object, Object> bySite = new LinkedHashMap<>();
        bySite.put(SiteContext.MASTER, master);

        if (properties.nodes() != null) {
            properties.nodes().forEach((code, node) -> {
                /* Nút chưa khai thì bỏ qua, không dựng pool rỗng. Các máy của
                   nhóm lên dần, nên ứng dụng phải chạy được với hai nút hôm
                   nay và bốn nút tuần sau mà không phải sửa gì. */
                if (node == null || node.url() == null || node.url().isBlank()) {
                    log.warn("Bo qua co so {}: chua khai ptitone.sites.nodes.{}.url", code, code);
                    return;
                }
                bySite.put(code.toUpperCase(), pool(code, node, properties.poolSize()));
            });
        }

        if (bySite.size() == 1) {
            log.warn("Chi cau hinh moi MASTER. Chua co co so nao trong ptitone.sites.nodes.*");
        }
        log.info("Dinh tuyen theo co so: {}", bySite.keySet());

        RoutingDataSource routing = new RoutingDataSource(master, bySite);
        routing.afterPropertiesSet();
        return routing;
    }

    private static HikariDataSource pool(String name, SiteProperties.Node node, int poolSize) {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("ptitone-" + name.toLowerCase());
        ds.setJdbcUrl(node.url());
        ds.setUsername(node.username());
        ds.setPassword(node.password());
        ds.setMaximumPoolSize(poolSize);

        /* ⚠️ BẮT BUỘC. Không có dòng này, một cơ sở đang tắt sẽ làm cả ứng dụng
           không khởi động nổi — tức là mất luôn kịch bản demo "tắt một site,
           ba site kia vẫn chạy". Với -1 thì pool để dành việc báo lỗi tới lúc
           thật sự có người hỏi tới cơ sở đó. */
        ds.setInitializationFailTimeout(-1);

        /* Site ở xa nối qua VPN. Thiếu hai mốc thời gian ngắn này, một site chết
           sẽ hút cạn thread pool và kéo sập cả những request cục bộ vốn đáng lẽ
           vẫn phải sống. */
        ds.setConnectionTimeout(5_000);
        ds.setValidationTimeout(3_000);
        return ds;
    }
}
