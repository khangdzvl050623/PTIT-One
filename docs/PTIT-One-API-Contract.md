# PTIT One — API Contract

Hợp đồng giữa `apps/api` và `apps/web`. Cập nhật 02/10/2026.

**Đây là mô tả code đang chạy, không phải đề xuất.** Mọi endpoint dưới đây đã
có test tích hợp trên SQL Server thật. Đổi hợp đồng thì sửa file này **cùng
commit** với code, không để lệch.

---

## 1. Quy ước chung

**Đường dẫn tiếng Anh, trường JSON tiếng Việt không dấu.** Trường JSON trùng
tên cột trong DB (`maMonHoc`, `soLuongToiDa`) để đọc response là biết đang xem
cột nào — đúng quy ước `AGENTS.md`: bảng và cột tiếng Việt, code tiếng Anh.

**Thời gian** dùng ISO-8601 UTC: `2026-09-01T00:00:00Z`. Riêng `ngayBatDau`,
`ngayKetThuc` của học kỳ là ngày trần: `"2026-09-01"`.

**Mã trạng thái thành công**: `POST` tạo mới trả **`201`** kèm bản ghi vừa tạo
(`/api/courses`, `/api/classes`, `/api/enrollment-periods`); `logout` và
`logout-all` trả **`204`** không thân; còn lại `200`.

### Xác thực

Không có `Authorization` header. Hai cookie `HttpOnly` do server đặt:

| Cookie | Path | Sống |
|---|---|---|
| `PTITONE_AT` | `/api` | 15 phút |
| `PTITONE_RT` | `/api/auth` | 7 ngày (hạn tuyệt đối từ lúc đăng nhập) |

Frontend **không đọc được** hai cookie này và cũng không cần. Chỉ cần
`credentials: 'include'` ở mọi request.

### CSRF

Mọi request **ghi** (`POST`, `PUT`, `DELETE`) phải có header
`X-XSRF-TOKEN` lấy từ cookie `XSRF-TOKEN` (cookie này JS đọc được).
Chưa có cookie thì gọi `GET /api/auth/csrf` trước.

Server **đổi token CSRF khi đăng nhập**, nên đọc lại cookie ở mỗi request
thay vì nhớ giá trị trong bộ nhớ.

### Hình dạng lỗi

Mọi lỗi đều trả JSON cùng một dạng:

```json
{
  "code": "CLASS_CAPACITY_BELOW_ENROLLED",
  "message": "Không hạ được sức chứa xuống 1: lớp đang có 2 sinh viên.",
  "fieldErrors": { "soLuongToiDa": "Sức chứa tối thiểu là 1." },
  "traceId": "25a818cf-f6c6-4aa7-b112-a4a5314f977a"
}
```

`fieldErrors` chỉ có ở lỗi validation. `traceId` cũng nằm trong header
`X-Trace-Id` của **mọi** response — đưa mã này khi báo lỗi để tra log.

**Rẽ nhánh theo `code`, không theo `message`.** Message là câu hiển thị cho
người dùng và có thể đổi bất cứ lúc nào.

---

## 2. Bảng endpoint

`—` = chỉ cần đăng nhập. Vai trò ghi ra là **bắt buộc**.

