# PTIT One — API Contract

Cập nhật 02/10/2026.

## Hai nguồn, không chép lẫn nhau

| Hỏi gì | Xem đâu |
|---|---|
| Có endpoint nào, nhận gì, trả gì, giới hạn độ dài/khoảng giá trị | **OpenAPI** — `apps/api/openapi.json`, xem ở `/swagger-ui.html` |
| Vai trò nào gọi được, mã lỗi nghĩa gì, quy tắc nghiệp vụ | **File này** |

OpenAPI được **sinh từ controller và DTO**, không viết tay — thêm endpoint là
spec tự có. `openapi.json` được commit và `OpenApiContractTest` làm build đỏ nếu
file lệch với code, nên thay đổi contract hiện ra trong diff của PR.

Sinh lại sau khi đổi API:

```bash
cd apps/api && ./mvnw test -Dtest=OpenApiContractTest -Dptitone.openapi.write=true
```

**Ba thứ springdoc không sinh được**, nên chúng ở đây và phải sửa tay: quyền theo
vai trò (phần lớn kiểm trong service, không phải `@PreAuthorize`), 36 mã lỗi (ném
từ service qua `ApiException` — spec chỉ có `200`/`201`), và quy tắc nghiệp vụ.

---

## 1. Quy ước chung

**Đường dẫn tiếng Anh, trường JSON tiếng Việt không dấu.** Trường JSON trùng tên
cột DB (`maMonHoc`, `soLuongToiDa`) — đúng quy ước `AGENTS.md`.

**Thời gian** ISO-8601 UTC (`2026-09-01T00:00:00Z`); `ngayBatDau`/`ngayKetThuc`
của học kỳ là ngày trần (`2026-09-01`).

### Xác thực

Không có `Authorization` header. Hai cookie `HttpOnly` do server đặt:

| Cookie | Path | Sống |
|---|---|---|
| `PTITONE_AT` | `/api` | 15 phút |
| `PTITONE_RT` | `/api/auth` | 7 ngày (hạn tuyệt đối từ lúc đăng nhập) |

Frontend không đọc được hai cookie này và cũng không cần — chỉ cần
`credentials: 'include'`.

### CSRF

Mọi request **ghi** phải có header `X-XSRF-TOKEN` lấy từ cookie `XSRF-TOKEN`
(cookie này JS đọc được). Chưa có cookie thì gọi `GET /api/auth/csrf` trước.

Server **đổi token CSRF khi đăng nhập** → đọc lại cookie ở mỗi request thay vì
nhớ giá trị trong bộ nhớ.

Swagger UI đã bật `springdoc.swagger-ui.csrf` nên Try-it-out gọi được cả `POST`
và `PUT`. Bỏ ba dòng cấu hình đó thì mọi lệnh ghi trong Swagger UI trả `403`.

### Hình dạng lỗi

```json
{
  "code": "CLASS_CAPACITY_BELOW_ENROLLED",
  "message": "Không hạ được sức chứa xuống 1: lớp đang có 2 sinh viên.",
  "fieldErrors": { "soLuongToiDa": "Sức chứa tối thiểu là 1." },
  "traceId": "25a818cf-f6c6-4aa7-b112-a4a5314f977a"
}
```

`fieldErrors` chỉ có ở lỗi validation. `traceId` cũng nằm trong header
`X-Trace-Id` của **mọi** response.

**Rẽ nhánh theo `code`, không theo `message`** — message là câu hiển thị cho
người dùng và đổi được bất cứ lúc nào.

---

## 2. Quyền theo vai trò (B3)

`—` = chỉ cần đăng nhập. Vai trò ghi ra là **bắt buộc**.

