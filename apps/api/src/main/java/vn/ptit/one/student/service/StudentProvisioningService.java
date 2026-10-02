package vn.ptit.one.student.service;

import java.time.LocalDate;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.ActivationCode;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.service.AccountService;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.student.model.ProvisionedStudent;
import vn.ptit.one.student.repository.StudentRepository;

/**
 * Cấp hồ sơ sinh viên kèm tài khoản chưa kích hoạt (F02), trong MỘT giao dịch:
 * lỗi ở bất kỳ bước nào thì không để lại hồ sơ hay tài khoản nửa vời.
 *
 * <p>Chỉ Admin Master gọi (kiểm ở controller). Phần 2: Master ghi danh bạ +
 * Outbox, worker dựng {@code SinhVien} + {@code TaiKhoan} ở cơ sở nhà.
 */
@Service
@Profile("central")
public class StudentProvisioningService {

    private final StudentRepository students;
    private final AccountService accounts;

    public StudentProvisioningService(StudentRepository students, AccountService accounts) {
        this.students = students;
        this.accounts = accounts;
    }

    @Transactional
    public ProvisionedStudent create(String maSinhVien, String hoTen, LocalDate ngaySinh, String maCoSoNha,
            String maCTDT) {
        accounts.requireAvailable(maSinhVien, maCoSoNha);
        if (students.exists(maSinhVien)) {
            throw studentExists(maSinhVien);
        }
        try {
            if (students.insert(maSinhVien, hoTen.trim(), ngaySinh, maCoSoNha, maCTDT) != 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PROGRAM_NOT_FOUND",
                        "Không có chương trình đào tạo %s.".formatted(maCTDT));
            }
        } catch (DuplicateKeyException ex) {
            throw studentExists(maSinhVien);
        }
        ActivationCode code = accounts.provision(maSinhVien, Role.SINH_VIEN, maCoSoNha);
        return new ProvisionedStudent(students.findOne(maSinhVien).orElseThrow(), code);
    }

    private static ApiException studentExists(String maSinhVien) {
        return new ApiException(HttpStatus.CONFLICT, "STUDENT_EXISTS",
                "Đã có hồ sơ sinh viên %s.".formatted(maSinhVien));
    }
}
