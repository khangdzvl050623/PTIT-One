package vn.ptit.one.course.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.course.model.Faculty;
import vn.ptit.one.course.model.Term;
import vn.ptit.one.course.service.CourseService;

/**
 * Danh mục tra cứu dùng chung: khoa và học kỳ.
 *
 * <p>Chỉ đọc. Ghi hai bảng này là việc của Admin Master và chưa có màn quản trị
 * nào cần tới, nên không mở endpoint ghi cho tới khi thực sự dùng.
 */
@RestController
@RequestMapping("/api/catalog")
@Profile("central")
public class CatalogController {

    private final CourseService courses;

    public CatalogController(CourseService courses) {
        this.courses = courses;
    }

    @GetMapping("/khoa")
    public List<Faculty> faculties() {
        return courses.faculties();
    }

    @GetMapping("/hoc-ky")
    public List<Term> terms() {
        return courses.terms();
    }
}
