package vn.ptit.one.student.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import vn.ptit.one.auth.model.ActivationCode;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.service.AccountService;
import vn.ptit.one.auth.service.AccountService.Reservation;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.shared.outbox.OutboxStore;
import vn.ptit.one.shared.routing.SiteContext;
import vn.ptit.one.student.model.ProvisionedStudent;
import vn.ptit.one.student.model.StudentCreated;
import vn.ptit.one.student.model.StudentProfile;
import vn.ptit.one.student.repository.StudentRepository;

/**
 * Cấp hồ sơ sinh viên kèm tài khoản chưa kích hoạt (F02), trong MỘT giao dịch:
 * lỗi ở bất kỳ bước nào thì không để lại hồ sơ hay tài khoản nửa vời.
 *
 * <p>Chỉ Admin Master gọi (kiểm ở controller). Phần 2: Master không có bảng
 * {@code SinhVien}, nên Master ghi danh bạ + Outbox, worker dựng {@code SinhVien}
 * + {@code TaiKhoan} ở cơ sở nhà — "Vòng đời sinh viên" (C2).
 */
@Service
@Profile("central")
public class StudentProvisioningService {

    private final StudentRepository students;
    private final AccountService accounts;
    private final OutboxStore outbox;
    private final ObjectMapper json;
    private final Clock clock;

    public StudentProvisioningService(StudentRepository students, AccountService accounts,
            OutboxStore outbox, ObjectMapper json, Clock clock) {
        this.students = students;
        this.accounts = accounts;
        this.outbox = outbox;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public ProvisionedStudent create(String maSinhVien, String hoTen, LocalDate ngaySinh, String coSoNhap,
            String maCTDT, String email) {
        String maCoSoNha = accounts.requireAvailable(maSinhVien, coSoNhap);
        if (SiteContext.MASTER.equals(SiteContext.explicit())) {
            return reserve(maSinhVien, hoTen.trim(), ngaySinh, maCoSoNha, maCTDT, email);
        }
        if (students.exists(maSinhVien)) {
            throw studentExists(maSinhVien);
        }
        try {
            if (students.insert(maSinhVien, hoTen.trim(), ngaySinh, maCoSoNha, maCTDT) != 1) {
                throw programNotFound(maCTDT);
            }
        } catch (DuplicateKeyException ex) {
            throw studentExists(maSinhVien);
        }
        ActivationCode code = accounts.provision(maSinhVien, Role.SINH_VIEN, maCoSoNha, email);
        return new ProvisionedStudent(students.findOne(maSinhVien).orElseThrow(), code);
    }

    /** Danh bạ {@code CHO_KICH_HOAT} + sự kiện, cùng giao dịch tại Master. */
    private ProvisionedStudent reserve(String maSinhVien, String hoTen, LocalDate ngaySinh, String maCoSoNha,
            String maCTDT, String email) {
        if (!students.programExists(maCTDT)) {
            throw programNotFound(maCTDT);
        }
        Reservation r = accounts.reserve(maSinhVien, Role.SINH_VIEN, maCoSoNha, email);
        StudentCreated ev = new StudentCreated(maSinhVien, hoTen, ngaySinh, maCoSoNha, maCTDT, r.taiKhoan());
        outbox.enqueue(UUID.randomUUID(), StudentCreated.LOAI, maSinhVien, toJson(ev), 1, now());
        return new ProvisionedStudent(
                new StudentProfile(maSinhVien, hoTen, maCoSoNha, maCTDT, StudentProfile.DANG_HOC), r.kichHoat());
    }

    /** Worker, tại cơ sở nhà — bước TRƯỚC. Idempotent theo mã sinh viên. */
    @Transactional
    public void materialize(StudentCreated ev) {
        if (!students.exists(ev.maSinhVien())
                && students.insert(ev.maSinhVien(), ev.hoTen(), ev.ngaySinh(), ev.maCoSoNha(), ev.maCTDT()) != 1) {
            // CTĐT chưa nhân bản tới cơ sở này — ném để worker thử lại sau.
            throw new IllegalStateException("Cơ sở %s chưa có CTĐT %s".formatted(ev.maCoSoNha(), ev.maCTDT()));
        }
        accounts.materialize(ev.taiKhoan());
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String toJson(StudentCreated ev) {
        try {
            return json.writeValueAsString(ev);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Không ghi được sự kiện cấp hồ sơ.", ex);
        }
    }

    private static ApiException programNotFound(String maCTDT) {
        return new ApiException(HttpStatus.BAD_REQUEST, "PROGRAM_NOT_FOUND",
                "Không có chương trình đào tạo %s.".formatted(maCTDT));
    }

    private static ApiException studentExists(String maSinhVien) {
        return new ApiException(HttpStatus.CONFLICT, "STUDENT_EXISTS",
                "Đã có hồ sơ sinh viên %s.".formatted(maSinhVien));
    }
}
