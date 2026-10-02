package vn.ptit.one.student.model;

/**
 * Hồ sơ sinh viên — phần mà các module khác cần đọc.
 *
 * @param trangThai {@code DANG_HOC} · {@code BAO_LUU} · {@code THOI_HOC} · {@code TOT_NGHIEP}
 */
public record StudentProfile(
        String maSinhVien,
        String hoTen,
        String maCoSoNha,
        String maCTDT,
        String trangThai) {

    public static final String DANG_HOC = "DANG_HOC";

    public boolean dangHoc() {
        return DANG_HOC.equals(trangThai);
    }
}
