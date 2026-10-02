/**
 * Thông báo học vụ và trạng thái đọc (nhóm cho phép bổ sung ngày 02/10/2026).
 *
 * <p>Sở hữu {@code ThongBao} và {@code ThongBaoNguoiNhan} (V4). Hai loại:
 * <ul>
 *   <li><b>soạn tay</b> — Admin Master / Admin cơ sở / giảng viên soạn nháp rồi
 *       gửi theo phạm vi; danh sách người nhận CHỐT lúc gửi;</li>
 *   <li><b>tự sinh</b> — {@code enrollment} và {@code grade} gọi
 *       {@code NotificationPublisher} TRONG giao dịch nghiệp vụ của chúng: nghiệp
 *       vụ rollback thì thông báo cũng không còn.</li>
 * </ul>
 *
 * <p>Phụ thuộc: {@code notification → course, student, teacher}. Danh sách sinh
 * viên của lớp lấy qua interface {@code ClassRecipients} do {@code enrollment}
 * hiện thực — {@code enrollment} đã gọi sang đây để phát sự kiện, nên gọi ngược
 * trực tiếp sẽ thành phụ thuộc vòng.
 */
package vn.ptit.one.notification;
