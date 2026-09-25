# PTIT One — cài và chạy Phần 1

> Dành cho **Phần 1** (một database tập trung). Máy Master/site của phần phân
> tán là chuyện khác: [hướng dẫn máy HN/ĐN](PTIT-One-Cai-Dat-May-Moi.md).
>
> Cập nhật 25/09/2026. Phiên bản lấy từ `apps/api/pom.xml` và
> `apps/web/package.json`. Ai nâng phiên bản trong repo thì sửa luôn file này.

## Cách làm việc: mỗi máy chạy đủ bộ

**Không dùng VPS, không cần VPN.** Ai viết code thì cài cả ba tầng trên máy
mình: SQL Server + API + web. Mỗi người một `PTITONE_CENTRAL` riêng, dùng
chung migration và seed.

Mất một buổi cài lần đầu. Đổi lại là không bao giờ phải chờ ai bật máy.

Các DB cá nhân **không đồng bộ với nhau** — chúng là môi trường phát triển
độc lập, không phải site phân tán. Ai đổi schema thì viết migration mới,
người khác `git pull` rồi chạy lại trên DB của mình.

## Ai cài gì

| Người | SQL Server | JDK 21 | Node |
|---|---|---|---|
| TV2 schema | ✅ | — | — |
| TV5 backend | ✅ | ✅ | ✅ |
| TV6 UI | ✅ | ✅ | ✅ |
| TV1 môi trường | ✅ | ✅ | ✅ |
| TV4 test | — | ✅ | — |
| TV3 tài liệu | — | — | — |

TV6 cài đủ bộ vì ghép API là việc hằng ngày. Không cài thì mỗi lần ghép phải
nhờ người khác bật máy.

---

# Phần A — Cài 4 thứ

## A1. SQL Server 2019 Developer

**T1 và mọi người cài giống hệt nhau.** Không có bản riêng cho T1. T1 chỉ làm
thêm một việc ở mốc M1/M2: dựng DB tích hợp chung cho cả nhóm.

> Đã có instance `PTITONE` trên máy rồi thì **bỏ qua mục này**, dùng lại.
> Đừng cài đè.

### Cài

**1.** Tải **SQL Server 2019 Developer Edition** (miễn phí). Bộ CAB gồm hai
file `SQLServer2019-DEV-x64-ENU.exe` và `.box` — để **cạnh nhau**, chạy file
`.exe` để giải nén, rồi mở `setup.exe` trong thư mục vừa giải nén.

**2.** Chọn **Installation → New SQL Server stand-alone installation**.

**3.** Edition: **Developer**.

**4. Feature Selection: chỉ tích `Database Engine Services`.**
Phần 1 không cần Replication, không cần Agent.

> Máy nào chắc chắn sẽ làm site ở Phần 2 (HCM / HN / ĐN) thì tích luôn
> **SQL Server Replication** ngay bây giờ — thêm sau phải chạy lại bộ cài.

**5. Instance Configuration:** chọn **Named instance**, gõ `PTITONE` cho cả
Instance name và Instance ID. Nhiều máy cùng đặt tên này không sao, vì tên
máy khác nhau.

**6. Server Configuration:** để SQL Server Database Engine = **Automatic**
(tự chạy khi bật máy). Các dịch vụ khác để mặc định.

**7. ⚠️ Collation — chỗ quan trọng nhất, sai là không sửa được.**
Sang tab **Collation → Customize** → chọn **Windows collation designator**,
designator = **`Vietnamese`**, và **chỉ tích `Accent-sensitive`**.
Bấm OK, ô Collation phải hiện đúng:

```
Vietnamese_CI_AS
```

Sai chỗ này thì so sánh chuỗi tiếng Việt khác nhau giữa các máy, và lỗi sẽ
hiện ra ở chỗ không ai ngờ tới. **Không đổi được sau khi cài** — phải gỡ
instance cài lại.

**8. Database Engine Configuration:** bấm **Add Current User** để tài khoản
Windows của bạn thành quản trị. Windows Authentication là đủ cho Phần 1.

**9. Data Directories:** máy có ổ D thì trỏ data/log/backup/TempDB sang D.
Không có thì để mặc định.

**10.** Bấm **Install**, đợi tới khi báo **Succeeded**.

### Cài thêm hai thứ

- **SSMS** (SQL Server Management Studio) — tải riêng, không nằm trong bộ cài trên.
- **sqlcmd bản ODBC** — script của nhóm dùng tùy chọn của bản này. **Không
  phải bản Go.** Thường đã có sẵn khi cài SSMS.

