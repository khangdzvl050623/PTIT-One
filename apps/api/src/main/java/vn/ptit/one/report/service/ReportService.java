package vn.ptit.one.report.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.course.service.CourseService;
import vn.ptit.one.grade.policy.GradePolicy;
import vn.ptit.one.report.model.CourseReport;
import vn.ptit.one.report.model.ReportScope;
import vn.ptit.one.report.model.ReportSummary;
import vn.ptit.one.report.repository.ReportRepository;
import vn.ptit.one.report.repository.ReportRepository.SummaryRow;
import vn.ptit.one.shared.exception.ApiException;

/**
 * Thống kê học vụ cho hai cấp quản trị.
 *
 * <p>Phạm vi cơ sở lấy từ JWT: Admin cơ sở luôn chỉ thấy cơ sở mình; bộ lọc
 * {@code maCoSo} chỉ có tác dụng với Admin Master và không bao giờ MỞ RỘNG quyền.
 */
@Service
@Profile("central")
public class ReportService {

    private final ReportRepository reports;
    private final CourseService courses;
    private final GradePolicy policy;
    private final Clock clock;

    public ReportService(ReportRepository reports, CourseService courses, GradePolicy policy, Clock clock) {
        this.reports = reports;
        this.courses = courses;
        this.policy = policy;
        this.clock = clock;
    }

    public ReportSummary summary(AuthenticatedUser user, String maHocKy, String maCoSo, String maMonHoc) {
        ReportScope scope = scope(user, maHocKy, maCoSo, maMonHoc);
        SummaryRow row = reports.summary(scope);
        return new ReportSummary(scope,
                new ReportSummary.Registrations(row.soLop(), row.luotDangKy(), row.soSinhVien()),
                new ReportSummary.Capacity(row.tongSucChua(), row.tongDaDangKy(),
                        ratio(row.tongDaDangKy(), row.tongSucChua()),
                        row.soLopDay(), row.soLop() - row.soLopDay()),
                new ReportSummary.GradeProgress(row.soLop() - row.daCongBo() - row.daKhoa(),
                        row.daCongBo(), row.daKhoa()),
                clock.instant());
    }

    public CourseReport byCourse(AuthenticatedUser user, String maHocKy, String maCoSo) {
        ReportScope scope = scope(user, maHocKy, maCoSo, null);
        return new CourseReport(scope, reports.byCourse(scope, policy.nguongDat()).stream()
                .map(r -> new CourseReport.Row(r.maMonHoc(), r.tenMonHoc(), r.soLop(), r.luotDangKy(),
                        r.tongSucChua(), r.tongDaDangKy(), ratio(r.tongDaDangKy(), r.tongSucChua()),
                        r.soDat(), r.soTruot(), r.luotDangKy() - r.soDat() - r.soTruot(), r.phanBoDiem()))
                .toList(), clock.instant());
    }

    private ReportScope scope(AuthenticatedUser user, String maHocKy, String maCoSo, String maMonHoc) {
        courses.requireTerm(maHocKy);
        String loc = blankToNull(maCoSo);
        String campus = switch (user.role()) {
            case ADMIN_MASTER -> loc;
            case ADMIN_CO_SO -> {
                if (loc != null && !loc.equals(user.homeCampus())) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                            "Bạn chỉ xem được thống kê của cơ sở %s.".formatted(user.homeCampus()));
                }
                yield user.homeCampus();
            }
            default -> throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ quản trị mới xem được thống kê.");
        };
        return new ReportScope(maHocKy, campus, blankToNull(maMonHoc));
    }

    /** Tỉ lệ trên TỔNG, không phải trung bình phần trăm từng lớp. */
    private static BigDecimal ratio(int part, int whole) {
        if (whole == 0) {
            return null;
        }
        return BigDecimal.valueOf(part).divide(BigDecimal.valueOf(whole), 4, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
