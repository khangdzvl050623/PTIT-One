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
| Migration | 6 (`V1`–`V6`) |
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
| F00 | Spring Boot 4.1.1 / JDK 21, profile `central` một DataSource, Flyway `V1`–`V6`, CI hai job | Xong |
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

Bốn mục dưới đây đã đối chiếu với source hôm nay. Không mục nào chặn tiến độ.

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

### 6.4 Dữ liệu thử còn sót trong database máy dev

Database `PTITONE_CENTRAL` trên máy đang phát triển có hai hồ sơ tạo tay khi thử
F02: `N23DVCN027` và `N23DVCN0277`, cả hai `MaCoSoNha = 'hcm'` **chữ thường**.

Lưu ý: code hiện tại **có** chuẩn hoá mã cơ sở — `findCampus` trả về giá trị
chuẩn từ bảng `CoSo` — nên đây là dữ liệu cũ, không phải lỗi đang chạy. Nhưng vì
SQL Server dùng collation không phân biệt hoa thường còn so sánh trong Java thì
có, hồ sơ lưu sai hoa thường sẽ hành xử không nhất quán. Nên xoá hai dòng này
cùng các dòng liên quan (`DanhBaNguoiDung`, `TaiKhoan`, `MaKichHoat`,
`PhienDangNhap`) theo thứ tự an toàn khoá ngoại.

Không ảnh hưởng CI vì CI không có database.

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
