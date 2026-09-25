# PTIT One — Kế hoạch module auth, Phần 1

Lập 25/09/2026. **Bản đề xuất triển khai, chưa phải contract đã chốt.**
Không thay thế quyết định trong [thiết kế](PTIT-One-Thiet-Ke.md).

**Mục tiêu:** mở web → đăng nhập bằng tài khoản trong SQL Server → nhận đúng
danh tính/quyền → gọi API được phép → đăng xuất → phiên cũ bị từ chối.

**Hiện trạng:** `apps/api` đã có JDBC, Flyway và profile `central`. `auth` mới
chỉ có `package-info.java`. `db/central/migrations/` chưa có schema. Web đã có
`LoginForm` nhưng chưa nối API.

---

## 1. Phân mốc

> Môn này chấm **CSDL phân tán** (~75% điểm phần bắt buộc). Auth là hạ tầng
> phục vụ nghiệp vụ, **không phải hạng mục được chấm**.

| Mốc | Gồm | Ước lượng |
|---|---|---|
| **A0** — nền auth | Security, Argon2id, access JWT 15 phút, refresh 7 ngày **có rotation + replay**, `PhienDangNhap` + `TokenLamMoi`, logout / logout-all, kiểm `sid`+`accountVersion` mỗi request, phân quyền theo role, tài khoản seed, nối web | 5–7 ngày |
| **A1** — siết chặt | Ma trận test tương tranh, rate limit `429`, đổi mật khẩu, bootstrap/recovery Admin | khi còn thời gian |
| **B** — F02 | Cấp hồ sơ, mã kích hoạt một lần, khóa/ngừng tài khoản | sau A0 |

⚠️ **Cổng vào Phần 2 là A0, không phải A1.** Xong A0 thì F03–F09 có principal
đáng tin để chạy, và hạ tầng phân tán bắt đầu được.

**Vì sao rotation + replay vẫn ở A0 dù nghe nặng:** trong thiết kế này **mỗi
phiên chính là một token family**, nên "thu hồi family" và "thu hồi phiên" là
cùng một thao tác — không thêm bảng, không thêm khái niệm. Phát hiện replay chỉ
là `UPDATE token cũ WHERE chưa dùng` rồi kiểm `@@ROWCOUNT`; bằng 0 nghĩa là
token đã dùng được trình lại → thu hồi phiên. Phần tốn thời gian thật là **ma
trận test**, đã đẩy sang A1.

Không làm ở Phần 1: quên mật khẩu qua email/SMS, OAuth mạng xã hội, MFA, saga,
Outbox, Redis, định tuyến đa DataSource.

## 2. Quyết định cần chốt với TV4 (TV2 tham gia phần dữ liệu)

| Chủ đề | Chốt | Lưu ý |
|---|---|---|
| Xác thực | Spring Security + JWT có chữ ký, dùng JOSE/Nimbus của Spring | Không thêm thư viện JWT thứ hai |
| Băm mật khẩu | **Argon2id** qua `DelegatingPasswordEncoder`, id `{argon2id-v1}` | `m=19456 KiB`, `t=2`, `p=1`, salt 16B, hash 32B — điểm bắt đầu benchmark, **đo trên máy thật**. Cần BouncyCastle. Không bật `noop` |
| Cookie | Hai cookie `HttpOnly`, `Secure` khi HTTPS, `SameSite=Lax`, host-only; access path `/api`, refresh path `/api/auth` | Không lưu token trong `localStorage`. CSRF bảo vệ cả login/refresh/logout |
| Thời hạn | Access **15 phút**, refresh **7 ngày** | 7 ngày là hạn **tuyệt đối** từ lần login; rotation không kéo dài. Access không sống quá hạn phiên |
| Thu hồi | JWT chứa `sid` + `accountVersion`; **mỗi request** kiểm phiên và version trong CENTRAL | Đây là thứ làm revoke có hiệu lực ngay thay vì chờ hết 15 phút. Cái giá là một lượt đọc DB mỗi request — cần index |
| Danh tính | `username`, `role`, `entityId`, `homeCampus`, `accountVersion` | Backend suy ra từ DB; **client không gửi role/campus** để quyết định quyền |
| Role | `SINH_VIEN`, `GIANG_VIEN`, `ADMIN_CO_SO`, `ADMIN_MASTER` | `ADMIN_MASTER` có `MaCoSo = NULL`; không tạo mã cơ sở giả `MASTER` |
| Trạng thái | Chỉ `HOAT_DONG` được đăng nhập | `CHO_KICH_HOAT`, `NGUNG`, `DANG_CHUYEN` bị từ chối |

