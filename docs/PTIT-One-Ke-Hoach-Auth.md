# PTIT One — Kế hoạch module auth, Phần 1

Ngày lập: 25/09/2026. Trạng thái: **bản đề xuất triển khai, chưa phải contract đã chốt**.
Tài liệu này không thay thế các quyết định trong `PTIT-One-Thiet-Ke.md`.
Theo yêu cầu cập nhật: **access token 15 phút, refresh token 7 ngày**.
Argon2id, cơ chế rotation/revoke và quy trình cấp tài khoản bên dưới là đề xuất
kỹ thuật/nghiệp vụ để đưa vào contract; chưa phải chức năng đã triển khai.

## 1. Mục tiêu và hiện trạng

Hoàn thành một luồng thật: mở web → đăng nhập bằng tài khoản trong SQL Server
→ nhận đúng danh tính/quyền → gọi API được phép → đăng xuất → phiên cũ bị từ chối.
Sau đó bổ sung cấp và kích hoạt tài khoản theo F02.

- Backend: `apps/api`, Spring Boot 4.1.1, Java 21; đã có JDBC, Flyway và profile `central`.
- `auth` hiện chỉ có `package-info.java`; chưa có Spring Security, login hoặc quản lý phiên.
- `db/central/migrations/` và `seed/` chưa có schema/fixture nghiệp vụ.
- Web đã có `LoginForm` nhận `username/password` qua `onSubmit`, chưa nối API.
- Mọi truy vấn xác thực dùng **một database `PTITONE_CENTRAL`** theo D20.
- F01 dùng tài khoản seed; không cần chờ luồng tạo tài khoản F02.

## 2. Phạm vi và các mốc bàn giao

> **Nguyên tắc phân mốc (chốt 25/09/2026):** môn này chấm **CSDL phân tán**,
> phần bắt buộc ~75% điểm. Auth là hạ tầng phục vụ nghiệp vụ, **không phải
> hạng mục được chấm**. Vì vậy **A0 xong là mở khóa F03–F09 và bắt đầu được
> Phần 2**; A1 không phải điều kiện để sang Phần 2.

| Mốc | Bao gồm | Điều kiện hoàn thành |
|---|---|---|
| **A0 — Nền auth, mở khóa F03–F09** | Spring Security, Argon2id, access JWT 15 phút, refresh 7 ngày **có rotation**, `PhienDangNhap` + `TokenLamMoi`, logout / logout-all, kiểm `sid`+`accountVersion` mỗi request, phân quyền theo role, tài khoản seed, tích hợp web | Luồng thật qua CENTRAL: login → reload → access hết hạn → refresh → logout → phiên cũ bị từ chối. Ca sai quyền cơ bản đạt |
| **A1 — Siết chặt, làm khi còn thời gian** | Ma trận test tương tranh refresh/replay, rate limit `429`, đổi mật khẩu, công cụ bootstrap/recovery Admin | Theo mục 8; **không chặn Phần 2** |
| **B — Cấp tài khoản / F02** | Tạo hồ sơ tối thiểu, cấp mã kích hoạt SV/GV, khóa/ngừng tài khoản | Tài khoản mới đăng nhập được; chống chiếm hồ sơ, kích hoạt trùng, nâng quyền, lỗi nửa giao dịch |

**Vì sao rotation + replay nằm ở A0 dù nghe có vẻ nặng:** trong thiết kế này
**mỗi phiên chính là một token family**, nên "thu hồi family" và "thu hồi phiên"
là **cùng một thao tác** — không thêm bảng, không thêm khái niệm. Phát hiện
replay chỉ là `UPDATE token cũ WHERE chưa dùng` rồi kiểm `@@ROWCOUNT`; bằng 0
nghĩa là token đã dùng được trình lại → thu hồi phiên. Đúng idiom mà `AGENTS.md`
đã bắt dùng ở mọi chỗ khác. Phần thật sự tốn thời gian là **ma trận test**
tương tranh, và nó nằm ở A1.

**Tài khoản demo dùng seed** (chốt 25/09/2026). A0 **không** cần công cụ
bootstrap/recovery riêng; chúng chuyển sang A1.

Quên mật khẩu tự phục vụ qua email/SMS, OAuth mạng xã hội và MFA không thuộc
bản đầu. Phần 2 mới bổ sung định tuyến đa DataSource/replica; không dựng saga,
Outbox hay Redis cho auth Phần 1.

## 3. Các quyết định cần ghi vào contract trước khi code

Các lựa chọn dưới đây là **đề xuất để TV5 + TV4 đối soát; TV2 tham gia phần dữ liệu**.

