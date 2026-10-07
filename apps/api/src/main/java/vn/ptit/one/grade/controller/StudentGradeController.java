package vn.ptit.one.grade.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.grade.model.StudentGrade;
import vn.ptit.one.grade.model.TranscriptTerm;
import vn.ptit.one.grade.service.StudentGradeService;

/** Bảng điểm của sinh viên đang đăng nhập (F07). Bỏ trống {@code maHocKy} là mọi học kỳ. */
@RestController
@RequestMapping("/api/me")
@Profile("central")
public class StudentGradeController {

    private final StudentGradeService grades;

    public StudentGradeController(StudentGradeService grades) {
        this.grades = grades;
    }

    @GetMapping("/grades")
    public List<StudentGrade> grades(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String maHocKy) {
        return grades.myGrades(user, maHocKy);
    }

    /**
     * Bảng điểm gom theo học kỳ, kèm trung bình kỳ và luỹ kế. Kỳ mới nhất trước.
     *
     * <p>Khác {@code /grades} ở chỗ đã tính sẵn số liệu — giao diện không phải
     * tự quy đổi thang 4 hay tự chọn lần điểm cao nhất.
     */
    @GetMapping("/transcript")
    public List<TranscriptTerm> transcript(@AuthenticationPrincipal AuthenticatedUser user) {
        return grades.myTranscript(user);
    }
}
