package vn.ptit.one.enrollment.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.enrollment.model.EnrollmentPeriod;
import vn.ptit.one.enrollment.repository.EnrollmentPeriodRepository;
import vn.ptit.one.shared.exception.ApiException;

/**
 * Đợt đăng ký (F04).
 *
 * <p>Cũng là API công khai để module khác hỏi "học kỳ nào đang mở đăng ký" —
 * ví dụ {@code course} cần biết trước khi cho đổi môn tiên quyết. Module khác
 * KHÔNG tự join vào {@code DotDangKy}.
 */
@Service
@Profile("central")
public class EnrollmentPeriodService {

    private static final List<String> TRANG_THAI = List.of(
            EnrollmentPeriod.CHUA_MO, EnrollmentPeriod.DANG_MO, EnrollmentPeriod.DA_DONG);

    private final EnrollmentPeriodRepository periods;
    private final Clock clock;

    public EnrollmentPeriodService(EnrollmentPeriodRepository periods, Clock clock) {
        this.periods = periods;
        this.clock = clock;
    }

    // --- API công khai cho module khác -----------------------------------

    /** Học kỳ đang có đợt mở ở bất kỳ cơ sở nào. */
    public List<String> openTerms() {
        return periods.openTerms(clock.instant());
    }

    /** Cơ sở này có đang mở đăng ký cho học kỳ đó không. Dùng cho F08. */
    public boolean isOpen(String maHocKy, String maCoSo) {
        Instant now = clock.instant();
        return periods.search(maHocKy, maCoSo).stream().anyMatch(dot -> dot.dangMo(now));
    }

    // --- Quản trị ---------------------------------------------------------

    public List<EnrollmentPeriod> search(AuthenticatedUser user, String maHocKy) {
        String campusScope = user.role() == Role.ADMIN_MASTER ? null : user.homeCampus();
        return periods.search(maHocKy, campusScope);
    }

    @Transactional
    public EnrollmentPeriod create(AuthenticatedUser user, String maHocKy,
            Instant thoiGianMo, Instant thoiGianDong, String trangThai) {
        String campus = requireCampusAdmin(user);
        validate(thoiGianMo, thoiGianDong, trangThai);

        String prefix = "%s-%s-".formatted(campus, maHocKy);
        String maDot = "%s%02d".formatted(prefix, periods.nextSequence(prefix));
        try {
            periods.insert(new EnrollmentPeriod(maDot, maHocKy, campus,
                    thoiGianMo, thoiGianDong, trangThai));
        } catch (DuplicateKeyException ex) {
            throw alreadyOpen(campus, maHocKy);
        }
        return require(maDot);
    }

    @Transactional
    public EnrollmentPeriod update(AuthenticatedUser user, String maDot,
            Instant thoiGianMo, Instant thoiGianDong, String trangThai) {
        EnrollmentPeriod dot = require(maDot);
        String campus = requireCampusAdmin(user);
        if (!campus.equals(dot.maCoSo())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Đợt %s thuộc cơ sở khác.".formatted(maDot));
        }
        validate(thoiGianMo, thoiGianDong, trangThai);

        try {
            periods.update(maDot, thoiGianMo, thoiGianDong, trangThai);
        } catch (DuplicateKeyException ex) {
            /* Unique filtered index trong V3 chặn: một cơ sở chỉ được MỘT đợt
               DANG_MO trong một học kỳ. Muốn mở đợt bổ sung thì đóng đợt cũ trước. */
            throw alreadyOpen(dot.maCoSo(), dot.maHocKy());
        }
        return require(maDot);
    }

    public EnrollmentPeriod require(String maDot) {
        return periods.findOne(maDot)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PERIOD_NOT_FOUND",
                        "Không tìm thấy đợt đăng ký %s.".formatted(maDot)));
    }

    private static void validate(Instant thoiGianMo, Instant thoiGianDong, String trangThai) {
        if (!TRANG_THAI.contains(trangThai)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PERIOD_STATUS_INVALID",
                    "Trạng thái đợt không hợp lệ: %s.".formatted(trangThai));
        }
        if (!thoiGianMo.isBefore(thoiGianDong)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PERIOD_WINDOW_INVALID",
                    "Thời gian mở phải trước thời gian đóng.");
        }
    }

    private static ApiException alreadyOpen(String maCoSo, String maHocKy) {
        return new ApiException(HttpStatus.CONFLICT, "PERIOD_ALREADY_OPEN",
                "Cơ sở %s đã có một đợt đang mở cho học kỳ %s. Đóng đợt đó trước."
                        .formatted(maCoSo, maHocKy));
    }

    private static String requireCampusAdmin(AuthenticatedUser user) {
        if (user.role() != Role.ADMIN_CO_SO || user.homeCampus() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ quản trị đào tạo của cơ sở mới quản lý được đợt đăng ký.");
        }
        return user.homeCampus();
    }
}
