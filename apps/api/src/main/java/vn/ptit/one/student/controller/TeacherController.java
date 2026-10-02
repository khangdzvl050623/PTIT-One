package vn.ptit.one.student.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.student.model.Teacher;
import vn.ptit.one.student.service.TeacherDirectory;

/**
 * Tra cứu giảng viên, phục vụ màn phân công lớp.
 *
 * <p>Phạm vi cơ sở lấy từ principal đã ký, KHÔNG nhận tham số {@code maCoSo}
 * của client — tin tham số đó là mở đường leo thang đặc quyền. Chỉ
 * {@code ADMIN_MASTER} mới thấy mọi cơ sở (B3: đọc toàn hệ thống).
 */
@RestController
@RequestMapping("/api/teachers")
@Profile("central")
public class TeacherController {

    private final TeacherDirectory teachers;

    public TeacherController(TeacherDirectory teachers) {
        this.teachers = teachers;
    }

    @GetMapping
    public List<Teacher> search(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String maKhoa) {
        String campusScope = user.role() == Role.ADMIN_MASTER ? null : user.homeCampus();
        return teachers.search(campusScope, maKhoa);
    }
}