| Chủ đề | Đề xuất ban đầu | Hệ quả cần thống nhất |
|---|---|---|
| Cơ chế xác thực | Spring Security và JWT có chữ ký, dùng hỗ trợ JOSE/Nimbus của Spring | Chốt dependency tương thích BOM hiện tại; không tự thêm một thư viện JWT thứ hai |
| Truyền token trên web | Hai cookie `HttpOnly`, `Secure` khi HTTPS, `SameSite=Lax`, host-only; access path `/api`, refresh path `/api/auth` | Không lưu token trong localStorage; bảo vệ CSRF cả login/refresh/logout; cấu hình HTTP local tách riêng |
| Thời hạn | **Access JWT 15 phút; refresh token 7 ngày** | Đề xuất 7 ngày là hạn tuyệt đối từ lần login, rotation không kéo dài; access cấp cuối kỳ không sống quá hạn phiên |
| Thu hồi | JWT chứa `sid` và `accountVersion`; mỗi request kiểm phiên còn hiệu lực, trạng thái và phiên bản tài khoản trong CENTRAL | Chặn cả access token còn hạn sau revoke; thêm lượt đọc DB, khác cache danh bạ Phần 2 và cần ghi trong contract |
| Logout | Logout thường thu hồi phiên hiện tại; logout-all thu hồi mọi phiên và tăng phiên bản tài khoản | Thay đề xuất logout-all mặc định trước đây; không chỉ xóa cookie |
| Khóa/đổi quyền/đổi hoặc reset mật khẩu | Đổi dữ liệu, tăng phiên bản và thu hồi mọi phiên trong một transaction | Request bắt đầu sau commit phải bị chặn với token cũ; không hứa hủy request đã chạy trước commit |
| Băm mật khẩu | Đề xuất **Argon2id qua `DelegatingPasswordEncoder`**; cấu hình mới ghi Argon2id | Có thể xác minh BCrypt cũ nếu cần; không coi factory mặc định là đã chọn Argon2id; đo chi phí trên máy thật |
| Tên đăng nhập | Duy nhất toàn hệ thống; thống nhất chuẩn hóa và collation SQL Server | Chốt phân biệt hoa/thường, khoảng trắng và độ dài; không trim hay đổi nội dung mật khẩu |
| Danh tính | `username`, `role`, `entityId`, `homeCampus`, `accountVersion` | Backend suy ra từ dữ liệu thật; client không gửi role/campus để quyết định quyền |
| F02 | Admin được cấp quyền tạo hồ sơ SV/GV, gắn role/cơ sở và cấp mã kích hoạt một lần | Người nhận chỉ đặt mật khẩu cho hồ sơ đã cấp; không tự nhận vai trò GV/Admin |

JWT còn có `iss`, `aud`, `iat`, `exp` và `sub` là tên đăng nhập duy nhất.
Decoder kiểm chữ ký, thuật toán cho phép, issuer, audience và thời hạn. Khóa ký
nằm ngoài Git và ngoài biến `VITE_*`. Phần 1 dùng cấu hình khóa trực tiếp tại API,
không cần dựng một authorization server riêng.

DB lỗi khi kiểm phiên: từ chối xử lý nghiệp vụ, trả lỗi dịch vụ chung; không
chấp nhận JWT chỉ vì chữ ký còn hợp lệ. Logout chỉ báo thành công sau khi việc
thu hồi đã commit; nếu DB lỗi thì không khẳng định token đã bị thu hồi.

### 3.1. BCrypt và Argon2id

`DelegatingPasswordEncoder` là bộ chọn encoder theo prefix `{id}`, không phải
thuật toán băm. Dùng được cả BCrypt lẫn Argon2id, và hỗ trợ nâng cấp định dạng.

| Tiêu chí | BCrypt | Argon2id |
|---|---|---|
| Chi phí tấn công | Điều chỉnh chi phí CPU; phù hợp hệ thống đang dùng BCrypt | Điều chỉnh cả bộ nhớ và thời gian, gây tốn kém hơn cho tấn công phần cứng song song |
| Giới hạn đáng lưu ý | Thường tối đa 72 byte mật khẩu, không phải 72 ký tự | Không mang giới hạn 72 byte này; ứng dụng vẫn giới hạn input hợp lý |
| Spring | Tích hợp đơn giản | `Argon2PasswordEncoder` hiện cần BouncyCastle |
| Lựa chọn cho PTIT One mới | Có thể dùng nếu bị ràng buộc môi trường; không cần chuyển ngược từ Argon2 | **Đề xuất ưu tiên**, theo OWASP cho hệ thống mới |

Thông số khởi điểm đề xuất: Argon2id `m=19456 KiB` (19 MiB), `t=2`, `p=1`,
salt 16 byte, hash 32 byte. Đây là điểm bắt đầu benchmark, không phải cấu hình
đã kiểm tải. Đo độ trễ và RAM với nhiều login đồng thời; rate limit trước thao tác
băm nặng. Không mặc nhiên coi mọi preset Spring đều đáp ứng đúng mức đã chọn.

Dùng id encoder có phiên bản cấu hình, ví dụ `{argon2id-v1}`; seed dùng cùng
encoder. Nếu có hash `{bcrypt}` cũ, xác minh bằng BCrypt rồi rehash sau login
thành công khi cần; không thể đổi hash cũ sang Argon2 chỉ bằng SQL. Không bật
`noop` hay fallback chấp nhận mật khẩu plaintext.

