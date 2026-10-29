package vn.ptit.one.teacher.model;

import vn.ptit.one.auth.model.ReservedAccount;

/** Nội dung sự kiện {@code GiangVienDuocTao} trong Outbox của Master. */
public record TeacherCreated(
        String maGiangVien,
        String hoTen,
        String maCoSo,
        String maKhoa,
        String hocVi,
        ReservedAccount taiKhoan) {

    public static final String LOAI = "GiangVienDuocTao";
}
