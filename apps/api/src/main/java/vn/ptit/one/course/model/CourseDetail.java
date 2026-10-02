package vn.ptit.one.course.model;

import java.util.List;

/**
 * Môn học kèm cả hai chiều của quan hệ tiên quyết.
 *
 * @param tienQuyet   môn phải đạt TRƯỚC khi học môn này
 * @param monPhuThuoc môn đang lấy môn này làm tiên quyết — cần cho màn quản trị,
 *                    vì sửa hoặc gỡ một môn ảnh hưởng tới chúng
 */
public record CourseDetail(
        CourseSummary mon,
        List<CourseSummary> tienQuyet,
        List<CourseSummary> monPhuThuoc) {
}
