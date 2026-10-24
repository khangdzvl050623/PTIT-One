package vn.ptit.one.enrollment.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.enrollment.dto.CrossSiteRequestBody;
import vn.ptit.one.enrollment.model.CrossSiteRequest;
import vn.ptit.one.enrollment.service.CrossSiteEnrollmentService;

/**
 * Đăng ký học phần ở cơ sở khác (saga). Chỉ có ở profile {@code distributed}.
 */
@RestController
@RequestMapping("/api/me/cross-campus")
@Profile("distributed")
public class CrossSiteEnrollmentController {

    private final CrossSiteEnrollmentService crossSite;

    public CrossSiteEnrollmentController(CrossSiteEnrollmentService crossSite) {
        this.crossSite = crossSite;
    }

    @GetMapping
    public List<CrossSiteRequest> list(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String maHocKy) {
        return crossSite.list(user, maHocKy);
    }

    @PostMapping
    public CrossSiteRequest request(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CrossSiteRequestBody body) {
        return crossSite.request(user, body.maCoSoHost(), body.maLopHP());
    }

    @PostMapping("/{maYeuCau}/cancel")
    public CrossSiteRequest cancel(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maYeuCau) {
        return crossSite.cancel(user, maYeuCau);
    }

    /** Chạy lại bước Host cho yêu cầu còn treo vì mất mạng. */
    @PostMapping("/{maYeuCau}/retry")
    public CrossSiteRequest retry(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maYeuCau) {
        return crossSite.retry(user, maYeuCau);
    }
}