| Method | Path | Quyền |
|---|---|---|
| GET | `/api/health` | public |
| GET | `/api/health/db` | `ADMIN_CO_SO` · `ADMIN_MASTER` |
| GET | `/api/auth/csrf` | public |
| POST | `/api/auth/login` | public |
| POST | `/api/auth/refresh` | public *(dùng cookie)* |
| POST | `/api/auth/logout` | public *(dùng cookie)* |
| POST | `/api/auth/logout-all` | — |
| GET | `/api/auth/me` | — |
| GET | `/api/courses` | — |
| GET | `/api/courses/{maMonHoc}` | — |
| POST | `/api/courses` | `ADMIN_MASTER` |
| PUT | `/api/courses/{maMonHoc}` | `ADMIN_MASTER` |
| PUT | `/api/courses/{maMonHoc}/prerequisites` | `ADMIN_MASTER` |
| GET | `/api/faculties` | — |
| GET | `/api/terms` | — |
| GET | `/api/teachers` | — |
| GET | `/api/classes` | — |
| GET | `/api/classes/{maLopHP}` | — |
| POST | `/api/classes` | `ADMIN_CO_SO` |
| PUT | `/api/classes/{maLopHP}` | `ADMIN_CO_SO` |
| PUT | `/api/classes/{maLopHP}/teacher` | `ADMIN_CO_SO` |
| GET | `/api/classes/{maLopHP}/schedule` | — |
| PUT | `/api/classes/{maLopHP}/schedule` | `ADMIN_CO_SO` |
| GET | `/api/enrollment-periods` | — |
| POST | `/api/enrollment-periods` | `ADMIN_CO_SO` |
| PUT | `/api/enrollment-periods/{maDot}` | `ADMIN_CO_SO` |

### ⚠️ Hai quy tắc phạm vi mà frontend phải biết

**Không có tham số `maCoSo` ở bất kỳ endpoint nào.** Cơ sở luôn lấy từ JWT đã
ký. Gửi lên cũng bị bỏ qua — đây là chủ ý chống leo thang đặc quyền, không
phải thiếu sót.

**`ADMIN_MASTER` chỉ ĐỌC lớp học phần và đợt đăng ký** (B3). Tạo lớp là việc
của `ADMIN_CO_SO`, và chỉ trong cơ sở của mình. UI đừng hiện nút "Tạo lớp"
cho Admin Master.

---

## 3. Auth

### `POST /api/auth/login`

```json
{ "username": "B26DCCN001", "password": "..." }
```

`200` — cùng hình dạng với `GET /api/auth/me` và `POST /api/auth/refresh`:

```json
{
  "username": "B26DCCN001",
  "role": "SINH_VIEN",
  "entityId": "B26DCCN001",
  "homeCampus": "HCM",
  "expiresAt": "2026-10-09T06:28:23.940Z",
  "accessExpiresAt": "2026-10-02T06:43:23.940Z"
}
```

`role` ∈ `SINH_VIEN` · `GIANG_VIEN` · `ADMIN_CO_SO` · `ADMIN_MASTER`.
`entityId` là mã SV/GV, `null` với tài khoản quản trị.
`homeCampus` là `null` với `ADMIN_MASTER` — Master không thuộc cơ sở nào.

`401 AUTH_INVALID_CREDENTIALS` cho **mọi** lý do từ chối: sai mật khẩu, tài
khoản chưa kích hoạt, tài khoản bị ngừng. Thông báo chung là cố ý — để form
đăng nhập không thành công cụ dò xem tài khoản nào có thật. **Đừng suy đoán
thêm lý do trên UI.**

### `POST /api/auth/refresh`

Không cần access còn hạn. Trả cùng hình dạng trên, rotate cookie refresh.

⚠️ **Chỉ một lượt refresh tại một thời điểm, kể cả giữa nhiều tab.** Trình lại
một refresh token đã rotate bị coi là replay và server **thu hồi cả phiên** —
token mới nhất cũng chết theo. `apps/web` đã xử lý bằng Web Locks
(`features/auth/model/refreshCoordinator.ts`); client khác phải tự lo.

### `POST /api/auth/logout` · `logout-all`

`204`. `logout` thu hồi phiên hiện tại; `logout-all` thu hồi **mọi phiên** và
tăng phiên bản tài khoản — có hiệu lực ngay ở request kế tiếp của mọi thiết bị.

---

## 4. Danh mục môn học

### `GET /api/courses?maKhoa=&q=`

```json
[ { "maMonHoc": "INT1154", "tenMonHoc": "Lập trình C",
    "soTinChi": 3, "maKhoa": "CNTT", "tenKhoa": "Công nghệ thông tin" } ]
```

### `GET /api/courses/{maMonHoc}`

