package vn.ptit.one.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Cột {@code datetime2} không mang múi giờ; quy ước lưu UTC.
 *
 * <p>Không dùng {@code java.sql.Timestamp}: driver đổi nó theo múi giờ JVM,
 * máy đặt giờ Việt Nam sẽ ghi lệch 7 tiếng mà không báo lỗi.
 */
final class SqlTime {

    private SqlTime() {
    }

    static LocalDateTime toDb(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    static Instant fromDb(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value != null ? value.toInstant(ZoneOffset.UTC) : null;
    }
}