### 3.2. Refresh token: lưu, rotation và revoke

Đề xuất refresh token là **chuỗi opaque ngẫu nhiên 32 byte từ CSPRNG**, không
cần JWT. Chỉ trả token gốc qua cookie; DB giữ SHA-256 của token để tra cứu.
SHA-256 ở đây phù hợp bí mật ngẫu nhiên entropy cao, **không dùng thay Argon2
cho mật khẩu con người**. Response token dùng `Cache-Control: no-store` và log
không chứa cookie/token.

Mỗi lần login tạo một `PhienDangNhap` (UUID), cũng là một token family; các lần
refresh trong phiên tạo chuỗi `R0 → R1 → R2`. Hạn phiên cố định `loginAt + 7 ngày`.
Hạn access là `min(now + 15 phút, hạn phiên)`; cookie không sống quá hạn tương ứng.

Luồng `POST /api/auth/refresh`:

1. Kiểm CSRF, đọc riêng cookie refresh và tra hash; không yêu cầu access JWT còn hạn.
2. Trong một transaction tại CENTRAL, kiểm tài khoản hoạt động, version đúng,
   phiên chưa thu hồi/chưa hết hạn và token chưa dùng/chưa thu hồi.
3. Serialize theo phiên; đánh dấu token cũ đã dùng bằng UPDATE có điều kiện,
   kiểm số dòng ảnh hưởng, rồi INSERT token mới. Không được có hai successor.
4. Commit trước khi trả cookie access/refresh mới. Rotation không kéo dài hạn 7 ngày.
5. Nếu token đã dùng được trình lại, coi là replay: **thu hồi cả family/phiên**,
   kể cả token mới nhất, commit việc thu hồi rồi trả `401`. Không throw lỗi khiến
   transaction rollback mất thao tác revoke. Token ngẫu nhiên không khớp DB chỉ
   bị từ chối; không được dùng `sid` client tự khai để thu hồi người khác.

Giữ hash token đã dùng cho đến ít nhất hết hạn family để phát hiện replay; không
xóa ngay khi rotation. Dọn lịch sử đã hết hạn theo chính sách retention đã chốt.
Refresh, logout và revoke phải dùng cùng giao thức khóa; thao tác toàn tài khoản
tuần tự hóa với tạo/refresh phiên theo thứ tự tài khoản → phiên → token, để logout-all
hoặc đổi mật khẩu không đua với refresh tạo phiên hợp lệ trở lại.

| Sự kiện | Hành động |
|---|---|
| Logout thường | Đánh dấu phiên hiện tại bị thu hồi; mọi refresh token thuộc phiên không còn giá trị; xóa cả hai cookie |
| Logout-all | Tăng `PhienBanTaiKhoan`, thu hồi mọi phiên và xóa cookie hiện tại |
| Đổi/reset mật khẩu, khóa/ngừng, đổi role/cơ sở | Cập nhật tài khoản + tăng version + thu hồi mọi phiên trong một transaction |
| Dùng lại refresh token đã rotate | Thu hồi family tương ứng; phiên thiết bị khác không tự bị thu hồi |
| Qua 7 ngày | Từ chối refresh và access của phiên, yêu cầu login lại |

**Revoke refresh token tự nó không hủy JWT access đã cấp.** Đề xuất kiểm `sid`,
trạng thái phiên và version trên mỗi API mới tạo ra hiệu lực chặn ngay sau commit,
thay vì phải đợi tối đa 15 phút. Với DB lỗi, không bỏ qua bước kiểm này.

Frontend chỉ thực hiện một refresh tại một thời điểm, phối hợp giữa các tab dùng
chung cookie, rồi retry request bị từ chối trước nghiệp vụ tối đa một lần. Không
tự retry thao tác ghi khi timeout/không biết server đã xử lý hay chưa. Nếu response
refresh bị mất sau commit, retry token cũ có thể bị xem là replay: bản đầu chấp nhận
buộc login lại, không mở cửa cho token cũ dùng nhiều lần. Phải kiểm ca logout khi
access đã hết hạn, tránh bộ lọc access chặn mất refresh/logout.

## 4. Dữ liệu cần TV2 bàn giao

Giữ các khái niệm trong thiết kế; đề xuất đặt `DanhBaNguoiDung`, `TaiKhoan`,
`TaiKhoanMaster` cùng CENTRAL theo kế hoạch Phần 1. Chốt mapping trước migration,
không tự tạo thêm một hệ tài khoản song song.