| Method | Path | Quyền |
|---|---|---|
| GET | `/api/health` | public |
| GET | `/api/health/db` | `ADMIN_CO_SO` · `ADMIN_MASTER` |
| GET | `/api/auth/csrf` | public |
| POST | `/api/auth/login` · `refresh` · `logout` | public *(dùng cookie)* |
| POST | `/api/auth/logout-all` | — |
| GET | `/api/auth/me` | — |
| GET | `/api/courses` · `/api/courses/{maMonHoc}` | — |
| POST · PUT | `/api/courses` · `/{maMonHoc}` · `/{maMonHoc}/prerequisites` | `ADMIN_MASTER` |
| GET | `/api/faculties` · `/api/terms` · `/api/teachers` | — |
| GET | `/api/classes` · `/api/classes/{maLopHP}` | — |
| POST · PUT | `/api/classes` · `/{maLopHP}` · `/{maLopHP}/teacher` | `ADMIN_CO_SO` |
| GET | `/api/classes/{maLopHP}/schedule` | — |
| PUT | `/api/classes/{maLopHP}/schedule` | `ADMIN_CO_SO` |
| GET | `/api/enrollment-periods` | — |
| POST · PUT | `/api/enrollment-periods` · `/{maDot}` | `ADMIN_CO_SO` |

### Hai quy tắc phạm vi

**Không endpoint nào nhận tham số `maCoSo`.** Cơ sở luôn lấy từ JWT đã ký; gửi
lên cũng bị bỏ qua. Đây là chủ ý chống leo thang đặc quyền, không phải thiếu sót.

**`ADMIN_MASTER` chỉ ĐỌC lớp học phần và đợt đăng ký.** Tạo lớp là việc của
`ADMIN_CO_SO` và chỉ trong cơ sở của mình → UI đừng hiện nút "Tạo lớp" cho Admin
Master. Ngược lại, danh mục môn học chỉ `ADMIN_MASTER` ghi được.

---

## 3. Quy tắc nghiệp vụ

Phần OpenAPI không nói được — đây là lý do một request hợp lệ về hình dạng vẫn
bị từ chối.

### Sức chứa

- Hạ `soLuongToiDa` xuống **dưới** sĩ số hiện tại → `409`. Xuống **đúng bằng**
  sĩ số thì được.
- Phép kiểm nằm **trong câu `UPDATE`** rồi đọc `@@ROWCOUNT`, không `SELECT` rồi
  `IF` — hai request song song đều qua được bước `SELECT` trước khi ai kịp ghi.
- Bộ đếm `SoLuongDaDangKy` do **ứng dụng** sở hữu; không trigger nào được cộng
  nó, nếu không mỗi lần đăng ký sẽ nhảy 2.

### Lịch học

- `PUT .../schedule` **thay toàn bộ**; danh sách rỗng = xoá hết lịch.
- Lớp **đã có sinh viên đăng ký** thì khoá lịch (`409`). Đổi lịch sau khi sinh
  viên đã xếp thời khóa biểu là đổi cam kết mà hệ thống chưa có cách báo lại.
- Chặn ba loại trùng: **giảng viên**, **phòng** (so sau khi cắt khoảng trắng và
  bỏ phân biệt hoa thường), và **chính lớp đó tự trùng giờ với mình**.
- `sp_getapplock` theo `MaHocKy` lấy **trước** mọi phép kiểm trùng. Khoá sau khi
  kiểm là quá muộn: hai admin sửa hai lớp khác nhau cùng qua bước kiểm rồi cùng
  ghi, sinh ra trùng lịch thật.
- `thu`: 2 = thứ Hai … 8 = Chủ nhật. Tuần tính từ `HocKy.NgayBatDau` — frontend
  cần `GET /api/terms` để quy đổi tuần thành ngày thật.
- `tietBatDau + soTiet - 1 ≤ 12` là ràng buộc **riêng**, không suy ra được từ
  giới hạn từng trường: tiết 11 + 4 tiết hợp lệ từng trường nhưng tổng vượt
  ngày. Lỗi này trả `SCHEDULE_SLOT_INVALID`, không phải `VALIDATION_ERROR`.

### Phân công giảng viên

- Giảng viên phải **thuộc cùng cơ sở** với lớp.
- Không được đã dạy lớp khác **trùng khung giờ**. Có hai đường vào cùng một ràng
  buộc: gán GV cho lớp đã có lịch, và sửa lịch của lớp đã có GV — **cả hai đều
  phải chặn**, thiếu một đường là lọt.
