# Migration — T2 phụ trách, T5 tích hợp

Thư mục này hiện **chưa có migration nghiệp vụ**. T2 thêm file thật theo mẫu
`V1__central_schema.sql`, `V2__central_constraints.sql`, rồi tăng phiên bản.
Không tạo V1 rỗng để đánh dấu đã dựng schema.

- Đây là nguồn SQL duy nhất. T5 đóng gói các file `*.sql` vào `db/migration`
  của API bằng Maven; không chép tay thành bản thứ hai trong `apps/api`.
- Dùng T-SQL tương thích SQL Server 2019. Không có `USE`, `CREATE DATABASE`,
  biến SQLCMD `$(...)`, `:r` hoặc `:ON ERROR EXIT` trong migration Flyway.
  Có thể dùng `GO` để tách batch. Kiểm tra DB đích CENTRAL trước DDL.
- Flyway quản lý phiên bản/checksum và transaction của migration phù hợp.
  Không bọc thêm transaction lồng khi không cần. Migration đã áp dụng lên
  DB chung thì không sửa/xóa; thêm migration mới.
- Tham khảo `db/master/` và `db/site/`, rồi hợp nhất theo Phần 1. Không gọi
  nguyên script phân tán; không mang theo phụ thuộc snapshot/mirror/outbox.
- Giữ mã định danh và mã cơ sở. Chốt với T5 FK trong một DB, trạng thái,
  công thức điểm, ngưỡng đạt và quy tắc tài khoản trước khi tạo fixture.
- Seed demo đặt ở `../seed/`, không tự chạy mỗi lần mở backend.

Trước khi T5 tích hợp Flyway, có thể thử bản nháp bằng SSMS trên CENTRAL
cá nhân và lưu ghi chú. DB tích hợp phải chạy qua công cụ migration đã chốt.
DB cá nhân đã chạy DDL thủ công nên chuyển sang một DB CENTRAL thử nghiệm
mới khi kiểm Flyway; không bật `baseline-on-migrate` để che schema chưa đối soát.

Nghiệm thu: dựng trên DB trống; migrate lần hai không áp dụng lại phiên bản
cũ; thêm phiên bản mới giữ dữ liệu; ca sai FK/CHECK/UNIQUE bị từ chối; lỗi
trong transaction rollback đúng. Bổ sung ca parse vào `db/tests/Test-Scripts.ps1`
cho mỗi file SQL mới và kiểm chứng riêng trên SQL Server.
