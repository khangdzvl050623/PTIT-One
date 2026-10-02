package vn.ptit.one.grade.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import vn.ptit.one.grade.policy.GradePolicy;
import vn.ptit.one.grade.repository.GradeRepository;

/**
 * API công khai của module {@code grade} cho đăng ký học phần (F08).
 *
 * <p>Phụ thuộc chỉ một chiều {@code enrollment → grade}: ghi danh mới mở dòng
 * điểm rỗng ở đây, nên bảng điểm không cần đọc ngược bảng ghi danh.
 */
@Service
@Profile("central")
public class GradeRecords {

    private final GradeRepository grades;
    private final GradePolicy policy;

    public GradeRecords(GradeRepository grades, GradePolicy policy) {
        this.grades = grades;
        this.policy = policy;
    }

    /**
     * Kết quả của mỗi môn sinh viên đã học, theo điểm CAO NHẤT đã công bố
     * (quyết định nhóm 02/10/2026). Môn chưa có điểm công bố thì không có mặt.
     *
     * @return mã môn → {@code DAT} / {@code KHONG_DAT}
     */
    public Map<String, String> bestResults(String maSinhVien) {
        Map<String, String> results = new HashMap<>();
        grades.bestPublishedByCourse(maSinhVien)
                .forEach((maMonHoc, diem) -> results.put(maMonHoc, policy.ketQua(diem)));
        return results;
    }

    /** Mở dòng điểm rỗng cho ghi danh mới — "chưa có điểm", không phải 0. */
    public void openRecord(String maLopHP, String maSinhVien) {
        grades.insertEmpty(maLopHP, maSinhVien);
    }

    /**
     * Xoá dòng điểm khi huỷ đăng ký.
     *
     * @return {@code false} nếu đã có điểm — huỷ phải bị chặn, không xoá điểm để huỷ
     */
    public boolean removeEmptyRecord(String maLopHP, String maSinhVien) {
        return grades.deleteIfEmpty(maLopHP, maSinhVien) == 1 || !grades.exists(maLopHP, maSinhVien);
    }
}
