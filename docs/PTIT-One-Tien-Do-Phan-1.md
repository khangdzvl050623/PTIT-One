# PTIT One — Báo cáo tiến độ Phần 1

Chốt số liệu ngày **03/10/2026**, nhánh `dev`. Mọi con số dưới đây lấy từ source
và từ một lần `clean verify` thật, không lấy từ tài liệu khác.

**Kết luận một dòng:** API Phần 1 đã xong về chức năng; phần còn lại của Phần 1
nằm gần hết ở frontend.

---

## 1. Số liệu

| Hạng mục | Số lượng |
|---|---|
| Endpoint | **68** (trên 55 đường dẫn) |
| Module nghiệp vụ | 11 |
| Migration | 7 (`V1`–`V7`) |
| Mã lỗi | 82 |
| File test / số ca test | 22 / **137** |
| Dòng code `apps/api` (main / test) | 10.090 / 3.982 |
| Dòng code `apps/web` | 2.675 |
| Dòng SQL `db/` | 4.040 |

## 2. Trạng thái build

```
clean verify trên dev, 03/10/2026
Tests run: 137, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

Ca duy nhất bị skip là benchmark băm mật khẩu (`PasswordEncoderBenchmarkTest`),
chỉ chạy khi đặt biến `PTITONE_BENCHMARK` — đúng thiết kế.

Trên CI (không có SQL Server) các ca tích hợp **skip chứ không đỏ**;
`OpenApiContractTest` vẫn chạy vì không cần database.

---

## 3. Đã làm

### Nền và xác thực

| Gói | Nội dung | Trạng thái |
|---|---|---|
| F00 | Spring Boot 4.1.1 / JDK 21, profile `central` một DataSource, Flyway `V1`–`V7`, CI hai job | Xong |
| F01 / A0 | Đăng nhập, phiên, refresh có rotation (replay thu hồi cả phiên), logout / logout-all, phân quyền theo vai trò, CSRF double-submit | Xong |
| F02 | Admin Master cấp hồ sơ SV/GV kèm tài khoản, kích hoạt bằng mã một lần, cấp lại mã, khoá/mở tài khoản | Xong |
| A1 | Email + xác minh, đổi mật khẩu, quên mật khẩu bằng mã 6 số qua Brevo, giới hạn tần suất `429` | Xong |

### Nghiệp vụ học vụ

| Gói | Nội dung | Trạng thái |
|---|---|---|
| F03 | Môn học, tiên quyết (chặn chu trình dài), khoa, học kỳ, chương trình đào tạo | Xong |
| F04 | Lớp học phần, phân công GV, lịch học (chặn trùng GV / trùng phòng), đợt đăng ký, huỷ lớp | Xong |
| F05 | GV xem lớp phụ trách, danh sách SV, sĩ số, lịch dạy | Xong |
| F06 | Nhập điểm (kiểm phiên bản), công bố, khoá điểm | Xong |
| F07 | SV xem bảng điểm | Xong |
| F08 | Đăng ký và huỷ học phần, **chỉ lớp cùng cơ sở** (quyết định 02/10/2026) | Xong |
| F09 | SV xem thời khoá biểu | Xong |
| — | Thống kê (module `report`, chỉ đọc) | Xong |
| — | Thông báo soạn tay + tự sinh (module `notification`) | Xong phần chính |

### Những ràng buộc khó đã có test

Đây là phần dễ làm sai nhất của đồ án, nên ghi rõ là đã kiểm bằng test tự động
trên SQL Server thật:

- **Chống vượt sức chứa**: điều kiện nằm trong chính câu `UPDATE` rồi đọc
  `@@ROWCOUNT`, không `SELECT` trước rồi `IF`.
- **Tương tranh F08**: lớp 30 chỗ, 100 sinh viên đăng ký đồng thời → đúng 30
  thành công, đối soát bộ đếm không lệch.
- **Trần tín chỉ** trên `SinhVienHocKy` (khoá kép), không phải bộ đếm phẳng.
- **`sp_getapplock` lấy trước mọi phép kiểm** tín chỉ / trùng lịch / trùng môn.
- **Chống trùng môn** bằng unique filtered index, không dựa vào `UNIQUE(SV, LopHP)`.
- **Một đợt `DANG_MO`** mỗi cơ sở mỗi học kỳ, chặn ở DB bằng filtered index (`V3`).
- **Trùng lịch GV theo cả hai đường**: gán GV cho lớp đã có lịch, và đặt lịch cho
  lớp đã có GV.
- **Hợp đồng API không lệch code**: `openapi.json` sinh từ controller/DTO,
  `OpenApiContractTest` làm build đỏ nếu file lệch.

### Quyền sử dụng endpoint

Gom theo vai trò cho dễ đối chiếu với B3. Chi tiết từng endpoint vẫn ở
[API Contract](PTIT-One-API-Contract.md); bảng này để thấy toàn bộ bề mặt API
trong một chỗ.

Quyền đến từ ba nơi trong code: `SecurityConfig` (route công khai),
`@PreAuthorize` (chỉ 6 chỗ), và **phần lớn là phép kiểm trong service** —
nên không đọc `@PreAuthorize` mà suy ra quyền được.

**Công khai — không cần đăng nhập** (9)

`GET /api/health` · `GET /api/auth/csrf` ·
`POST /api/auth/login` `refresh` `logout` ·
`POST /api/auth/activate` `activate/resend` ·
`POST /api/auth/forgot-password` `reset-password`

Bốn endpoint cuối phải công khai vì người chưa kích hoạt hoặc quên mật khẩu thì
chưa đăng nhập được.

**Chỉ cần đăng nhập, mọi vai trò** (18)

| Nhóm | Endpoint |
|---|---|
| Phiên | `POST /api/auth/logout-all` · `GET /api/auth/me` |
| Email & mật khẩu | `GET`/`PUT /api/auth/email` · `POST /api/auth/email/resend` `email/verify` · `POST /api/auth/change-password` |
| Danh mục | `GET /api/courses` `/{maMonHoc}` · `/api/faculties` · `/api/terms` · `/api/programs` `/{maCTDT}` |
| Có giới hạn phạm vi cơ sở | `GET /api/classes` `/{maLopHP}` `/{maLopHP}/schedule` · `/api/teachers` · `/api/enrollment-periods` |

Năm endpoint cuối ai đăng nhập cũng gọi được, nhưng **chỉ thấy dữ liệu cơ sở của
mình**; `ADMIN_MASTER` thấy mọi cơ sở. Phạm vi lấy từ JWT, không nhận tham số
`maCoSo`.

**Theo vai trò** (41)

| Vai trò | Endpoint |
|---|---|
| `SINH_VIEN` | `GET /api/me/timetable` · `GET`/`POST /api/me/enrollments` · `DELETE /api/me/enrollments/{maLopHP}` · `GET /api/me/grades` |
| `SINH_VIEN` + `GIANG_VIEN` | `GET /api/me/notifications` `unread-count` · `POST /api/me/notifications/{maThongBao}/read` `read-all` |
| `GIANG_VIEN` | `GET /api/me/teaching-classes` · `GET /api/me/teaching-schedule` |
| **GV phụ trách lớp đó** | `PUT /api/classes/{maLopHP}/grades` · `POST /api/classes/{maLopHP}/grades/publish` |
| **GV phụ trách lớp · Admin cơ sở của lớp · Admin Master** | `GET /api/classes/{maLopHP}/students` · `GET /api/classes/{maLopHP}/grades` |
| `ADMIN_CO_SO`, **chỉ cơ sở mình** | `POST /api/classes` · `PUT /api/classes/{maLopHP}` `/{maLopHP}/teacher` `/{maLopHP}/schedule` · `POST /api/classes/{maLopHP}/cancel` `/{maLopHP}/grades/lock` · `POST /api/enrollment-periods` · `PUT /api/enrollment-periods/{maDot}` |
| `ADMIN_CO_SO` + `ADMIN_MASTER` | `GET /api/health/db` · `GET /api/reports/summary` `/api/reports/courses` |
| `ADMIN_MASTER` | `GET /api/accounts` · `POST /api/accounts/{tenDangNhap}/activation-code` · `PUT /api/accounts/{tenDangNhap}/status` · `POST /api/courses` · `PUT /api/courses/{maMonHoc}` · `PUT /api/courses/{maMonHoc}/prerequisites` · `POST /api/students` · `POST /api/teachers` |
| Mọi vai trò **trừ** `SINH_VIEN` | `GET`/`POST /api/notifications` · `POST /api/notifications/preview` · `GET`/`PUT`/`DELETE /api/notifications/{maThongBao}` · `POST /api/notifications/{maThongBao}/send` |

**Bốn chỗ quyền phụ thuộc dữ liệu, không chỉ vai trò**

| Nơi | Quy tắc |
|---|---|
| Nhập / công bố điểm | Phải là **giảng viên đang được phân công lớp đó** (`requireTeacherOf`); GV khác bị `403` |
| Xem danh sách SV và bảng điểm lớp | `requireStaffAccess` — **sinh viên không qua được, kể cả sinh viên của lớp**, vì danh sách chứa thông tin người khác |
| Khoá điểm | `ADMIN_CO_SO` của lớp, **không phải** giảng viên — tách khỏi quyền nhập điểm có chủ đích |
| Phạm vi gửi thông báo | `TOAN_TRUONG` chỉ `ADMIN_MASTER`; `CO_SO` là Admin cơ sở (cơ sở mình) hoặc Master, GV bị từ chối; `LOP_HOC_PHAN` thì GV chỉ gửi được cho lớp mình dạy |

Thống kê cũng thu hẹp theo vai trò: `ADMIN_MASTER` xem mọi cơ sở, `ADMIN_CO_SO`
bị ép về cơ sở mình ngay cả khi truyền `?maCoSo=` của cơ sở khác (`403`).

Cộng lại: **9 công khai + 18 chỉ cần đăng nhập + 41 theo vai trò = 68 endpoint**,
khớp số ở mục 1.

---

## 4. Chưa làm — Phần 1

### Frontend là phần còn lại lớn nhất

Trong 11 thư mục `features/` của `apps/web`, chỉ **3** có code thật:

| Thư mục | Dòng code | Trạng thái |
|---|---|---|
| `auth/` | 688 | Có |
| `thong-bao/` | 357 | Có |
| `thong-ke/` | 90 | Có |
| `bang-diem/` `bao-cao/` `dang-ky/` `danh-muc/` `lich-hoc/` `nhap-diem/` `xray/` | 1 mỗi thư mục | **Rỗng, chỉ có khung** |
| `lien-co-so/` | 1 | Rỗng — **đúng, vì liên cơ sở thuộc Phần 2** |

Các trang hiện có phần lớn là `PlaceholderPage`. Toàn bộ màn hình nghiệp vụ của
F03–F09 chưa dựng, dù API đã sẵn — đây là việc nên ưu tiên ngay.

### Backend còn thiếu

- **Bootstrap Admin Master đầu tiên.** Hiện phụ thuộc tài khoản `admin.master`
  của seed. Máy mới không chạy seed thì không có đường tạo admin đầu tiên.
- **Hai sự kiện thông báo**: đổi lịch / đổi phòng, và nhắc đợt đăng ký sắp đóng.
Liên cơ sở **không** nằm trong danh sách này: Phần 1 chỉ đăng ký lớp cùng cơ sở
theo quyết định 02/10/2026, phần liên cơ sở để Phần 2.

---

## 5. Chưa làm — Phần 2 (đúng kế hoạch)

Phần 2 chưa bắt đầu, **theo đúng quyết định chia phạm vi ngày 24/09/2026**, không
phải chậm tiến độ. Những việc còn nguyên:

- **Đăng ký liên cơ sở** (lớp trực tuyến, Home kiểm và Host tự kiểm lại). Phần 1
  từ chối mọi lớp khác cơ sở; cờ `ChoPhepLienCoSo` và ràng buộc
  `CROSS_CAMPUS_REQUIRES_ONLINE` đã có sẵn làm nền cho Phần 2.
- Phân mảnh ngang theo cơ sở; vị từ dẫn xuất `DangKyHocPhan ⋉ LopHocPhan`
  (và `Diem` dẫn xuất bậc 2).
- Nhân bản một chiều Master → site, trigger `NOT FOR REPLICATION` ở Subscriber,
  `DENY` ghi trên bảng nhân bản.
- Giao dịch phân tán cho **chuyển cơ sở sinh viên** (chỉ dùng ở đây).
- Truy vấn phân tán và ba port `CrossSiteQuery`, `GlobalReport`, `CatalogHealth`.
- `SiteContext`, `RoutingDataSource`, `OutboxWorker` và luồng Outbox cho sinh
  viên khách.
- `LichHocMirror` tại Home và bộ đếm `SoTinChiDangGiuCho` theo luồng Home/Host.

Hai điểm đã ghi nhận trước để Phần 2 không vỡ:

- `PhienDangNhap` và `TokenLamMoi` **phải site-local**, không nhân bản.
- `PhienBanTaiKhoan` nằm trong `DanhBaNguoiDung` được nhân bản, nên `logout-all`
  sẽ thành "có hiệu lực sau độ trễ nhân bản" (đo được khoảng 3 giây).

---

## 6. Nợ kỹ thuật và rủi ro đã biết

Bốn mục dưới đây đã đối chiếu với source hôm nay. Mục 6.4 từng chặn thật và
đã vá; ba mục còn lại không chặn gì.

### 6.1 Một chỗ đọc mở sớm hơn nhu cầu (nhỏ, không chặn gì)

Phần 1 **chỉ đăng ký lớp cùng cơ sở** theo quyết định 02/10/2026;
`EnrollmentService` từ chối mọi lớp khác cơ sở **vô điều kiện**, không xét
`ChoPhepLienCoSo`. `GET /api/classes` lọc cứng theo cơ sở người gọi — đúng hành
vi, không phải lỗ hổng.

Chỉ còn một chỗ hơi lệch: `ClassSectionService.requireReadable` cho sinh viên cơ
sở khác **đọc chi tiết** lớp có `ChoPhepLienCoSo = 1`, kèm chú thích "đó là điều
kiện để sinh viên nơi khác biết mà đăng ký". Ở Phần 1 họ không đăng ký được, nên
đường đọc đó chưa phục vụ gì — nó là phần dọn trước cho Phần 2. Không sai về bảo
mật (lớp đó cố ý công khai), chỉ là chú thích dễ gây hiểu nhầm rằng Phần 1 có
liên cơ sở. Sửa chú thích là đủ.

### 6.2 Tài liệu API Contract lệch ở hai chỗ nhỏ

- Thiếu mã `ACCOUNT_STATUS_INVALID` trong bảng mã lỗi (81 trên 82 mã).
- Mục "⚠️ Lỗ hổng cần vá: `SCHEDULE_TEACHER_CLASH`" **đã lạc hậu** — cả hai
  đường vào ràng buộc trùng lịch GV giờ đều có test; bảng kịch bản đã cập nhật
  nhưng đoạn văn bên dưới thì chưa.

Phần hình dạng API thì không thể lệch, vì có `OpenApiContractTest` chặn. Chỉ các
phần viết tay (quyền, mã lỗi, quy tắc) mới trôi được — đúng giới hạn đã lường trước.

### 6.3 Hai ca biên chưa có test

- Đặt lịch rỗng để xoá hết lịch của lớp.
- `/api/health/db` trả `503` kèm `DOWN` khi SQL Server tắt.

### 6.4 Mã cơ sở lưu sai chữ hoa — **đã xảy ra thật, đã vá bằng `V7`**

Bản đầu của báo cáo này xếp mục đó là "dữ liệu cũ, không ảnh hưởng gì". **Đánh
giá đó sai mức độ.** Ngày 03/10/2026 nó làm một tài khoản sinh viên **không đăng
ký được học phần nào**, báo `ENROLLMENT_CROSS_CAMPUS` dù sinh viên và lớp cùng ở
TP.HCM.

Cơ chế của lỗi — đáng ghi lại vì nó sẽ lặp ở Phần 2 với mọi cột mã:

- Hồ sơ lưu `MaCoSoNha = 'hcm'`, lớp có `MaCoSoHost = 'HCM'`.
- `EnrollmentService` so sánh bằng `String.equals` của Java → **phân biệt hoa
  thường** → coi là khác cơ sở.
- SQL Server dùng collation `Vietnamese_CI_AS` → **không** phân biệt hoa thường
  → khoá ngoại tới `dbo.CoSo` nhận `'hcm'` mà không báo gì.
- Tầng SQL thấy hợp lệ, tầng Java thấy khác nhau. Sai lệch im lặng, không log.

Ba lớp đã vá:

| Lớp | Cách vá |
|---|---|
| Đường ghi | `fdd68a0` — `findCampus` lấy giá trị chuẩn từ `dbo.CoSo`, không lưu nguyên chuỗi client gửi |
| Dữ liệu cũ | `V7` sửa cả 9 cột mã cơ sở trên 9 bảng, chạy trên mọi máy qua Flyway |
| Phòng tái diễn | `V7` thêm 10 CHECK constraint ép chữ hoa |

⚠️ **Bài học kỹ thuật:** trong database collation CI, `CHECK (MaCoSo =
UPPER(MaCoSo))` là **vô dụng** — hai vế luôn bằng nhau nên ràng buộc không bao
giờ chặn. Phải ép collation nhị phân:

```sql
CHECK (MaCoSo COLLATE Latin1_General_BIN2 = UPPER(MaCoSo) COLLATE Latin1_General_BIN2)
```

Cùng lý do, `WHERE MaCoSo <> 'HCM'` **không** tìm ra dòng lưu `'hcm'`. Muốn soát
dữ liệu sai hoa thường thì phải `COLLATE` nhị phân.

Đã nghiệm thu trên DB thật: 0 dòng còn sai, 10 CHECK có hiệu lực, và thử ghi lại
`'hcm'` thì bị từ chối đúng như mong đợi.

---

## 7. Việc nên làm tiếp, theo thứ tự

1. **Dựng màn hình frontend cho F03–F09.** API đã xong; đây là đường tới sản
   phẩm chạy được và là phần còn thiếu lớn nhất của Phần 1.
2. Bootstrap Admin Master đầu tiên, để máy mới không phải dựa vào seed.
3. Hai sự kiện thông báo còn thiếu; hai ca test biên ở mục 6.3.
4. Sửa hai chỗ lệch của API Contract (mục 6.2) và chú thích ở mục 6.1 — gộp một
   lần, mất vài phút.
5. Sau khi Phần 1 chạy được đầu-cuối: bắt đầu Phần F cài đặt vật lý của Phần 2.

**Backend Phần 1 không còn việc nào đáng kể.** Toàn bộ đường tới sản phẩm chạy
được nằm ở mục 1.

---

## Liên quan

[Trạng thái backend](PTIT-One-Backend-Khoi-Dong.md) ·
[API Contract](PTIT-One-API-Contract.md) ·
[Kế hoạch Phần 1](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md) ·
[Thiết kế](PTIT-One-Thiet-Ke.md) ·
[Cài đặt Phần 1](PTIT-One-Cai-Dat-Phan-1.md)
