package vn.ptit.one.course.service;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.repository.ClassSectionRepository;
import vn.ptit.one.course.repository.CourseRepository;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.teacher.service.TeacherDirectory;

/**
 * Lớp học phần (F04).
 *
 * <p>Quyền theo B3: {@code ADMIN_CO_SO} đọc/ghi trong cơ sở mình;
 * {@code ADMIN_MASTER} chỉ đọc mọi cơ sở; SV/GV chỉ đọc. Cơ sở LUÔN lấy từ
 * principal đã ký, không bao giờ từ tham số client.
 */
@Service
@Profile("central")
public class ClassSectionService {

    /** Thử lại khi hai admin cùng tạo lớp và đua số thứ tự. */
    private static final int MAX_CODE_ATTEMPTS = 3;

    private static final List<String> HINH_THUC_HOC = List.of("TRUC_TIEP", "TRUC_TUYEN", "KET_HOP");
    public static final String MO = "MO";
    public static final String DA_KHOA = "DA_KHOA";
    private static final List<String> TRANG_THAI = List.of("DU_KIEN", MO, DA_KHOA, "DA_HUY");

    private final ClassSectionRepository classes;
    private final CourseRepository courses;
    private final TeacherDirectory teachers;

    public ClassSectionService(ClassSectionRepository classes, CourseRepository courses,
            TeacherDirectory teachers) {
        this.classes = classes;
        this.courses = courses;
        this.teachers = teachers;
    }

    public List<ClassSection> search(AuthenticatedUser user, String maHocKy, String maMonHoc,
            String maGiangVien) {
        /* Admin Master đọc được mọi cơ sở (B3). Các vai trò khác bị giới hạn
           trong cơ sở của chính mình — lấy từ principal, không nhận từ query. */
        String campusScope = user.role() == Role.ADMIN_MASTER ? null : user.homeCampus();
        return classes.search(maHocKy, maMonHoc, campusScope, maGiangVien);
    }

    public ClassSection detail(AuthenticatedUser user, String maLopHP) {
        ClassSection lop = require(maLopHP);
        requireReadable(user, lop);
        return lop;
    }

    /**
     * Lớp giảng viên đang phụ trách (F05). Giảng viên lấy từ principal, nên
     * không có tham số nào để xem lớp của người khác.
     */
    public List<ClassSection> taughtBy(AuthenticatedUser user, String maHocKy) {
        if (user.role() != Role.GIANG_VIEN || user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ giảng viên mới có danh sách lớp phụ trách.");
        }
        return classes.search(maHocKy, null, null, user.entityId());
    }

