package vn.ptit.one.student.model;

import java.time.LocalDate;

/**
 * Hồ sơ đầy đủ cho màn "Thông tin sinh viên" (F01).
 *
 * <p>Khác {@link StudentProfile}: bản kia là phần tối thiểu mà module khác cần
 * để kiểm điều kiện đăng ký, nên giữ nguyên hình dạng — đổi nó sẽ lan ra
 * {@code enrollment} và {@code notification}. Bản này chỉ để hiển thị.
 *
 * <p>Nhóm trường lý lịch ({@code gioiTinh} → {@code hoKhau}) do {@code V8} thêm
 * và đều NULL được: hồ sơ cũ không có dữ liệu này.
 *
 * @param anhDaiDien URL ảnh trên dịch vụ ngoài, không phải nhị phân trong DB
 */
public record StudentDetail(
        String maSinhVien,
        String hoTen,
        LocalDate ngaySinh,
        /** Email trường cấp, lấy từ `TaiKhoan`. */
        String email,
        String gioiTinh,
        String dienThoai,
        String soCCCD,
        /** Email cá nhân, khác email trường cấp. */
        String emailCaNhan,
        String noiSinh,
        String danToc,
        String tonGiao,
        String hoKhau,
        String anhDaiDien,
        String maCoSoNha,
        String tenCoSo,
        String trangThai,
        String maCTDT,
        String tenCTDT,
        String tenKhoa,
        int tongTinChiCTDT,
        int soTinChiTichLuy) {
}
