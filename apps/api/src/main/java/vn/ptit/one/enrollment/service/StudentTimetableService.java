package vn.ptit.one.enrollment.service;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.enrollment.repository.EnrollmentRepository;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.timetable.model.Timetable;
import vn.ptit.one.timetable.service.ScheduleService;

/**
 * Thời khoá biểu của sinh viên (F09).
 *
 * <p>Nằm ở {@code enrollment} chứ không ở {@code timetable}: lịch của sinh viên
 * là lịch của các lớp họ đang ghi danh. F08 cũng cần {@code enrollment → timetable}
 * để kiểm trùng lịch, nên đặt ở đây giữ phụ thuộc một chiều.
 */
@Service
@Profile("central")
public class StudentTimetableService {

    private final EnrollmentRepository enrollments;
    private final ScheduleService schedules;

    public StudentTimetableService(EnrollmentRepository enrollments, ScheduleService schedules) {
        this.enrollments = enrollments;
        this.schedules = schedules;
    }

    public Timetable timetable(AuthenticatedUser user, String maHocKy, Integer tuan) {
        if (user.role() != Role.SINH_VIEN || user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ sinh viên mới có thời khoá biểu học tập.");
        }
        return schedules.timetable(maHocKy, enrollments.activeClasses(user.entityId(), maHocKy), tuan);
    }
}
