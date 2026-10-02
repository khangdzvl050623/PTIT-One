package vn.ptit.one.timetable.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.model.Term;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.course.service.CourseService;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.timetable.model.ClassSchedule;
import vn.ptit.one.timetable.model.ScheduleSlot;
import vn.ptit.one.timetable.model.Timetable;
import vn.ptit.one.timetable.model.TimetableEntry;
import vn.ptit.one.timetable.repository.ScheduleRepository;
import vn.ptit.one.timetable.repository.ScheduleRepository.ClashRow;

/**
 * Lịch học của lớp (F04).
 *
 * <p>Module này phụ thuộc {@code course} (hỏi lớp có tồn tại và có quyền sửa
 * không). Chiều ngược lại KHÔNG tồn tại — {@code course} không gọi sang đây,
 * nếu không sẽ thành phụ thuộc vòng.
 */
@Service
@Profile("central")
public class ScheduleService {

    private static final int LOCK_TIMEOUT_MS = 5_000;
    /** Khớp `KhungGioTiet`: 12 tiết mỗi ngày. */
    private static final int TIET_CUOI_NGAY = 12;

    private final ScheduleRepository schedules;
    private final ClassSectionService classes;
    private final CourseService courses;

    public ScheduleService(ScheduleRepository schedules, ClassSectionService classes,
            CourseService courses) {
        this.schedules = schedules;
        this.classes = classes;
        this.courses = courses;
    }

    public ClassSchedule read(AuthenticatedUser user, String maLopHP) {
        ClassSection lop = classes.detail(user, maLopHP);
        return new ClassSchedule(lop.maLopHP(), lop.phienBanLich(), schedules.findByClass(maLopHP));
    }

    /**
     * Thời khoá biểu của một tập lớp trong học kỳ — API công khai, dùng cho
     * lịch sinh viên (F09, gọi từ {@code enrollment}) và lịch dạy giảng viên.
     * Không kiểm quyền: chỗ gọi đã quyết định người dùng được xem lớp nào.
     *
     * @param tuan {@code null} là cả học kỳ
     */
    public Timetable timetable(String maHocKy, Collection<String> maLopHP, Integer tuan) {
        if (tuan != null && tuan < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Tuần học bắt đầu từ 1.");
        }
        Term term = courses.requireTerm(maHocKy);
        List<TimetableEntry> buoiHoc = schedules.entriesFor(maLopHP);
        if (tuan != null) {
            buoiHoc = buoiHoc.stream().filter(buoi -> buoi.coTrongTuan(tuan)).toList();
        }
        return new Timetable(term.maHocKy(), term.ngayBatDau(), tuan, buoiHoc);
    }

    /** Lịch dạy của giảng viên đang đăng nhập, gom mọi lớp đang phụ trách. */
    public Timetable teachingSchedule(AuthenticatedUser user, String maHocKy, Integer tuan) {
        List<String> lop = classes.taughtBy(user, maHocKy).stream()
                .filter(l -> !"DA_HUY".equals(l.trangThai()))
                .map(ClassSection::maLopHP)
                .toList();
        return timetable(maHocKy, lop, tuan);
    }

    /**
     * Thay toàn bộ lịch của lớp.
     *
     * <p>Thứ tự: kiểm quyền → chặn nếu đã có đăng ký → khoá học kỳ → kiểm trùng
     * → ghi. Khoá trước khi kiểm, vì hai admin sửa hai lớp khác nhau có thể
     * cùng vượt qua bước kiểm rồi mới ghi và tạo ra trùng lịch thật.
     */
    @Transactional
    public ClassSchedule replace(AuthenticatedUser user, String maLopHP, List<ScheduleSlot> buoiHoc) {
        ClassSection lop = classes.requireManageableBy(user, maLopHP);

        /* Quyết định nhóm 02/10/2026: lớp đã có sinh viên thì khoá lịch. Đổi
           lịch sau khi sinh viên đã xếp thời khoá biểu là đổi cam kết với họ
           mà hệ thống chưa có cách báo lại. */
        if (lop.soLuongDaDangKy() > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CLASS_HAS_ENROLLMENTS",
                    "Lớp %s đã có %d sinh viên đăng ký, không sửa được lịch."
                            .formatted(maLopHP, lop.soLuongDaDangKy()));
        }

        List<ScheduleSlot> slots = normalise(buoiHoc);
        validateShape(slots);
        validateInternalOverlap(slots);

