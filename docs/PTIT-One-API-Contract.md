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
vai trò (phần lớn kiểm trong service, không phải `@PreAuthorize`), 81 mã lỗi (ném
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
| POST | `/api/auth/activate` · `/activate/resend` · `forgot-password` · `reset-password` | public |
| GET · PUT · POST | `/api/auth/email` · `/email/resend` · `/email/verify` · `/change-password` | — |
| POST | `/api/students` · `/api/teachers` | `ADMIN_MASTER` |
| GET · POST · PUT | `/api/accounts` · `/{tenDangNhap}/activation-code` · `/{tenDangNhap}/status` | `ADMIN_MASTER` |
| GET | `/api/courses` · `/api/courses/{maMonHoc}` | — |
| POST · PUT | `/api/courses` · `/{maMonHoc}` · `/{maMonHoc}/prerequisites` | `ADMIN_MASTER` |
| GET | `/api/faculties` · `/api/terms` · `/api/teachers` | — |
| GET | `/api/programs` · `/api/programs/{maCTDT}` | — |
| GET | `/api/classes` · `/api/classes/{maLopHP}` | — |
| POST · PUT | `/api/classes` · `/{maLopHP}` · `/{maLopHP}/teacher` | `ADMIN_CO_SO` |
| POST | `/api/classes/{maLopHP}/cancel` | `ADMIN_CO_SO` cùng cơ sở |
| GET | `/api/classes/{maLopHP}/students` | GV **phụ trách lớp** · `ADMIN_CO_SO` cùng cơ sở · `ADMIN_MASTER` |
| GET | `/api/classes/{maLopHP}/grades` | GV **phụ trách lớp** · `ADMIN_CO_SO` cùng cơ sở · `ADMIN_MASTER` |
| PUT · POST | `/api/classes/{maLopHP}/grades` · `/grades/publish` | GV **phụ trách lớp** |
| POST | `/api/classes/{maLopHP}/grades/lock` | `ADMIN_CO_SO` cùng cơ sở |
| GET | `/api/classes/{maLopHP}/schedule` | — |
| PUT | `/api/classes/{maLopHP}/schedule` | `ADMIN_CO_SO` |
| GET | `/api/enrollment-periods` | — |
| POST · PUT | `/api/enrollment-periods` · `/{maDot}` | `ADMIN_CO_SO` |
| GET | `/api/me/teaching-classes` · `/api/me/teaching-schedule` | `GIANG_VIEN` |
| GET | `/api/me/grades` · `/api/me/timetable` · `/api/me/enrollments` | `SINH_VIEN` |
| POST · DELETE | `/api/me/enrollments` · `/api/me/enrollments/{maLopHP}` | `SINH_VIEN` |
| GET | `/api/reports/summary` · `/api/reports/courses` | `ADMIN_CO_SO` (cơ sở mình) · `ADMIN_MASTER` |
| GET · POST | `/api/me/notifications` · `/unread-count` · `/{id}/read` · `/read-all` | `SINH_VIEN` · `GIANG_VIEN` |
| GET · POST · PUT · DELETE | `/api/notifications` · `/preview` · `/{id}` · `/{id}/send` | `ADMIN_MASTER` · `ADMIN_CO_SO` · `GIANG_VIEN` — chỉ bản **mình soạn** |

### Hai quy tắc phạm vi

**Không endpoint ghi nào nhận tham số `maCoSo`.** Cơ sở luôn lấy từ JWT đã ký.
Đây là chủ ý chống leo thang đặc quyền, không phải thiếu sót. Ngoại lệ duy nhất
là bộ lọc **chỉ đọc** của `/api/reports/*`: có tác dụng với Admin Master; Admin
cơ sở gửi cơ sở khác thì nhận `403`, không bao giờ được mở rộng phạm vi.

**Đường `/api/me/*` lấy danh tính từ JWT.** Không nhận mã sinh viên hay mã
giảng viên từ client, nên không có cách xem dữ liệu của người khác qua đường này.

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
- `PUT /api/classes/{maLopHP}` **không** đặt được `DA_KHOA` hay `DA_HUY` — hai
  trạng thái này chỉ đi qua khoá điểm và huỷ lớp. Lớp đã huỷ không sửa được
  (`409 CLASS_CANCELLED`).
- Từ vựng: `trangThai` ∈ `DU_KIEN` · `MO` · `DA_KHOA` · `DA_HUY`;
  `hinhThucHoc` ∈ `TRUC_TIEP` · `TRUC_TUYEN` · `KET_HOP`.

### Huỷ lớp

- `POST /api/classes/{maLopHP}/cancel`, thân `{"lyDo"}` tuỳ chọn (≤ 500 ký tự).
- Trong **một** giao dịch: huỷ mọi ghi danh hai phía, **trả tín chỉ** cho từng SV,
  xoá dòng điểm rỗng, sĩ số về 0, lớp sang `DA_HUY`; báo SV ("xem lớp còn chỗ để
  đăng ký bổ sung") và GV phụ trách, kèm lý do.
- Chặn khi lớp đã khoá điểm (`GRADE_LOCKED`) hoặc đã có SV có điểm
  (`CLASS_HAS_GRADES`) — không xoá điểm để huỷ. Lỗi thì không gì thay đổi.
- Huỷ lại lớp đã huỷ trả `200`, `soDangKyDaHuy: 0`, không báo lần hai.
- Có SV đăng ký chen đúng lúc huỷ → `409 CLASS_CANCEL_RETRY`, thử lại.
- Không mở lại được lớp đã huỷ.

### Đợt đăng ký

- **"Đang mở" cần CẢ HAI**: `trangThai = "DANG_MO"` **và** thời điểm hiện tại
  trong `[thoiGianMo, thoiGianDong]`. Chỉ xem trạng thái là bỏ sót đợt đã hết giờ
  mà quên đóng.
- Một cơ sở chỉ được **một** đợt `DANG_MO` mỗi học kỳ, chặn bằng unique filtered
  index ở DB (`V3`) chứ không chỉ ở code. Muốn mở đợt bổ sung thì đóng đợt cũ.
- `trangThai` ∈ `CHUA_MO` · `DANG_MO` · `DA_DONG`. Mã đợt do server sinh.

### Danh sách lớp và lớp phụ trách (F05)

- `GET /api/me/teaching-classes` chỉ trả lớp mà giảng viên **đang** được phân
  công. Lọc theo `maHocKy` nếu gửi.
- `GET .../students`: quyền kiểm theo **lớp**, không theo tham số. Đổi mã lớp
  trên URL sang lớp của GV khác → `403`. Sinh viên không xem được, **kể cả sinh
  viên của chính lớp đó**, vì danh sách chứa thông tin người khác.
- Danh sách gồm ghi danh còn giữ chỗ (`DANG_XU_LY` · `DA_DANG_KY` · `DANG_HUY`).
  Response trả **cả** bộ đếm `lop.soLuongDaDangKy` **lẫn** danh sách; hai số này
  phải bằng nhau. Lệch nhau là lỗi dữ liệu, đừng tự sửa trên UI.

### Nhập, công bố và khoá điểm (F06)

- Luồng: GV phụ trách **nhập nháp** → **công bố** cả lớp → Admin cơ sở **khoá**.
  Admin không nhập thay GV; GV không tự khoá.
- `PUT .../grades` lưu **một loạt** dòng trong một giao dịch: một dòng lỗi thì
  **không dòng nào** được ghi. Gửi `null` cho một điểm thành phần là xoá điểm đó.
- **Mỗi dòng phải gửi lại `version`** đã nhận từ `GET`. Lệch → `409
  GRADE_VERSION_CONFLICT`; UI tải lại bảng điểm, không tự ghi đè.
- `diemTongKet` do **server** tính (`0.1·CC + 0.3·GK + 0.6·CK`, làm tròn 1 chữ
  số); thiếu một điểm thành phần thì `null`. Client không gửi tổng kết. Trọng số
  là giả định demo, cấu hình ở `ptitone.grade.*`.
- Điểm 0–10, tối đa 1 chữ số thập phân.
- Công bố bị chặn khi còn SV thiếu điểm thành phần (`GRADE_INCOMPLETE`). Công bố
  rồi **vẫn sửa được**, SV thấy ngay; thời điểm công bố đầu được giữ.
- Khoá cần mọi dòng đã công bố (`GRADE_NOT_PUBLISHED`). Khoá rồi gọi lại vẫn `200`.
  Khoá xong **không có đường mở**: mở khoá/cải chính nằm ngoài bản basic.
- `trangThai` của bảng điểm ∈ `NHAP` (còn dòng nháp) · `DA_CONG_BO` · `DA_KHOA`.
- `PUT /api/classes/{maLopHP}` **không** đặt được `DA_KHOA` (`400
  CLASS_STATUS_INVALID`) và không sửa được lớp đã khoá (`409 GRADE_LOCKED`).

### Bảng điểm sinh viên (F07)

- Mỗi môn đang ghi danh có **một dòng**, kể cả khi chưa có điểm.
- Điểm **chưa công bố** thì mọi cột điểm và `ketQua` là `null`, `daCongBo: false`.
  Hiển thị "Chưa có điểm", **không** hiện 0.
- `ketQua` ∈ `DAT` · `KHONG_DAT` · `null`. Đạt khi `diemTongKet ≥ 4.0`. Ngưỡng
  này là **giả định demo** (cấu hình `ptitone.grade.nguong-dat`), chưa phải quy chế.
- Bỏ trống `maHocKy` thì trả mọi học kỳ, kỳ mới nhất trước.

### Thời khoá biểu (F09)

- `maHocKy` bắt buộc; `tuan` (≥ 1) tuỳ chọn, bỏ trống thì trả cả học kỳ.
- Response có `ngayBatDau` của học kỳ: tuần `n` bắt đầu từ `ngayBatDau + 7·(n−1)`
  ngày, `thu` 2 = thứ Hai … 8 = Chủ nhật.
- `gioBatDau`/`gioKetThuc` lấy từ khung giờ tiết, nên UI không cần tự tra giờ.
- Chỉ gồm lớp còn giữ chỗ; lớp đã huỷ không xuất hiện.

### Lịch dạy giảng viên

- `GET /api/me/teaching-schedule?maHocKy=&tuan=` gom mọi lớp GV đang phụ trách
  (trừ lớp đã huỷ). **Cùng hình dạng** với `/api/me/timetable` để UI dùng chung
  một màn lịch tuần.

### Đăng ký và huỷ học phần (F08)

- `POST /api/me/enrollments` chỉ gửi `{"maLopHP"}`. Server tự kiểm mọi điều
  kiện, **không** tin cờ "đủ điều kiện" từ client.
- Kiểm theo thứ tự, lỗi đầu tiên gặp được trả về:
  SV `DANG_HOC` → lớp **cùng cơ sở** (Phần 1 chưa có liên cơ sở) → đợt của cơ sở
  **đang mở** → lớp `MO` → môn thuộc **CTĐT** của SV → chưa giữ lớp khác **cùng
  môn** trong kỳ → đã **đạt mọi** môn tiên quyết (chỉ tính điểm **đã công bố**)
  → không **trùng lịch** (cùng thứ, chồng tiết **và** chồng tuần) → không vượt
  **trần tín chỉ** → lớp **còn chỗ**.
- **`201`** khi vừa đăng ký; **`200`** khi gửi lại đúng lớp đang giữ (bấm hai
  lần) — không cộng sĩ số lần nữa. UI coi cả hai là thành công.
- `loaiDangKy` ∈ `HOC_MOI` · `HOC_LAI` (đã trượt) · `CAI_THIEN` (đã đạt). Đăng ký
  lại được **mọi** môn; khi học nhiều lần, **điểm cao nhất** được tính.
- Lớp đầy → `409 CLASS_FULL` ngay, **không có hàng chờ**.
- Phần 1 đi thẳng `DA_DANG_KY` / `DA_HUY` vì mỗi thao tác là một giao dịch cục
  bộ; `DANG_XU_LY` / `DANG_HUY` dành cho Phần 2.
- `DELETE .../{maLopHP}` chỉ khi đợt **còn mở** và **chưa có điểm** nào. Trả chỗ,
  trả tín chỉ, đổi trạng thái và xoá dòng điểm rỗng trong **một** giao dịch.
  Không xoá điểm để huỷ. Huỷ rồi đăng ký lại được, kể cả sang lớp khác cùng môn.
- `GET /api/me/enrollments?maHocKy=` trả các môn đang giữ chỗ và
  `soTinChiDaDangKy` / `tranTinChi` (`null` khi chưa đăng ký gì trong kỳ). Trần
  24 là **giả định demo**, cấu hình `ptitone.enrollment.tran-tin-chi`.
- Tương tranh: `sp_getapplock` theo (SV, kỳ) **trước** mọi phép kiểm; sức chứa và
  tín chỉ kiểm **trong câu `UPDATE`** rồi đọc `@@ROWCOUNT`; thứ tự ghi luôn
  `SinhVienHocKy → LopHocPhan → DangKyHocPhan → DangKyMonHoc → Diem` ở cả đăng
  ký lẫn huỷ.

### Thống kê

- `maHocKy` bắt buộc. `/summary` lọc thêm được `maMonHoc`.
- Lớp và điểm tính theo cơ sở **mở lớp**. Chỉ tính lớp `MO` và `DA_KHOA` — bỏ
  lớp dự kiến và đã huỷ.
- `luotDangKy` ≠ `soSinhVien`: một SV học sáu môn là sáu lượt, một sinh viên.
- `tiLeLapDay` = tổng đã đăng ký / tổng sức chứa (0–1, 4 chữ số), **không** lấy
  trung bình phần trăm từng lớp. `null` khi không có lớp nào.
- Đạt/trượt chỉ tính điểm **đã công bố**, cùng ngưỡng với bảng điểm.
  `chuaCoKetQua` = lượt chưa có điểm công bố — **không** phải trượt.
- `phanBoDiem` gồm 5 khoảng `<4.0`, `4.0–5.4`, `5.5–6.9`, `7.0–8.4`, `≥8.5`; chỉ
  để vẽ biểu đồ, không phải xếp loại chính thức.
- `tienDoDiem` đếm theo lớp: `daKhoa`, `daCongBo` (mọi dòng đã công bố),
  `chuaCongBo` (còn lại, kể cả lớp chưa có SV).
- Báo cáo liên cơ sở thuộc Phần 2.

### Thông báo

**Hộp thư** (`/api/me/notifications`, SV và GV):

- Thông báo **tự sinh**: đăng ký thành công, huỷ thành công, điểm vừa công bố,
  điểm đã công bố bị sửa, lớp bị huỷ (`suKien` ∈ `DANG_KY` · `HUY_DANG_KY` ·
  `CONG_BO_DIEM` · `SUA_DIEM` · `LOP_BI_HUY`). Ghi **cùng giao dịch** nghiệp vụ: nghiệp vụ thất bại thì không có
  thông báo. Bấm đăng ký hai lần không ra hai thông báo; huỷ rồi đăng ký lại thì ra
  thông báo mới. Sửa điểm mà giá trị không đổi thì không báo.
- `daDoc` là **của riêng người đọc**; nhãn "Mới" theo thời gian là việc của UI.
  **Mở hộp thư không tự đánh dấu đã đọc** — chỉ `/{id}/read` hoặc `/read-all`.
  Đọc lại là không làm gì.
- `?chuaDoc=true` lọc chưa đọc; `trang` từ 0, `kichThuoc` 1–50. `soChuaDoc` là của
  cả hộp thư. Chuông dùng `/unread-count` cho nhẹ.
- `lienKet` là đường dẫn nội bộ của web (ví dụ `/sinh-vien/bang-diem?maHocKy=2026-1`).
- Admin không có hộp thư (`403`).

**Soạn tay** (`/api/notifications`):

| Người gửi | `phamVi` được dùng |
|---|---|
| `ADMIN_MASTER` | `TOAN_TRUONG` · `CO_SO` (bất kỳ) · `LOP_HOC_PHAN` (bất kỳ) |
| `ADMIN_CO_SO` | `CO_SO` (của mình; bỏ trống `maCoSo` là cơ sở mình) · `LOP_HOC_PHAN` do cơ sở mình mở |
| `GIANG_VIEN` | `LOP_HOC_PHAN` đang được phân công |

- `doiTuong` ∈ `SINH_VIEN` · `GIANG_VIEN` · `TAT_CA`; `mucDo` ∈ `THONG_THUONG` ·
  `QUAN_TRONG`. "Lớp" ở đây là **lớp học phần**, không phải lớp hành chính.
- Người nhận: `CO_SO` / `TOAN_TRUONG` lấy SV theo **cơ sở nhà**, chỉ SV **đang học
  hoặc bảo lưu**; GV theo cơ sở công tác. `LOP_HOC_PHAN` lấy SV đang giữ chỗ và GV
  phụ trách. Người gửi không tự nhận.
- Luồng: `POST` tạo **nháp** → `POST /preview` hoặc `GET /{id}` xem **số người nhận
  dự kiến** → `POST /{id}/send` **chốt** danh sách người nhận. SV chuyển lớp sau đó
  không làm lịch sử đổi theo.
- Quyền kiểm **cả lúc gửi**: GV bị gỡ phân công thì không gửi được nháp cũ.
- Đã gửi thì không sửa, không xoá, không gửi lại (`409 NOTIFICATION_ALREADY_SENT`).
  Chỉ xoá được nháp.
- `lienKet` chỉ nhận đường dẫn nội bộ bắt đầu bằng `/` (không nhận `https://…`
  hay `//…`) — thông báo chính thức không được dẫn sang trang lạ.
- Bản soạn của người khác trả `404`, không lộ là có tồn tại.

### Cấp và kích hoạt tài khoản (F02)

- **Chỉ `ADMIN_MASTER` cấp tài khoản** (chốt 02/10/2026). Danh bạ là bảng Master
  sở hữu, B3 cho Admin cơ sở chỉ đọc, và ở Phần 2 site bị `DENY` ghi bảng nhân
  bản — nên UI đừng hiện nút "Thêm sinh viên/giảng viên" cho Admin cơ sở.
- `POST /api/students` / `POST /api/teachers` tạo hồ sơ + danh bạ + tài khoản
  **chưa có mật khẩu** trong **một** giao dịch; lỗi ở bước nào thì không để lại
  gì. Tên đăng nhập chính là mã SV/GV. Mã kích hoạt dạng `XXXX-XXXX-XXXX-XXXX`,
  hạn 7 ngày, server chỉ giữ hash. Thân có `email` (tuỳ chọn) **và** đã bật gửi
  thư → mã **chỉ đi qua thư**: `kichHoat.maKichHoat = null`,
  `kichHoat.guiToiEmail` là địa chỉ nhận, và kích hoạt thành công thì email được
  coi là **đã xác minh**. Không thì `maKichHoat` có giá trị — **lần duy nhất mã
  gốc xuất hiện** — Admin trao tay.
- Người dùng gọi `POST /api/auth/activate` `{tenDangNhap, maKichHoat, matKhauMoi}`
  khi chưa đăng nhập (vẫn cần CSRF). Thành công `204`, **không tự đăng nhập**.
  Mã gõ thường hay thiếu gạch vẫn khớp. Mật khẩu 8–128 ký tự, không chứa tên
  đăng nhập.
- Mọi lý do từ chối kích hoạt — sai mã, hết hạn, đã dùng, tài khoản bị ngừng,
  không có tài khoản — đều là `400 ACTIVATION_INVALID`. Sai **5 lần** thì mã bị
  thu hồi; Admin cấp lại bằng `POST /api/accounts/{tenDangNhap}/activation-code`
  (mã cũ mất hiệu lực, chỉ cho tài khoản chưa kích hoạt). `?guiEmail=false` để
  nhận mã trao tay khi thư không tới được — khi đó kích hoạt không xác minh email.
- Người dùng tự xin gửi lại: `POST /api/auth/activate/resend` `{tenDangNhap}`
  (nút "Không nhận được mã?" ở trang kích hoạt). Mã mới đi tới **email Admin đã
  lưu**, mã cũ mất hiệu lực. Luôn `202` — tài khoản không có, đã kích hoạt hay
  không có email đều không gửi gì và không báo khác đi. Không có email thì phải
  nhờ Admin Master cấp lại mã trao tay.
- `PUT /api/accounts/{tenDangNhap}/status` `{trangThai: HOAT_DONG | NGUNG}`.
  Khoá thu hồi mọi phiên và tăng phiên bản trong cùng giao dịch — access token
  cũ bị từ chối ngay. Không khoá được Admin Master.
- `GET /api/accounts?maCoSo=&loaiNguoiDung=` liệt kê danh bạ, có `daKichHoat`.

### Email, đổi mật khẩu, quên mật khẩu (A1)

- **Mã khôi phục chỉ gửi tới email ĐÃ LƯU và ĐÃ XÁC MINH.** Email gõ ở form
  quên mật khẩu chỉ để đối chiếu, không bao giờ là nơi nhận.
  `POST /api/auth/forgot-password` `{tenDangNhap, email}` luôn trả `202` — kể cả
  khi tài khoản không có, email sai hay chưa xác minh — để form không thành công
  cụ dò tài khoản. UI chỉ nói "nếu thông tin đúng, mã đã được gửi".
- Mã 6 chữ số, hạn 10 phút, dùng một lần, sai 5 lần thì bị thu hồi; xin mã mới
  thì mã cũ mất hiệu lực. `POST /api/auth/reset-password`
  `{tenDangNhap, maXacThuc, matKhauMoi}` → `204`, **thu hồi mọi phiên**.
- `PUT /api/auth/email` `{email, matKhauHienTai}` cần mật khẩu hiện tại; lưu email
  **chưa xác minh** và gửi mã tới chính địa chỉ đó. Xác minh bằng
  `POST /api/auth/email/verify` `{maXacThuc}`. Đổi email là mất trạng thái xác minh.
  `POST /api/auth/email/resend` gửi lại mã tới email đang chờ, không cần mật khẩu.
- `login`, `refresh`, `/me` trả thêm `email` và `emailDaXacMinh`. **UI hiện banner
  nhắc** khi `emailDaXacMinh = false`: chưa có email thì mời thêm, có rồi thì nút
  "Gửi lại mã" — chưa xác minh thì không tự khôi phục mật khẩu được.
- `POST /api/auth/change-password` `{matKhauHienTai, matKhauMoi}` → `204`, thu hồi
  **mọi phiên kể cả phiên đang dùng** và xoá cookie — UI chuyển về đăng nhập.
- Thư đi qua Brevo: API HTTP nếu có `PTITONE_BREVO_API_KEY` (`xkeysib-`), không
  thì SMTP relay (`PTITONE_MAIL_HOST` + SMTP key `xsmtpsib-`). Thiếu
  `PTITONE_MAIL_FROM`, hoặc thiếu cả hai đường, thì đổi email, gửi lại mã và quên
  mật khẩu trả `503 MAIL_DISABLED`, không giả vờ đã gửi.
- **Giới hạn tần suất** (`429 AUTH_TOO_MANY_ATTEMPTS`, cửa sổ 15 phút, trong bộ
  nhớ): đăng nhập sai 10 lần/tài khoản hoặc 50 lần/IP; xin mã khôi phục 3
  lần/tài khoản hoặc 20 lần/IP; nhập sai mã khôi phục 20 lần/IP; sai mật khẩu
  hiện tại 10 lần/tài khoản. Chỉ lần **sai** mới bị đếm khi đăng nhập.

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
| `TERM_NOT_FOUND` | 400 | Mã học kỳ không có (tạo lớp, thời khoá biểu) |
| `PROGRAM_NOT_FOUND` | 404 / 400 | Không có chương trình đào tạo (`400` khi cấp hồ sơ SV) |
| `GRADE_VERSION_CONFLICT` | 409 | Dòng điểm đã bị người khác sửa sau khi tải |
| `GRADE_STUDENT_NOT_ENROLLED` | 400 | Sinh viên không thuộc lớp |
| `GRADE_INCOMPLETE` | 409 | Công bố khi còn SV thiếu điểm thành phần |
| `GRADE_NOT_PUBLISHED` | 409 | Khoá khi còn điểm chưa công bố |
| `GRADE_LOCKED` | 409 | Ghi vào lớp đã khoá điểm (cả qua API lớp) |
| `GRADE_CLASS_NOT_OPEN` | 409 | Lớp không ở trạng thái `MO` |
| `GRADE_BUSY` | 503 | Người khác đang ghi bảng điểm lớp này |
| `STUDENT_NOT_FOUND` | 404 | Tài khoản SV không có hồ sơ |
| `STUDENT_NOT_ACTIVE` | 409 | SV không ở trạng thái `DANG_HOC` |
| `ENROLLMENT_CROSS_CAMPUS` | 409 | Lớp thuộc cơ sở khác (Phần 1 chưa hỗ trợ) |
| `ENROLLMENT_PERIOD_CLOSED` | 409 | Không có đợt đang mở — tính cả giờ đóng |
| `CLASS_NOT_OPEN` | 409 | Lớp không ở trạng thái `MO` |
| `COURSE_NOT_IN_PROGRAM` | 409 | Môn không thuộc CTĐT của SV |
| `ENROLLMENT_DUPLICATE_COURSE` | 409 | Đã giữ lớp khác của cùng môn trong kỳ |
| `PREREQUISITE_NOT_MET` | 409 | Chưa đạt tiên quyết; `message` nêu tên môn |
| `SCHEDULE_CLASH` | 409 | Trùng lịch với lớp đã đăng ký |
| `CREDIT_LIMIT_EXCEEDED` | 409 | Vượt trần tín chỉ học kỳ |
| `CLASS_FULL` | 409 | Lớp đã đủ chỗ |
| `ENROLLMENT_NOT_FOUND` | 404 | Huỷ lớp mình không đăng ký |
| `ENROLLMENT_HAS_GRADE` | 409 | Huỷ khi đã có điểm |
| `ENROLLMENT_BUSY` | 503 | Đang xử lý một yêu cầu khác của chính SV đó |
| `NOTIFICATION_NOT_FOUND` | 404 | Không có, hoặc không phải của mình |
| `NOTIFICATION_INVALID` | 400 | `phamVi` / `doiTuong` / `mucDo` sai, thiếu lớp |
| `NOTIFICATION_ALREADY_SENT` | 409 | Sửa, xoá hoặc gửi lại bản đã gửi |
| `NOTIFICATION_NO_RECIPIENTS` | 409 | Phạm vi hiện không có ai nhận |
| `CAMPUS_NOT_FOUND` | 400 | Mã cơ sở không có |
| `ACCOUNT_EXISTS` | 409 | Mã SV/GV đã có tài khoản |
| `STUDENT_EXISTS` | 409 | Đã có hồ sơ sinh viên mã này |
| `TEACHER_EXISTS` | 409 | Đã có hồ sơ giảng viên mã này |
| `ACCOUNT_NOT_FOUND` | 404 | Không có tài khoản |
| `ACCOUNT_ALREADY_ACTIVATED` | 409 | Cấp lại mã cho tài khoản đã kích hoạt |
| `ACCOUNT_NOT_MANAGEABLE` | 409 | Khoá/cấp mã cho Admin Master, hoặc tài khoản đang chuyển cơ sở |
| `ACTIVATION_INVALID` | 400 | Mã kích hoạt sai, hết hạn, đã dùng, hoặc tài khoản không kích hoạt được |
| `PASSWORD_TOO_WEAK` | 400 | Mật khẩu chứa tên đăng nhập |
| `PASSWORD_INCORRECT` | 400 | Sai mật khẩu hiện tại (đổi mật khẩu/email) |
| `PASSWORD_UNCHANGED` | 400 | Mật khẩu mới trùng mật khẩu hiện tại |
| `EMAIL_CODE_INVALID` | 400 | Mã xác minh email sai, hết hạn hoặc đã dùng |
| `RESET_CODE_INVALID` | 400 | Mã khôi phục sai, hết hạn hoặc đã dùng |
| `MAIL_DISABLED` | 503 | Chưa cấu hình gửi thư |
| `EMAIL_NOT_SET` | 409 | Gửi lại mã xác minh khi chưa có email |
| `EMAIL_ALREADY_VERIFIED` | 409 | Gửi lại mã xác minh khi email đã xác minh |
| `AUTH_TOO_MANY_ATTEMPTS` | 429 | Vượt giới hạn tần suất; đợi rồi thử lại |
| `CLASS_CANCELLED` | 409 | Sửa lớp đã huỷ |
| `CLASS_HAS_GRADES` | 409 | Huỷ lớp đã có SV có điểm |
| `CLASS_CANCEL_RETRY` | 409 | Có thay đổi đăng ký đúng lúc huỷ lớp; thử lại |
| `PREREQUISITE_SELF` | 400 | Môn tự làm tiên quyết của chính nó |
| `PREREQUISITE_UNKNOWN` | 400 | Môn tiên quyết không tồn tại |
| `PREREQUISITE_CYCLE` | 409 | Tạo thành chu trình |
| `CATALOG_BUSY` | 503 | Người khác đang sửa đồ thị tiên quyết |
| `CLASS_NOT_FOUND` | 404 | Không có lớp |
| `CLASS_MODE_INVALID` | 400 | `hinhThucHoc` sai |
| `CLASS_STATUS_INVALID` | 400 | `trangThai` sai, hoặc đặt thẳng `DA_KHOA` |
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
| Đợt `DANG_MO` nhưng đã quá `thoiGianDong` | không tính là đang mở — đăng ký và huỷ đều `409` | ✓ |
| `POST` thiếu header `X-XSRF-TOKEN` | `403`, chặn trước khi vào controller | ✓ |
| Dùng lại refresh token đã rotate | `401 AUTH_REFRESH_INVALID`, **cả phiên bị thu hồi** | ✓ |
| `logout-all` rồi gọi API bằng access cũ | `401 AUTH_SESSION_INVALID` ngay | ✓ |
| Chữ ký đúng nhưng phiên đã chết | `401`, không cho qua | ✓ |
| Tài khoản chưa kích hoạt đăng nhập | `401`, không nói rõ lý do | ✓ |
| Admin Master cấp SV → kích hoạt → đăng nhập | `201` có mã · `204` · `200` | ✓ |
| Dùng lại mã đã kích hoạt | `400 ACTIVATION_INVALID`, mật khẩu không đổi | ✓ |
| `ADMIN_CO_SO` / GV / SV cấp hồ sơ | `403`, không ghi gì | ✓ |
| Cấp SV với CTĐT không có | `400 PROGRAM_NOT_FOUND`, không còn hồ sơ hay danh bạ mồ côi | ✓ |
| Nhập sai mã 5 lần rồi nhập đúng | `400` — mã đã bị thu hồi | ✓ |
| Cấp lại mã | mã trước mất hiệu lực, mã mới dùng được | ✓ |
| Khoá tài khoản đang có phiên | phiên cũ `401` ngay; đăng nhập lại `401`; mở lại thì vào được | ✓ |
| Cấp hồ sơ có email | mã chỉ đi qua thư; kích hoạt xong email đã xác minh | ✓ |
| Quên mật khẩu với email gõ sai | `202`, không có thư nào | ✓ |
| Quên mật khẩu khi email chưa xác minh | `202`, không có thư nào | ✓ |
| Khôi phục mật khẩu | phiên cũ `401`, mật khẩu cũ hỏng, mã không dùng lại được | ✓ |
| Đổi email sai mật khẩu hiện tại | `400 PASSWORD_INCORRECT` | ✓ |
| Đổi mật khẩu | mọi phiên `401`, đăng nhập bằng mật khẩu mới | ✓ |
| Xin mã khôi phục lần 4 trong 15 phút | `429` | ✓ |
| Đăng nhập sai 10 lần rồi đúng | `429` | ✓ |
| Tự xin gửi lại mã kích hoạt | mã mới tới email đã lưu, mã cũ hỏng; tài khoản không có / đã kích hoạt vẫn `202`, không thư | ✓ |
| `/me` khi chưa có email, rồi sau khi xác minh | `emailDaXacMinh` `false` → `true` | ✓ |
| Gửi lại mã xác minh | mã trước mất hiệu lực; đã xác minh thì `409` | ✓ |
| Profile `central` + SQL Server tắt | API vẫn khởi động | ✓ |
| `/api/health/db` khi SQL Server tắt | `503`, thân báo `DOWN` | **✗** |
| `openapi.json` lệch với code | build đỏ ở `OpenApiContractTest` | ✓ |
| GV xem danh sách lớp của GV khác (đổi mã lớp trên URL) | `403 AUTH_FORBIDDEN` | ✓ |
| SV xem danh sách lớp, kể cả lớp mình học | `403` | ✓ |
| Admin cơ sở xem danh sách lớp cơ sở khác | `403` | ✓ |
| Danh sách lớp trả bộ đếm và các dòng ghi danh để đối soát | hai số khớp | ✓ |
| Bảng điểm: môn đang học chưa có điểm | `null`, không phải `0`; `daCongBo: false` | ✓ |
| Bảng điểm: đạt/trượt theo ngưỡng 4.0 | `DAT` / `KHONG_DAT` | ✓ |
| Thời khoá biểu lọc tuần ngoài khoảng học | `buoiHoc` rỗng | ✓ |
| Thời khoá biểu học kỳ không tồn tại | `400 TERM_NOT_FOUND` | ✓ |
| Lưu điểm: tổng kết do server tính (9 / 7.5 / 8 → 8.0) | `200`, vẫn `NHAP` | ✓ |
| Hai lần sửa từ cùng một `version` | lần sau `409 GRADE_VERSION_CONFLICT`, lần đầu giữ nguyên | ✓ |
| Một dòng SV ngoài lớp trong loạt lưu | `400`, cả loạt không ghi | ✓ |
| Điểm 10.5 | `400 VALIDATION_ERROR` | ✓ |
| Admin nhập điểm / GV tự khoá / admin cơ sở khác khoá | `403` | ✓ |
| Công bố khi còn thiếu điểm | `409 GRADE_INCOMPLETE` | ✓ |
| Khoá khi chưa công bố | `409 GRADE_NOT_PUBLISHED` | ✓ |
| Sửa sau công bố, trước khoá | `200` | ✓ |
| Sửa sau khoá, kể cả qua `PUT /api/classes` | `409 GRADE_LOCKED` | ✓ |
| Đặt thẳng `DA_KHOA` qua API lớp | `400 CLASS_STATUS_INVALID` | ✓ |
| Lịch dạy GV chỉ gồm lớp mình phụ trách | không lẫn lớp GV khác | ✓ |
| **Lớp 30 chỗ, 100 SV đăng ký đồng thời** | đúng 30 thành công, 70 `CLASS_FULL`, đối soát 5 nguồn khớp | ✓ |
| Một SV bấm đăng ký 10 lần cùng lúc | 1 lần `201`, 9 lần `200`, sĩ số +1 | ✓ |
| Huỷ rồi đăng ký lại | dùng lại dòng cũ, bộ đếm không lệch | ✓ |
| Chưa đạt tiên quyết | `409 PREREQUISITE_NOT_MET`, nêu tên môn | ✓ |
| Trùng lịch / trùng môn | `409 SCHEDULE_CLASH` / `ENROLLMENT_DUPLICATE_COURSE` | ✓ |
| Lớp đầy | `409 CLASS_FULL`, tín chỉ không bị trừ | ✓ |
| Vượt trần tín chỉ | `409 CREDIT_LIMIT_EXCEEDED` | ✓ |
| Lớp cơ sở khác / lớp chưa mở | `409` | ✓ |
| Môn đã trượt / đã đạt đăng ký lại | `HOC_LAI` / `CAI_THIEN` | ✓ |
| Huỷ khi đã có điểm | `409 ENROLLMENT_HAS_GRADE`, sĩ số và tín chỉ giữ nguyên | ✓ |
| Thống kê: 2 lượt của 1 SV | `luotDangKy 2`, `soSinhVien 1` | ✓ |
| Thống kê: lớp chưa công bố điểm | `chuaCoKetQua`, không tính trượt | ✓ |
| Admin cơ sở lọc thống kê cơ sở khác | `403` | ✓ |
| Cơ sở không có lớp | `tiLeLapDay: null` | ✓ |
| Đăng ký → bấm lại → huỷ → đăng ký lại | đúng 3 thông báo | ✓ |
| Đăng ký bị từ chối | không có thông báo | ✓ |
| Công bố điểm / sửa điểm đã công bố / lưu lại cùng điểm | báo / báo / không báo | ✓ |
| Soạn → xem trước → gửi → đọc → đọc tất cả | đếm người nhận và chưa đọc đúng | ✓ |
| Gửi hai lần, sửa bản đã gửi | `409` | ✓ |
| Admin cơ sở gửi toàn trường / cơ sở khác; GV gửi lớp người khác | `403` | ✓ |
| Liên kết `https://…` hoặc `//…` | `400` | ✓ |
| Huỷ lớp có 2 SV | ghi danh huỷ, tín chỉ trả, sĩ số 0, SV và GV được báo | ✓ |
| Huỷ lại lớp đã huỷ | `200`, không báo lần hai | ✓ |
| Huỷ lớp đã có điểm / đã khoá điểm | `409`, không gì thay đổi | ✓ |
| Admin cơ sở khác, Admin Master, GV, SV huỷ lớp | `403` | ✓ |
| `PUT` đặt `DA_HUY`; sửa hoặc đăng ký vào lớp đã huỷ | `400` / `409` / `409 CLASS_NOT_OPEN` | ✓ |

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
| A1 | Bootstrap Admin Master đầu tiên + cờ bắt buộc đổi mật khẩu |


Gọi các đường dẫn chưa có trả `404` — đó là hành vi đúng.

### Đã chốt: cấp tài khoản chỉ ở Master

B3 ghi `DanhBaNguoiDung` là **R only** với Admin cơ sở, và ở Phần 2 Subscriber bị
`DENY INSERT/UPDATE/DELETE`. Nhóm chốt ngày 02/10/2026: **giữ B3, cấp tài khoản
chỉ ở Master** — Phần 2 đi đúng luồng "Vòng đời sinh viên" (danh bạ + Outbox ở
Master, worker dựng hồ sơ và tài khoản ở site nhà). Trạng thái kích hoạt nằm ở
`TaiKhoan.MatKhauHash` (site), không ở danh bạ, nên kích hoạt không phải ghi bảng
nhân bản.

---

[Kế hoạch Phần 1](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md) ·
[Trạng thái backend](PTIT-One-Backend-Khoi-Dong.md) ·
[Kế hoạch auth](PTIT-One-Ke-Hoach-Auth.md)