- `maGiangVien` để `null` hoặc rỗng = gỡ phân công.

### Môn học và tiên quyết

- `PUT .../prerequisites` thay **toàn bộ** tập; rỗng = gỡ hết, là thao tác hợp lệ.
- Nhiều tiên quyết nghĩa là phải đạt **tất cả**; điều kiện "hoặc" chưa có.
- Chặn chu trình **dài**, không chỉ môn tự trỏ chính nó: `A → B → C → A` cũng bị
  bắt. Lỗi thì **giữ nguyên tập cũ**.
- Không đổi được tiên quyết khi môn có lớp trong học kỳ **đang mở đợt đăng ký** —
  sinh viên đã đăng ký theo điều kiện cũ.
- `GET /api/courses/{maMonHoc}` trả **cả hai chiều**: `tienQuyet` (phải đạt
  trước) và `monPhuThuoc` (môn đang cần môn này). Chiều sau cần cho màn quản
  trị, vì sửa môn này ảnh hưởng tới chúng.

### Lớp học phần

- **Không gửi `maLopHP` và `maCoSo`.** Server sinh mã từ môn + kỳ + cơ sở trong
  JWT: `BAS1203-2026-1-HCM01`. Lớp mới luôn ở `DU_KIEN`.
- Môn, kỳ, cơ sở **không đổi được** — cả ba nằm trong mã lớp.
- `choPhepLienCoSo: true` chỉ hợp lệ khi `hinhThucHoc = "TRUC_TUYEN"` (D18):
  kiểm "không trùng tiết" là vô nghĩa khi hai điểm cách nhau 1.700 km.
- Từ vựng: `trangThai` ∈ `DU_KIEN` · `MO` · `DA_KHOA` · `DA_HUY`;
  `hinhThucHoc` ∈ `TRUC_TIEP` · `TRUC_TUYEN` · `KET_HOP`.

### Đợt đăng ký

- **"Đang mở" cần CẢ HAI**: `trangThai = "DANG_MO"` **và** thời điểm hiện tại
  trong `[thoiGianMo, thoiGianDong]`. Chỉ xem trạng thái là bỏ sót đợt đã hết giờ
  mà quên đóng.
- Một cơ sở chỉ được **một** đợt `DANG_MO` mỗi học kỳ, chặn bằng unique filtered
  index ở DB (`V3`) chứ không chỉ ở code. Muốn mở đợt bổ sung thì đóng đợt cũ.
- `trangThai` ∈ `CHUA_MO` · `DANG_MO` · `DA_DONG`. Mã đợt do server sinh.

### Phiên đăng nhập

- `401 AUTH_INVALID_CREDENTIALS` cho **mọi** lý do từ chối: sai mật khẩu, chưa
  kích hoạt, bị ngừng. Thông báo chung là cố ý, để form đăng nhập không thành
  công cụ dò tài khoản nào có thật. **Đừng suy đoán thêm lý do trên UI.**
- **Chỉ một lượt refresh tại một thời điểm, kể cả giữa nhiều tab.** Trình lại
  refresh token đã rotate bị coi là replay và server **thu hồi cả phiên** — token
  mới nhất cũng chết theo. `apps/web` xử lý bằng Web Locks
  ([refreshCoordinator.ts](../apps/web/src/features/auth/model/refreshCoordinator.ts));
  client khác phải tự lo.
- `logout` thu hồi phiên hiện tại; `logout-all` thu hồi **mọi phiên** và tăng
  phiên bản tài khoản — có hiệu lực ngay ở request kế tiếp của mọi thiết bị.

---

## 4. Ba điểm lệch đã biết, chưa sửa

1. **`COURSE_NOT_FOUND` trả hai HTTP status**: `404` khi mở môn không tồn tại,
   `400` khi tham chiếu môn không tồn tại lúc tạo lớp. Rẽ nhánh theo cặp
   `(status, code)`, đừng chỉ theo `code`.
2. **`PUT /api/enrollment-periods/{maDot}` bỏ qua `maHocKy`** trong thân request.
   Gửi hay không đều như nhau; muốn đổi kỳ thì tạo đợt mới.
