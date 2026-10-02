package vn.ptit.one.teacher.service;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.ActivationCode;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.service.AccountService;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.teacher.model.ProvisionedTeacher;
import vn.ptit.one.teacher.repository.TeacherRepository;

/**
 * Cấp hồ sơ giảng viên kèm tài khoản chưa kích hoạt (F02), trong MỘT giao
 * dịch. Chỉ Admin Master gọi (kiểm ở controller).
 */
@Service
@Profile("central")
public class TeacherProvisioningService {

    private final TeacherRepository teachers;
    private final AccountService accounts;

    public TeacherProvisioningService(TeacherRepository teachers, AccountService accounts) {
        this.teachers = teachers;
        this.accounts = accounts;
    }

    @Transactional
    public ProvisionedTeacher create(String maGiangVien, String hoTen, String maCoSo, String maKhoa,
            String hocVi, String email) {
        accounts.requireAvailable(maGiangVien, maCoSo);
        if (teachers.findOne(maGiangVien).isPresent()) {
            throw teacherExists(maGiangVien);
        }
        String hocViGon = hocVi == null || hocVi.isBlank() ? null : hocVi.trim();
        try {
            if (teachers.insert(maGiangVien, hoTen.trim(), maCoSo, maKhoa, hocViGon) != 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "FACULTY_UNKNOWN",
                        "Không có khoa %s.".formatted(maKhoa));
            }
        } catch (DuplicateKeyException ex) {
            throw teacherExists(maGiangVien);
        }
        ActivationCode code = accounts.provision(maGiangVien, Role.GIANG_VIEN, maCoSo, email);
        return new ProvisionedTeacher(teachers.findOne(maGiangVien).orElseThrow(), code);
    }

    private static ApiException teacherExists(String maGiangVien) {
        return new ApiException(HttpStatus.CONFLICT, "TEACHER_EXISTS",
                "Đã có hồ sơ giảng viên %s.".formatted(maGiangVien));
    }
}