        if (!schedules.acquireTermLock(lop.maHocKy(), LOCK_TIMEOUT_MS)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TIMETABLE_BUSY",
                    "Lịch học kỳ này đang được người khác sửa. Vui lòng thử lại.");
        }

        checkTeacherClash(lop, slots);
        checkRoomClash(lop, slots);

        schedules.deleteByClass(maLopHP);
        for (ScheduleSlot slot : slots) {
            schedules.insert(maLopHP, slot);
        }
        schedules.bumpScheduleVersion(maLopHP);

        return read(user, maLopHP);
    }

    private void checkTeacherClash(ClassSection lop, List<ScheduleSlot> slots) {
        if (lop.maGiangVien() == null) {
            return;
        }
        List<ClashRow> existing = schedules.teacherSlots(lop.maHocKy(), lop.maGiangVien(), lop.maLopHP());
        ClashRow clash = firstClash(slots, existing);
        if (clash != null) {
            throw new ApiException(HttpStatus.CONFLICT, "SCHEDULE_TEACHER_CLASH",
                    "Giảng viên %s đã dạy lớp %s vào khung giờ này."
                            .formatted(lop.maGiangVien(), clash.maLopHP()));
        }
    }

    private void checkRoomClash(ClassSection lop, List<ScheduleSlot> slots) {
        for (ScheduleSlot slot : slots) {
            if (slot.phongHoc() == null || slot.phongHoc().isBlank()) {
                continue;
            }
            String key = slot.phongHoc().trim().toUpperCase(Locale.ROOT);
            List<ClashRow> existing = schedules.roomSlots(lop.maHocKy(), key, lop.maLopHP());
            ClashRow clash = firstClash(List.of(slot), existing);
            if (clash != null) {
                throw new ApiException(HttpStatus.CONFLICT, "SCHEDULE_ROOM_CLASH",
                        "Phòng %s đã được lớp %s dùng vào khung giờ này."
                                .formatted(slot.phongHoc().trim(), clash.maLopHP()));
            }
        }
    }

    private static ClashRow firstClash(List<ScheduleSlot> slots, List<ClashRow> existing) {
        for (ScheduleSlot slot : slots) {
            for (ClashRow row : existing) {
                if (slot.trungTiet(row.slot()) && slot.trungTuan(row.slot())) {
                    return row;
                }
            }
        }
        return null;
    }

    /** Cắt khoảng trắng phòng; giữ nguyên chữ hoa thường như admin nhập. */
    private static List<ScheduleSlot> normalise(List<ScheduleSlot> buoiHoc) {
        List<ScheduleSlot> ket = new ArrayList<>();
        for (ScheduleSlot slot : buoiHoc == null ? List.<ScheduleSlot>of() : buoiHoc) {
            String phong = slot.phongHoc() == null || slot.phongHoc().isBlank()
                    ? null : slot.phongHoc().trim();
            ket.add(new ScheduleSlot(slot.thu(), slot.tietBatDau(), slot.soTiet(), phong,
                    slot.tuanBatDau(), slot.tuanKetThuc()));
        }
        return ket;
    }

    private static void validateShape(List<ScheduleSlot> slots) {
        for (ScheduleSlot slot : slots) {
            if (slot.tietKetThuc() > TIET_CUOI_NGAY) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SCHEDULE_SLOT_INVALID",
                        "Buổi thứ %d bắt đầu tiết %d kéo %d tiết là vượt quá tiết %d."
                                .formatted(slot.thu(), slot.tietBatDau(), slot.soTiet(), TIET_CUOI_NGAY));
            }
            if (slot.tuanBatDau() > slot.tuanKetThuc()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SCHEDULE_SLOT_INVALID",
                        "Tuần bắt đầu phải nhỏ hơn hoặc bằng tuần kết thúc.");
            }
        }
    }

    /** Chính lớp đó không được tự trùng giờ với mình. */
    private static void validateInternalOverlap(List<ScheduleSlot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                ScheduleSlot a = slots.get(i);
                ScheduleSlot b = slots.get(j);
                if (a.trungTiet(b) && a.trungTuan(b)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "SCHEDULE_SELF_OVERLAP",
                            "Hai buổi học của cùng lớp chồng nhau vào thứ %d.".formatted(a.thu()));
                }
            }
        }
    }
}
