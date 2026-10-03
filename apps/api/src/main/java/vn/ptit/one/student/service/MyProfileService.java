package vn.ptit.one.student.service;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.student.model.StudentDetail;
import vn.ptit.one.student.repository.StudentRepository;

/**
 * Hồ sơ của chính sinh viên đang đăng nhập.
 *
 * <p>Không có tham số mã sinh viên: mã lấy từ principal đã ký, nên không có
 * đường nào xem hồ sơ người khác. Muốn cho Admin xem hồ sơ sinh viên thì mở
 * endpoint riêng có kiểm quyền riêng, đừng thêm tham số vào đây.
 */
@Service
@Profile("central")
public class MyProfileService {

    private final StudentRepository students;

    public MyProfileService(StudentRepository students) {
        this.students = students;
    }

    public StudentDetail myProfile(AuthenticatedUser user) {
        if (user.role() != Role.SINH_VIEN || user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ sinh viên mới có hồ sơ sinh viên.");
        }
        return students.findDetail(user.entityId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND",
                        "Không có hồ sơ sinh viên %s.".formatted(user.entityId())));
    }
}