Trả **cả hai chiều** của quan hệ tiên quyết:

```json
{
  "mon": { "maMonHoc": "INT1155", "...": "..." },
  "tienQuyet":   [ { "maMonHoc": "INT1154", "...": "..." } ],
  "monPhuThuoc": [ { "maMonHoc": "INT1306", "...": "..." } ]
}
```

`tienQuyet` = phải đạt **trước**. `monPhuThuoc` = môn đang cần môn này —
cần cho màn quản trị, vì sửa môn này ảnh hưởng tới chúng.

### `PUT /api/courses/{maMonHoc}/prerequisites`

```json
{ "tienQuyet": ["INT1154"] }
```

Thay **toàn bộ** tập. Danh sách rỗng = gỡ hết, đó là thao tác hợp lệ.
Nhiều tiên quyết nghĩa là phải đạt **tất cả**; điều kiện "hoặc" chưa có.

Server chặn chu trình **dài**, không chỉ môn tự trỏ chính nó:
`A → B → C → A` cũng bị bắt. Lỗi thì **giữ nguyên tập cũ**.

### `GET /api/faculties` · `GET /api/terms`

```json
[ { "maKhoa": "CNTT", "tenKhoa": "Công nghệ thông tin" } ]
```

```json
[ { "maHocKy": "2026-1", "tenHocKy": "Học kỳ 1", "namHoc": "2026-2027",
    "ngayBatDau": "2026-09-01", "ngayKetThuc": "2026-12-31" } ]
```

`ngayBatDau` là mốc để quy đổi `tuanBatDau`/`tuanKetThuc` của lịch học thành
ngày thật — frontend cần nó để vẽ thời khóa biểu theo tuần.

Hai bảng này chỉ đọc; chưa có endpoint ghi vì chưa màn quản trị nào cần.

---

## 5. Lớp học phần

### `GET /api/classes?maHocKy=&maMonHoc=&maGiangVien=`

```json
[ {
  "maLopHP": "BAS1203-2026-1-HCM01", "maMonHoc": "BAS1203",
  "tenMonHoc": "Đại số tuyến tính", "soTinChi": 3,
  "maHocKy": "2026-1", "maCoSoHost": "HCM",
  "maGiangVien": "GVHCM002", "tenGiangVien": "Trần Thị Mỹ Linh",
  "soLuongToiDa": 3, "soLuongDaDangKy": 2,
  "trangThai": "MO", "choPhepLienCoSo": false,
  "hinhThucHoc": "TRUC_TIEP", "phienBanLich": 1
} ]
```

`trangThai` ∈ `DU_KIEN` · `MO` · `DA_KHOA` · `DA_HUY`
`hinhThucHoc` ∈ `TRUC_TIEP` · `TRUC_TUYEN` · `KET_HOP`

### `POST /api/classes`

```json
{ "maMonHoc": "BAS1203", "maHocKy": "2026-1", "soLuongToiDa": 30,
  "hinhThucHoc": "TRUC_TIEP", "choPhepLienCoSo": false,
  "maGiangVien": null }
```

**Không gửi `maLopHP` và `maCoSo`.** Server sinh mã từ môn + kỳ + cơ sở trong
JWT: `BAS1203-2026-1-HCM01`. Lớp mới luôn ở `DU_KIEN`.

`choPhepLienCoSo: true` chỉ hợp lệ khi `hinhThucHoc = "TRUC_TUYEN"` (D18).

### `PUT /api/classes/{maLopHP}`

```json
{ "soLuongToiDa": 40, "trangThai": "MO",
  "hinhThucHoc": "TRUC_TIEP", "choPhepLienCoSo": false }
```

Môn, kỳ và cơ sở **không đổi được** — cả ba nằm trong mã lớp.
Hạ `soLuongToiDa` xuống **dưới** sĩ số hiện tại bị từ chối; hạ xuống **đúng
bằng** sĩ số thì được.

### `PUT /api/classes/{maLopHP}/teacher`

```json
{ "maGiangVien": "GVHCM002" }
```