| Dữ liệu | Yêu cầu |
|---|---|
| `CoSo` | Các mã cơ sở có thật để tham chiếu |
| `DanhBaNguoiDung` | Tên đăng nhập, loại người dùng, mã thực thể, cơ sở, trạng thái, phiên bản tài khoản |
| `TaiKhoan` | Tên đăng nhập, mật khẩu băm và mapping quyền/thực thể/cơ sở nhất quán với danh bạ |
| `TaiKhoanMaster` | Tài khoản `ADMIN_MASTER`; cùng CENTRAL ở Phần 1 |
| Hồ sơ `SinhVien`, `GiangVien` tối thiểu | Đủ FK và đối soát danh tính; phối hợp module sở hữu hồ sơ |
| `PhienDangNhap` — bổ sung F01 | `MaPhien` UUID, `TenDangNhap`, version lúc tạo, thời điểm tạo/hết hạn/thu hồi, lý do thu hồi; mỗi phiên là một token family |
| `TokenLamMoi` — bổ sung F01 | UUID, FK phiên, `TokenHash` unique, thời điểm tạo/hết hạn/đã dùng/thu hồi, liên kết token thay thế nếu cần; không lưu token gốc |
| Bổ sung F02 | Dữ liệu hash mã kích hoạt, hạn dùng và đã sử dụng; chỉ tạo migration sau khi chốt cấu trúc |

- Bốn role: `SINH_VIEN`, `GIANG_VIEN`, `ADMIN_CO_SO`, `ADMIN_MASTER`.
- `ADMIN_MASTER` có `MaCoSo = NULL`; không tạo mã cơ sở giả `MASTER`.
- Chỉ `HOAT_DONG` được đăng nhập. `CHO_KICH_HOAT`, `NGUNG`, `DANG_CHUYEN` bị từ chối.
- Chốt nguồn quyền authoritative khi role có ở cả danh bạ và tài khoản; sai lệch bị từ chối và ghi log kỹ thuật, không tự chọn quyền cao hơn.
- Ngăn cùng username xuất hiện như hai danh tính ở hai bảng tài khoản; PK từng bảng riêng lẻ chưa đủ. Chốt FK/constraint và transaction kiểm soát việc tạo tài khoản.
- SQL ở repository, có tham số; migration nguồn duy nhất là `db/central/migrations/`, không dùng `IDENTITY` hoặc `MERGE`.
- Seed tối thiểu: hai SV, hai GV, hai nhóm Admin, thêm tài khoản chưa kích hoạt/ngừng. Có fixture khác cơ sở để thử phạm vi quyền.
- Seed chạy chủ động, chạy lại không nhân đôi hoặc reset dữ liệu ngoài fixture; không tự seed khi API khởi động.
- Hai bảng phiên/token phục vụ vòng đời xác thực được yêu cầu, cần TV2 chốt migration, FK, index tra hash/tài khoản và cleanup; không phải CRUD danh mục mới.
- **Seed phải băm bằng đúng encoder API dùng** (`{argon2id-v1}`). Cách chắc ăn:
  TV5 sinh hash rồi đưa chuỗi đã băm cho TV2 dán vào seed — mỗi người tự băm
  một kiểu thì login sai 100% trong khi nhìn dữ liệu vẫn thấy "có tài khoản".

### 4.2. Ba ghi chú bắt buộc ghi vào migration — nếu không, Phần 2 phải mổ lại

Phần 1 dùng một database nên ba điều dưới đây chưa lộ ra. Nhưng vị trí bảng
được quyết định **ngay ở migration đầu tiên**, nên phải ghi từ bây giờ.

| Bảng | Ghi chú |
|---|---|
| `PhienDangNhap` | **Cục bộ tại site, KHÔNG nhân bản.** Phiên được ghi lúc đăng nhập; nếu lỡ xếp chung nhóm với các bảng tham chiếu thì `DENY` ở Subscriber sẽ chặn chính việc đăng nhập |
| `TokenLamMoi` | Như trên — cục bộ, không nhân bản |
| `PhienBanTaiKhoan` | ⚠️ Cột này nằm trong **`DanhBaNguoiDung` — một trong 9 bảng được nhân bản**, do MASTER sở hữu và site **chỉ đọc**. Sang Phần 2, "logout-all có hiệu lực ngay" trở thành "**có hiệu lực sau độ trễ nhân bản**" (đo được ~3 giây trên máy HCM), và site không tự ghi được cột đó |

Hệ quả của dòng thứ ba cần chốt ở Phần 2, **không phải bây giờ**: hoặc thao tác
tăng version đi qua Master, hoặc bổ sung một cơ chế thu hồi cục bộ tại site.
Phần 1 chỉ cần **ghi nhận là đã biết**, đừng để phát hiện lúc đang dựng replication.

### 4.1. Cấp tài khoản SV, GV và Admin — đề xuất nghiệp vụ

**Sinh viên:** nhà trường có danh sách nhập học/hồ sơ hợp lệ → Admin đào tạo tạo
hoặc nhập hồ sơ → cấp tài khoản role `SINH_VIEN`, thường dùng mã SV làm username
→ chuyển mã kích hoạt qua kênh đã xác minh → SV đặt mật khẩu → tài khoản hoạt động.
Giao diện công khai nên ghi **Kích hoạt tài khoản**, không hứa rằng bất kỳ ai
điền MSSV cũng được tạo tài khoản. Không dùng MSSV/ngày sinh làm bí mật xác thực.

