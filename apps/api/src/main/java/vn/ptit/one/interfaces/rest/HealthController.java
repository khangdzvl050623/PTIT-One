package vn.ptit.one.interfaces.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reports HTTP application liveness only, not database readiness. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("ptit-one-api", "UP");
    }

    public record HealthResponse(String service, String status) {
    }
}
