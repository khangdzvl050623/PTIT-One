package vn.ptit.one.auth.model;

/**
 * Một dòng danh bạ cho màn quản trị tài khoản. Không mang hash mật khẩu.
 *
 * @param maCoSo     {@code null} với Admin Master
 * @param maThucThe  MaSinhVien/MaGiangVien; {@code null} với tài khoản quản trị
 * @param daKichHoat đã đặt mật khẩu bằng mã kích hoạt (hoặc tạo từ seed)
 */
public record AccountSummary(
        String tenDangNhap,
        Role loaiNguoiDung,
        String maCoSo,
        String maThucThe,
        String trangThai,
        boolean daKichHoat) {
}
