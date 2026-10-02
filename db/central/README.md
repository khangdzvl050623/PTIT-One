# Dựng `PTITONE_CENTRAL` trên máy mới

Phần 1 dùng **một database duy nhất**, `PTITONE_CENTRAL`. Mỗi người dựng một bản
trên máy mình. Các bản dùng chung migration và seed nhưng **không đồng bộ dữ
liệu** với nhau: mỗi bản là một môi trường dev độc lập.

Làm lần lượt 6 bước dưới đây. Mọi lệnh PowerShell chạy tại **gốc repo**
(ví dụ `D:\javabtap\uisptitv2`), không phải ở `apps/api`.

```text
1. SQL Server sẵn sàng  →  2. Tạo DB rỗng  →  3. Tạo 2 login
→  4. Điền apps/api/.env  →  5. Chạy API (tự tạo bảng)  →  6. Nạp seed
```

---

## Bước 1 — SQL Server sẵn sàng

Chưa cài SQL Server thì làm theo [hướng dẫn cài Phần 1](../../docs/PTIT-One-Cai-Dat-Phan-1.md),
mục 1 và 1b. Bước này cần 4 điều:

| Cần | Kiểm bằng |
|---|---|
| Instance tên `PTITONE`, collation `Vietnamese_CI_AS` | `sqlcmd -S "localhost\PTITONE" -E -Q "SELECT SERVERPROPERTY('Collation')"` |
| TCP/IP bật, cổng cố định `14330` | SQL Server Configuration Manager → Protocols for PTITONE |
| Mixed Mode (cho phép đăng nhập bằng user/mật khẩu SQL) | SSMS → Properties của server → Security |
| `sqlcmd` có trong PATH | `sqlcmd -?` |

⚠️ Collation sai thì **không sửa được**, phải cài lại instance.

## Bước 2 — Tạo database rỗng

```powershell
Copy-Item .\db\central\config.example.psd1 .\db\central\config.local.psd1
.\db\central\run.ps1 -Action CreateDatabase
.\db\central\run.ps1 -Action VerifyDatabase
```

- `config.local.psd1` không vào Git. Chỉ cần sửa khi instance của bạn **không**
  phải `localhost\PTITONE`.
- Script dùng tài khoản Windows đang đăng nhập, không hỏi mật khẩu.
- Script tạo DB với `Vietnamese_CI_AS`, bật RCSI, recovery `SIMPLE`. Chạy lại
  thì chỉ kiểm tra, **không xoá** DB đã có.

**Đúng khi:** `VerifyDatabase` in ra `ONLINE`, `Vietnamese_CI_AS`, RCSI `1`.
Lúc này DB có 0 bảng, vì bảng được tạo ở bước 5.

## Bước 3 — Tạo 2 login SQL

Tách 2 login theo nguyên tắc quyền tối thiểu:

| Login | Dùng để | Quyền |
|---|---|---|
| `ptitone_migrator` | Flyway tạo/sửa bảng (chỉ khi có migration mới) | `db_owner` **trên riêng** `PTITONE_CENTRAL` |
| `ptitone_api` | API chạy hằng ngày | chỉ đọc/ghi dữ liệu, **không** sửa được cấu trúc |

Nhờ vậy, nếu API bị lỗi SQL injection thì kẻ tấn công cũng không `DROP TABLE`
hay gỡ ràng buộc được.

Mở SSMS bằng tài khoản Windows của bạn, **tự đặt hai mật khẩu** rồi chạy:

```sql
USE master;
CREATE LOGIN ptitone_migrator WITH PASSWORD = N'<mat khau migration>',
    CHECK_POLICY = ON, DEFAULT_DATABASE = PTITONE_CENTRAL;
CREATE LOGIN ptitone_api WITH PASSWORD = N'<mat khau api>',
    CHECK_POLICY = ON, DEFAULT_DATABASE = PTITONE_CENTRAL;
GO

USE PTITONE_CENTRAL;
CREATE USER ptitone_migrator FOR LOGIN ptitone_migrator WITH DEFAULT_SCHEMA = dbo;
CREATE USER ptitone_api      FOR LOGIN ptitone_api      WITH DEFAULT_SCHEMA = dbo;

ALTER ROLE db_owner      ADD MEMBER ptitone_migrator;
ALTER ROLE db_datareader ADD MEMBER ptitone_api;
ALTER ROLE db_datawriter ADD MEMBER ptitone_api;
GO
```