3. **`GET /api/classes` lọc cứng theo cơ sở, `GET` chi tiết thì không.** Sinh
   viên HN không thấy lớp trực tuyến liên cơ sở của HCM trong danh sách, dù đọc
   được chi tiết nếu biết mã. Chưa sai vì chưa có đăng ký liên cơ sở — **F08 sẽ
   phải mở đường tìm lớp**, và đó là chỗ sửa, không phải sửa ở chi tiết lớp.

---

## 5. Mã lỗi

| Code | HTTP | Khi nào |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Dữ liệu không hợp lệ; xem `fieldErrors` |
| `AUTH_INVALID_CREDENTIALS` | 401 | Sai thông tin đăng nhập, hoặc tài khoản không được vào |
| `AUTH_SESSION_INVALID` | 401 | Thiếu/hết hạn/bị thu hồi phiên |
| `AUTH_REFRESH_INVALID` | 401 | Refresh không hợp lệ, hết hạn, hoặc replay |
| `AUTH_FORBIDDEN` | 403 | Đã đăng nhập nhưng sai vai trò hoặc sai cơ sở |
| `CSRF_INVALID` | 403 | Thiếu hoặc sai header `X-XSRF-TOKEN` |
| `SERVICE_UNAVAILABLE` | 503 | Lỗi DB; không trả chi tiết SQL ra ngoài |
| `COURSE_NOT_FOUND` | 404 / 400 | Xem mục 4 |
| `COURSE_DUPLICATE` | 409 | Mã môn đã tồn tại |
| `COURSE_REGISTRATION_OPEN` | 409 | Đổi tiên quyết khi môn có lớp trong kỳ đang mở đợt |
| `FACULTY_UNKNOWN` | 400 | Mã khoa không có |
| `TERM_NOT_FOUND` | 400 | Mã học kỳ không có |
| `PREREQUISITE_SELF` | 400 | Môn tự làm tiên quyết của chính nó |
| `PREREQUISITE_UNKNOWN` | 400 | Môn tiên quyết không tồn tại |
| `PREREQUISITE_CYCLE` | 409 | Tạo thành chu trình |
| `CATALOG_BUSY` | 503 | Người khác đang sửa đồ thị tiên quyết |
| `CLASS_NOT_FOUND` | 404 | Không có lớp |
| `CLASS_MODE_INVALID` | 400 | `hinhThucHoc` sai |
| `CLASS_STATUS_INVALID` | 400 | `trangThai` sai |
| `CROSS_CAMPUS_REQUIRES_ONLINE` | 400 | Liên cơ sở mà không phải lớp trực tuyến |
| `CLASS_CAPACITY_BELOW_ENROLLED` | 409 | Hạ sức chứa xuống dưới sĩ số |
| `CLASS_CODE_RACE` | 409 | Hai người cùng tạo lớp; thử lại |
| `CLASS_HAS_ENROLLMENTS` | 409 | Sửa lịch lớp đã có sinh viên |
| `TEACHER_NOT_FOUND` | 400 | Không có giảng viên |
| `TEACHER_WRONG_CAMPUS` | 400 | Giảng viên khác cơ sở với lớp |
| `TEACHER_SCHEDULE_CLASH` | 409 | Giảng viên đã dạy lớp khác trùng giờ |
| `SCHEDULE_SLOT_INVALID` | 400 | Vượt tiết 12, hoặc tuần bắt đầu sau tuần kết thúc |
| `SCHEDULE_SELF_OVERLAP` | 400 | Hai buổi của cùng lớp chồng nhau |
| `SCHEDULE_TEACHER_CLASH` | 409 | Lịch trùng giảng viên |
| `SCHEDULE_ROOM_CLASH` | 409 | Lịch trùng phòng |
| `TIMETABLE_BUSY` | 503 | Người khác đang sửa lịch cùng học kỳ |
| `PERIOD_NOT_FOUND` | 404 | Không có đợt đăng ký |
| `PERIOD_STATUS_INVALID` | 400 | `trangThai` đợt sai |
| `PERIOD_WINDOW_INVALID` | 400 | Thời gian mở không trước thời gian đóng |
| `PERIOD_ALREADY_OPEN` | 409 | Cơ sở đã có đợt mở trong học kỳ đó |

