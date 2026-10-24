package vn.ptit.one.shared.routing;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Một {@link DataSource} giả: mỗi lần cần kết nối thì nhìn {@link SiteContext}
 * để chọn trong các kết nối thật.
 *
 * <p>Nhờ nó, toàn bộ repository của Phần 1 chạy nguyên si trên bản phân tán —
 * chúng vẫn nhận một {@code JdbcTemplate} như cũ và không biết gì về chuyện có
 * bốn database.
 *
 * <p>⚠️ Khoá được phân giải <b>một lần</b>, lúc mượn connection. Trong một
 * giao dịch, connection đã mượn thì giữ nguyên tới lúc commit — nên đổi
 * {@code SiteContext} giữa giao dịch KHÔNG đổi được nơi ghi. {@link
 * SiteContext#runAt} chặn sẵn trường hợp đó.
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    RoutingDataSource(DataSource fallback, Map<Object, Object> bySite) {
        setDefaultTargetDataSource(fallback);
        setTargetDataSources(bySite);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return SiteContext.current();
    }
}
