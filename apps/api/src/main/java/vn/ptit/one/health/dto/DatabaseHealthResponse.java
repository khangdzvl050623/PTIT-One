package vn.ptit.one.health.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import vn.ptit.one.health.model.DatabaseIdentity;

/**
 * Trạng thái kết nối database. Khác {@link HealthResponse}: cái kia chỉ nói
 * ứng dụng HTTP còn sống, cái này nói API có nối được đúng database không.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatabaseHealthResponse(String status, String database, String login, String error) {

    public static DatabaseHealthResponse up(DatabaseIdentity identity) {
        return new DatabaseHealthResponse("UP", identity.databaseName(), identity.loginName(), null);
    }

    public static DatabaseHealthResponse down(String error) {
        return new DatabaseHealthResponse("DOWN", null, null, error);
    }
}