    /**
     * Lớp mà người dùng được xem dữ liệu nội bộ (danh sách sinh viên, bảng điểm):
     * giảng viên ĐANG phụ trách lớp, Admin cơ sở của lớp, hoặc Admin Master.
     *
     * <p>Sinh viên không qua được, kể cả sinh viên của lớp — danh sách lớp chứa
     * thông tin của người khác.
     */
    public ClassSection requireStaffAccess(AuthenticatedUser user, String maLopHP) {
        ClassSection lop = require(maLopHP);
        boolean allowed = switch (user.role()) {
            case GIANG_VIEN -> user.entityId() != null && user.entityId().equals(lop.maGiangVien());
            case ADMIN_CO_SO -> lop.maCoSoHost().equals(user.homeCampus());
            case ADMIN_MASTER -> true;
            case SINH_VIEN -> false;
        };
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Bạn không phụ trách lớp %s.".formatted(maLopHP));
        }
        return lop;
    }

    /**
     * Tạo lớp. Mã lớp do SERVER sinh từ môn + kỳ + cơ sở trong principal, nên
     * client không thể tạo lớp mang mã của cơ sở khác.
     */
    @Transactional
    public ClassSection create(AuthenticatedUser user, String maMonHoc, String maHocKy,
            int soLuongToiDa, String hinhThucHoc, boolean choPhepLienCoSo, String maGiangVien) {
        String campus = requireCampusAdmin(user);

        if (!courses.exists(maMonHoc)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "COURSE_NOT_FOUND",
                    "Không có môn học %s.".formatted(maMonHoc));
        }
        if (!courses.termExists(maHocKy)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TERM_NOT_FOUND",
                    "Không có học kỳ %s.".formatted(maHocKy));
        }
        validateMode(hinhThucHoc, choPhepLienCoSo);
        if (maGiangVien != null && !maGiangVien.isBlank()) {
            teachers.requireInCampus(maGiangVien.trim(), campus);
        }

        String prefix = "%s-%s-%s".formatted(maMonHoc, maHocKy, campus);
        for (int attempt = 1; ; attempt++) {
            String maLopHP = "%s%02d".formatted(prefix, classes.nextSequence(prefix));
            try {
                classes.insert(new ClassSection(maLopHP, maMonHoc, null, 0, maHocKy, campus,
                        emptyToNull(maGiangVien), null, soLuongToiDa, 0, "DU_KIEN",
                        choPhepLienCoSo, hinhThucHoc, 1));
                return require(maLopHP);
            } catch (DuplicateKeyException ex) {
                // Hai admin cùng tạo lớp cho một môn/kỳ: người sau lấy số kế tiếp.
                if (attempt == MAX_CODE_ATTEMPTS) {
                    throw new ApiException(HttpStatus.CONFLICT, "CLASS_CODE_RACE",
                            "Không cấp được mã lớp do có người tạo cùng lúc. Vui lòng thử lại.");
                }
            }
        }
    }

    @Transactional
    public ClassSection update(AuthenticatedUser user, String maLopHP, int soLuongToiDa,
            String trangThai, String hinhThucHoc, boolean choPhepLienCoSo) {
        ClassSection lop = require(maLopHP);
        requireManageable(user, lop);
        validateMode(hinhThucHoc, choPhepLienCoSo);
        if (!TRANG_THAI.contains(trangThai)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CLASS_STATUS_INVALID",
                    "Trạng thái lớp không hợp lệ: %s.".formatted(trangThai));
        }
        /* DA_KHOA nghĩa là đã khoá điểm. Chỉ đi vào qua luồng khoá điểm (kiểm đã
           công bố đủ), và không có đường ra — mở khoá/cải chính ngoài bản basic. */
        if (DA_KHOA.equals(lop.trangThai())) {
            throw new ApiException(HttpStatus.CONFLICT, "GRADE_LOCKED",
                    "Lớp %s đã khoá điểm, không sửa được.".formatted(maLopHP));
        }
        if (DA_KHOA.equals(trangThai)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CLASS_STATUS_INVALID",
                    "Khoá điểm bằng thao tác khoá bảng điểm, không đặt trạng thái trực tiếp.");
        }

        /* Điều kiện "không hạ dưới sĩ số" nằm trong chính câu UPDATE rồi đọc số
           dòng — không SELECT trước rồi IF, vì sĩ số đổi được giữa hai câu lệnh. */
        if (classes.update(maLopHP, soLuongToiDa, trangThai, choPhepLienCoSo, hinhThucHoc) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "CLASS_CAPACITY_BELOW_ENROLLED",
                    "Không hạ được sức chứa xuống %d: lớp đang có %d sinh viên."
                            .formatted(soLuongToiDa, lop.soLuongDaDangKy()));
        }
        return require(maLopHP);
    }

    /**
     * Phân công giảng viên.
     *
     * <p>Chặn khi giảng viên đã dạy lớp khác trùng khung giờ: một người không
     * thể ở hai chỗ cùng lúc. Lớp chưa có lịch thì không có gì để đụng.
     */
    @Transactional
    public ClassSection assignTeacher(AuthenticatedUser user, String maLopHP, String maGiangVien) {
        ClassSection lop = require(maLopHP);
        requireManageable(user, lop);

        if (maGiangVien == null || maGiangVien.isBlank()) {
            classes.assignTeacher(maLopHP, null);
            return require(maLopHP);
        }

        String teacher = maGiangVien.trim();
        teachers.requireInCampus(teacher, lop.maCoSoHost());

        List<String> clashes = classes.teacherClashes(teacher, maLopHP);
        if (!clashes.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "TEACHER_SCHEDULE_CLASH",
                    "Giảng viên %s đã có lịch trùng ở lớp %s.".formatted(teacher, String.join(", ", clashes)));
        }

        classes.assignTeacher(maLopHP, teacher);
        return require(maLopHP);
    }

    /**
     * Giữ một chỗ trong lớp đang mở. API cho {@code enrollment}; bộ đếm do
     * ứng dụng sở hữu, không trigger nào cộng thêm.
     *
     * @return {@code false} nếu lớp đã đầy hoặc không còn mở
     */
    public boolean reserveSeat(String maLopHP) {
        return classes.reserveSeat(maLopHP) == 1;
    }

    /** Trả lại một chỗ khi huỷ đăng ký. */
    public void releaseSeat(String maLopHP) {
        if (classes.releaseSeat(maLopHP) != 1) {
            throw new IllegalStateException(
                    "Bộ đếm sĩ số lớp %s đã về 0 trước khi trả chỗ.".formatted(maLopHP));
        }
    }

    /**
     * Chuyển lớp đang mở sang {@code DA_KHOA}. API cho module {@code grade}: chỗ
     * gọi đã kiểm quyền và kiểm điểm đã công bố đủ.
     *
     * @return {@code false} nếu lớp không còn ở {@code MO} lúc ghi
     */
    @Transactional
    public boolean lockGrades(String maLopHP) {
        return classes.lockGrades(maLopHP) == 1;
    }

    /** Dùng chung cho module `timetable` khi cần lớp đã kiểm quyền. */
    public ClassSection requireManageableBy(AuthenticatedUser user, String maLopHP) {
        ClassSection lop = require(maLopHP);
        requireManageable(user, lop);
        return lop;
    }

    public ClassSection require(String maLopHP) {
        return classes.findOne(maLopHP)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND",
                        "Không tìm thấy lớp học phần %s.".formatted(maLopHP)));
    }

    private static void validateMode(String hinhThucHoc, boolean choPhepLienCoSo) {
        if (!HINH_THUC_HOC.contains(hinhThucHoc)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CLASS_MODE_INVALID",
                    "Hình thức học không hợp lệ: %s.".formatted(hinhThucHoc));
        }
        /* Quyết định D18: v1 chỉ cho đăng ký liên cơ sở với lớp trực tuyến —
           kiểm "không trùng tiết" là vô nghĩa khi hai điểm cách nhau 1.700 km. */
        if (choPhepLienCoSo && !"TRUC_TUYEN".equals(hinhThucHoc)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CROSS_CAMPUS_REQUIRES_ONLINE",
                    "Chỉ lớp trực tuyến mới cho phép đăng ký liên cơ sở.");
        }
    }

    /** Chỉ Admin cơ sở được ghi, và chỉ trong cơ sở của mình (B3). */
    private static String requireCampusAdmin(AuthenticatedUser user) {
        if (user.role() != Role.ADMIN_CO_SO || user.homeCampus() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ quản trị đào tạo của cơ sở mới mở được lớp học phần.");
        }
        return user.homeCampus();
    }

    private static void requireManageable(AuthenticatedUser user, ClassSection lop) {
        String campus = requireCampusAdmin(user);
        if (!campus.equals(lop.maCoSoHost())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Lớp %s thuộc cơ sở khác.".formatted(lop.maLopHP()));
        }
    }

    private static void requireReadable(AuthenticatedUser user, ClassSection lop) {
        if (user.role() == Role.ADMIN_MASTER) {
            return;
        }
        if (user.homeCampus() != null && !user.homeCampus().equals(lop.maCoSoHost())
                && !lop.choPhepLienCoSo()) {
            /* Lớp liên cơ sở cố ý mở cho mọi cơ sở xem — đó là điều kiện để sinh
               viên nơi khác biết mà đăng ký. */
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Lớp %s thuộc cơ sở khác.".formatted(lop.maLopHP()));
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Chuẩn hoá phòng để so trùng: cắt khoảng trắng, bỏ phân biệt hoa thường. */
    public static String normaliseRoom(String phong) {
        return phong == null ? null : phong.trim().toUpperCase(Locale.ROOT);
    }
}