### Kiểm tra cài đúng chưa

Mở SSMS, kết nối `localhost\PTITONE`, Windows Authentication. Gặp cảnh báo
chứng chỉ thì tích **Trust Server Certificate**. Mở New Query, chạy:

```sql
SELECT @@SERVERNAME                   AS TenServer,
       SERVERPROPERTY('Edition')      AS Edition,
       SERVERPROPERTY('Collation')    AS Collation;
```

Phải ra: tên máy kèm `\PTITONE` · `Developer Edition` · **`Vietnamese_CI_AS`**.

Rồi mở PowerShell kiểm sqlcmd:

```powershell
sqlcmd -?
```

Ra bảng tùy chọn là được. Báo "không tìm thấy lệnh" thì cài lại sqlcmd rồi
**mở terminal mới**.

## A2. JDK 21 — chỗ đã có người vấp

Cài JDK 21, rồi đặt biến môi trường `JAVA_HOME` trỏ vào thư mục JDK đó.

⚠️ `pom.xml` ghim Java 21. **JDK 17 build hỏng** với đúng dòng này:

```
Fatal error compiling: error: release version 21 not supported
```

Đã gặp thật trên máy đang giữ repo ngày 25/09/2026.

Cái bẫy: **IDE vẫn có thể chạy ngon** vì nó dùng JDK riêng, trong khi Maven
ngoài terminal lấy JDK trên PATH. Chạy được trong IDE không chứng minh máy
cài đúng.

Lệnh duy nhất đáng tin — nó in ra JDK mà **Maven thực sự dùng**:

```powershell
cd apps\api
.\mvnw.cmd --version
```

Dòng `Java version:` phải là 21.x. Ra 17 thì sửa `JAVA_HOME`, **mở terminal
mới** (terminal cũ giữ biến cũ).

**Không cần cài Maven.** Repo đã có Maven Wrapper.

## A3. Node

Cài Node LTS.

> ⚠️ Kế hoạch ghi **Node 24**, nhưng máy đang giữ repo chạy **Node 22.13** và
> web vẫn dựng được. Nhóm chọn một con số rồi ghi vào cả kế hoạch lẫn file
> này — để hai số ở hai chỗ là cách chắc chắn sinh lỗi "máy tôi chạy được".

## A4. Git

Cài Git. Chưa vào được repo thì báo TV1.

---

# Phần B — Dùng VS Code

Không bắt ai dùng IntelliJ. VS Code làm được đủ cả backend lẫn frontend.

## B1. Quy tắc vàng

**Terminal là chuẩn, IDE chỉ là chỗ gõ chữ.**

Dùng IDE nào cũng được, miễn `.\mvnw.cmd --version` ra JDK 21. IDE báo xanh mà
terminal hỏng thì máy đó vẫn là máy hỏng.

## B2. Java trong VS Code

**1. Cài JDK 21 trước** (mục A2). VS Code không tự cài JDK.

**2. Cài extension.** Ctrl+Shift+X, cài **Extension Pack for Java** (Microsoft).
Repo đã có `.vscode/extensions.json` nên mở project lên VS Code sẽ tự gợi ý.

**3. Chỉ cho VS Code dùng JDK 21.** Ctrl+Shift+P → gõ
`Preferences: Open User Settings (JSON)` → thêm vào:

```json
"java.configuration.runtimes": [
  {
    "name": "JavaSE-21",
    "path": "C:\\Program Files\\Java\\jdk-21",
    "default": true
  }
]
```

Sửa `path` cho khớp máy mình. Dùng `\\` (hai gạch chéo) trong JSON.

**4. Mở đúng thư mục.** File → Open Folder → chọn **`apps/api`**, không phải
gốc repo. Extension Java tìm `pom.xml` ngay trong thư mục vừa mở.

**5. Chạy.** Ctrl+` để mở terminal, rồi:

```powershell
.\mvnw.cmd spring-boot:run
```

**Không cần cài Maven riêng** — extension tự ưu tiên `mvnw` của repo.

## B3. Frontend trong VS Code

File → Open Folder → **`apps/web`**. Ctrl+` rồi:

```powershell
npm ci
npm run dev
```

Không có extension nào bắt buộc.

## B4. Mở cả backend lẫn frontend cùng lúc

File → **Add Folder to Workspace**, thêm cả `apps/api` và `apps/web`, rồi
File → **Save Workspace As** để lần sau mở một phát ra cả hai.