JWT có `iss`, `aud`, `iat`, `exp`, `sub`. Decoder kiểm chữ ký, thuật toán cho
phép, issuer, audience, thời hạn. **Khóa ký nằm ngoài Git và ngoài `VITE_*`.**

**DB lỗi khi kiểm phiên → từ chối**, không chấp nhận JWT chỉ vì chữ ký hợp lệ.
Logout chỉ báo thành công **sau khi** việc thu hồi đã commit.

## 3. Refresh token — rotation và revoke

Refresh là **chuỗi opaque 32 byte từ CSPRNG**, không phải JWT. Chỉ token gốc đi
qua cookie; DB giữ **SHA-256** của nó để tra cứu. SHA-256 hợp ở đây vì bí mật
có entropy cao — **không dùng thay Argon2 cho mật khẩu người**.

Mỗi login tạo một `PhienDangNhap` (UUID) = một token family. Refresh trong phiên
tạo chuỗi `R0 → R1 → R2`. Hạn phiên cố định `loginAt + 7 ngày`.

`POST /api/auth/refresh`, trong **một transaction** tại CENTRAL:

1. Kiểm CSRF, đọc cookie refresh, tra hash. Không đòi access còn hạn.
2. Kiểm tài khoản hoạt động, version đúng, phiên chưa thu hồi/hết hạn.
3. `UPDATE` token cũ có điều kiện "chưa dùng", **kiểm `@@ROWCOUNT`**, rồi
   `INSERT` token mới. Không được có hai successor.
4. Commit **trước khi** trả cookie mới. Rotation không kéo dài hạn 7 ngày.

⚠️ **Token đã dùng được trình lại = replay → thu hồi cả phiên**, commit việc thu
hồi **rồi mới** trả `401`. Đừng ném lỗi làm rollback mất luôn thao tác revoke.

Giữ hash token đã dùng tới hết hạn phiên để còn phát hiện replay; đừng xóa ngay
khi rotation.

| Sự kiện | Hành động |
|---|---|
| Logout | Thu hồi phiên hiện tại, xóa hai cookie |
| Logout-all | Tăng `PhienBanTaiKhoan`, thu hồi mọi phiên |
| Khóa / đổi role / đổi mật khẩu | Cập nhật + tăng version + thu hồi mọi phiên, **trong một transaction** |
| Dùng lại refresh đã rotate | Thu hồi phiên đó; phiên thiết bị khác không ảnh hưởng |

Frontend chỉ refresh **một lần tại một thời điểm**, phối hợp giữa các tab, retry
tối đa một lần. Không tự retry thao tác ghi khi timeout.

## 4. Dữ liệu cần

| Bảng | Ghi chú |
|---|---|
| `DanhBaNguoiDung`, `TaiKhoan`, `TaiKhoanMaster` | Theo thiết kế, cùng CENTRAL ở Phần 1 |
| `SinhVien`, `GiangVien` tối thiểu | Đủ FK để đối soát danh tính |
| `PhienDangNhap` *(mới)* | `MaPhien` UUID, `TenDangNhap`, version lúc tạo, thời điểm tạo/hết hạn/thu hồi, lý do |
| `TokenLamMoi` *(mới)* | UUID, FK phiên, `TokenHash` **unique**, thời điểm tạo/hết hạn/đã dùng/thu hồi. **Không lưu token gốc** |

- SQL ở repository, có tham số. Migration nguồn duy nhất `db/central/migrations/`.
  **Không `IDENTITY`, không `MERGE`.**
