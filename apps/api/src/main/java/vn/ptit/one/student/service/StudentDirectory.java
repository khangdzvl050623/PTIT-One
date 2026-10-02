package vn.ptit.one.student.service;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.student.model.StudentProfile;
import vn.ptit.one.student.repository.StudentRepository;

/**
 * API công khai của module `student`. Module khác (ví dụ `enrollment` khi đăng
 * ký học phần) gọi service này, KHÔNG đụng {@code StudentRepository}.
 */
@Service
@Profile("central")
public class StudentDirectory {

    private final StudentRepository students;

    public StudentDirectory(StudentRepository students) {
        this.students = students;
    }

    /**
     * Sinh viên còn thuộc trường theo cơ sở NHÀ — người nhận bản tin cơ sở/toàn
     * trường. Bỏ người đã thôi học và đã tốt nghiệp.
     *
     * @param maCoSo {@code null} là toàn trường
     */
    public List<String> currentStudentIds(String maCoSo) {
        return students.currentStudentIds(maCoSo);
    }

    public StudentProfile require(String maSinhVien) {
        return students.findOne(maSinhVien)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND",
                        "Không có hồ sơ sinh viên %s.".formatted(maSinhVien)));
    }
}