`null` hoặc rỗng = gỡ phân công. Giảng viên phải **thuộc cùng cơ sở** với lớp,
và không được đã dạy lớp khác trùng khung giờ.

---

## 6. Lịch học

### `GET /api/classes/{maLopHP}/schedule`

```json
{ "maLopHP": "BAS1203-2026-1-HCM01", "phienBanLich": 1,
  "buoiHoc": [ { "thu": 4, "tietBatDau": 1, "soTiet": 3,
                 "phongHoc": "A2-401", "tuanBatDau": 1, "tuanKetThuc": 15 } ] }
```

`thu`: 2 = thứ Hai … 8 = Chủ nhật. Tuần tính từ `HocKy.NgayBatDau`.
Tiết 1–12 theo `GET /api/terms` và bảng `KhungGioTiet`.

### `PUT /api/classes/{maLopHP}/schedule`

Gửi cả danh sách — **thay toàn bộ**, rỗng = xoá hết lịch.

Bị chặn khi: lớp **đã có sinh viên đăng ký**, trùng giảng viên, trùng phòng
(so không phân biệt hoa thường), hoặc hai buổi của chính lớp đó chồng nhau.

⚠️ **Danh sách lớp lọc cứng theo cơ sở, chi tiết thì không.**
`GET /api/classes` chỉ trả lớp của cơ sở người gọi (`ADMIN_MASTER` thấy hết),
nên sinh viên HN **không thấy** lớp trực tuyến liên cơ sở của HCM trong danh
sách — dù `GET /api/classes/{id}` cho họ đọc nếu biết mã. Hiện chưa sai vì
chưa có đăng ký liên cơ sở; **F08 sẽ phải mở đường tìm lớp liên cơ sở**, và
đó là chỗ sửa, không phải sửa ở chi tiết lớp.

---

⚠️ **Chi tiết lớp không kèm lịch.** `GET /api/classes/{id}` và
`GET /api/classes/{id}/schedule` là hai lời gọi riêng, vì module `timetable`
phụ thuộc `course` và chiều ngược lại sẽ thành phụ thuộc vòng.

---

## 7. Giảng viên và đợt đăng ký

### `GET /api/teachers?maKhoa=`

```json
[ { "maGiangVien": "GVHCM002", "hoTen": "Trần Thị Mỹ Linh",
    "maCoSo": "HCM", "maKhoa": "CNTT", "hocVi": "Thạc sĩ" } ]
```

Tự giới hạn trong cơ sở của người gọi; `ADMIN_MASTER` thấy mọi cơ sở.

### `GET` / `POST` / `PUT /api/enrollment-periods`

```json
{ "maHocKy": "2026-1",
  "thoiGianMo":   "2026-09-01T00:00:00Z",
  "thoiGianDong": "2026-12-31T23:59:00Z",
  "trangThai": "DANG_MO" }
```

`trangThai` ∈ `CHUA_MO` · `DANG_MO` · `DA_DONG`. Mã đợt do server sinh, trả
trong trường `maDot` cùng `maCoSo`.

⚠️ Khi `PUT`, trường `maHocKy` trong thân **bị bỏ qua** — học kỳ của đợt không
đổi được. Gửi hay không gửi đều như nhau; muốn đổi kỳ thì tạo đợt mới.

**"Đang mở" cần CẢ HAI**: `trangThai = "DANG_MO"` **và** thời điểm hiện tại
nằm trong khoảng. Một cơ sở chỉ được **một** đợt `DANG_MO` mỗi học kỳ — muốn
mở đợt bổ sung thì đóng đợt cũ trước.

---

## 8. Mã lỗi

