# PTIT One — cài và chạy Phần 1

> Phần 1 dùng **một database tập trung**. Hạ tầng phân tán Master/site xem
> [máy HN/ĐN](PTIT-One-Cai-Dat-May-Moi.md). Cập nhật 25/09/2026.

Mỗi người cài đủ ba tầng trên máy mình: **SQL Server + API + web**. Không VPS,
không VPN. Mỗi máy một `PTITONE_CENTRAL` riêng, dùng chung migration và seed —
các DB này **không đồng bộ với nhau**, chúng chỉ là môi trường dev độc lập.

| Người | SQL Server | JDK 21 | Node |
|---|---|---|---|
| TV2 schema | ✅ | — | — |
| TV5 backend | ✅ | ✅ | ✅ |
| TV6 UI | ✅ | ✅ | ✅ |
| TV1 môi trường | ✅ | ✅ | ✅ |
| TV4 test | — | ✅ | — |
| TV3 tài liệu | — | — | — |

---

# Cài

## 1. SQL Server 2019 Developer

Bộ cài có giao diện, phải bấm tay. Chỉ cần đúng **5 lựa chọn**, còn lại Next:

| Bước trong bộ cài | Chọn |
|---|---|
| Feature Selection | **chỉ** `Database Engine Services` |
| Instance Configuration | Named instance: **`PTITONE`** |
| Server Configuration | Database Engine = **Automatic** |
| Collation → **Customize** | Windows designator **`Vietnamese`**, chỉ tích **Accent-sensitive** → phải ra **`Vietnamese_CI_AS`** |
| Database Engine Configuration | bấm **Add Current User** |

⚠️ Collation sai thì **không sửa được**, phải gỡ instance cài lại. Kiểm kỹ ô
này trước khi bấm Next.

Cài thêm **SSMS** (tải riêng). `sqlcmd` bản ODBC thường đi kèm SSMS.

```powershell
sqlcmd -S "localhost\PTITONE" -E -Q "SELECT @@SERVERNAME, SERVERPROPERTY('Collation')"
```

## 1b. Mở đường cho API nối vào

> Chỉ người **chạy backend** cần (TV5, TV6, TV1). TV2 chỉ viết schema thì bỏ qua.

Cài xong SQL Server **vẫn chưa nối JDBC được**: mặc định instance **tắt TCP/IP**
và **chỉ nhận tài khoản Windows**. Hai công tắc này không nằm trong bộ cài.

**1. Bật TCP/IP và cố định cổng.** Win+R → gõ `SQLServerManager15.msc`
(Configuration Manager không có trong Start menu của Windows 11):

- `SQL Server Network Configuration` → `Protocols for PTITONE` → **TCP/IP** →
  chuột phải → **Enable**
- Chuột phải **TCP/IP** → **Properties** → tab **IP Addresses** → kéo xuống
  cuối, mục **IPAll**:
  - `TCP Dynamic Ports` → **xóa trắng**
  - `TCP Port` → **14330**

⚠️ **Đừng dùng 1433** nếu máy có instance `MSSQLSERVER` — nó sẽ tranh cổng sau
mỗi lần khởi động máy, và lỗi trông như ngẫu nhiên.

**2. Bật Mixed Mode.** SSMS → chuột phải server → **Properties** → **Security**
→ **SQL Server and Windows Authentication mode**.

Đây là bật thêm, không phải thay thế — vẫn dùng Windows Auth để quản trị.

**3. Restart service** `SQL Server (PTITONE)` cho cả hai thay đổi có hiệu lực.
Replication (nếu máy có) tự chạy lại.

Tạo login cho API làm ở phần **Chạy → bước 2b**, sau khi đã có database.

## 2. JDK 21

Tải JDK 21 rồi giải nén ra `D:\jdk21` (chỗ khác cũng được, sửa đường dẫn bên
dưới cho khớp). **Giải nén xong là chưa xong** — phải đặt biến môi trường:

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME','D:\jdk21','User')
$env:JAVA_HOME = 'D:\jdk21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

