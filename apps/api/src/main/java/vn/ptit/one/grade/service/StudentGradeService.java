package vn.ptit.one.grade.service;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.grade.model.StudentGrade;
import vn.ptit.one.grade.policy.GradePolicy;
import vn.ptit.one.grade.repository.GradeRepository;
import vn.ptit.one.grade.repository.GradeRepository.GradeRow;
import vn.ptit.one.shared.exception.ApiException;

/** Bảng điểm của chính sinh viên (F07). */
@Service
@Profile("central")
public class StudentGradeService {

    private final GradeRepository grades;
    private final GradePolicy policy;

    public StudentGradeService(GradeRepository grades, GradePolicy policy) {
        this.grades = grades;
        this.policy = policy;
    }

    public List<StudentGrade> myGrades(AuthenticatedUser user, String maHocKy) {
        if (user.role() != Role.SINH_VIEN || user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ sinh viên mới có bảng điểm.");
        }
        return grades.findByStudent(user.entityId(), maHocKy).stream().map(this::toView).toList();
    }

    /** Điểm nháp chưa được lộ cho sinh viên: giữ dòng môn học, che toàn bộ điểm. */
    private StudentGrade toView(GradeRow row) {
        boolean daCongBo = row.ngayCongBo() != null;
        return new StudentGrade(row.maHocKy(), row.tenHocKy(), row.maLopHP(), row.maMonHoc(),
                row.tenMonHoc(), row.soTinChi(),
                daCongBo ? row.diemChuyenCan() : null,
                daCongBo ? row.diemGiuaKy() : null,
                daCongBo ? row.diemCuoiKy() : null,
                daCongBo ? row.diemTongKet() : null,
                daCongBo ? policy.ketQua(row.diemTongKet()) : null,
                daCongBo,
                row.ngayCongBo());
    }
}
