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

```powershell
# 3. Chay API — terminal 1
cd apps\api
.\mvnw.cmd spring-boot:run
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