Mỗi thư mục có terminal riêng — chọn thư mục ở ô dropdown khi bấm New Terminal.

---

# Phần C — Chạy

Mọi lệnh chạy **tại gốc repo**, trừ chỗ ghi khác.

## C1. Lấy repo

```powershell
git clone https://github.com/khangdzvl050623/PTIT-One.git
cd PTIT-One
```

## C2. Tạo database

Tạo file cấu hình riêng (Git đã bỏ qua file này):

```powershell
Copy-Item .\db\central\config.example.psd1 .\db\central\config.local.psd1
```

Mở `db\central\config.local.psd1`, sửa cho khớp máy mình:

```powershell
@{
    SqlServer = 'localhost\PTITONE'
    DatabaseName = 'PTITONE_CENTRAL'
    TrustServerCertificate = $true
}
```

Chạy:

```powershell
.\db\central\run.ps1 -Action CreateDatabase
.\db\central\run.ps1 -Action VerifyDatabase
```

Phải thấy `ONLINE`, `Vietnamese_CI_AS`, RCSI = 1, `SIMPLE`.

**0 bảng là đúng** — TV2 chưa có migration.

## C3. Chạy API

```powershell
cd apps\api
.\mvnw.cmd spring-boot:run
```

Terminal khác:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Phải ra `{"service":"ptit-one-api","status":"UP"}`.

API hiện **chưa đọc DB**, nên bước C2 chưa xong vẫn chạy được.

## C4. Chạy web

```powershell
cd apps\web
npm ci
npm run dev
```

Mở `http://localhost:5173`. Kiểm proxy: `http://localhost:5173/api/health`
phải ra đúng JSON như C3.

Dùng `npm ci` chứ không `npm install` — `ci` dựng đúng theo lockfile nên mọi
máy giống nhau.

---

# Phần D — Xong chưa

| Lệnh | Kết quả đúng |
|---|---|
| `.\mvnw.cmd --version` (tại `apps/api`) | `Java version: 21.x` |
| `node --version` | v22 hoặc v24, theo số nhóm đã chốt |
| `.\db\central\run.ps1 -Action VerifyDatabase` | ONLINE, `Vietnamese_CI_AS`, RCSI 1 |
| `Invoke-RestMethod http://localhost:8080/api/health` | `status: UP` |
| Mở `http://localhost:5173/api/health` | cùng JSON như trên |

Đủ 5 dòng là máy bạn dựng lại được Phần 1 ở trạng thái hiện tại.

## Lỗi thường gặp

| Hiện tượng | Xử lý |
|---|---|
| `release version 21 not supported` | Maven đang dùng JDK 17. Sửa `JAVA_HOME`, mở terminal mới |
| IDE chạy được, `mvnw.cmd` hỏng | IDE dùng JDK riêng. Tin terminal, không tin IDE |
| Thiếu `config.local.psd1` | Chưa làm bước C2 |
| Không thấy `sqlcmd` | Cài bản ODBC, mở terminal mới |
| `Login failed` khi tạo DB | Thử kết nối bằng chính tài khoản Windows đó trong SSMS trước |
| `npm ci` báo thiếu lockfile | Đang sai thư mục, phải ở `apps/web` |
| `localhost:5173/api/...` trả 404 | API chưa chạy |
| Collation máy mình khác `Vietnamese_CI_AS` | Không sửa được sau khi cài; tạo instance mới hoặc báo TV1 |

## Chưa có gì — đừng tưởng đã xong

Chạy hết Phần C bạn có: SQL Server + CENTRAL **rỗng** + API skeleton + web +
proxy thông. Chưa có schema, migration, seed, JDBC, đăng nhập, và mọi API
nghiệp vụ (gọi thử trả 404 là **đúng**).

**Tạo được database rỗng không phải là xong ENV-04 hay F00.**

## Khi nào cần mạng chung

Việc hằng ngày không cần. Chỉ cần ở mốc tích hợp M1/M2 (gặp mặt thì LAN là
đủ) và ở **Phần 2** — lúc đó ba máy SQL Server ở ba cơ sở buộc phải nhìn thấy
nhau qua VPN.

## Liên quan

[Kế hoạch Phần 1](PTIT-One-Ke-Hoach-Chung-8-Tuan-Theo-Chuc-Nang.md) ·
[db/central](../db/central/README.md) ·
[apps/api](../apps/api/README.md) ·
[apps/web](../apps/web/README.md)