- Seed: 2 SV, 2 GV, 2 nhóm Admin, thêm tài khoản chưa kích hoạt/ngừng, có fixture
  khác cơ sở. Chạy chủ động; chạy lại không nhân đôi, không tự seed lúc khởi động.
- **Seed phải băm bằng đúng encoder API dùng.** TV5 sinh hash rồi đưa chuỗi đã
  băm cho TV2 — mỗi người tự băm một kiểu thì login sai 100% trong khi nhìn dữ
  liệu vẫn thấy "có tài khoản".
- Chốt nguồn quyền authoritative khi role có ở cả danh bạ lẫn tài khoản; lệch thì
  **từ chối**, không tự chọn quyền cao hơn.

### ⚠️ Ba ghi chú phải ghi vào migration — nếu không, Phần 2 mổ lại

Phần 1 một database nên chưa lộ, nhưng vị trí bảng quyết định ngay ở migration đầu.

| Bảng | Ghi chú |
|---|---|
| `PhienDangNhap` | **Cục bộ tại site, KHÔNG nhân bản.** Phiên được ghi lúc đăng nhập; xếp nhầm vào nhóm bảng tham chiếu thì `DENY` ở Subscriber chặn chính việc đăng nhập |
| `TokenLamMoi` | Như trên |
| `PhienBanTaiKhoan` | Cột này nằm trong **`DanhBaNguoiDung` — 1 trong 9 bảng được nhân bản**, MASTER sở hữu, site **chỉ đọc**. Sang Phần 2, "logout-all có hiệu lực ngay" thành "**sau độ trễ nhân bản**" (~3 giây đo trên máy HCM) |

Cách xử lý dòng thứ ba chốt ở Phần 2, không phải bây giờ. Phần 1 chỉ cần **ghi
nhận là đã biết**.

## 5. Contract API

| API | Vào / quyền | Ra |
|---|---|---|
| `GET /api/auth/csrf` | Public | Token CSRF; không phải phiên đã đăng nhập |
| `POST /api/auth/login` | `{username, password}` + CSRF | `200`, cookie access 15' + refresh 7d, thông tin tối thiểu |
| `GET /api/auth/me` | Phiên hợp lệ | `username`, `role`, `entityId`, `homeCampus`, `expiresAt` |
| `POST /api/auth/refresh` | Cookie refresh + CSRF; access có thể hết hạn | `200`, rotate refresh + access mới; hạn phiên không đổi |
| `POST /api/auth/logout` | CSRF; xác định phiên qua access hoặc hash refresh | `204` sau khi thu hồi + xóa cookie |
| `POST /api/auth/logout-all` | Access + CSRF | `204` sau khi tăng version + thu hồi mọi phiên |
| `POST /api/auth/change-password` — **A1** | Mật khẩu cũ/mới + CSRF | Cập nhật hash, tăng version, thu hồi mọi phiên, đăng nhập lại |
| `POST /api/auth/activate` — **B** | Định danh hồ sơ, mã kích hoạt, mật khẩu mới | Kích hoạt nguyên tử một lần |

Lỗi dùng chung `{ code, message, fieldErrors?, traceId }`:

`400 VALIDATION_ERROR` · `401 AUTH_INVALID_CREDENTIALS` (thông báo chung, không
lộ tài khoản tồn tại) · `401 AUTH_SESSION_INVALID` · `401 AUTH_REFRESH_INVALID`
· `403 AUTH_FORBIDDEN` · `403 CSRF_INVALID` · `429 AUTH_TOO_MANY_ATTEMPTS` (A1)
· `503 SERVICE_UNAVAILABLE` (không trả SQL exception ra ngoài).

## 6. Phân quyền và ranh giới module

| Vai trò | Phạm vi |
|---|---|
| SV | Chỉ dữ liệu của mình |
| GV | Lớp được phân công, SV/điểm thuộc lớp đó |
| Admin cơ sở | Hồ sơ, tài khoản, lớp, đợt trong cơ sở được cấp; **không** tự có quyền sửa điểm hay danh mục toàn trường |
| Admin Master | Danh mục toàn trường, quyền tài khoản theo B3; **không** mặc định sửa mọi nghiệp vụ |

