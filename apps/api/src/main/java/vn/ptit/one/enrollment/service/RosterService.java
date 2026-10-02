package vn.ptit.one.enrollment.service;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;
import vn.ptit.one.enrollment.model.ClassRoster;
import vn.ptit.one.enrollment.model.RosterEntry;
import vn.ptit.one.enrollment.repository.EnrollmentRepository;
import vn.ptit.one.notification.service.ClassRecipients;

/**
 * Danh sách sinh viên và sĩ số của lớp (F05). Cũng là nguồn người nhận khi
 * thông báo gửi cho một lớp học phần ({@link ClassRecipients}).
 */
@Service
@Profile("central")
public class RosterService implements ClassRecipients {

    private final EnrollmentRepository enrollments;
    private final ClassSectionService classes;

    public RosterService(EnrollmentRepository enrollments, ClassSectionService classes) {
        this.enrollments = enrollments;
        this.classes = classes;
    }

    /**
     * Quyền kiểm theo LỚP, không theo tham số: đổi mã lớp trên URL thành lớp
     * của giảng viên khác vẫn bị chặn ở {@code requireStaffAccess}.
     */
    public ClassRoster roster(AuthenticatedUser user, String maLopHP) {
        ClassSection lop = classes.requireStaffAccess(user, maLopHP);
        return new ClassRoster(lop, enrollments.roster(maLopHP));
    }

    @Override
    public List<String> studentsOf(String maLopHP) {
        return enrollments.roster(maLopHP).stream().map(RosterEntry::maSinhVien).toList();
    }
}
