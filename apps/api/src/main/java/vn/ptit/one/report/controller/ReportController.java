package vn.ptit.one.report.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.report.model.CourseReport;
import vn.ptit.one.report.model.ReportSummary;
import vn.ptit.one.report.service.ReportService;

/**
 * Thống kê. {@code maCoSo} là bộ lọc của Admin Master; Admin cơ sở luôn bị
 * giới hạn trong cơ sở của mình, gửi cơ sở khác thì {@code 403}.
 */
@RestController
@RequestMapping("/api/reports")
@Profile("central")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/summary")
    public ReportSummary summary(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String maHocKy,
            @RequestParam(required = false) String maCoSo,
            @RequestParam(required = false) String maMonHoc) {
        return reports.summary(user, maHocKy, maCoSo, maMonHoc);
    }

    @GetMapping("/courses")
    public CourseReport courses(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String maHocKy,
            @RequestParam(required = false) String maCoSo) {
        return reports.byCourse(user, maHocKy, maCoSo);
    }
}
