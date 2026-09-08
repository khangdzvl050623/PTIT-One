<!-- File hướng dẫn DÙNG CHUNG cho mọi coding agent.
     Claude Code đọc qua CLAUDE.md (import @AGENTS.md).
     Codex và các agent khác đọc trực tiếp file này.
     Sửa ở ĐÂY, không sửa CLAUDE.md. -->

# UISPTITv2 — Quản lý đăng ký tín chỉ đa cơ sở

Đồ án **Cơ sở dữ liệu phân tán**. SQL Server, nhiều cơ sở, phân mảnh ngang +
nhân bản một chiều + giao dịch phân tán + truy vấn phân tán.

**Thiết kế đã CHỐT.** Nguồn sự thật duy nhất: `docs/UISPTITv2-Thiet-Ke-v2.md`.
Trước khi sinh code, đọc mục **0.1b** (năm yêu cầu bắt buộc) và **0.1**
(bảng quyết định D1–D16). Đề xuất khác thiết kế thì nêu ra để nhóm quyết,
đừng tự đổi.

## Ràng buộc tầng CSDL — sai là hỏng bài

| Quy tắc | Chi tiết |
|---|---|
| **Vị từ phân mảnh dẫn xuất** | `DangKyHocPhan ⋉ LopHocPhan` — **KHÔNG** phải `⋉ SinhVien`. `Diem` dẫn xuất bậc 2 qua `DangKyHocPhan`. Đây là lỗi đã từng mắc, đừng lặp lại |
| **Trigger ở Subscriber** | **BẮT BUỘC** `CREATE TRIGGER … NOT FOR REPLICATION`. Thiếu nó thì trigger chặn chính Distribution Agent và replication chết với triệu chứng không liên quan |
| **Chống vượt sức chứa** | `UPDATE LopHocPhan SET SoLuongDaDangKy = SoLuongDaDangKy + 1 WHERE MaLopHP = … AND SoLuongDaDangKy < SoLuongToiDa;` rồi kiểm `@@ROWCOUNT`. **Cấm** `SELECT COUNT` rồi `IF` — đó là race condition |
| **Chống vượt trần tín chỉ** | Cùng kỹ thuật, nhưng ở **Home**, trên `SinhVien.SoTinChiDangKyKy`. Cộng ngay khi tạo yêu cầu `CHO_DUYET`, trả lại khi bị từ chối |
| **Bộ đếm do ứng dụng sở hữu** | Không trigger nào được cập nhật `SoLuongDaDangKy` — nếu không sẽ nhảy 2 mỗi lần đăng ký |
| **Thứ tự khóa** | Luôn `LopHocPhan` trước, `DangKyHocPhan` sau — ở **mọi** luồng, kể cả hủy đăng ký. Đảo ở một chỗ là sinh deadlock ngẫu nhiên |
| **Không dùng `MERGE`** | Dùng `UPDATE` trước, `INSERT` sau, có `UPDLOCK, HOLDLOCK`. `MERGE` của SQL Server không tự lấy khóa phù hợp |
| **Không dùng `IDENTITY`** | Chọn khóa **theo từng aggregate** (mục C9). Nhúng mã cơ sở vào khóa **chỉ khi** cơ sở là một phần ngữ nghĩa của thực thể — `LopHocPhan` thì đúng, `SinhVien` thì không |
| **Giao dịch phân tán** | `SET XACT_ABORT ON` là bắt buộc. Chỉ dùng cho **chuyển cơ sở sinh viên**, tuyệt đối không cho đăng ký học phần |
| **Subscriber chỉ đọc** | `DENY INSERT/UPDATE/DELETE` trên bảng nhân bản. `DENY` là lớp chính, trigger là lớp phụ |

## Ràng buộc tầng ứng dụng

| Quy tắc | Chi tiết |
|---|---|
| **Một giao dịch = một site** | `@Transactional` **không bao giờ** trải hai DataSource. `AbstractRoutingDataSource` phân giải khóa một lần; đổi site giữa chừng sẽ ghi nhầm site hoặc mất tính nguyên tử **mà không ném lỗi**. Ghép nhiều site bằng saga, không bằng transaction |
| **Cơ sở lấy từ JWT đã ký** | **Tuyệt đối không** tin tham số client gửi lên (`?campus=HN`). Đó là lỗ hổng leo thang đặc quyền |
| **JdbcTemplate cho đường nóng** | Đăng ký, truy vấn chéo site, benchmark — cần thấy chính xác SQL và đọc `@@ROWCOUNT`. JPA chỉ dùng cho CRUD danh mục nếu thật sự cần |
| **Đúng 3 port, không hơn** | `CrossSiteQuery`, `GlobalReport`, `CatalogHealth`. `SiteContext`, `RoutingDataSource`, `OutboxWorker` là class cụ thể, không phải interface |
| **`initialization-fail-timeout: -1`** | Bắt buộc, để ứng dụng vẫn khởi động khi một site đang tắt. Thiếu nó là hỏng kịch bản demo tắt site |
| **Thứ tự Outbox** | Upsert vào mirror **TRƯỚC**, đánh dấu `SENT` **SAU**. Đảo thứ tự là mất sự kiện vĩnh viễn |
| **Outbox chỉ cho sinh viên khách** | Sinh viên có cơ sở nhà trùng site thì điểm đã nằm đúng chỗ, không phát sự kiện |

## Quy ước đặt tên

- **Bảng và cột: tiếng Việt không dấu** (`SinhVien`, `MaCoSoNha`) — giảng viên đọc lược đồ
- **Code, interface, biến: tiếng Anh** (`CrossSiteQuery`, `SiteContext`)
- **Read model mang hậu tố `Mirror`** (`BangDiemMirror`) — nhìn tên là biết không phải nguồn sự thật
- Commit theo Conventional Commits rút gọn, có thêm loại `db:` — xem README

## Không được làm

Microservices · Kafka/RabbitMQ · Redis · Kubernetes · nhân bản hai chiều hoặc
merge replication · 2PC cho đăng ký học phần · phân mảnh dọc · port thứ tư ·
frontend nặng (shadcn, state library, router phức tạp) · thêm bảng CRUD không
phục vụ một khái niệm phân tán nào.

## Thứ tự ưu tiên

Phần **in đậm** trong tài liệu thiết kế là bắt buộc theo đề bài (~75% điểm).
Phần ➕ chỉ làm sau khi phần bắt buộc đã xong và đã chụp đủ screenshot.
**Cổng chặn cuối tuần 4.** Không viết code ứng dụng trước khi cài đặt vật lý
đã PASS.

## Git

`feature/* → dev → main`, không push thẳng. `dev` cần PR nhưng 0 approval;
`main` cần 1 approval của CODEOWNERS. Chi tiết ở README.