**Giảng viên:** Admin cơ sở được cấp quyền tạo hồ sơ từ danh sách nhân sự đã xác
minh, gắn mã GV, khoa, cơ sở và role `GIANG_VIEN`; gửi mã/link kích hoạt một lần
cho đúng người. GV tự đặt mật khẩu, Admin không cần biết mật khẩu đó. Quyền nhập
điểm còn phụ thuộc phân công lớp, không chỉ role. GV thỉnh giảng cũng theo luồng
được cấp hồ sơ; không có nút tự đăng ký làm GV. Việc nhập hàng loạt có thể làm sau
form cấp đơn lẻ. Phạm vi quyền người cấp vẫn phải chốt với TV4.

**Admin đầu tiên — thuộc A1.** A0 dùng tài khoản Admin trong seed, không cần
công cụ riêng. Khi làm A1: có một bước bootstrap chủ động trong quy trình tạo DB mới,
sau migration. Công cụ quản trị cấp một `ADMIN_MASTER` khi chưa có, username và
mật khẩu tạm duy nhất được cấu hình ngoài Git (hoặc sinh an toàn và chuyển qua
kênh quản trị); tuyệt đối không mặc định `admin/admin`. Khi đã tồn tại, chạy lại
không đổi mật khẩu/role hoặc mở lại tài khoản ngừng. Không reset Admin mỗi lần
restart API và không nhúng bí mật cố định trong migration.

Đề xuất cờ `BatBuocDoiMatKhau`: đăng nhập bằng mật khẩu tạm chỉ được nhận quyền
đổi mật khẩu/logout, chưa cấp phiên nghiệp vụ 7 ngày. Cập nhật mật khẩu xong mới
đăng nhập bình thường. Thêm bootstrap/recovery vào kiểm thử và hướng dẫn trước
khi bàn giao F01. Các Admin tiếp theo được cấp bởi Admin Master có thẩm quyền;
Admin cơ sở không được tự nâng quyền hoặc tạo Admin Master.

**Đổi mật khẩu và quên mật khẩu là hai luồng khác nhau:**

- Còn biết mật khẩu: vào Tài khoản → Đổi mật khẩu, xác minh mật khẩu hiện tại.
- Đã quên: cần chứng minh quyền sở hữu qua email đã xác minh hoặc quy trình hỗ
  trợ/quản trị; không được reset chỉ vì biết username. Luồng email tương lai dùng
  token ngẫu nhiên, có hạn, một lần, lưu hash, phản hồi chung để tránh lộ tài khoản.
- Phần đầu chưa nối email: SV/GV liên hệ Admin được cấp quyền; Admin Master cuối
  cùng dùng công cụ recovery tại máy chủ do người vận hành được ủy quyền chạy.
  Cấp bí mật khôi phục một lần có hạn, buộc đặt mật khẩu mới; audit ai/khi nào/tài
  khoản đích, không ghi bí mật. Không tạo DB lại để lấy mật khẩu mặc định.
- Reset hoàn tất phải thu hồi mọi phiên; Admin không được xem mật khẩu hiện tại.
  Link “Quên mật khẩu” nếu chưa tự phục vụ phải hướng dẫn hỗ trợ thật, không báo
  “đã gửi email” khi chưa có dịch vụ gửi.

**SSO/Google/Microsoft:** có thể bổ sung sau nếu trường có hệ thống danh tính.
Đăng nhập qua nhà cung cấp chỉ xác minh danh tính; vẫn cần ánh xạ đến hồ sơ SV/GV
được cấp và quyền nội bộ. Không tự cấp role theo email có đuôi trường. Đăng nhập
Google cũng không có nghĩa gửi mật khẩu Google cho PTIT One. Bản đầu dùng tài
khoản trường cấp; không cần Google/Facebook để hoàn thành nghiệp vụ UIS.

## 5. Contract API dự kiến

Đường dẫn và payload ở đây để nhóm đối soát, chưa phải API đã tồn tại.

| API | Đầu vào / quyền | Kết quả |
|---|---|---|
| `GET /api/auth/csrf` | Public; cơ chế phát token CSRF theo cấu hình Spring | Token CSRF và cách gửi lại header; không coi đây là phiên đã đăng nhập |
| `POST /api/auth/login` | `{ username, password }` và CSRF hợp lệ | `200`, cookie access 15 phút + refresh 7 ngày và thông tin tối thiểu; Admin còn mật khẩu tạm chỉ nhận quyền đổi mật khẩu, chưa có refresh nghiệp vụ |
| `GET /api/auth/me` | Phiên hợp lệ | `200`, `username`, `role`, `entityId`, `homeCampus`, `expiresAt` |
| `POST /api/auth/refresh` | Cookie refresh và CSRF hợp lệ; access có thể hết hạn | `200`, rotate refresh và cấp access mới; hạn tuyệt đối của phiên không đổi |
| `POST /api/auth/logout` | CSRF; xác định phiên qua access hợp lệ hoặc hash refresh khi access hết hạn | `204` sau thu hồi phiên hiện tại và xóa cả hai cookie; đã thu hồi/không có phiên có thể trả `204` và xóa cookie; DB lỗi không báo revoke thành công |
| `POST /api/auth/logout-all` | Access và CSRF hợp lệ | `204` sau tăng version/thu hồi mọi phiên, xóa cookie |
| `POST /api/auth/activate` — F02 | Định danh hồ sơ, mã kích hoạt, mật khẩu mới; CSRF hợp lệ | Kích hoạt nguyên tử một lần, sau đó đăng nhập qua login |
| `POST /api/auth/change-password` — **A1** | Mật khẩu hiện tại/mới, phiên thường hoặc quyền đổi mật khẩu ban đầu, CSRF hợp lệ | Cập nhật hash, bỏ cờ buộc đổi nếu có, tăng version và thu hồi mọi phiên trong một transaction; đăng nhập lại |