Đã có login từ trước thì bỏ hai dòng `CREATE LOGIN` / `CREATE USER` tương ứng;
chỉ chạy các dòng `ALTER ROLE`.

**Kiểm cả hai login** (lệnh hỏi mật khẩu; lệnh này hỏng thì sửa ở đây, đừng
sang Java đoán):

```powershell
sqlcmd -S "tcp:localhost,14330" -U ptitone_migrator -d PTITONE_CENTRAL -Q "SELECT SUSER_NAME(), DB_NAME()"
sqlcmd -S "tcp:localhost,14330" -U ptitone_api      -d PTITONE_CENTRAL -Q "SELECT SUSER_NAME(), DB_NAME()"
```

> **`db_owner` cho migration là chủ ý, chỉ áp cho DB dev cá nhân.** Migration
> vừa tạo bảng (DDL), vừa ghi lịch sử vào `flyway_schema_history` và chèn dữ
> liệu (DML), vừa tạo khoá ngoại. Chỉ cấp `db_ddladmin` thì hay vướng thiếu quyền
> lặt vặt. `db_owner` chỉ có hiệu lực trong `PTITONE_CENTRAL`, không phải
> quyền quản trị server như `sysadmin`/`sa`. **Login API tuyệt đối không có
> `db_owner` hay `db_ddladmin`.**

## Bước 4 — Điền `apps/api/.env`

```powershell
Copy-Item .\apps\api\.env.example .\apps\api\.env
```

Mở `apps/api/.env`, điền phần database:

```text
SPRING_PROFILES_ACTIVE=central
PTITONE_DB_URL=jdbc:sqlserver://localhost:14330;databaseName=PTITONE_CENTRAL;encrypt=true;trustServerCertificate=true
PTITONE_DB_USERNAME=ptitone_api
PTITONE_DB_PASSWORD=<mat khau api>

PTITONE_MIGRATE_ON_START=true
PTITONE_MIGRATION_USERNAME=ptitone_migrator
PTITONE_MIGRATION_PASSWORD=<mat khau migration>
```

Cùng file đó còn có `PTITONE_JWT_SECRET`, `PTITONE_OTP_SECRET` (bắt buộc) và
các biến gửi thư. Cách tạo từng giá trị ghi ngay trong `.env.example`.

- Không ghi mật khẩu vào URL.
- `trustServerCertificate=true` chỉ dùng cho máy dev có chứng chỉ tự ký.
- `.env` không vào Git. Không gửi file này qua chat nhóm.

## Bước 5 — Chạy API: tự tạo toàn bộ bảng

```powershell
.\scripts\dev-api.ps1
```

Flyway chạy lần lượt mọi file `db/central/migrations/V*.sql` bằng login
migration, sau đó API chạy bằng login API. **Đúng khi** log có
`Successfully applied N migrations` và:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

trả `status: UP`.

Sau này mỗi lần `git pull` có file `V<n>__` mới, **chỉ cần chạy lại API**. Không
chạy tay file `V`, không sửa file `V` đã merge (Flyway báo lệch checksum).

> Muốn chặt hơn: sau khi migrate xong thì xoá giá trị `PTITONE_MIGRATE_ON_START`
> để API không tự đổi schema. Nhớ bật lại khi `git pull` có migration mới.

## Bước 6 — Nạp dữ liệu demo (seed)

Chạy **sau** bước 5, vì seed cần bảng đã có:

```powershell
sqlcmd -S "localhost\PTITONE" -d PTITONE_CENTRAL -E -C -b -f 65001 -i db\central\seed\10-auth-seed.sql
sqlcmd -S "localhost\PTITONE" -d PTITONE_CENTRAL -E -C -b -f 65001 -i db\central\seed\20-hoc-vu-seed.sql
```

- Chạy lại bao nhiêu lần cũng không nhân đôi dữ liệu. Nên chạy lại mỗi khi
  `db/central/seed/` thay đổi.
- Seed in bảng đối soát ở cuối: **mọi cột `Lech` phải bằng 0**.
- Mọi tài khoản demo có mật khẩu `PtitOne@2026`: `admin.master`, `admin.hcm`,
  `admin.hn`, `GVHCM001`, `B26DCCN001`… Danh sách đầy đủ ở đầu file `10-auth-seed.sql`.

