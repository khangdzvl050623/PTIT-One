package vn.ptit.one.health.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.health.dto.DatabaseHealthResponse;
import vn.ptit.one.health.repository.DatabaseProbe;

/**
 * Kiểm tra kết nối database, tách khỏi liveness ở {@link HealthController}.
 *
 * <p>Chỉ tồn tại ở profile {@code central}. Ở profile mặc định (skeleton chưa
 * nối DB) endpoint này trả 404 — đúng hơn là trả UP giả.
 */
@RestController
@RequestMapping("/api/health/db")
@Profile("central")
public class DatabaseHealthController {

    private final DatabaseProbe probe;

    public DatabaseHealthController(DatabaseProbe probe) {
        this.probe = probe;
    }

    @GetMapping
    public ResponseEntity<DatabaseHealthResponse> database() {
        try {
            return ResponseEntity.ok(DatabaseHealthResponse.up(probe.currentIdentity()));
        } catch (DataAccessException ex) {
            /* SQL Server tắt, sai mật khẩu hoặc thiếu quyền đều rơi vào đây.
               Trả 503 để công cụ giám sát phân biệt được với lỗi ứng dụng. */
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(DatabaseHealthResponse.down(ex.getMostSpecificCause().getMessage()));
        }
    }
}
