package vn.ptit.one.course.model;

import java.util.List;

/** CTĐT kèm danh sách môn, xếp theo học kỳ gợi ý. */
public record ProgramDetail(StudyProgram chuongTrinh, List<ProgramCourse> monHoc) {
}
