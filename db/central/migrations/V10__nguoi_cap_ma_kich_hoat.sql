/* PTIT One — V10: ghi lại AI đã cấp mã kích hoạt.

   `POST /api/accounts/{u}/password-reset` là **công cụ chiếm tài khoản**: Admin
   Master cấp mã cho bất kỳ ai, nhận mã trong phản hồi khi tài khoản không có
   email, rồi đặt mật khẩu và đăng nhập thành người đó. Mọi hệ thống có chức
   năng "cấp lại mật khẩu" đều mang khả năng này — thứ phân biệt một hệ thống
   dùng được với một hệ thống không dùng được là nó có **truy ra ai làm** không.

   Không có cột này thì `MaKichHoat` chỉ nói "có một mã được cấp lúc 14:02",
   không nói ai bấm. Khi một sinh viên khiếu nại điểm bị sửa, đó là khác biệt
   giữa trả lời được và không.

   NULL được, vì hai lý do chính đáng:
   - dữ liệu cũ (mã cấp trước migration này) không suy ra được người cấp
   - `POST /api/auth/activate/resend` là người dùng TỰ xin, không có admin nào

   Không FK sang `TaiKhoan`: bản ghi kiểm toán phải sống sót kể cả khi tài
   khoản admin bị xoá, và Admin Master nằm ở bảng `TaiKhoanMaster` riêng. */

SET XACT_ABORT ON;
GO

ALTER TABLE dbo.MaKichHoat ADD NguoiCap varchar(50) NULL;
GO
