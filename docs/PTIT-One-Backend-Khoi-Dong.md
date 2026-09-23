# PTIT One khung backend và nhánh tiếp theo

Quyết định ngày 24/09/2026: triển khai Phần 1 UI/UX và chức năng cơ bản trên
một database tập trung trước; Phần 2 CSDL phân tán lập kế hoạch sau. Khung
backend được dựng sớm theo yêu cầu này, không chờ cổng G3 trong lịch cũ.

## Phạm vi nhánh feature/api-skeleton

- Dùng frontend đã có ở `fe-ptitone/`, không chuyển vị trí hoặc đổi thiết kế UI.
- Thêm `apps/api` với Spring Boot, JDK 21, Maven Wrapper và package theo J1.
- Có endpoint liveness `/api/health`, kiểm thử HTTP và Vite proxy `/api`.
- Chưa có nghiệp vụ, xác thực hoặc kết nối DB; các màn frontend vẫn dùng dữ liệu mẫu.
- Các script/DB Master và site hiện có không bị chạy hoặc chỉnh trong bước này.

Nhánh dựng trên commit `33b7016` của `FrontEnd` sau khi fetch. `dev` tại thời điểm
bắt đầu còn ở `f480b7d`, nên so sánh nhánh này với `dev` sẽ thấy cả lịch sử
frontend và các commit nền mà `dev` chưa nhận. Phần backend mới được giới hạn
trong commit `feat: them khung backend Spring Boot va proxy API`.

Khi review, dùng `FrontEnd...feature/api-skeleton` để xem phần backend bổ sung.
Người quản lý repo quyết định nhập nhánh frontend trước hay nhận toàn bộ qua
PR nhánh này vào `dev`; không cherry-pick lại những commit đã có sau khi merge.

## Nhánh nên làm tiếp

Mỗi nhánh là một task, tạo từ `dev` đã được cập nhật và đi qua PR theo README.
Danh sách này là đề xuất, chưa tạo các nhánh trống trên remote.

| Nhánh | Người chính | Đầu ra và phụ thuộc |
|---|---|---|
| `feature/db-central-schema` | TV2 | Schema/migration/seed cho `PTITONE_CENTRAL`; kế thừa nghiệp vụ trong thiết kế, chưa chạy replication |
| `feature/api-central-datasource` | TV5 | Sau schema: JDBC, SQL Server driver, migration và cấu hình một DataSource; kiểm transaction/rollback |
| `feature/api-auth` | TV5 | Sau DB và contract tài khoản: xác thực, phiên, role và quyền theo bản ghi |
| `feature/web-auth` | TV6 | Nối màn đăng nhập với contract auth; UI có thể làm trước bằng mock có nhãn |
| `feature/api-prerequisites` | TV5, TV2 hỗ trợ SQL | Sau auth/schema: API môn và tiên quyết, kiểm chu trình, quyền Admin |
| `feature/web-student-pages` | TV6 | Lịch và bảng điểm theo contract; nghiệm thu bằng API thật khi API sẵn sàng |

Không tạo nhánh dài hạn tên `Backend` để gom mọi tính năng. Nhánh `FrontEnd`
hiện tại được giữ nguyên; các task frontend mới cũng dùng `feature/<module>-<task>`.
Không push thẳng `dev` hoặc `main`, không force-push nhánh người khác.

## Chạy và bàn giao

Backend: [apps/api/README.md](../apps/api/README.md).
Frontend: [fe-ptitone/README.md](../fe-ptitone/README.md).

Chạy backend 8080 và Vite 5173; kiểm `/api/health` trực tiếp và qua Vite.
`mvnw.cmd verify` kiểm khởi động/HTTP; `npm run build` kiểm frontend.
Build skeleton thành công chưa đồng nghĩa đăng nhập, DB hoặc toàn bộ Phần 1 đã hoàn tất.
