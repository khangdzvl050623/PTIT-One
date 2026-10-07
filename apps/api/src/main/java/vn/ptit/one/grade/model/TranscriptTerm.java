package vn.ptit.one.grade.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Một học kỳ trong bảng điểm, kèm số liệu học kỳ và luỹ kế tính tới hết kỳ đó.
 *
 * <p>Mọi con số là {@code null} khi chưa có môn nào đủ điều kiện tính — KHÔNG
 * trả 0, vì "chưa có điểm" khác "trung bình 0".
 *
 * <p>Quy tắc tính nằm ở {@code GradePolicy} và {@code StudentGradeService}, một
 * chỗ duy nhất. Giao diện chỉ hiển thị, không tự quy đổi hay tự tính lại.
 *
 * @param tinChiDatHocKy  tín chỉ các môn ĐẠT trong kỳ này
 * @param tbTichLuy10     luỹ kế tới hết kỳ này, mỗi môn lấy LẦN ĐIỂM CAO NHẤT
 *                        và chỉ tính môn đạt (học lại / cải thiện)
 * @param tinChiTichLuy   tổng tín chỉ đã đạt tới hết kỳ này, không đếm trùng môn
 */
public record TranscriptTerm(
        String maHocKy,
        String tenHocKy,
        List<StudentGrade> monHoc,
        BigDecimal tbHocKy10,
        BigDecimal tbHocKy4,
        int tinChiDatHocKy,
        BigDecimal tbTichLuy10,
        BigDecimal tbTichLuy4,
        int tinChiTichLuy,
        String xepLoaiHocKy) {
}
