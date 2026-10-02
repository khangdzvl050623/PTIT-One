# PTIT One — trạng thái backend và nhánh tiếp theo

Quyết định ngày 24/09/2026 (D20): làm **Phần 1** — UI/UX và nghiệp vụ trên
**một database tập trung** — trước; Phần 2 CSDL phân tán lập kế hoạch sau.

Cách cài và chạy: [hướng dẫn cài Phần 1](PTIT-One-Cai-Dat-Phan-1.md).
Phân công và nghiệm thu: [kế hoạch Phần 1](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md).

## Trạng thái thật — cập nhật 02/10/2026

**Đã chạy được** (chi tiết endpoint, quyền, mã lỗi: [API Contract](PTIT-One-API-Contract.md)):

- `apps/api` Spring Boot 4.1.1, JDK 21; profile `central` nối một DataSource tới
  `PTITONE_CENTRAL`, profile mặc định không nối DB.
- Schema `V1`–`V5` chạy tự động bằng Flyway lúc API khởi động — cách chạy ở
  [hướng dẫn cài Phần 1](PTIT-One-Cai-Dat-Phan-1.md#migration-và-seed).
- **A0** xác thực: đăng nhập, refresh có rotation, logout/logout-all, phân quyền.
- **F02** Admin Master cấp hồ sơ SV/GV kèm tài khoản, kích hoạt bằng mã một lần,
  cấp lại mã, khoá/mở tài khoản. Cấp tài khoản chỉ ở Master (chốt 02/10/2026).
- **F03** môn học, tiên quyết (chặn chu trình), khoa, học kỳ, chương trình đào tạo.
- **F04** lớp học phần, phân công GV, lịch học (chặn trùng GV/phòng), đợt đăng ký, huỷ lớp.
- **F05** GV xem lớp phụ trách, danh sách SV và sĩ số; lịch dạy GV.
- **F06** nhập điểm (kiểm phiên bản), công bố, khoá điểm.
- **F07** SV xem bảng điểm; **F09** SV xem thời khoá biểu.
- **F08** đăng ký và huỷ học phần, có test tương tranh 30 chỗ / 100 SV.
- Thống kê (module `report`) và thông báo soạn tay + tự sinh (module `notification`).
- `openapi.json` sinh từ code; `OpenApiContractTest` làm build đỏ nếu lệch.

**Chưa có:**

- **A1** quên mật khẩu, giới hạn tần suất.
- Thông báo đổi lịch/phòng và nhắc đợt đăng ký sắp đóng.

## Quy ước nhánh

Mỗi task một nhánh `feature/<module>-<task>` tạo từ `dev`, đi qua PR theo
README. Không push thẳng `dev`/`main`, không force-push nhánh người khác.

## Cập nhật máy đang dùng thư mục frontend cũ

Ngày 24/09/2026 nhóm chuyển `fe-ptitone/` sang `apps/web/` (D21). Việc này
**đã xong trên `dev`**; máy clone mới không phải làm gì.

Máy nào còn nhánh riêng tạo từ trước mốc đó: `git fetch origin` rồi
`git merge origin/dev`, sau đó chạy `npm ci` tại `apps/web` và sửa lại run
configuration nếu còn trỏ thư mục cũ. `node_modules/` và `dist/` không do Git
quản lý nên có thể còn sót ở vị trí cũ, xóa được.

## Liên quan

[apps/api](../apps/api/README.md) · [apps/web](../apps/web/README.md) ·
[db/central](../db/central/README.md)
