package vn.ptit.one.health.dto;

/** HTTP liveness response; does not represent database readiness. */
public record HealthResponse(String service, String status) {
}
