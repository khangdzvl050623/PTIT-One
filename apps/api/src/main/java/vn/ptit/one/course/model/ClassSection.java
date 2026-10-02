package vn.ptit.one.course.model;

/**
 * Lớp học phần.
 *
 * <p>{@code maLopHP} nhúng mã cơ sở theo C9 ({@code BAS1150-2026-1-HCM01}) vì
 * cơ sở là một phần ngữ nghĩa của lớp — lớp thuộc về nơi mở nó. Ở Phần 2 bảng
 * này được phân mảnh ngang theo {@code maCoSoHost}.
 *
 * @param soLuongDaDangKy bộ đếm do ỨNG DỤNG sở hữu, không trigger nào chạm vào
 * @param phienBanLich    tăng mỗi lần đổi lịch; dùng cho sửa lịch có kiểm phiên bản
 */
public record ClassSection(
        String maLopHP,
        String maMonHoc,
        String tenMonHoc,
        int soTinChi,
        String maHocKy,
        String maCoSoHost,
        String maGiangVien,
        String tenGiangVien,
        int soLuongToiDa,
        int soLuongDaDangKy,
        String trangThai,
        boolean choPhepLienCoSo,
        String hinhThucHoc,
        int phienBanLich) {

    /** Chỗ còn lại; không bao giờ âm vì CHECK trong DB chặn vượt sức chứa. */
    public int conLai() {
        return soLuongToiDa - soLuongDaDangKy;
    }
}