Ra `21.x` là được. **Không cần cài Maven** — repo có sẵn wrapper.

<details>
<summary>Muốn <code>java</code> dùng được ở mọi terminal về sau (cần cho <code>java -jar</code>)</summary>

Mở PowerShell **Run as administrator**:

```powershell
$key = 'HKLM:\SYSTEM\CurrentControlSet\Control\Session Manager\Environment'
$raw = (Get-Item $key).GetValue('Path','',[Microsoft.Win32.RegistryValueOptions]::DoNotExpandEnvironmentNames)
if ($raw -notmatch [regex]::Escape('D:\jdk21\bin')) {
    Set-ItemProperty -LiteralPath $key -Name 'Path' -Value ('D:\jdk21\bin;' + $raw) -Type ExpandString
}
```

Rồi **khởi động lại máy**. Đừng thay bằng
`[Environment]::SetEnvironmentVariable` — nó làm hỏng `%SystemRoot%` trong PATH.
</details>

## 3. Node

Cài Node LTS.

```powershell
node --version
```

> Nhóm **chưa chốt** 22 hay 24. Kế hoạch ghi 24; máy đang giữ repo chạy 22.13
> vẫn dựng được. Chốt rồi thì sửa vào đây và vào kế hoạch.

## 4. Git

```powershell
git --version
```

## 5. VS Code — nếu không dùng IntelliJ

```powershell
code --install-extension vscjava.vscode-java-pack
```

Ctrl+Shift+P → `Preferences: Open User Settings (JSON)` → thêm:

```json
"java.configuration.runtimes": [
  { "name": "JavaSE-21", "path": "D:\\jdk21", "default": true }
]
```

Mở thư mục **`apps/api`**, không phải gốc repo — extension Java cần thấy
`pom.xml` ngay trong thư mục vừa mở. Muốn mở cả hai thì File → **Add Folder
to Workspace** thêm `apps/web`.

---

# Chạy

```powershell
# 1. Lay repo
git clone https://github.com/khangdzvl050623/PTIT-One.git
cd PTIT-One

# 2. Tao database
Copy-Item .\db\central\config.example.psd1 .\db\central\config.local.psd1
#    Mo file vua copy, sua SqlServer neu instance cua ban khac 'localhost\PTITONE'
.\db\central\run.ps1 -Action CreateDatabase
.\db\central\run.ps1 -Action VerifyDatabase
```

### 2b. Tạo login cho API — chỉ người chạy backend

Trong SSMS, **tự đặt mật khẩu của bạn**:

```sql
USE master;
CREATE LOGIN ptitone_api WITH PASSWORD = N'<mat khau cua ban>',
    CHECK_POLICY = ON, DEFAULT_DATABASE = PTITONE_CENTRAL;
GO
USE PTITONE_CENTRAL;
CREATE USER ptitone_api FOR LOGIN ptitone_api WITH DEFAULT_SCHEMA = dbo;
GO
```

Chưa cấp quyền bảng nào — cấp sau khi T2 có schema. Không cấp `db_owner`,
không dùng `sa` chạy ứng dụng.

**Kiểm bốn thứ bằng một lệnh** (TCP, Mixed Mode, login, mật khẩu):

```powershell
sqlcmd -S "tcp:localhost,14330" -U ptitone_api -d PTITONE_CENTRAL -Q "SELECT SUSER_NAME(), DB_NAME()"
```

Ra `ptitone_api` và `PTITONE_CENTRAL` là xong phần hạ tầng.
**Lệnh này hỏng thì sửa ở đây, đừng mở Java ra đoán.**

