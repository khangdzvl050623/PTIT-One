package vn.ptit.one.course.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import vn.ptit.one.course.dto.CreateCourseRequest;
import vn.ptit.one.course.dto.SetPrerequisitesRequest;
import vn.ptit.one.course.dto.UpdateCourseRequest;
import vn.ptit.one.course.model.CourseDetail;
import vn.ptit.one.course.model.CourseSummary;
import vn.ptit.one.course.service.CourseService;

/**
 * Danh mục môn học và quan hệ tiên quyết (F03).
 *
 * <p>Quyền theo B3: mọi vai trò ĐỌC được danh mục; chỉ {@code ADMIN_MASTER}
 * ghi — "`MonHoc`, `Khoa`, `ChuongTrinhDaoTao`, `HocKy`: R/W chỉ tại Master".
 * Ở Phần 2 các bảng này được nhân bản một chiều từ Master nên site không ghi
 * được; giới hạn ở đây khớp sẵn với điều đó.
 *
 * <p>Trả thẳng record của {@code model}: chúng đã đúng hình dạng API và không
 * mang trường nội bộ nào cần giấu. Khác {@code auth}, nơi principal có
 * {@code sessionId}/{@code accountVersion} nên bắt buộc phải có DTO riêng.
 */
@RestController
@RequestMapping("/api/courses")
@Profile("central")
public class CourseController {

    private final CourseService courses;

    public CourseController(CourseService courses) {
        this.courses = courses;
    }

    @GetMapping
    public List<CourseSummary> search(
            @RequestParam(required = false) String maKhoa,
            @RequestParam(required = false) String q) {
        return courses.search(maKhoa, q);
    }

    @GetMapping("/{maMonHoc}")
    public CourseDetail detail(@PathVariable String maMonHoc) {
        return courses.detail(maMonHoc);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public CourseDetail create(@Valid @RequestBody CreateCourseRequest body) {
        return courses.create(body.maMonHoc(), body.tenMonHoc(), body.soTinChi(), body.maKhoa());
    }

    @PutMapping("/{maMonHoc}")
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public CourseDetail update(@PathVariable String maMonHoc, @Valid @RequestBody UpdateCourseRequest body) {
        return courses.update(maMonHoc, body.tenMonHoc(), body.soTinChi(), body.maKhoa());
    }

    /** Thay toàn bộ tập tiên quyết. Danh sách rỗng = gỡ hết. */
    @PutMapping("/{maMonHoc}/tien-quyet")
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public CourseDetail setPrerequisites(@PathVariable String maMonHoc,
            @Valid @RequestBody SetPrerequisitesRequest body) {
        return courses.replacePrerequisites(maMonHoc, body.tienQuyet());
    }
}