API quản trị cấp/khóa tài khoản và cấp lại mã kích hoạt sẽ được đặc tả ở F02,
kèm scope quyền và contract tạo hồ sơ; không gom CRUD hồ sơ vào auth.

Lỗi dùng cấu trúc chung dự kiến `{ code, message, fieldErrors?, traceId }`:

- `400 VALIDATION_ERROR`: dữ liệu không hợp lệ.
- `401 AUTH_INVALID_CREDENTIALS`: sai tên/mật khẩu hoặc tài khoản không được đăng nhập; thông báo chung tránh lộ tài khoản tồn tại.
- `401 AUTH_SESSION_INVALID`: thiếu/hỏng/hết hạn/bị thu hồi token trên endpoint được bảo vệ.
- `401 AUTH_REFRESH_INVALID`: refresh không hợp lệ/hết hạn/replay; chi tiết replay chỉ ở audit, đã commit revoke trước khi trả lỗi.
- `403 AUTH_FORBIDDEN`: đã xác thực nhưng thiếu quyền; `403 CSRF_INVALID` cho CSRF thiếu/sai.
- `429 AUTH_TOO_MANY_ATTEMPTS`: giới hạn thử login/kích hoạt; chốt ngưỡng cấu hình, phạm vi theo tài khoản/IP và thời gian tự giải phóng.
- `503 SERVICE_UNAVAILABLE`: phụ thuộc DB không sẵn sàng, không trả SQL exception hoặc thông tin kết nối.

Chốt thêm mã lỗi kích hoạt sai/hết hạn/đã dùng và xử lý tài nguyên ngoài phạm vi
(`403` hoặc `404` nhất quán) trong contract F02 và từng API nghiệp vụ.

## 6. Phân quyền và ranh giới module

| Vai trò | Phạm vi quyền cơ sở để xây F01 |
|---|---|
| SV | Danh tính của mình; các module sau chỉ cho xem điểm/lịch và đăng ký của mình |
| GV | Danh tính của mình; lớp được phân công, SV/điểm thuộc lớp đó |
| Admin cơ sở | Hồ sơ, tài khoản, lớp, đợt đăng ký trong cơ sở được cấp; không tự có quyền sửa điểm hoặc danh mục toàn trường |
| Admin Master | Danh mục toàn trường, quyền tài khoản theo B3, đọc dữ liệu toàn hệ thống; không mặc định được sửa mọi nghiệp vụ |

Ma trận chi tiết theo endpoint cần đối soát B3 với TV4 trước khi hiện thực.
Một DB không có nghĩa bỏ giới hạn cơ sở hoặc gộp hai nhóm Admin.

- `auth` xác thực và cung cấp principal/API danh tính cho module khác.
- `grade`, `timetable`, `course`, `enrollment`, `student` tự kiểm quyền bản ghi trong service/query của mình từ principal đáng tin cậy.
- Không tin `studentId`, `teacherId`, `campus`, `role` do client gửi để xác lập quyền; nếu là bộ lọc nghiệp vụ thì phải kiểm scope.
- Không cho module khác đọc thẳng `auth.repository`; auth không truy cập repository hồ sơ của module khác.
- `shared` chỉ giữ kỹ thuật dùng chung như định dạng lỗi, không import auth hoặc module nghiệp vụ.

Cấu trúc dự kiến, chỉ thêm package/class khi có chức năng:

```text
auth/
  controller/   AuthController, ActivationController khi làm F02
  service/      AuthenticationService, SessionService, AccountService, ActivationService
  repository/   AccountRepository, SessionRepository, RefreshTokenRepository, ActivationRepository
  dto/          request/response của API auth
  model/        Account, AccountRole, AccountStatus, CurrentUser
  security/     SecurityConfig, JWT/cookie/CSRF và kiểm phiên
  policy/       chỉ thêm nếu quy tắc thuần đủ phức tạp
```

Controller gọi service; transaction nằm ở service; SQL nằm trong repository.
`model/policy` không import Spring/JDBC; không tạo interface chỉ để ghép với từng class.
Profile mặc định tiếp tục chạy liveness khi không có DB; auth thật chỉ hoạt động
khi đủ cấu hình `central`. Thiếu khóa ký ở profile chạy auth phải báo lỗi cấu hình rõ.
`/api/health` vẫn public; `/api/health/db` cần chính sách truy cập nội bộ/quản trị
và bỏ thông tin lỗi SQL nhạy cảm trước khi đưa ra môi trường công khai.

