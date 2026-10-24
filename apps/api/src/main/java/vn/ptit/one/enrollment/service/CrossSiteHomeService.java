package vn.ptit.one.enrollment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.course.service.CourseService;
import vn.ptit.one.enrollment.model.CrossSiteRequest;
import vn.ptit.one.enrollment.model.EnrollmentStatus;
import vn.ptit.one.enrollment.model.HostDecision;
import vn.ptit.one.enrollment.repository.CrossSiteRepository;
import vn.ptit.one.enrollment.repository.EnrollmentRepository;
import vn.ptit.one.grade.policy.GradePolicy;
import vn.ptit.one.grade.service.GradeRecords;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.student.model.StudentProfile;
import vn.ptit.one.student.service.StudentDirectory;
import vn.ptit.one.timetable.model.ScheduleSlot;
import vn.ptit.one.timetable.service.ScheduleService;

/**
 * Bước Home của saga: ghi nhận yêu cầu, rồi áp kết quả Host trả về.
 *
 * <p>Home bảo vệ ràng buộc theo <b>sinh viên</b>: trần tín chỉ theo kỳ, trùng
 * môn, trùng lịch, tiên quyết.
 */
@Service
@Profile("central")
@EnableConfigurationProperties(EnrollmentProperties.class)
public class CrossSiteHomeService {

    private static final int LOCK_TIMEOUT_MS = 5_000;

    private final CrossSiteRepository crossSite;
    private final EnrollmentRepository enrollments;
    private final ScheduleService schedules;
    private final CourseService courses;
    private final GradeRecords grades;
    private final StudentDirectory students;
    private final EnrollmentProperties properties;
    private final ObjectMapper json;
    private final Clock clock;

    public CrossSiteHomeService(CrossSiteRepository crossSite, EnrollmentRepository enrollments,
            ScheduleService schedules, CourseService courses, GradeRecords grades,
            StudentDirectory students, EnrollmentProperties properties, ObjectMapper json,
            Clock clock) {
        this.crossSite = crossSite;
        this.enrollments = enrollments;
        this.schedules = schedules;
        this.courses = courses;
        this.grades = grades;
        this.students = students;
        this.properties = properties;
        this.json = json;
        this.clock = clock;
    }

    /** Dữ liệu Home cần gửi kèm khi gọi sang Host. */
    public record Pending(String maYeuCau, String maLopHP, String maSinhVien, String maCoSoNhaSV,
            String hoTen, int phienBanLich) {
    }

    /**
     * Ghi nhận yêu cầu ở trạng thái {@code DANG_XU_LY} và giữ chỗ tại Home.
     *
     * <p>Khoá theo (sinh viên, học kỳ) lấy TRƯỚC mọi phép kiểm — khoá lúc cộng
     * bộ đếm là quá muộn, hai request đã cùng vượt qua bước kiểm rồi.
     */
    @Transactional
    public Pending open(String maSinhVien, ClassSection lop, List<ScheduleSlot> lichHoc) {
        StudentProfile sv = students.require(maSinhVien);
        String maHocKy = lop.maHocKy();
        if (!enrollments.acquireStudentTermLock(sv.maSinhVien(), maHocKy, LOCK_TIMEOUT_MS)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ENROLLMENT_BUSY",
                    "Đang xử lý một yêu cầu đăng ký khác của bạn. Vui lòng thử lại.");
        }

        if (!sv.dangHoc()) {
            throw conflict("STUDENT_NOT_ACTIVE",
                    "Sinh viên đang ở trạng thái %s.".formatted(sv.trangThai()));
        }
        if (lop.maCoSoHost().equals(sv.maCoSoNha())) {
            throw conflict("CROSS_CAMPUS_SAME_SITE",
                    "Lớp %s thuộc chính cơ sở của bạn — dùng đăng ký thường.".formatted(lop.maLopHP()));
        }
        if (!ClassSectionService.TRUC_TUYEN.equals(lop.hinhThucHoc())) {
            throw conflict("CROSS_CAMPUS_REQUIRES_ONLINE",
                    "Chỉ lớp trực tuyến mới đăng ký liên cơ sở được.");
        }
        if (!courses.isInProgram(sv.maCTDT(), lop.maMonHoc())) {
            throw conflict("COURSE_NOT_IN_PROGRAM",
                    "Môn %s không thuộc chương trình đào tạo %s.".formatted(lop.maMonHoc(), sv.maCTDT()));
        }
        if (enrollments.activeClassOfCourse(sv.maSinhVien(), maHocKy, lop.maMonHoc()).isPresent()) {
            throw conflict("ENROLLMENT_DUPLICATE_COURSE",
                    "Bạn đã giữ chỗ môn %s trong học kỳ này.".formatted(lop.maMonHoc()));
        }

