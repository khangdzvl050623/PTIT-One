package vn.ptit.one.course.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;

/**
 * Lớp giảng viên phụ trách (F05).
 *
 * <p>Tách khỏi {@code GET /api/classes?maGiangVien=}: đường đó lọc theo tham số
 * client gửi, còn đây lấy giảng viên từ principal đã ký.
 */
@RestController
@RequestMapping("/api/me")
@Profile("central")
public class TeachingController {

    private final ClassSectionService classes;

    public TeachingController(ClassSectionService classes) {
        this.classes = classes;
    }

    @GetMapping("/teaching-classes")
    public List<ClassSection> teachingClasses(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String maHocKy) {
        return classes.taughtBy(user, maHocKy);
    }
}
