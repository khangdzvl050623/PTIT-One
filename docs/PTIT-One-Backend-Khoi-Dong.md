# PTIT One — trạng thái backend và nhánh tiếp theo

Quyết định ngày 24/09/2026 (D20): làm **Phần 1** — UI/UX và nghiệp vụ trên
**một database tập trung** — trước; Phần 2 CSDL phân tán lập kế hoạch sau.

Cách cài và chạy: [hướng dẫn cài Phần 1](PTIT-One-Cai-Dat-Phan-1.md).
Phân công và nghiệm thu: [kế hoạch Phần 1](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md).

## Trạng thái thật — cập nhật 25/09/2026

**Đã chạy được:**

- `apps/api` Spring Boot 4.1.1, JDK 21, Maven Wrapper; chia module `auth`,
  `student`, `course`, `enrollment`, `grade`, `timetable`, `health`, `shared`.
- `GET /api/health` — liveness, không nói gì về DB.
- **Đã nối database.** Profile `central` cấu hình một DataSource;
  `GET /api/health/db` trả về database và login thật mà API đang dùng.
  Profile mặc định **không** nối DB nên máy chưa cài SQL Server vẫn chạy được API.
- `scripts/dev-api.ps1` nạp `apps/api/.env` vào môi trường tiến trình.
- **Xác thực A0 xong.** Đăng nhập, `/me`, refresh có rotation và phát hiện
  replay, logout / logout-all, kiểm `sid` + `accountVersion` mỗi request,
  phân quyền theo vai trò. Web đã nối, có chặn tuyến theo vai trò.
- **Schema CENTRAL xong.** `V1` lát cắt auth, `V2` học vụ (danh mục + vận hành),
  kèm seed fixture và test ràng buộc.

**Chưa có:**

- Mọi API nghiệp vụ. Sáu module nghiệp vụ hiện chỉ có `package-info.java`
  (trừ `auth`); gọi thử endpoint nghiệp vụ trả 404 là đúng.
- F02 cấp và kích hoạt tài khoản, A1 quên mật khẩu qua email — đang hoãn.
  Chưa có bảng mã một lần, chưa có cột email, chưa có endpoint `activate`.

## Nhánh nên làm tiếp

Mỗi nhánh là một task, tạo từ `dev` đã cập nhật, đi qua PR theo README.
Không push thẳng `dev`/`main`, không force-push nhánh người khác.

| Nhánh | Người | Trạng thái và phụ thuộc |
|---|---|---|
| `feature/api-central-datasource` | TV5 | ✅ **Xong** — JDBC, một DataSource, `/api/health/db` |
| `feature/db-central-schema` | TV2 | ⏳ **Đang chặn nhiều việc nhất.** Schema/migration/seed cho `PTITONE_CENTRAL` |
| `feature/api-auth` | TV5 | Cần schema + contract F01 đã chốt với TV4 |
| `feature/web-auth` | TV6 | Nối màn đăng nhập với contract auth; UI làm trước bằng mock có nhãn |
| `feature/api-prerequisites` | TV5, TV2 hỗ trợ SQL | Sau auth/schema: API môn và tiên quyết, kiểm chu trình, quyền Admin |
| `feature/web-student-pages` | TV6 | Lịch và bảng điểm theo contract; nghiệm thu bằng API thật |

Không tạo nhánh dài hạn tên `Backend` gom mọi tính năng. Nhánh `FrontEnd`
giữ nguyên để tham chiếu; task frontend mới cũng dùng `feature/<module>-<task>`.

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
