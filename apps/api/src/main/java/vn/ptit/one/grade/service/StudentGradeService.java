package vn.ptit.one.grade.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.grade.model.StudentGrade;
import vn.ptit.one.grade.model.TranscriptTerm;
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
        return grades.findByStudent(requireStudent(user), maHocKy).stream().map(this::toView).toList();
    }

    /**
     * Bảng điểm gom theo học kỳ, kèm trung bình kỳ và luỹ kế.
     *
     * <p>Tính ở đây chứ không ở giao diện: quy đổi thang 4, chọn lần điểm cao
     * nhất và ngưỡng xếp loại đều là QUY TẮC, phải có một chỗ duy nhất. Trả về
     * kỳ mới nhất trước.
     */
    public List<TranscriptTerm> myTranscript(AuthenticatedUser user) {
        List<StudentGrade> all = grades.findByStudent(requireStudent(user), null).stream()
                .map(this::toView)
                .toList();

        Map<String, List<StudentGrade>> theoKy = all.stream()
                .collect(Collectors.groupingBy(StudentGrade::maHocKy, LinkedHashMap::new, Collectors.toList()));

        /* Luỹ kế phải đi từ kỳ CŨ tới kỳ MỚI: con số của một kỳ là tính tới hết
           kỳ đó. Mã học kỳ dạng `yyyy-n` nên so chuỗi là đúng thứ tự thời gian. */
        List<String> tuCuDenMoi = theoKy.keySet().stream().sorted().toList();

        // Môn -> lần điểm cao nhất tới thời điểm đang xét (học lại / cải thiện).
        Map<String, StudentGrade> caoNhat = new HashMap<>();
        List<TranscriptTerm> ket = new ArrayList<>();

        for (String maHocKy : tuCuDenMoi) {
            List<StudentGrade> monTrongKy = theoKy.get(maHocKy);
            List<StudentGrade> daCoDiem = monTrongKy.stream()
                    .filter(g -> g.daCongBo() && g.diemTongKet() != null)
                    .toList();

            for (StudentGrade g : daCoDiem) {
                StudentGrade truoc = caoNhat.get(g.maMonHoc());
                if (truoc == null || g.diemTongKet().compareTo(truoc.diemTongKet()) > 0) {
                    caoNhat.put(g.maMonHoc(), g);
                }
            }
            List<StudentGrade> tichLuy = caoNhat.values().stream()
                    .filter(g -> policy.dat(g.diemTongKet()))
                    .toList();

            BigDecimal tbKy4 = policy.trungBinh(canNang(daCoDiem, StudentGrade::diemHe4));
            ket.add(new TranscriptTerm(maHocKy,
                    monTrongKy.isEmpty() ? maHocKy : monTrongKy.get(0).tenHocKy(),
                    monTrongKy,
                    policy.trungBinh(canNang(daCoDiem, StudentGrade::diemTongKet)),
                    tbKy4,
                    tinChi(daCoDiem.stream().filter(g -> policy.dat(g.diemTongKet())).toList()),
                    policy.trungBinh(canNang(tichLuy, StudentGrade::diemTongKet)),
                    policy.trungBinh(canNang(tichLuy, StudentGrade::diemHe4)),
                    tinChi(tichLuy),
                    policy.xepLoai(tbKy4)));
        }

        return ket.reversed();
    }

    private static List<GradePolicy.TinChiDiem> canNang(List<StudentGrade> mon,
            Function<StudentGrade, BigDecimal> diem) {
        return mon.stream().map(g -> new GradePolicy.TinChiDiem(diem.apply(g), g.soTinChi())).toList();
    }

    private static int tinChi(List<StudentGrade> mon) {
        return mon.stream().mapToInt(StudentGrade::soTinChi).sum();
    }

    private static String requireStudent(AuthenticatedUser user) {
        if (user.role() != Role.SINH_VIEN || user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ sinh viên mới có bảng điểm.");
        }
        return user.entityId();
    }

    /** Điểm nháp chưa được lộ cho sinh viên: giữ dòng môn học, che toàn bộ điểm. */
    private StudentGrade toView(GradeRow row) {
        boolean daCongBo = row.ngayCongBo() != null;
        BigDecimal tongKet = daCongBo ? row.diemTongKet() : null;
        return new StudentGrade(row.maHocKy(), row.tenHocKy(), row.maLopHP(), row.maMonHoc(),
                row.tenMonHoc(), row.soTinChi(),
                daCongBo ? row.diemChuyenCan() : null,
                daCongBo ? row.diemGiuaKy() : null,
                daCongBo ? row.diemCuoiKy() : null,
                tongKet,
                policy.diemChu(tongKet),
                policy.diemHe4(tongKet),
                policy.ketQua(tongKet),
                daCongBo,
                row.ngayCongBo());
    }
}
