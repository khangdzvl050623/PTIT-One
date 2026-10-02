package vn.ptit.one.enrollment.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.enrollment.dto.SaveEnrollmentPeriodRequest;
import vn.ptit.one.enrollment.model.EnrollmentPeriod;
import vn.ptit.one.enrollment.service.EnrollmentPeriodService;

/** Đợt đăng ký (F04). Ghi chỉ Admin cơ sở, trong cơ sở mình (B3). */
@RestController
@RequestMapping("/api/enrollment-periods")
@Profile("central")
public class EnrollmentPeriodController {

    private final EnrollmentPeriodService periods;

    public EnrollmentPeriodController(EnrollmentPeriodService periods) {
        this.periods = periods;
    }

    @GetMapping
    public List<EnrollmentPeriod> search(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String maHocKy) {
        return periods.search(user, maHocKy);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentPeriod create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SaveEnrollmentPeriodRequest body) {
        return periods.create(user, body.maHocKy(), body.thoiGianMo(),
                body.thoiGianDong(), body.trangThai());
    }

    @PutMapping("/{maDot}")
    public EnrollmentPeriod update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maDot, @Valid @RequestBody SaveEnrollmentPeriodRequest body) {
        return periods.update(user, maDot, body.thoiGianMo(), body.thoiGianDong(), body.trangThai());
    }
}
