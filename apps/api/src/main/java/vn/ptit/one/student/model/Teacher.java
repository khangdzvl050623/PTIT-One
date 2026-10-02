package vn.ptit.one.student.model;

/** Hồ sơ giảng viên ở mức tra cứu. Đủ để phân công lớp và hiển thị danh sách. */
public record Teacher(
        String maGiangVien,
        String hoTen,
        String maCoSo,
        String maKhoa,
        String hocVi) {
}
