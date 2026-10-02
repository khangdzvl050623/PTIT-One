package vn.ptit.one.grade.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.grade.dto.SaveGradesRequest;
import vn.ptit.one.grade.model.GradeEntry;
import vn.ptit.one.grade.model.GradeSheet;
import vn.ptit.one.grade.policy.GradePolicy;
import vn.ptit.one.grade.repository.GradeRepository;
import vn.ptit.one.grade.repository.GradeRepository.ClassGradeRow;
import vn.ptit.one.shared.exception.ApiException;

/**
 * Nhập, công bố và khoá điểm của một lớp (F06).
 *
 * <p>Quyết định nhóm 02/10/2026: giảng viên phụ trách nhập nháp rồi công bố;
 * đã công bố vẫn sửa được (sinh viên thấy ngay) cho tới khi Admin cơ sở khoá.
 * Khoá rồi thì không ai sửa — mở khoá/cải chính ngoài bản basic.
 *
 * <p>Mọi thao tác ghi lấy khoá theo lớp TRƯỚC, rồi mới đọc trạng thái lớp.
 * Đọc trạng thái trước khi khoá là quá muộn: admin khoá xen giữa thì điểm vẫn
 * lọt vào lớp đã khoá.
 */
@Service
@Profile("central")
public class GradeBookService {

    private static final int LOCK_TIMEOUT_MS = 5_000;

    private final GradeRepository grades;
    private final ClassSectionService classes;
    private final GradePolicy policy;
    private final Clock clock;

    public GradeBookService(GradeRepository grades, ClassSectionService classes,
            GradePolicy policy, Clock clock) {
        this.grades = grades;
        this.classes = classes;
        this.policy = policy;
        this.clock = clock;
    }

    public GradeSheet sheet(AuthenticatedUser user, String maLopHP) {
        return sheetOf(classes.requireStaffAccess(user, maLopHP));
    }

    /**
     * Lưu một loạt dòng điểm trong MỘT giao dịch. Tổng kết do server tính,
     * client không gửi. Một dòng lỗi (sai phiên bản, sinh viên không thuộc lớp)
     * thì cả loạt rollback.
     */
    @Transactional
    public GradeSheet save(AuthenticatedUser user, String maLopHP, List<SaveGradesRequest.Row> rows) {
        requireTeacherOf(user, maLopHP);
        ClassSection lop = lockAndRequireOpen(maLopHP);
        requireNoDuplicates(rows);

        for (SaveGradesRequest.Row row : rows) {
            String maSinhVien = row.maSinhVien().trim();
            BigDecimal tongKet = policy.tongKet(row.diemChuyenCan(), row.diemGiuaKy(), row.diemCuoiKy());
            int updated = grades.updateScores(maLopHP, maSinhVien, row.diemChuyenCan(),
                    row.diemGiuaKy(), row.diemCuoiKy(), tongKet, row.version());
            if (updated == 0) {
                throw grades.exists(maLopHP, maSinhVien)
                        ? new ApiException(HttpStatus.CONFLICT, "GRADE_VERSION_CONFLICT",
                                "Điểm của %s vừa được người khác sửa. Tải lại bảng điểm rồi nhập lại."
                                        .formatted(maSinhVien))
                        : new ApiException(HttpStatus.BAD_REQUEST, "GRADE_STUDENT_NOT_ENROLLED",
                                "Sinh viên %s không có trong lớp %s.".formatted(maSinhVien, maLopHP));
            }
        }
        return sheetOf(lop);
    }