## 7. Thứ tự công việc và đầu ra

Ước lượng **A0: 5–7 ngày làm việc** với TV5/TV6 phối hợp và đủ môi trường —
đã bỏ bootstrap/recovery, đổi mật khẩu, rate limit và ma trận test tương tranh
sang A1. Không phải cam kết lịch, chưa tính thời gian chờ duyệt contract.
A1 và mốc B ước lượng riêng khi tới lượt.

| Thẻ | Mốc | Việc làm | Phụ thuộc | Chủ trì / phối hợp | Đầu ra nghiệm thu |
|---|---|---|---|---|---|
| AUTH-01 | A0 | Chốt contract danh tính, schema mapping, role/scope, phiên/cookie/CSRF/logout, mã lỗi | Tài liệu thiết kế | TV5 + TV4 + TV2 | Contract và ma trận quyền có ví dụ request/response |
| AUTH-02 | A0 | Migration lát cắt auth (`DanhBaNguoiDung`, `TaiKhoan`, `PhienDangNhap`, `TokenLamMoi`) + seed | AUTH-01 | **TV5**; TV2 review | DB mới migrate được; seed chạy lại không nhân đôi; ba ghi chú Phần 2 ở mục 4.2 có trong migration |
| AUTH-03 | A0 | Spring Security, Argon2id, JWT, cookie/CSRF, JSON 401/403; **khóa `/api/health/db` khỏi truy cập công khai** | AUTH-01 | TV5 | Kiểm token/route policy và benchmark encoder; liveness vẫn hoạt động |
| AUTH-04 | A0 | Login/me, refresh + rotation + replay→thu hồi phiên, logout/logout-all | AUTH-02 + 03 | TV5 | Luồng thật qua CENTRAL; thu hồi bền vững, rotation nguyên tử |
| AUTH-05 | A0 | Nối web: trạng thái đăng nhập, role routing, refresh phối hợp giữa tab, logout | AUTH-01; tích hợp sau 04 | TV6; TV5 | Login → reload → access hết hạn → refresh → logout chạy thật; CSRF đúng vòng đời |
| AUTH-06 | A0 | Kiểm thử và bàn giao nền auth | AUTH-04 + 05 | TV4; TV2/TV5/TV6 sửa lỗi | Ma trận test cơ bản, fixture, bằng chứng và hướng dẫn chạy |
| AUTH-1a | A1 | Ma trận test tương tranh refresh/replay, rate limit `429` | AUTH-06 | TV4 + TV5 | Mục 8 các ca 8–10 |
| AUTH-1b | A1 | Đổi mật khẩu, công cụ bootstrap/recovery Admin | AUTH-06 | TV5 | Mục 8 ca 11 |
| AUTH-07 | — | Đóng đầy đủ ca quyền theo bản ghi F01 | API F03–F09 tương ứng | TV4 + owner module | SV không xem người khác; GV không sửa lớp khác; Admin không vượt scope |
| AUTH-08 | B | Contract/migration/API và UI cấp, kích hoạt, khóa tài khoản F02 | Mốc A0 và chính sách hồ sơ | TV5 + TV2 + TV6; TV4 kiểm | Tài khoản mới, mã một lần, rollback và chống nâng quyền |

**Vì sao AUTH-02 đổi sang TV5 chủ trì:** TV2 đang nợ `feature/db-central-schema`,
thứ chặn toàn bộ F03–F09 — họ đang là đường găng. Lát cắt auth khép kín, không
cần schema học vụ, nên TV5 tự làm để không xếp hàng sau một người đã quá tải.
TV2 vẫn review để mapping và quy ước đặt tên thống nhất với phần còn lại.

⚠️ **Cổng vào Phần 2 là AUTH-06, không phải A1.** Xong A0 thì F03–F09 có
principal đáng tin để chạy, và hạ tầng phân tán bắt đầu được.

AUTH-02 và AUTH-03 có thể làm đồng thời sau contract; web dựng trạng thái theo
contract trong lúc chờ API, có nhãn mock khi chưa tích hợp. Không cần đợi toàn bộ
schema học vụ mới làm auth, nhưng phải có đầy đủ bảng/FK của lát cắt tài khoản.

Nhánh đề xuất: `feature/auth-contract`, `feature/db-auth-schema`,
`feature/api-auth`, `feature/web-auth`, sau đó `feature/api-account-activation`.
Mỗi nhánh từ `dev`, PR về `dev`; không push thẳng. Cập nhật CodeGraph sau đợt thêm code.

## 8. Kiểm thử và cổng nghiệm thu

