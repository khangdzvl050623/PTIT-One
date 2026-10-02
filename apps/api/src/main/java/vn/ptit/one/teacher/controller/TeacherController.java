package vn.ptit.one.teacher.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.teacher.dto.CreateTeacherRequest;
import vn.ptit.one.teacher.model.ProvisionedTeacher;
import vn.ptit.one.teacher.model.Teacher;
import vn.ptit.one.teacher.service.TeacherDirectory;
import vn.ptit.one.teacher.service.TeacherProvisioningService;

/**
 * Tra cứu giảng viên, phục vụ màn phân công lớp.
 *
 * <p>Phạm vi cơ sở lấy từ principal đã ký, KHÔNG nhận tham số {@code maCoSo}
 * của client — tin tham số đó là mở đường leo thang đặc quyền. Chỉ
 * {@code ADMIN_MASTER} mới thấy mọi cơ sở (B3: đọc toàn hệ thống).
 *
 * <p>Cấp hồ sơ kèm tài khoản (F02) chỉ {@code ADMIN_MASTER} — cấp tài khoản
 * chỉ ở Master vì danh bạ là bảng Master sở hữu.
 */
@RestController
@RequestMapping("/api/teachers")
@Profile("central")
public class TeacherController {

    private final TeacherDirectory teachers;
    private final TeacherProvisioningService provisioning;

    public TeacherController(TeacherDirectory teachers, TeacherProvisioningService provisioning) {
        this.teachers = teachers;
        this.provisioning = provisioning;
    }

    @GetMapping
    public List<Teacher> search(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String maKhoa) {
        String campusScope = user.role() == Role.ADMIN_MASTER ? null : user.homeCampus();
        return teachers.search(campusScope, maKhoa);
    }

    /**
     * Response chứa mã kích hoạt — lần duy nhất mã gốc rời khỏi server. Có
     * {@code email} và đã bật gửi thư thì mã chỉ đi qua thư, response không có mã.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public ProvisionedTeacher create(@Valid @RequestBody CreateTeacherRequest body) {
        return provisioning.create(body.maGiangVien(), body.hoTen(), body.maCoSo(), body.maKhoa(), body.hocVi(),
                body.email());
    }
}
