package vn.ptit.one.enrollment.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.enrollment.model.CrossSiteRequest;
import vn.ptit.one.enrollment.model.HostDecision;
import vn.ptit.one.enrollment.service.CrossSiteHomeService.Pending;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.shared.routing.SiteContext;
import vn.ptit.one.timetable.model.ScheduleSlot;
import vn.ptit.one.timetable.service.ScheduleService;

/**
 * Điều phối saga đăng ký liên cơ sở.
 *
 * <p><b>Không</b> {@code @Transactional}: mỗi bước là một giao dịch cục bộ
 * riêng, và {@code SiteContext.runAt} từ chối đổi cơ sở khi đang trong giao
 * dịch. Ghép nhiều site bằng saga, không bằng transaction.
 *
 * <p>Chỉ có ở profile {@code distributed} — Phần 1 một database thì không có
 * cơ sở nào khác để gọi sang.
 */
@Service
@Profile("distributed")
public class CrossSiteEnrollmentService {

    private final CrossSiteHomeService home;
    private final CrossSiteHostService host;
    private final ScheduleService schedules;

    public CrossSiteEnrollmentService(CrossSiteHomeService home, CrossSiteHostService host,
            ScheduleService schedules) {
        this.home = home;
        this.host = host;
        this.schedules = schedules;
    }

    /**
     * Gửi yêu cầu học một lớp ở cơ sở khác.
     *
     * <p>Cơ sở NHÀ của sinh viên lấy từ JWT đã ký. {@code maCoSoHost} chỉ nói
     * "đọc lớp ở database nào" — khai sai thì không tìm thấy lớp, không có
     * đường leo thang đặc quyền nào.
     */
    public CrossSiteRequest request(AuthenticatedUser user, String maCoSoHost, String maLopHP) {
        String maSinhVien = requireStudent(user);

        // 0. Đọc lớp + lịch ở Host. Chỉ đọc, chưa ghi gì.
        ClassSection lop = SiteContext.runAt(maCoSoHost, () -> host.readClass(maLopHP));
        List<ScheduleSlot> lichHoc = SiteContext.runAt(maCoSoHost,
                () -> schedules.classSlots(maLopHP));
        if (!lop.maCoSoHost().equals(maCoSoHost)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND",
                    "Không tìm thấy lớp %s ở cơ sở %s.".formatted(maLopHP, maCoSoHost));
        }

        // 1. Home ghi nhận yêu cầu và giữ chỗ.
        Pending cho = home.open(maSinhVien, lop, lichHoc);

        // 2. Host xét. 3. Home áp kết quả.
        apply(maCoSoHost, cho);
        return home.read(cho.maYeuCau());
    }

    /** Huỷ: Home ghi nhận ý định, Host xác nhận, rồi Home mới trả hạn mức. */
    public CrossSiteRequest cancel(AuthenticatedUser user, String maYeuCau) {
        String maSinhVien = requireStudent(user);
        CrossSiteRequest yc = home.read(maYeuCau);
        Pending cho = home.openCancel(maSinhVien, maYeuCau);

        String maThaoTacHuy = UUID.randomUUID().toString();
        SiteContext.runAt(yc.maCoSoHost(),
                () -> host.confirmCancel(maYeuCau, cho.maLopHP(), cho.maSinhVien(), maThaoTacHuy));
        home.applyCancel(maYeuCau);
        return home.read(maYeuCau);
    }

    /**
     * Chạy lại bước Host cho một yêu cầu còn treo.
     *
     * <p>Mất mạng ở bước 3 để lại yêu cầu ở {@code DANG_XU_LY} và tín chỉ bị
     * giữ. Gọi lại với cùng {@code MaYeuCau} là tất định: Host trả nguyên kết
     * quả đã ghi, không đánh giá lại.
     */
    public CrossSiteRequest retry(AuthenticatedUser user, String maYeuCau) {
        requireStudent(user);
        CrossSiteRequest yc = home.pending(maYeuCau)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                        "CROSS_CAMPUS_ALREADY_CLOSED", "Yêu cầu đã có kết luận."));
        apply(yc.maCoSoHost(), new Pending(maYeuCau, yc.maLopHP(), yc.maSinhVien(), null, null,
                yc.phienBanLich()));
        return home.read(maYeuCau);
    }

    public List<CrossSiteRequest> list(AuthenticatedUser user, String maHocKy) {
        return home.list(requireStudent(user), maHocKy);
    }

    private void apply(String maCoSoHost, Pending cho) {
        HostDecision quyetDinh = SiteContext.runAt(maCoSoHost,
                () -> host.decide(cho.maYeuCau(), cho.maLopHP(), cho.maSinhVien(),
                        cho.maCoSoNhaSV(), cho.hoTen(), cho.phienBanLich()));
        home.applyDecision(cho.maYeuCau(), quyetDinh);
    }

    private static String requireStudent(AuthenticatedUser user) {
        if (user.role() != Role.SINH_VIEN || user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ sinh viên đăng ký học phần.");
        }
        return user.entityId();
    }
}