`429 AUTH_TOO_MANY_ATTEMPTS` **chưa có** — giới hạn tần suất thuộc A1.

---

## 6. Kịch bản kiểm thử

Mỗi dòng là một ca phải xanh. **✓ = đã có test tự động; ✗ = chưa có.** Cột này
được đối chiếu với source test, không ghi theo cảm giác.

| Kịch bản | Mong đợi | |
|---|---|---|
| Hạ sức chứa xuống dưới sĩ số (lớp 3/2 → 1) | `409 CLASS_CAPACITY_BELOW_ENROLLED` | ✓ |
| Hạ sức chứa xuống **đúng bằng** sĩ số (3/2 → 2) | `200` | ✓ |
| Sửa lịch lớp đã có sinh viên | `409 CLASS_HAS_ENROLLMENTS` | ✓ |
| Gán GV đã dạy lớp khác trùng giờ | `409 TEACHER_SCHEDULE_CLASH` | ✓ |
| Đặt lịch trùng giờ GV đang dạy lớp khác | `409 SCHEDULE_TEACHER_CLASH` | **✗** |
| Đặt lịch trùng phòng, khác hoa thường (`a2-201` vs `A2-201`) | `409 SCHEDULE_ROOM_CLASH` | ✓ |
| Hai buổi của cùng lớp chồng nhau | `400 SCHEDULE_SELF_OVERLAP` | ✓ |
| Tiết 11 kéo 4 tiết | `400 SCHEDULE_SLOT_INVALID` | **✗** |
| Lịch rỗng | `200`, xoá hết lịch | **✗** |
| Chu trình tiên quyết dài `A → B → C → A` | `409 PREREQUISITE_CYCLE`, tập cũ còn nguyên | ✓ |
| Môn tự làm tiên quyết của chính nó | `400 PREREQUISITE_SELF` | ✓ |
| Tiên quyết không tồn tại | `400 PREREQUISITE_UNKNOWN` | ✓ |
| Đổi tiên quyết khi kỳ đang mở đợt | `409 COURSE_REGISTRATION_OPEN` | ✓ |
| `choPhepLienCoSo` + `TRUC_TIEP` | `400 CROSS_CAMPUS_REQUIRES_ONLINE` | ✓ |
| Gán GV khác cơ sở | `400 TEACHER_WRONG_CAMPUS` | ✓ |
| `ADMIN_CO_SO` sửa lớp của cơ sở khác | `403 AUTH_FORBIDDEN` | ✓ |
| `ADMIN_MASTER` tạo lớp | `403 AUTH_FORBIDDEN` | ✓ |
| `ADMIN_CO_SO` ghi danh mục môn học | `403 AUTH_FORBIDDEN` | ✓ |
| Tạo lớp thì mã do server sinh, cơ sở từ JWT | mã mang đúng cơ sở của admin | ✓ |
| Mở đợt thứ hai cùng cơ sở + kỳ | `409 PERIOD_ALREADY_OPEN` | ✓ |
| `thoiGianMo` không trước `thoiGianDong` | `400 PERIOD_WINDOW_INVALID` | ✓ |
| Đợt `DANG_MO` nhưng đã quá `thoiGianDong` | không tính là đang mở | **✗** |
| `POST` thiếu header `X-XSRF-TOKEN` | `403`, chặn trước khi vào controller | ✓ |
| Dùng lại refresh token đã rotate | `401 AUTH_REFRESH_INVALID`, **cả phiên bị thu hồi** | ✓ |
| `logout-all` rồi gọi API bằng access cũ | `401 AUTH_SESSION_INVALID` ngay | ✓ |
| Chữ ký đúng nhưng phiên đã chết | `401`, không cho qua | ✓ |
| Tài khoản chưa kích hoạt đăng nhập | `401`, không nói rõ lý do | ✓ |
| Profile `central` + SQL Server tắt | API vẫn khởi động | ✓ |
| `/api/health/db` khi SQL Server tắt | `503`, thân báo `DOWN` | **✗** |
| `openapi.json` lệch với code | build đỏ ở `OpenApiContractTest` | ✓ |