Rồi điền `apps/api/.env` — xem [hướng dẫn backend](../apps/api/README.md#nối-database--profile-central).

```powershell
# 3. Chay API — terminal 1
cd apps\api
.\mvnw.cmd spring-boot:run          # skeleton, KHONG noi DB

# ...hoac chay voi DB, tu goc repo, sau khi da dien .env:
.\scripts\dev-api.ps1
```

```powershell
# 4. Chay web — terminal 2
cd apps\web
npm ci
npm run dev
```

Mở `http://localhost:5173`.

---

# Xong chưa

| Lệnh | Kết quả đúng |
|---|---|
| `.\mvnw.cmd --version` (tại `apps/api`) | `Java version: 21.x` |
| `.\db\central\run.ps1 -Action VerifyDatabase` | ONLINE, `Vietnamese_CI_AS`, RCSI 1 |
| `Invoke-RestMethod http://localhost:8080/api/health` | `status: UP` |
| Mở `http://localhost:5173/api/health` | cùng JSON như trên |
| *(chạy backend)* `sqlcmd -S "tcp:localhost,14330" -U ptitone_api -d PTITONE_CENTRAL -Q "SELECT SUSER_NAME()"` | `ptitone_api` |
| *(chạy backend)* `Invoke-RestMethod http://localhost:8080/api/health/db` | `status: UP`, `login: ptitone_api` |

**0 bảng trong CENTRAL là đúng** — TV2 chưa có migration.

# Lỗi thường gặp

| Hiện tượng | Xử lý |
|---|---|
| `release version 21 not supported` | Maven đang chạy JDK 17. Đặt `JAVA_HOME`, mở terminal mới |
| `UnsupportedClassVersionError ... 65.0 ... up to 61.0` | Class build bằng 21, JVM chạy 17. Sửa `JAVA_HOME` rồi `.\mvnw.cmd clean verify` — bắt buộc có `clean` |
| `mvnw --version` ra 21 nhưng `java -version` ra 17 | Không phải lỗi: `JAVA_HOME` đã đúng, `PATH` chưa. Build vẫn chạy, chỉ `java -jar` hỏng |
| Sửa PATH rồi mà `java -version` vẫn 17 | `explorer.exe` giữ biến cũ, nên VS Code mở lại vẫn nhận biến cũ. `Stop-Process -Name explorer -Force` rồi mở lại VS Code từ Start, hoặc reboot |
| IDE chạy được, `mvnw.cmd` hỏng | IDE dùng JDK riêng. Tin terminal, đừng tin IDE |
| Thiếu `config.local.psd1` | Chưa chạy bước `Copy-Item` |
| `Login failed` khi tạo DB | Thử kết nối bằng chính tài khoản Windows đó trong SSMS trước |
| `npm ci` báo thiếu lockfile | Sai thư mục, phải ở `apps/web` |
| `localhost:5173/api/...` trả 404 | API chưa chạy |
| Collation khác `Vietnamese_CI_AS` | Không sửa được; gỡ instance cài lại hoặc báo TV1 |
| `Login failed for user 'ptitone_api'` dù mật khẩu đúng | Login **bị khóa** do connection pool thử lại nhiều lần với mật khẩu sai. Kiểm: `SELECT LOGINPROPERTY('ptitone_api','IsLocked')`. Mở khóa: `ALTER LOGIN ptitone_api WITH PASSWORD = N'<mk>' UNLOCK;` |
| Chạy `dev-api.ps1` xong không gõ được lệnh tiếp | Đúng vậy — nó giữ terminal khi app đang chạy. Gọi API ở **terminal thứ hai** |
| `.env` điền rồi vẫn không nối được | Kiểm dán lặp tên biến: dòng phải là `PTITONE_DB_URL=jdbc:...`, không phải `PTITONE_DB_URL=PTITONE_DB_URL=jdbc:...` |

# Chưa có gì

Chạy hết phần trên bạn có: SQL Server + CENTRAL **rỗng** + API skeleton + web
+ proxy thông. **Chưa có** schema, migration, seed, JDBC, đăng nhập và mọi API
nghiệp vụ — gọi thử trả 404 là **đúng**.

**Tạo được database rỗng không phải là xong ENV-04 hay F00.**

---

[Kế hoạch Phần 1](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md) ·
[db/central](../db/central/README.md) ·
[apps/api](../apps/api/README.md) ·
[apps/web](../apps/web/README.md)
