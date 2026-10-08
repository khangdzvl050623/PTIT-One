package vn.ptit.one.teacher.model;

/**
 * Hồ sơ đầy đủ của một giảng viên, cho màn "Thông tin".
 *
 * <p>Khác {@link Teacher}: cái kia là mức tra cứu (đủ để phân công lớp), cái
 * này gộp thêm tên hiển thị của khoa và cơ sở, email tài khoản, và phần lý lịch
 * giảng viên tự điền (`V9`).
 *
 * <p>Bốn trường đầu sau {@code maGiangVien} là **hành chính** — Phòng Đào tạo
 * quản, giảng viên không tự sửa.
 */
public record TeacherDetail(
        String maGiangVien,
        String hoTen,
        String hocVi,
        String maKhoa,
        String tenKhoa,
        String maCoSo,
        String tenCoSo,
        /** Email trường cấp, lấy từ `TaiKhoan`; `null` khi chưa có tài khoản. */
        String email,

        // --- Lý lịch (V9), giảng viên tự điền ---
        String gioiTinh,
        String dienThoai,
        String soCCCD,
        String emailCaNhan,
        String noiSinh,
        String danToc,
        String tonGiao,
        String hoKhau,
        String anhDaiDien) {
}
