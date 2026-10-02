package vn.ptit.one.teacher.service;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.teacher.model.Teacher;
import vn.ptit.one.teacher.repository.TeacherRepository;

/**
 * API công khai của module `teacher`.
 *
 * <p>Module khác (ví dụ `course` khi phân công lớp) gọi service này, KHÔNG
 * đụng {@code TeacherRepository} — đó là repository nội bộ của module này.
 */
@Service
@Profile("central")
public class TeacherDirectory {

    private final TeacherRepository teachers;

    public TeacherDirectory(TeacherRepository teachers) {
        this.teachers = teachers;
    }

    public List<Teacher> search(String maCoSo, String maKhoa) {
        return teachers.search(maCoSo, maKhoa);
    }

    public Teacher require(String maGiangVien) {
        return teachers.findOne(maGiangVien)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "TEACHER_NOT_FOUND",
                        "Không có giảng viên %s.".formatted(maGiangVien)));
    }

    /**
     * Giảng viên phải thuộc đúng cơ sở mở lớp.
     *
     * <p>Ở Phần 2 bảng {@code GiangVien} được phân mảnh theo cơ sở, nên lớp của
     * site này về mặt vật lý không nhìn thấy giảng viên site khác. Kiểm ngay từ
     * Phần 1 để luật nghiệp vụ không đổi khi tách site.
     */
    public Teacher requireInCampus(String maGiangVien, String maCoSo) {
        Teacher teacher = require(maGiangVien);
        if (!teacher.maCoSo().equals(maCoSo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TEACHER_WRONG_CAMPUS",
                    "Giảng viên %s thuộc cơ sở %s, không dạy được lớp của cơ sở %s."
                            .formatted(maGiangVien, teacher.maCoSo(), maCoSo));
        }
        return teacher;
    }
}
