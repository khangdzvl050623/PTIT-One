package vn.ptit.one.enrollment.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.enrollment.dto.CancelClassRequest;
import vn.ptit.one.enrollment.model.CancelledClass;
import vn.ptit.one.enrollment.service.ClassCancellationService;

/**
 * Huỷ lớp học phần. Thuộc module ghi danh vì phần việc chính là huỷ ghi danh và
 * trả tín chỉ; chỉ Admin cơ sở của lớp gọi được.
 */
@RestController
@RequestMapping("/api/classes")
@Profile("central")
public class ClassCancellationController {

    private final ClassCancellationService cancellations;

    public ClassCancellationController(ClassCancellationService cancellations) {
        this.cancellations = cancellations;
    }

    @PostMapping("/{maLopHP}/cancel")
    public CancelledClass cancel(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP, @Valid @RequestBody(required = false) CancelClassRequest body) {
        return cancellations.cancel(user, maLopHP, body == null ? null : body.lyDo());
    }
}