| Code | HTTP | Khi nào |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Dữ liệu không hợp lệ; xem `fieldErrors` |
| `AUTH_INVALID_CREDENTIALS` | 401 | Sai thông tin đăng nhập, hoặc tài khoản không được vào |
| `AUTH_SESSION_INVALID` | 401 | Thiếu/hết hạn/bị thu hồi phiên |
| `AUTH_REFRESH_INVALID` | 401 | Refresh không hợp lệ, hết hạn, hoặc replay |
| `AUTH_FORBIDDEN` | 403 | Đã đăng nhập nhưng sai vai trò hoặc sai cơ sở |
| `CSRF_INVALID` | 403 | Thiếu hoặc sai header `X-XSRF-TOKEN` |
| `SERVICE_UNAVAILABLE` | 503 | Lỗi DB; không trả chi tiết SQL ra ngoài |
| `COURSE_NOT_FOUND` | 404 / 400 | 404 khi mở môn không có; **400** khi tham chiếu môn không có lúc tạo lớp |
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

⚠️ `COURSE_NOT_FOUND` dùng ở hai ngữ cảnh với **hai mã HTTP khác nhau**.
Rẽ nhánh theo cặp `(status, code)` chứ đừng chỉ theo `code`.

Mã `429 AUTH_TOO_MANY_ATTEMPTS` **chưa có** — giới hạn tần suất thuộc A1.

---

## 9. Giới hạn dữ liệu vào

Vi phạm các mức này trả `400 VALIDATION_ERROR` kèm `fieldErrors`. Đưa luôn vào
form để người dùng biết trước khi bấm gửi.

| Trường | Giới hạn |
|---|---|
| `username` · `password` | ≤ 50 · ≤ 128 ký tự |
| `maMonHoc` · `tenMonHoc` | ≤ 20 · ≤ 200 ký tự |
| `soTinChi` | 1 – 15 |
| `soLuongToiDa` | 1 – 500 |
| `tienQuyet` | ≤ 20 môn |
| `buoiHoc` | ≤ 14 buổi mỗi lớp |
| `thu` | 2 – 8 |
| `tietBatDau` · `soTiet` | 1 – 12 mỗi trường |

`tietBatDau + soTiet - 1 ≤ 12` là ràng buộc **riêng**, không suy ra được từ hai
mức trên: tiết 11 + 4 tiết đều hợp lệ từng trường nhưng tổng vượt ngày, và trả
`SCHEDULE_SLOT_INVALID` chứ không phải `VALIDATION_ERROR`.

---

## 10. Quy tắc cho frontend

**Gặp 401 ở API nghiệp vụ** → làm mới phiên **một lần**, thử lại **một lần**.
401 nghĩa là server chưa xử lý gì nên thử lại an toàn, kể cả với `POST`.

**Gặp 401 ở `/api/auth/*`** → **đừng** tự làm mới. Ở đó 401 là kết luận thật
(sai mật khẩu, phiên đã mất), không phải access hết hạn.

**Không lưu token ở `localStorage`.** Không có gì để lưu — token nằm trong
cookie `HttpOnly`.

**Chặn tuyến theo vai trò chỉ là trải nghiệm**, không phải bảo vệ. Quyền thật
do backend kiểm ở mỗi request; ẩn nút không thay thế được điều đó.

---

## 11. Chưa có

Các gói còn lại của Phần 1, chưa có endpoint nào:

| Gói | Nội dung |
|---|---|
| F02 | Cấp hồ sơ, kích hoạt tài khoản, quên mật khẩu |
| F05 | GV xem lớp phụ trách, danh sách SV, sĩ số |
| F06 | Nhập, công bố và khóa điểm |
| F07 | SV xem bảng điểm |
| F08 | Đăng ký và hủy học phần |
| F09 | SV xem thời khóa biểu |

Danh mục chương trình đào tạo (`ChuongTrinhDaoTao`, `CTDT_MonHoc`) đã có bảng
và seed nhưng **chưa có endpoint** — phần còn thiếu của F03.

Gọi thử các đường dẫn chưa có trả `404`, đó là hành vi đúng.

---

[Kế hoạch Phần 1](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md) ·
[Trạng thái backend](PTIT-One-Backend-Khoi-Dong.md) ·
[Kế hoạch auth](PTIT-One-Ke-Hoach-Auth.md)
