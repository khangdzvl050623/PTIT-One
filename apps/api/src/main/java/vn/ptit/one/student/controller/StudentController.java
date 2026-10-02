package vn.ptit.one.student.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.student.dto.CreateStudentRequest;
import vn.ptit.one.student.model.ProvisionedStudent;
import vn.ptit.one.student.service.StudentProvisioningService;

/**
 * Cấp hồ sơ sinh viên (F02). Chỉ {@code ADMIN_MASTER} — cấp tài khoản chỉ ở
 * Master vì danh bạ là bảng Master sở hữu (B3, chốt 02/10/2026).
 */
@RestController
@RequestMapping("/api/students")
@Profile("central")
public class StudentController {

    private final StudentProvisioningService provisioning;

    public StudentController(StudentProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    /**
     * Response chứa mã kích hoạt — lần duy nhất mã gốc rời khỏi server. Có
     * {@code email} và đã bật gửi thư thì mã chỉ đi qua thư, response không có mã.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public ProvisionedStudent create(@Valid @RequestBody CreateStudentRequest body) {
        return provisioning.create(body.maSinhVien(), body.hoTen(), body.ngaySinh(), body.maCoSoNha(),
                body.maCTDT(), body.email());
    }
}