---

## Xong chưa

| Kiểm | Kết quả đúng |
|---|---|
| `.\db\central\run.ps1 -Action VerifyDatabase` | `ONLINE`, `Vietnamese_CI_AS`, RCSI `1`, số bảng > 0 |
| Đăng nhập `admin.master` / `PtitOne@2026` ở `http://localhost:8080/swagger-ui.html` | `200`, `role: ADMIN_MASTER` |
| `GET /api/health/db` (sau khi đăng nhập admin) | `status: UP`, `login: ptitone_api` |
| SSMS: `SELECT TOP 1 version FROM flyway_schema_history ORDER BY installed_rank DESC` | số V lớn nhất trong `db/central/migrations` |
| `sqlcmd ... -i db\central\tests\10-verify-hoc-vu.sql` | các ca ghi sai đều bị ràng buộc chặn |

## Lỗi thường gặp

| Hiện tượng | Nguyên nhân và cách sửa |
|---|---|
| `Missing config` khi chạy `run.ps1` | Chưa làm lệnh `Copy-Item` ở bước 2 |
| `sqlcmd` không nhận | Cài sqlcmd bản ODBC (đi kèm SSMS), mở terminal mới |
| `Login failed` ở `sqlcmd -U ...` | Chưa bật Mixed Mode, hoặc chưa restart service sau khi bật |
| Không kết nối được `tcp:localhost,14330` | TCP/IP chưa bật hoặc cổng chưa đặt 14330; restart service |
| `CREATE TABLE permission denied` khi chạy API | Login migration thiếu quyền: chạy lại `ALTER ROLE db_owner ...` ở bước 3, hoặc thiếu `PTITONE_MIGRATION_USERNAME` nên Flyway đang chạy bằng login API |
| `Invalid object name 'dbo.TaiKhoan'` | Chưa migrate: kiểm `PTITONE_MIGRATE_ON_START=true` rồi chạy lại API |
| `Validate failed: Migration checksum mismatch` | Ai đó sửa file `V` đã chạy. Lấy lại file gốc từ Git; đổi schema thì thêm file `V` mới |
| `Login failed for user 'ptitone_api'` dù mật khẩu đúng | Login bị khoá do thử sai nhiều lần: `ALTER LOGIN ptitone_api WITH PASSWORD = N'<mk>' UNLOCK;` |
| Seed báo lỗi thiếu bảng | Chạy seed trước khi migrate; làm bước 5 trước |
| API không khởi động, báo thiếu `PTITONE_OTP_SECRET` | Thêm biến đó vào `.env` (cách tạo ghi trong `.env.example`) |

## Thư mục này có gì

```text
db/central/
├── config.example.psd1      mẫu cấu hình cho run.ps1 (copy thành config.local.psd1)
├── run.ps1                  tạo DB rỗng / kiểm cấu hình DB (bước 2)
├── 00-create-database.sql   script mà run.ps1 gọi
├── migrations/V*.sql        schema — API chạy bằng Flyway (bước 5)
├── seed/*.sql               dữ liệu demo — chạy tay (bước 6)
└── tests/                   kiểm cấu hình DB và các ràng buộc
```

Kiểm cú pháp mọi file SQL mà không cần SQL Server:
`powershell -NoProfile -File db/tests/Test-Scripts.ps1`.

## Dùng chung một DB qua mạng (tuỳ chọn)

Mặc định mỗi người một DB. Nếu nhóm cần một DB chung, người giữ máy chủ:
mở cổng `14330` trên tường lửa, tạo login riêng cho từng người như bước 3, rồi
gửi host/IP và cổng qua kênh riêng. Người dùng chỉ đổi `localhost` trong
`PTITONE_DB_URL` thành IP đó. **Một người duy nhất** chạy migration trên DB
chung, và backup trước mỗi đợt migration. Không ai tự xoá hay nạp lại dữ liệu chung.

Phần 2 (nhiều cơ sở, phân mảnh, nhân bản) dựng ở chỗ khác, xem
[máy HN/ĐN](../../docs/PTIT-One-Cai-Dat-May-Moi.md). Không đổi tên CENTRAL thành MASTER/site.