        Map<String, String> ketQua = grades.bestResults(sv.maSinhVien());
        List<String> chuaDat = courses.prerequisitesOf(lop.maMonHoc()).stream()
                .filter(tq -> !GradePolicy.DAT.equals(ketQua.get(tq)))
                .toList();
        if (!chuaDat.isEmpty()) {
            throw conflict("PREREQUISITE_NOT_MET",
                    "Chưa đạt môn tiên quyết: %s.".formatted(String.join(", ", chuaDat)));
        }

        /* Lịch lớp tại chỗ và lịch mirror BÙ nhau, không chồng nhau: mirror chỉ
           có dòng cho lớp của site khác. Gộp rồi so là đủ. */
        List<ScheduleSlot> dangGiu = new ArrayList<>(
                schedules.slotsOfClasses(enrollments.activeClasses(sv.maSinhVien(), maHocKy)));
        dangGiu.addAll(crossSite.mirrorSlots(sv.maSinhVien(), maHocKy));
        if (ScheduleService.firstOverlap(lichHoc, dangGiu).isPresent()) {
            throw conflict("SCHEDULE_CLASH",
                    "Lớp %s trùng lịch với lớp bạn đã giữ chỗ.".formatted(lop.maLopHP()));
        }

        enrollments.ensureTermRow(sv.maSinhVien(), maHocKy, properties.tranTinChi());
        if (crossSite.holdCredits(sv.maSinhVien(), maHocKy, lop.soTinChi()) != 1) {
            throw conflict("CREDIT_LIMIT_EXCEEDED",
                    "Thêm %d tín chỉ của %s sẽ vượt trần học kỳ."
                            .formatted(lop.soTinChi(), lop.maMonHoc()));
        }

        String maYeuCau = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        crossSite.insertRequest(new CrossSiteRequest(maYeuCau, sv.maSinhVien(), lop.maLopHP(),
                lop.maCoSoHost(), EnrollmentStatus.DANG_XU_LY, null, maHocKy, lop.maMonHoc(),
                lop.tenMonHoc(), lop.soTinChi(), lop.hinhThucHoc(), phongHoc(lichHoc),
                lop.phienBanLich(), lichHoc, now), toJson(lichHoc), now);
        crossSite.insertPendingCourse(sv.maSinhVien(), maHocKy, lop.maMonHoc(), lop.maLopHP(),
                lop.maCoSoHost(), lop.soTinChi(), lop.phienBanLich(), maYeuCau,
                EnrollmentStatus.DANG_XU_LY);
        for (ScheduleSlot slot : lichHoc) {
            crossSite.insertMirrorSlot(sv.maSinhVien(), maHocKy, lop.maMonHoc(), slot,
                    lop.phienBanLich());
        }

