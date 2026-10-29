package vn.ptit.one.teacher.service;

import java.time.Clock;
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
import vn.ptit.one.teacher.model.ProvisionedTeacher;
import vn.ptit.one.teacher.model.Teacher;
import vn.ptit.one.teacher.model.TeacherCreated;
import vn.ptit.one.teacher.repository.TeacherRepository;

/**
 * Cấp hồ sơ giảng viên kèm tài khoản chưa kích hoạt (F02), trong MỘT giao
 * dịch. Chỉ Admin Master gọi (kiểm ở controller). Phần 2 đi qua Outbox của
 * Master như sinh viên — xem {@code StudentProvisioningService}.
 */
@Service
@Profile("central")
public class TeacherProvisioningService {

    private final TeacherRepository teachers;
    private final AccountService accounts;
    private final OutboxStore outbox;
    private final ObjectMapper json;
    private final Clock clock;

    public TeacherProvisioningService(TeacherRepository teachers, AccountService accounts,
            OutboxStore outbox, ObjectMapper json, Clock clock) {
        this.teachers = teachers;
        this.accounts = accounts;
        this.outbox = outbox;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public ProvisionedTeacher create(String maGiangVien, String hoTen, String coSoNhap, String maKhoa,
            String hocVi, String email) {
        String maCoSo = accounts.requireAvailable(maGiangVien, coSoNhap);
        String hocViGon = hocVi == null || hocVi.isBlank() ? null : hocVi.trim();
        if (SiteContext.MASTER.equals(SiteContext.explicit())) {
            return reserve(new Teacher(maGiangVien, hoTen.trim(), maCoSo, maKhoa, hocViGon), email);
        }
        if (teachers.findOne(maGiangVien).isPresent()) {
            throw teacherExists(maGiangVien);
        }
        try {
            if (teachers.insert(maGiangVien, hoTen.trim(), maCoSo, maKhoa, hocViGon) != 1) {
                throw facultyUnknown(maKhoa);
            }
        } catch (DuplicateKeyException ex) {
            throw teacherExists(maGiangVien);
        }
        ActivationCode code = accounts.provision(maGiangVien, Role.GIANG_VIEN, maCoSo, email);
        return new ProvisionedTeacher(teachers.findOne(maGiangVien).orElseThrow(), code);
    }

    /** Danh bạ {@code CHO_KICH_HOAT} + sự kiện, cùng giao dịch tại Master. */
    private ProvisionedTeacher reserve(Teacher gv, String email) {
        if (!teachers.facultyExists(gv.maKhoa())) {
            throw facultyUnknown(gv.maKhoa());
        }
        Reservation r = accounts.reserve(gv.maGiangVien(), Role.GIANG_VIEN, gv.maCoSo(), email);
        TeacherCreated ev = new TeacherCreated(gv.maGiangVien(), gv.hoTen(), gv.maCoSo(), gv.maKhoa(),
                gv.hocVi(), r.taiKhoan());
        outbox.enqueue(UUID.randomUUID(), TeacherCreated.LOAI, gv.maGiangVien(), toJson(ev), 1, now());
        return new ProvisionedTeacher(gv, r.kichHoat());
    }

    /** Worker, tại cơ sở — bước TRƯỚC. Idempotent theo mã giảng viên. */
    @Transactional
    public void materialize(TeacherCreated ev) {
        if (teachers.findOne(ev.maGiangVien()).isEmpty()
                && teachers.insert(ev.maGiangVien(), ev.hoTen(), ev.maCoSo(), ev.maKhoa(), ev.hocVi()) != 1) {
            // Khoa chưa nhân bản tới cơ sở này — ném để worker thử lại sau.
            throw new IllegalStateException("Cơ sở %s chưa có khoa %s".formatted(ev.maCoSo(), ev.maKhoa()));
        }
        accounts.materialize(ev.taiKhoan());
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String toJson(TeacherCreated ev) {
        try {
            return json.writeValueAsString(ev);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Không ghi được sự kiện cấp hồ sơ.", ex);
        }
    }

    private static ApiException facultyUnknown(String maKhoa) {
        return new ApiException(HttpStatus.BAD_REQUEST, "FACULTY_UNKNOWN",
                "Không có khoa %s.".formatted(maKhoa));
    }

    private static ApiException teacherExists(String maGiangVien) {
        return new ApiException(HttpStatus.CONFLICT, "TEACHER_EXISTS",
                "Đã có hồ sơ giảng viên %s.".formatted(maGiangVien));
    }
}