Một DB **không** có nghĩa bỏ giới hạn cơ sở hay gộp hai nhóm Admin.

- `auth` cấp principal; module khác **tự kiểm quyền bản ghi** trong service/query.
- Không tin `studentId`, `teacherId`, `campus`, `role` client gửi.
- Không cho module khác đọc thẳng `auth.repository`. `shared` không import auth.

```text
auth/
  controller/  AuthController
  service/     AuthenticationService, SessionService, AccountService
  repository/  AccountRepository, SessionRepository, RefreshTokenRepository
  dto/  model/  security/   (policy/ chỉ khi quy tắc đủ phức tạp)
```

Controller gọi service; transaction ở service; SQL ở repository.
`model/policy` không import Spring/JDBC.

## 7. Thứ tự công việc

| Thẻ | Mốc | Việc | Phụ thuộc | Chủ trì | Nghiệm thu |
|---|---|---|---|---|---|
| AUTH-01 | A0 | Chốt contract danh tính, role/scope, cookie/CSRF, mã lỗi | — | TV5 + TV4 + TV2 | Contract + ma trận quyền có ví dụ request/response |
| AUTH-02 | A0 | Migration lát cắt auth + seed | 01 | **TV5**, TV2 review | DB mới migrate được; seed chạy lại không nhân đôi; **ba ghi chú Phần 2 có trong migration** |
| AUTH-03 | A0 | Security, Argon2id, JWT, cookie/CSRF, JSON 401/403; **khóa `/api/health/db`** | 01 | TV5 | Route policy đúng; benchmark encoder; liveness vẫn chạy |
| AUTH-04 | A0 | Login/me, refresh + rotation + replay, logout/logout-all | 02+03 | TV5 | Luồng thật qua CENTRAL; thu hồi bền vững, rotation nguyên tử |
| AUTH-05 | A0 | Nối web: trạng thái đăng nhập, role routing, refresh giữa các tab, logout | 01, ghép sau 04 | TV6 + TV5 | Login → reload → access hết hạn → refresh → logout chạy thật |
| AUTH-06 | A0 | Kiểm thử và bàn giao — **cổng vào Phần 2** | 04+05 | TV4 | Demo chạy thật + bằng chứng |
| AUTH-1a | A1 | Test tương tranh refresh/replay, rate limit | 06 | TV4 + TV5 | |
| AUTH-1b | A1 | Đổi mật khẩu, bootstrap/recovery Admin | 06 | TV5 | |
| AUTH-07 | — | Ca quyền theo bản ghi trên API nghiệp vụ thật | F03–F09 | TV4 + owner | SV không xem người khác; GV không sửa lớp khác |
| AUTH-08 | B | F02: cấp, kích hoạt, khóa tài khoản | A0 | TV5 + TV2 + TV6 | Mã một lần, rollback, chống nâng quyền |

**AUTH-02 do TV5 chủ trì** vì TV2 đang nợ `feature/db-central-schema` — thứ chặn
toàn bộ F03–F09 — nên họ là đường găng. Lát cắt auth khép kín, không cần schema
học vụ. TV2 vẫn review để thống nhất quy ước đặt tên.

AUTH-02 và AUTH-03 làm song song được sau contract. Web dựng theo contract
trong lúc chờ API, mock phải có nhãn.

Nhánh: `feature/auth-contract` → `feature/db-auth-schema` → `feature/api-auth`
→ `feature/web-auth`. Mỗi nhánh từ `dev`, PR về `dev`.

**Không lấy H2 hoặc mock repository làm bằng chứng đã tích hợp SQL Server.**

## 8. Tài liệu đối chiếu

[Thiết kế: 0.1b, B3, C2/C3, J1](PTIT-One-Thiet-Ke.md) ·
[Kế hoạch chung: F00–F02](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md) ·
[Trạng thái backend](PTIT-One-Backend-Khoi-Dong.md) ·
[Spring JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) ·
[Spring Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html) ·
[Spring CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) ·
[OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) ·
[RFC 9700 §4.14.2 — rotation/replay](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14.2)
