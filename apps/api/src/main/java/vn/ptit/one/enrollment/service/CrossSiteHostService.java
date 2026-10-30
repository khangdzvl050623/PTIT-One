package vn.ptit.one.enrollment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.enrollment.model.EnrollmentStatus;
import vn.ptit.one.enrollment.model.HostDecision;
import vn.ptit.one.enrollment.repository.HostRequestRepository;
import vn.ptit.one.grade.service.GradeRecords;
import vn.ptit.one.shared.exception.ApiException;

/**
 * Bước Host của saga. Mọi phương thức chạy trong
 * {@code SiteContext.runAt(HOST, …)}, tức trên database của cơ sở khác.
 *
 * <p>Host bảo vệ ràng buộc theo <b>lớp</b>: sức chứa, đợt đăng ký, điều kiện
 * lớp. Ràng buộc theo sinh viên là việc của Home.
 */
@Service
@Profile("central")
public class CrossSiteHostService {

    private static final int LOCK_TIMEOUT_MS = 5_000;

    private final HostRequestRepository requests;
    private final ClassSectionService classes;
    private final EnrollmentPeriodService periods;
    private final GradeRecords grades;
    private final Clock clock;

    public CrossSiteHostService(HostRequestRepository requests, ClassSectionService classes,
            EnrollmentPeriodService periods, GradeRecords grades, Clock clock) {
        this.requests = requests;
        this.classes = classes;
        this.periods = periods;
        this.grades = grades;
        this.clock = clock;
    }

    /**
     * Xét một yêu cầu đăng ký. Trả lại nguyên kết quả cũ nếu đã xét rồi.
     *
     * @param phienBanLich phiên bản lịch lúc Home kiểm trùng giờ
     */
    @Transactional
    public HostDecision decide(String maYeuCau, String maLopHP, String maSinhVien,
            String maCoSoNhaSV, String hoTen, int phienBanLich) {
        acquire(maYeuCau);
        var daXet = requests.findDecision(maYeuCau);
        if (daXet.isPresent()) {
            return daXet.get();
        }

        HostDecision quyetDinh = xet(maLopHP, phienBanLich);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (quyetDinh.duocDuyet()) {
            requests.upsertEnrollment(maLopHP, maSinhVien, maCoSoNhaSV, hoTen, maYeuCau, now);
            grades.openRecord(maLopHP, maSinhVien);
        }
        requests.insertDecision(maYeuCau, quyetDinh, maLopHP, maSinhVien, now);
        return quyetDinh;
    }

    /**
     * Xác nhận huỷ. Idempotent, và ghi <b>bia mộ</b> khi chưa có đăng ký nào —
     * nếu không, lệnh đăng ký tới muộn sẽ tạo một đăng ký mồ côi mà Home tưởng
     * đã huỷ.
     */
    @Transactional
    public HostDecision confirmCancel(String maYeuCau, String maLopHP, String maSinhVien,
            String maThaoTacHuy) {
        acquire(maYeuCau);
        var daXet = requests.findDecision(maYeuCau);
        if (daXet.isEmpty()) {
            HostDecision bia = HostDecision.daHuy();
            requests.insertDecision(maYeuCau, bia, maLopHP, maSinhVien,
                    LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
            return bia;
        }
        /* Chỉ luồng THẮNG mới trả chỗ. Cả ba việc trong MỘT giao dịch: nếu câu
           UPDATE đầu commit rồi tiến trình chết, retry sẽ thấy DA_HUY và không
           trả chỗ nữa — chỗ đó mất vĩnh viễn. */
        if (requests.moveDecision(maYeuCau, EnrollmentStatus.DA_DANG_KY,
                EnrollmentStatus.DA_HUY, maThaoTacHuy) == 1) {
            // LopHocPhan trước DangKyHocPhan — cùng thứ tự với mọi luồng khác, đảo là deadlock.
            classes.releaseSeat(maLopHP);
            requests.cancelEnrollment(maYeuCau);
        } else {
            requests.moveDecision(maYeuCau, EnrollmentStatus.TU_CHOI,
                    EnrollmentStatus.DA_HUY, maThaoTacHuy);
        }
        return HostDecision.daHuy();
    }

    /** Thông tin lớp để Home kiểm trước khi ghi yêu cầu. Chỉ đọc. */
    public ClassSection readClass(String maLopHP) {
        return classes.require(maLopHP);
    }

    private HostDecision xet(String maLopHP, int phienBanLich) {
        ClassSection lop = classes.require(maLopHP);

        /* Lịch đổi giữa chừng thì phép kiểm trùng giờ Home vừa làm là vô nghĩa. */
        if (lop.phienBanLich() != phienBanLich) {
            return HostDecision.tuChoi("SCHEDULE_VERSION_STALE",
                    "Lịch lớp %s đã thay đổi, hãy xem lại rồi đăng ký lại.".formatted(maLopHP));
        }
        if (!ClassSectionService.MO.equals(lop.trangThai())) {
            return HostDecision.tuChoi("CLASS_NOT_OPEN",
                    "Lớp %s không còn mở đăng ký.".formatted(maLopHP));
        }
        if (!lop.choPhepLienCoSo()) {
            return HostDecision.tuChoi("CROSS_CAMPUS_NOT_ALLOWED",
                    "Lớp %s không nhận sinh viên cơ sở khác.".formatted(maLopHP));
        }
        /* Home đã kiểm, nhưng Host mới sở hữu lớp — phòng thủ nhiều lớp, không
           tin đầu vào từ site khác. */
        if (!ClassSectionService.TRUC_TUYEN.equals(lop.hinhThucHoc())) {
            return HostDecision.tuChoi("CROSS_CAMPUS_REQUIRES_ONLINE",
                    "Chỉ lớp trực tuyến mới cho đăng ký liên cơ sở.");
        }
        if (!periods.isOpen(lop.maHocKy(), lop.maCoSoHost())) {
            return HostDecision.tuChoi("ENROLLMENT_PERIOD_CLOSED",
                    "Cơ sở %s không có đợt đăng ký đang mở.".formatted(lop.maCoSoHost()));
        }
        if (!classes.reserveSeat(maLopHP)) {
            return HostDecision.tuChoi("CLASS_FULL",
                    "Lớp %s đã đủ %d sinh viên.".formatted(maLopHP, lop.soLuongToiDa()));
        }
        return HostDecision.duyet();
    }

    private void acquire(String maYeuCau) {
        if (!requests.acquireRequestLock(maYeuCau, LOCK_TIMEOUT_MS)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CROSS_CAMPUS_BUSY",
                    "Yêu cầu này đang được xử lý. Vui lòng thử lại.");
        }
    }
}
