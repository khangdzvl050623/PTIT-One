package vn.ptit.one.health;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;

import vn.ptit.one.health.controller.DatabaseHealthController;
import vn.ptit.one.health.dto.DatabaseHealthResponse;
import vn.ptit.one.health.model.DatabaseIdentity;
import vn.ptit.one.health.repository.DatabaseProbe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@code /api/health/db} khi SQL Server tắt: 503 + {@code DOWN}, không ném lỗi
 * 500. Dùng mock vì cần giả lập DB hỏng; ca DB sống đã có ở test tích hợp.
 */
class DatabaseHealthControllerTest {

    @Test
    void sqlServerTatThiTra503Down() {
        DatabaseProbe probe = mock(DatabaseProbe.class);
        when(probe.currentIdentity()).thenThrow(new DataAccessResourceFailureException("Connection refused"));

        ResponseEntity<DatabaseHealthResponse> response = new DatabaseHealthController(probe).database();

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DOWN");
        assertThat(response.getBody().database()).isNull();
    }

    @Test
    void sqlServerSongThiTraUp() {
        DatabaseProbe probe = mock(DatabaseProbe.class);
        when(probe.currentIdentity()).thenReturn(new DatabaseIdentity("PTITONE_CENTRAL", "ptitone_api"));

        ResponseEntity<DatabaseHealthResponse> response = new DatabaseHealthController(probe).database();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().status()).isEqualTo("UP");
        assertThat(response.getBody().database()).isEqualTo("PTITONE_CENTRAL");
    }
}
