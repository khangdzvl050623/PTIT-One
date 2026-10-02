package vn.ptit.one.course.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.course.model.Faculty;
import vn.ptit.one.course.model.ProgramDetail;
import vn.ptit.one.course.model.StudyProgram;
import vn.ptit.one.course.model.Term;
import vn.ptit.one.course.service.CourseService;

/**
 * Danh mục tra cứu dùng chung: khoa ({@code /api/faculties}), học kỳ
 * ({@code /api/terms}) và chương trình đào tạo ({@code /api/programs}).
 *
 * <p>Chỉ đọc. Ghi các bảng này là việc của Admin Master và chưa có màn quản trị
 * nào cần tới, nên không mở endpoint ghi cho tới khi thực sự dùng.
 */
@RestController
@RequestMapping("/api")
@Profile("central")
public class CatalogController {

    private final CourseService courses;

    public CatalogController(CourseService courses) {
        this.courses = courses;
    }

    @GetMapping("/faculties")
    public List<Faculty> faculties() {
        return courses.faculties();
    }

    @GetMapping("/terms")
    public List<Term> terms() {
        return courses.terms();
    }

    @GetMapping("/programs")
    public List<StudyProgram> programs() {
        return courses.programs();
    }

    @GetMapping("/programs/{maCTDT}")
    public ProgramDetail program(@PathVariable String maCTDT) {
        return courses.program(maCTDT);
    }
}
