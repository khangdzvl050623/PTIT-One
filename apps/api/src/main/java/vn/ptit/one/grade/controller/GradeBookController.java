package vn.ptit.one.grade.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.grade.dto.SaveGradesRequest;
import vn.ptit.one.grade.model.GradeSheet;
import vn.ptit.one.grade.service.GradeBookService;

/**
 * Bảng điểm của lớp (F06). Xem: GV phụ trách, Admin cơ sở, Admin Master.
 * Lưu và công bố: chỉ GV phụ trách. Khoá: chỉ Admin cơ sở của lớp.
 */
@RestController
@RequestMapping("/api/classes/{maLopHP}/grades")
@Profile("central")
public class GradeBookController {

    private final GradeBookService gradeBook;

    public GradeBookController(GradeBookService gradeBook) {
        this.gradeBook = gradeBook;
    }

    @GetMapping
    public GradeSheet sheet(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String maLopHP) {
        return gradeBook.sheet(user, maLopHP);
    }

    @PutMapping
    public GradeSheet save(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String maLopHP,
            @Valid @RequestBody SaveGradesRequest body) {
        return gradeBook.save(user, maLopHP, body.diem());
    }

    @PostMapping("/publish")
    public GradeSheet publish(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String maLopHP) {
        return gradeBook.publish(user, maLopHP);
    }

    @PostMapping("/lock")
    public GradeSheet lock(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String maLopHP) {
        return gradeBook.lock(user, maLopHP);
    }
}
