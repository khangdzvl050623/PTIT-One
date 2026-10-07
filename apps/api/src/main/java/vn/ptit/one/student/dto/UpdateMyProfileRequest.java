package vn.ptit.one.student.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sinh viên tự điền phần LÝ LỊCH của hồ sơ mình.
 *
 * <p>Không có {@code hoTen}, {@code ngaySinh}, {@code maCoSoNha}, {@code maCTDT}
 * hay {@code trangThai}: đó là dữ liệu hành chính do Phòng Đào tạo quản, sinh
 * viên sửa được thì hồ sơ mất giá trị pháp lý.
 *
 * <p>Mọi trường đều cho {@code null} — để trống là hợp lệ. Gửi {@code null}
 * nghĩa là XOÁ giá trị cũ, vì đây là thay toàn bộ phần lý lịch chứ không vá
 * từng ô.
 *
 * <p>Mỗi vị từ đều mở đầu bằng nhánh "rỗng hoặc khoảng trắng": biểu mẫu web
 * gửi ô trống thành chuỗi rỗng, không phải {@code null}. Thiếu nhánh đó thì
 * "bỏ trống để xoá" sẽ trả {@code 400} ở đúng những ô có định dạng — tức là
 * không bao giờ xoá được. Khoảng trắng cũng tính là bỏ trống, cùng cách
 * {@code MyProfileService} cắt chuỗi trước khi ghi.
 */
public record UpdateMyProfileRequest(
        @Pattern(regexp = "\\s*|NAM|NU", message = "Giới tính chỉ nhận NAM hoặc NU.") String gioiTinh,
        @Pattern(regexp = "\\s*|[0-9 +().-]{8,20}", message = "Số điện thoại không hợp lệ.")
        String dienThoai,
        /* CCCD 12 số; CMND cũ 9 số — nhận cả hai vì hồ sơ cũ còn dùng CMND. */
        @Pattern(regexp = "\\s*|[0-9]{9}|[0-9]{12}", message = "Số CCCD phải là 9 hoặc 12 chữ số.")
        String soCCCD,
        /* Tự viết vị từ thay vì @Email: @Email cho qua chuỗi rỗng nhưng CHẶN
           chuỗi khoảng trắng, nên chín ô sẽ có hai cách hiểu "bỏ trống". */
        @Pattern(regexp = "\\s*|[^\\s@]+@[^\\s@]+\\.[^\\s@]+", message = "Email cá nhân không hợp lệ.")
        @Size(max = 254, message = "Email cá nhân quá dài.") String emailCaNhan,
        @Size(max = 150, message = "Nơi sinh quá dài.") String noiSinh,
        @Size(max = 50, message = "Dân tộc quá dài.") String danToc,
        @Size(max = 50, message = "Tôn giáo quá dài.") String tonGiao,
        @Size(max = 255, message = "Hộ khẩu quá dài.") String hoKhau,
        /* URL ảnh trên dịch vụ ngoài (Cloudinary). Chỉ nhận https để không
           nhúng ảnh qua kết nối không mã hoá vào trang đã chạy https. */
        @Pattern(regexp = "\\s*|https://.+", message = "Ảnh đại diện phải là liên kết https.")
        @Size(max = 500, message = "Liên kết ảnh quá dài.") String anhDaiDien) {
}
