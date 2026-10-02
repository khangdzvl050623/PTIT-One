package vn.ptit.one.enrollment.model;

import java.time.Instant;

/** Một sinh viên trong danh sách lớp. */
public record RosterEntry(
        String maSinhVien,
        String hoTen,
        String maCoSoNha,
        Instant ngayDangKy,
        String trangThai) {
}