    /**
     * Công bố mọi dòng còn nháp. Chặn khi còn sinh viên thiếu điểm thành phần:
     * công bố một bảng điểm dở dang là công bố "chưa có điểm" như kết quả.
     */
    @Transactional
    public GradeSheet publish(AuthenticatedUser user, String maLopHP) {
        requireTeacherOf(user, maLopHP);
        ClassSection lop = lockAndRequireOpen(maLopHP);

        List<String> thieu = incomplete(grades.findByClass(maLopHP));
        if (!thieu.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "GRADE_INCOMPLETE",
                    "Còn %d sinh viên chưa đủ điểm thành phần: %s."
                            .formatted(thieu.size(), String.join(", ", thieu)));
        }
        grades.publish(maLopHP, clock.instant());
        return sheetOf(lop);
    }

    /** Khoá điểm. Chỉ Admin cơ sở của lớp; gọi lại khi đã khoá là không làm gì. */
    @Transactional
    public GradeSheet lock(AuthenticatedUser user, String maLopHP) {
        classes.requireManageableBy(user, maLopHP);
        acquire(maLopHP);
        ClassSection lop = classes.require(maLopHP);
        if (ClassSectionService.DA_KHOA.equals(lop.trangThai())) {
            return sheetOf(lop);
        }
        requireOpen(lop);

        List<ClassGradeRow> rows = grades.findByClass(maLopHP);
        boolean conNhap = rows.stream().anyMatch(row -> row.ngayCongBo() == null);
        if (conNhap || !incomplete(rows).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "GRADE_NOT_PUBLISHED",
                    "Lớp %s còn điểm chưa công bố, chưa khoá được.".formatted(maLopHP));
        }
        if (!classes.lockGrades(maLopHP)) {
            throw notOpen(classes.require(maLopHP));
        }
        return sheetOf(classes.require(maLopHP));
    }

    // --- Nội bộ ----------------------------------------------------------

    /** Chỉ giảng viên ĐANG phụ trách lớp được nhập và công bố — admin không nhập thay. */
    private void requireTeacherOf(AuthenticatedUser user, String maLopHP) {
        ClassSection lop = classes.require(maLopHP);
        if (user.role() != Role.GIANG_VIEN || user.entityId() == null
                || !user.entityId().equals(lop.maGiangVien())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ giảng viên phụ trách lớp %s mới nhập được điểm.".formatted(maLopHP));
        }
    }

    /** Khoá trước, đọc trạng thái lớp SAU khi đã giữ khoá. */
    private ClassSection lockAndRequireOpen(String maLopHP) {
        acquire(maLopHP);
        ClassSection lop = classes.require(maLopHP);
        requireOpen(lop);
        return lop;
    }

    private void acquire(String maLopHP) {
        if (!grades.acquireClassLock(maLopHP, LOCK_TIMEOUT_MS)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GRADE_BUSY",
                    "Bảng điểm lớp %s đang được người khác ghi. Vui lòng thử lại.".formatted(maLopHP));
        }
    }

    private static void requireOpen(ClassSection lop) {
        if (!ClassSectionService.MO.equals(lop.trangThai())) {
            throw notOpen(lop);
        }
    }

    private static ApiException notOpen(ClassSection lop) {
        if (ClassSectionService.DA_KHOA.equals(lop.trangThai())) {
            return new ApiException(HttpStatus.CONFLICT, "GRADE_LOCKED",
                    "Lớp %s đã khoá điểm, không sửa được.".formatted(lop.maLopHP()));
        }
        return new ApiException(HttpStatus.CONFLICT, "GRADE_CLASS_NOT_OPEN",
                "Lớp %s đang ở trạng thái %s, không nhập điểm được."
                        .formatted(lop.maLopHP(), lop.trangThai()));
    }

    private static void requireNoDuplicates(List<SaveGradesRequest.Row> rows) {
        Set<String> seen = new HashSet<>();
        for (SaveGradesRequest.Row row : rows) {
            if (!seen.add(row.maSinhVien().trim())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "Sinh viên %s xuất hiện hai lần trong cùng một lần lưu.".formatted(row.maSinhVien()));
            }
        }
    }

    private static List<String> incomplete(List<ClassGradeRow> rows) {
        return rows.stream().filter(row -> row.diemTongKet() == null).map(ClassGradeRow::maSinhVien).toList();
    }

    private GradeSheet sheetOf(ClassSection lop) {
        List<ClassGradeRow> rows = grades.findByClass(lop.maLopHP());
        List<GradeEntry> diem = rows.stream()
                .map(row -> new GradeEntry(row.maSinhVien(), row.hoTen(), row.diemChuyenCan(),
                        row.diemGiuaKy(), row.diemCuoiKy(), row.diemTongKet(),
                        policy.ketQua(row.diemTongKet()), row.version(), row.ngayCongBo()))
                .toList();

        String trangThai;
        if (ClassSectionService.DA_KHOA.equals(lop.trangThai())) {
            trangThai = GradeSheet.DA_KHOA;
        } else if (!rows.isEmpty() && rows.stream().allMatch(row -> row.ngayCongBo() != null)) {
            trangThai = GradeSheet.DA_CONG_BO;
        } else {
            trangThai = GradeSheet.NHAP;
        }
        return new GradeSheet(lop, trangThai, diem);
    }
}