        return new Pending(maYeuCau, lop.maLopHP(), sv.maSinhVien(), sv.maCoSoNha(), sv.hoTen(),
                lop.phienBanLich());
    }

    /**
     * Áp kết quả Host trả về.
     *
     * <p>Mệnh đề {@code AND TrangThai = DANG_XU_LY} chặn phản hồi đến muộn ghi
     * đè {@code DANG_HUY}, và canh cho bộ đếm tín chỉ chỉ bị đụng đúng một lần.
     */
    @Transactional
    public void applyDecision(String maYeuCau, HostDecision quyetDinh) {
        CrossSiteRequest yc = require(maYeuCau);
        String den = quyetDinh.duocDuyet() ? EnrollmentStatus.DA_DANG_KY : EnrollmentStatus.TU_CHOI;
        if (crossSite.moveRequest(maYeuCau, EnrollmentStatus.DANG_XU_LY, den,
                quyetDinh.lyDo()) != 1) {
            return;      // đã có kết luận khác (sinh viên bấm huỷ trước) — bỏ qua
        }
        crossSite.moveCourse(maYeuCau, EnrollmentStatus.DANG_XU_LY, den);
        if (quyetDinh.duocDuyet()) {
            crossSite.heldToUsed(yc.maSinhVien(), yc.maHocKy(), yc.soTinChi());
            crossSite.bumpCrossSiteCount(yc.maSinhVien(), 1);
        } else {
            crossSite.releaseHeld(yc.maSinhVien(), yc.maHocKy(), yc.soTinChi());
            crossSite.deleteMirrorSlots(yc.maSinhVien(), yc.maHocKy(), yc.maMonHoc());
        }
    }

    /**
     * Sinh viên bấm huỷ: ghi nhận ý định, <b>chưa</b> trả tín chỉ.
     *
     * <p>Hạn mức chỉ hoàn khi Host xác nhận. Trả sớm tạo cửa sổ hai bên bất
     * đồng: Home tưởng đã huỷ, Host vẫn giữ chỗ.
     */
    @Transactional
    public Pending openCancel(String maSinhVien, String maYeuCau) {
        CrossSiteRequest yc = require(maYeuCau);
        if (!yc.maSinhVien().equals(maSinhVien)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CROSS_CAMPUS_REQUEST_NOT_FOUND",
                    "Không tìm thấy yêu cầu %s.".formatted(maYeuCau));
        }
        if (yc.daKetThuc()) {
            throw conflict("CROSS_CAMPUS_ALREADY_CLOSED",
                    "Yêu cầu đã ở trạng thái %s.".formatted(yc.trangThai()));
        }
        if (!enrollments.acquireStudentTermLock(maSinhVien, yc.maHocKy(), LOCK_TIMEOUT_MS)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ENROLLMENT_BUSY",
                    "Đang xử lý một yêu cầu khác của bạn. Vui lòng thử lại.");
        }

        boolean daDuyet = EnrollmentStatus.DA_DANG_KY.equals(yc.trangThai());
        if (crossSite.moveRequest(maYeuCau, yc.trangThai(), EnrollmentStatus.DANG_HUY, null) != 1) {
            throw conflict("CROSS_CAMPUS_ALREADY_CLOSED", "Yêu cầu vừa đổi trạng thái.");
        }
        crossSite.moveCourse(maYeuCau, yc.trangThai(), EnrollmentStatus.DANG_HUY);
        if (daDuyet) {
            /* Tổng hạn mức chiếm KHÔNG đổi: chỉ chuyển từ "đã dùng" về "đang giữ". */
            crossSite.usedToHeld(yc.maSinhVien(), yc.maHocKy(), yc.soTinChi());
            crossSite.bumpCrossSiteCount(yc.maSinhVien(), -1);
        }
        return new Pending(maYeuCau, yc.maLopHP(), yc.maSinhVien(), null, null, yc.phienBanLich());
    }

    /** Host đã xác nhận huỷ — giờ mới trả hạn mức. */
    @Transactional
    public void applyCancel(String maYeuCau) {
        CrossSiteRequest yc = require(maYeuCau);
        if (crossSite.moveRequest(maYeuCau, EnrollmentStatus.DANG_HUY,
                EnrollmentStatus.DA_HUY, null) != 1) {
            return;
        }
        crossSite.moveCourse(maYeuCau, EnrollmentStatus.DANG_HUY, EnrollmentStatus.DA_HUY);
        crossSite.releaseHeld(yc.maSinhVien(), yc.maHocKy(), yc.soTinChi());
        crossSite.deleteMirrorSlots(yc.maSinhVien(), yc.maHocKy(), yc.maMonHoc());
    }

    public List<CrossSiteRequest> list(String maSinhVien, String maHocKy) {
        return crossSite.listRequests(maSinhVien, maHocKy).stream()
                .map(this::withSchedule)
                .toList();
    }

    public CrossSiteRequest read(String maYeuCau) {
        return withSchedule(require(maYeuCau));
    }

    private CrossSiteRequest require(String maYeuCau) {
        return crossSite.findRequest(maYeuCau)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "CROSS_CAMPUS_REQUEST_NOT_FOUND",
                        "Không tìm thấy yêu cầu %s.".formatted(maYeuCau)));
    }

    private CrossSiteRequest withSchedule(CrossSiteRequest yc) {
        List<ScheduleSlot> lich = crossSite.scheduleJson(yc.maYeuCau())
                .map(this::fromJson).orElse(List.of());
        return new CrossSiteRequest(yc.maYeuCau(), yc.maSinhVien(), yc.maLopHP(), yc.maCoSoHost(),
                yc.trangThai(), yc.lyDoTuChoi(), yc.maHocKy(), yc.maMonHoc(), yc.tenMonHoc(),
                yc.soTinChi(), yc.hinhThucHoc(), yc.phongHoc(), yc.phienBanLich(), lich,
                yc.thoiDiemDongBo());
    }

    private static String phongHoc(List<ScheduleSlot> lichHoc) {
        return lichHoc.stream().map(ScheduleSlot::phongHoc).filter(p -> p != null && !p.isBlank())
                .findFirst().orElse(null);
    }

    private String toJson(List<ScheduleSlot> lichHoc) {
        try {
            return json.writeValueAsString(lichHoc);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Không ghi được lịch lớp vào yêu cầu.", ex);
        }
    }

    private List<ScheduleSlot> fromJson(String raw) {
        try {
            return List.of(json.readValue(raw, ScheduleSlot[].class));
        } catch (JacksonException ex) {
            return List.of();
        }
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    /** Dùng cho worker/retry: yêu cầu còn chờ Host trả lời. */
    public Optional<CrossSiteRequest> pending(String maYeuCau) {
        return crossSite.findRequest(maYeuCau).filter(CrossSiteRequest::dangGiuTinChi);
    }
}
