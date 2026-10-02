package vn.ptit.one.course.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.course.model.CourseDetail;
import vn.ptit.one.course.model.CourseSummary;
import vn.ptit.one.course.model.Faculty;
import vn.ptit.one.course.model.Term;
import vn.ptit.one.course.repository.CourseRepository;
import vn.ptit.one.shared.exception.ApiException;

/** Danh mục môn học và quan hệ tiên quyết. Đọc mở cho mọi vai trò; ghi chỉ Admin Master (B3). */
@Service
@Profile("central")
public class CourseService {

    /** Chờ khoá đồ thị tối đa bấy nhiêu; quá thì báo bận thay vì treo request. */
    private static final int LOCK_TIMEOUT_MS = 5_000;

    private final CourseRepository courses;

    public CourseService(CourseRepository courses) {
        this.courses = courses;
    }

    public List<CourseSummary> search(String maKhoa, String tuKhoa) {
        return courses.search(maKhoa, tuKhoa);
    }

    public List<Faculty> faculties() {
        return courses.findFaculties();
    }

    public List<Term> terms() {
        return courses.findTerms();
    }

    public CourseDetail detail(String maMonHoc) {
        CourseSummary mon = courses.findOne(maMonHoc).orElseThrow(() -> notFound(maMonHoc));
        return new CourseDetail(mon, courses.findPrerequisites(maMonHoc), courses.findDependents(maMonHoc));
    }

    @Transactional
    public CourseDetail create(String maMonHoc, String tenMonHoc, int soTinChi, String maKhoa) {
        String ma = maMonHoc.trim();
        requireFaculty(maKhoa);
        try {
            courses.insert(ma, tenMonHoc.trim(), soTinChi, maKhoa.trim());
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_DUPLICATE",
                    "Mã môn học %s đã tồn tại.".formatted(ma));
        }
        return detail(ma);
    }

    @Transactional
    public CourseDetail update(String maMonHoc, String tenMonHoc, int soTinChi, String maKhoa) {
        requireFaculty(maKhoa);
        if (courses.update(maMonHoc, tenMonHoc.trim(), soTinChi, maKhoa.trim()) != 1) {
            throw notFound(maMonHoc);
        }
        return detail(maMonHoc);
    }

    /**
     * Thay TOÀN BỘ tập tiên quyết của một môn, trong một transaction.
     *
     * <p>Thứ tự bắt buộc: khoá → kiểm → xoá tập cũ → kiểm chu trình → ghi tập mới.
     * Kiểm trước khi khoá là vô nghĩa: hai request cùng vượt qua bước kiểm rồi
     * mới ghi thì chu trình vẫn hình thành. Lỗi ở bất kỳ bước nào cũng rollback,
     * nên tập cũ được giữ nguyên.
     */
    @Transactional
    public CourseDetail replacePrerequisites(String maMonHoc, List<String> tienQuyet) {
        if (!courses.exists(maMonHoc)) {
            throw notFound(maMonHoc);
        }
        if (!courses.acquirePrerequisiteLock(LOCK_TIMEOUT_MS)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_BUSY",
                    "Danh mục đang được người khác sửa. Vui lòng thử lại.");
        }

        /* Khoá đồ thị không che được việc sinh viên đang đăng ký: đổi tiên quyết
           giữa đợt khiến hai người nộp cùng lúc bị xét theo hai bộ quy tắc. */
        if (courses.hasOpenRegistration(maMonHoc)) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_REGISTRATION_OPEN",
                    "Môn %s đang có đợt đăng ký mở. Đóng đợt trước khi đổi môn tiên quyết."
                            .formatted(maMonHoc));
        }

        Set<String> canDat = normalise(maMonHoc, tienQuyet);

        for (String tq : canDat) {
            if (!courses.exists(tq)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PREREQUISITE_UNKNOWN",
                        "Không có môn học %s.".formatted(tq));
            }
        }

        // Xoá trước rồi mới xét: đồ thị lúc này không còn cạnh cũ của chính môn đó.
        courses.deletePrerequisites(maMonHoc);

        for (String tq : canDat) {
            /* Thêm cạnh maMonHoc → tq tạo chu trình khi và chỉ khi từ tq đi
               theo chuỗi tiên quyết quay về được maMonHoc. */
            if (courses.reaches(tq, maMonHoc)) {
                throw new ApiException(HttpStatus.CONFLICT, "PREREQUISITE_CYCLE",
                        "Đặt %s làm tiên quyết của %s tạo thành chu trình.".formatted(tq, maMonHoc));
            }
            courses.insertPrerequisite(maMonHoc, tq);
        }

        return detail(maMonHoc);
    }

    /** Bỏ trùng, giữ thứ tự nhập, chặn môn tự làm tiên quyết của chính nó. */
    private static Set<String> normalise(String maMonHoc, List<String> tienQuyet) {
        Set<String> ket = new LinkedHashSet<>();
        for (String raw : tienQuyet == null ? List.<String>of() : tienQuyet) {
            if (raw == null || raw.isBlank()) continue;
            String ma = raw.trim();
            if (ma.equalsIgnoreCase(maMonHoc)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PREREQUISITE_SELF",
                        "Một môn không thể là tiên quyết của chính nó.");
            }
            ket.add(ma);
        }
        return ket;
    }

    private void requireFaculty(String maKhoa) {
        if (maKhoa == null || maKhoa.isBlank() || !courses.facultyExists(maKhoa.trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FACULTY_UNKNOWN",
                    "Không có khoa %s.".formatted(maKhoa));
        }
    }

    private static ApiException notFound(String maMonHoc) {
        return new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND",
                "Không tìm thấy môn học %s.".formatted(maMonHoc));
    }
}