1. **Đăng nhập:** đủ bốn role; đúng/sai mật khẩu, username không tồn tại, trống/quá dài, tài khoản ngừng/chưa kích hoạt; không lộ hash/token/mật khẩu trong response hoặc log.
2. **Token:** thiếu, hết hạn, sai chữ ký/issuer/audience/thuật toán, sửa role/campus; đều bị chặn phù hợp. `/me` đúng mapping, kể cả Admin Master có campus null.
3. **Vòng đời phiên:** access còn hạn bị chặn sau revoke; logout chỉ ảnh hưởng phiên đó, logout-all/khóa/đổi hoặc reset mật khẩu ảnh hưởng mọi phiên; hai trình duyệt chứng minh scope. Access hết hạn vẫn refresh/logout được.
4. **Web/CSRF:** reload khôi phục qua `/me` và refresh khi cần; login/refresh/logout hoạt động sau làm mới CSRF; thiếu/sai CSRF bị từ chối, GET không đổi trạng thái; nhiều tab không tự gây replay do refresh đồng thời.
5. **Quyền:** gọi API trực tiếp với role sai hoặc ID thuộc người/lớp/cơ sở khác; thử sửa request và truy cập URL trực tiếp, không chỉ kiểm việc ẩn nút.
6. **SQL Server:** migration/seed thật, unique/FK, thao tác tăng version đồng thời không mất cập nhật, rollback khi lỗi giữa giao dịch; DB mất kết nối không bỏ qua kiểm phiên.
7. **F02:** hai request kích hoạt đồng thời chỉ một thành công; sai/hết hạn/đã dùng bị chặn; cấp lại làm mã cũ mất hiệu lực; lỗi tạo hồ sơ/tài khoản không để dữ liệu dở dang. Không chỉ dùng mã SV để nhận tài khoản.
8. **Giới hạn thử sai:** vượt ngưỡng trả 429, tự giải phóng đúng thời hạn; không ghi bí mật vào log, không để bộ nhớ giới hạn tăng vô hạn.
9. **Rotation:** hai request dùng một refresh không tạo hai successor; dùng lại token cũ thu hồi cả family và việc revoke không rollback khi trả 401. Token giả không revoke người khác. DB chỉ giữ hash, lịch sử đủ để phát hiện replay.
10. **Thời hạn/tương tranh:** kiểm biên 15 phút/7 ngày bằng clock kiểm soát; rotation không gia hạn phiên, access không vượt hạn phiên; refresh đua logout-all/khóa/đổi mật khẩu không hồi sinh phiên. Mất response refresh có hướng xử lý login lại rõ ràng.
11. **Bootstrap/recovery:** chỉ cấp Admin khi cần khởi tạo, chạy lại không reset; mật khẩu tạm không truy cập nghiệp vụ/refresh; đổi mật khẩu mở đúng luồng; recovery có hạn/một lần, audit và thu hồi mọi phiên. Kiểm bộ lọc quyền đổi mật khẩu, không chỉ kiểm UI.

Unit test cho quy tắc; Spring Security/HTTP integration test cho bộ lọc và API;
integration test trên SQL Server cho SQL, constraints, concurrency, rollback;
kiểm luồng trình duyệt cho UI. Không lấy H2 hoặc mock repository làm bằng chứng
đã tích hợp SQL Server.

**Mốc A0 bàn giao nền auth** khi AUTH-01 đến 06 đạt, và đó là **cổng vào Phần 2**. Nếu API điểm/lịch/lớp chưa có,
ca quyền tương ứng vẫn theo dõi ở AUTH-07; test endpoint giả không chứng minh
quyền trên nghiệp vụ thật. **F01 chỉ nghiệm thu đầy đủ** khi các ca quyền bản ghi
liên quan chạy trên API thật. **F02 chỉ xong** khi tài khoản mới được cấp/kích hoạt
qua luồng thật, không lấy seed thay cho bằng chứng đó.

## 9. Tài liệu đối chiếu

- [Thiết kế: 0.1, 0.1b, B3, C2/C3, luồng đăng nhập, J1](PTIT-One-Thiet-Ke.md).
- [Kế hoạch chung: F00, F01, F02 và mục 9](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md).
- [Trạng thái backend](PTIT-One-Backend-Khoi-Dong.md).
- Spring Security có hỗ trợ kiểm JWT và tùy biến decoder/converter: [JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
- Cơ chế encoder và lưu mật khẩu: [Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
- Thiết kế CSRF cho trình duyệt/SPA: [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
- Ưu tiên Argon2id và thông số tối thiểu: [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
- Tham khảo rotation/replay từ OAuth Security BCP; PTIT One chưa tuyên bố là OAuth authorization server: [RFC 9700, mục 4.14.2](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14.2).
- Quy trình khôi phục mật khẩu: [OWASP Forgot Password](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
- Ví dụ thực tế, riêng hệ thống quản lý tốt nghiệp PTIT: tài khoản SV được cấp theo mã SV, đổi mật khẩu lần đầu, khôi phục qua email đã cập nhật; không suy rộng thành chính sách mọi cổng UIS: [Hướng dẫn SV](https://ptit.edu.vn/giaovu/wp-content/uploads/2024/04/HDSD_SV_V1.pdf).
