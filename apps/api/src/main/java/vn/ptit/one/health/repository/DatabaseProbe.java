package vn.ptit.one.health.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.health.model.DatabaseIdentity;

/**
 * Truy vấn tối thiểu để xác nhận đường dây JDBC đang sống.
 *
 * <p>Hỏi thẳng SQL Server xem phiên hiện tại đang ở database nào và đăng nhập
 * bằng tài khoản nào, thay vì đọc lại chính cấu hình của mình — cấu hình sai
 * thì đọc lại cấu hình vẫn ra kết quả "đúng".
 *
 * <p>Gắn {@code @Profile("central")} vì {@code JdbcTemplate} chỉ tồn tại khi
 * profile đó bật. Thiếu dòng này, profile mặc định sẽ quét trúng bean và chết
 * lúc khởi động — kể cả trên máy chưa cài SQL Server.
 */
@Repository
@Profile("central")
public class DatabaseProbe {

    private final JdbcTemplate jdbc;

    public DatabaseProbe(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DatabaseIdentity currentIdentity() {
        return jdbc.queryForObject(
                "SELECT DB_NAME() AS database_name, SUSER_SNAME() AS login_name",
                (rs, rowNum) -> new DatabaseIdentity(
                        rs.getString("database_name"),
                        rs.getString("login_name")));
    }
}
