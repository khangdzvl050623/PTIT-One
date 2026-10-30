package vn.ptit.one.student.model;

import java.time.LocalDate;

import vn.ptit.one.auth.model.ReservedAccount;

/** Nội dung sự kiện {@code SinhVienDuocTao} trong Outbox của Master. */
public record StudentCreated(
        String maSinhVien,
        String hoTen,
        LocalDate ngaySinh,
        String maCoSoNha,
        String maCTDT,
        ReservedAccount taiKhoan) {

    public static final String LOAI = "SinhVienDuocTao";
}