Chạy: `.\scripts\dev-api.ps1 -MavenArguments verify`. Các ca tích hợp cần
SQL Server và biến `PTITONE_DB_URL`; thiếu thì chúng **skip chứ không đỏ**.
`OpenApiContractTest` không cần DB nên chạy được cả trên CI.

### ⚠️ Lỗ hổng cần vá: `SCHEDULE_TEACHER_CLASH`

Ràng buộc "một giảng viên không ở hai chỗ cùng lúc" có **hai đường vào**, và
hiện chỉ một đường có test:

- Gán GV cho lớp **đã có lịch** → `TEACHER_SCHEDULE_CLASH` — **có** test.
- Đặt lịch cho lớp **đã có GV** → `SCHEDULE_TEACHER_CLASH` — **chưa** có test.

Đường thứ hai chính là đường seed từng để lọt (`GVHCM001` dạy hai lớp trùng giờ,
sửa ở commit `3db5312`). Code đã chặn, nhưng không có test nên lần tới ai refactor
`ScheduleService` sẽ không biết mình vừa bỏ mất phép kiểm. Bốn ca `✗` còn lại ít
nghiêm trọng hơn, đều là ca biên.

---

## 7. Quy tắc cho frontend

**Gặp 401 ở API nghiệp vụ** → làm mới phiên **một lần**, thử lại **một lần**. 401
nghĩa là server chưa xử lý gì nên thử lại an toàn, kể cả với `POST`.

**Gặp 401 ở `/api/auth/*`** → **đừng** tự làm mới. Ở đó 401 là kết luận thật (sai
mật khẩu, phiên đã mất), không phải access hết hạn.

**Không lưu token ở `localStorage`** — không có gì để lưu, token nằm trong cookie
`HttpOnly`.

**Chặn tuyến theo vai trò chỉ là trải nghiệm**, không phải bảo vệ. Quyền thật do
backend kiểm ở mỗi request; ẩn nút không thay thế được điều đó.

---

## 8. Chưa có

| Gói | Nội dung |
|---|---|
| F02 | Cấp hồ sơ, kích hoạt tài khoản, quên mật khẩu |
| F05 | GV xem lớp phụ trách, danh sách SV, sĩ số |
| F06 | Nhập, công bố và khóa điểm |
| F07 | SV xem bảng điểm |
| F08 | Đăng ký và hủy học phần |
| F09 | SV xem thời khóa biểu |

Danh mục chương trình đào tạo (`ChuongTrinhDaoTao`, `CTDT_MonHoc`) đã có bảng và
seed nhưng **chưa có endpoint** — phần còn thiếu của F03.

Quy tắc đăng ký/hủy (`DANG_XU_LY → DANG_HUY → DA_HUY`, trần tín chỉ trên
`SinhVienHocKy`, thứ tự khoá `LopHocPhan` trước `DangKyHocPhan`) đã chốt ở
[AGENTS.md](../AGENTS.md) và [PTIT-One-Thiet-Ke.md](PTIT-One-Thiet-Ke.md); viết
vào đây khi F08 có code, không viết trước.

Gọi các đường dẫn chưa có trả `404` — đó là hành vi đúng.

### Vấn đề còn treo

B3 ghi `DanhBaNguoiDung` là **R only** với Admin cơ sở, và ở Phần 2 Subscriber bị
`DENY INSERT/UPDATE/DELETE`, nên Admin cơ sở **không thể** tạo tài khoản dù code
hiện cho phép. F02 phải chọn: cấp tài khoản chỉ ở Master, hay đổi B3.

---

[Kế hoạch Phần 1](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md) ·
[Trạng thái backend](PTIT-One-Backend-Khoi-Dong.md) ·
[Kế hoạch auth](PTIT-One-Ke-Hoach-Auth.md)
