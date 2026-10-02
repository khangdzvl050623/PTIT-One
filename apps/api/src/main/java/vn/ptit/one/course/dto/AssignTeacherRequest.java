package vn.ptit.one.course.dto;

/** {@code null} hoặc rỗng nghĩa là gỡ phân công, không phải thiếu dữ liệu. */
public record AssignTeacherRequest(String maGiangVien) {
}
