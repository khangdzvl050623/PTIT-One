# CENTRAL Phần 1 — hướng dẫn cho T1, T2 và T5

**Trạng thái:** có khung thư mục, cấu hình mẫu, runner tạo DB rỗng và kiểm tra
môi trường. Chưa có schema nghiệp vụ, migration, seed, tài khoản ứng dụng
hoặc kết nối JDBC trong backend. Các lệnh dưới đây là hướng dẫn để thành
viên tự chạy; việc thêm file vào repo không tự tạo database.

Phần 1: `Web → API Spring Boot → một PTITONE_CENTRAL`. CENTRAL chứa cả danh
mục, hồ sơ, tài khoản, lớp, lịch, đăng ký và điểm. Không cần Master/site,
replication, Linked Server hoặc MS DTC để bắt đầu. Không dùng `db/run.ps1`
hay đổi tên DB trong toàn bộ script phân tán để dựng CENTRAL.

## 1. Ai làm gì và bàn giao theo thứ tự nào?

| Người | Việc thực hiện | Đầu ra cần bàn giao |
|---|---|---|
| **T1** | SQL Server, tạo DB tích hợp, kết nối/quyền, backup/restore, hỗ trợ máy cá nhân | Server/port/DB, cách kết nối, tài khoản cấp riêng, kết quả kiểm tra môi trường |
| **T2** | Schema, migration, ràng buộc, seed và SQL nghiệp vụ | File có phiên bản trong `migrations/`, fixture trong `seed/`, test và thứ tự chạy |
| **T5** | JDBC, một DataSource nghiệp vụ, đóng gói/chạy migration, API và transaction | Cấu hình backend, truy vấn/rollback và kiểm quyền thực tế |

Thứ tự bàn giao: **T1 tạo DB → T2/T5 thống nhất schema và cách chạy migration
→ migrate + seed → T5 nối nghiệp vụ → kiểm thử tích hợp**. T2 thiết kế schema
và T5 chuẩn bị cấu hình có thể làm song song với T1.

- **DB cá nhân:** T2/T5 có thể tự chạy cùng bộ script trên SQL Server của mình.
  Mỗi máy đều dùng tên `PTITONE_CENTRAL` được. Nếu dùng chung một instance
  nhưng muốn tách DB thử nghiệm, đặt `PTITONE_CENTRAL_T2`, `PTITONE_CENTRAL_T5`.
- **DB tích hợp:** T1 quản lý một CENTRAL chung, nạp migration/seed đã review.
  Thống nhất người chạy migration mỗi đợt; không tự reset dữ liệu chung.
- T6 gọi API; T3/T4 dùng web/API kiểm thử. Không bắt mọi thành viên cài SQL.
- Những DB cá nhân là các môi trường phát triển độc lập, không phải các site
  phân tán; cùng migration/seed nhưng không đồng bộ dữ liệu qua replication.

## 2. Cấu trúc và phần đã có

```text
db/central/
├── README.md
├── .gitignore
├── config.example.psd1        mẫu không chứa mật khẩu
├── config.local.psd1          mỗi người tự tạo, Git bỏ qua
├── run.ps1                    chỉ tạo DB / kiểm tra môi trường
├── 00-create-database.sql     tạo DB rỗng, chưa tạo bảng
├── migrations/README.md       T2 thêm migration nghiệp vụ ở đây
├── seed/README.md             T2 thêm fixture và lệnh nạp ở đây
├── tests/
│   ├── README.md
│   └── 00-verify-database.sql  truy vấn chỉ đọc
└── examples/
    ├── application-central.properties.example
    └── .env.example           tham khảo tên biến DB; mẫu dev ở apps/api
```

> 🚀 **Máy mới, chưa cài gì?** Đi theo
> [hướng dẫn cài Phần 1](../../docs/PTIT-One-Cai-Dat-Phan-1.md) — một đường đi
> duy nhất từ máy trắng tới chạy được cả SQL Server, API và web. Trang này là
> phần chi tiết về DB; trang kia ghép cả ba tầng.

## 3. T1/T2: chuẩn bị SQL Server trên máy cá nhân

1. Cài hoặc dùng lại **SQL Server 2019 Developer**, SSMS và **sqlcmd bản ODBC**.
   Có thể tham khảo phần cài phần mềm ở
   [hướng dẫn máy mới](../../docs/PTIT-One-Cai-Dat-May-Moi.md#1-cài-phần-mềm-trên-máy-mới).
   Với CENTRAL chỉ cần Database Engine; không cần cài/chạy replication hoặc Agent.
2. Nếu đã có instance `PTITONE`, dùng lại instance đó. SSMS kết nối
   `localhost\PTITONE` bằng Windows Authentication. Database mới được tạo
   riêng; `PTITONE_MASTER`/`PTITONE_HCM` hiện có không phải đích của runner này.
3. Kiểm tra `sqlcmd -?` trong PowerShell. Hướng dẫn này dùng tùy chọn ODBC
   (`-E`, `-N`, `-C`, `-f 65001`); không mặc định dùng cú pháp bản Go.
4. Chạy bước tạo DB bằng tài khoản Windows có quyền tạo/ALTER database trên
   instance đích. Tài khoản quản trị này không phải tài khoản chạy API.

Các lệnh tiếp theo chạy trong **PowerShell tại gốc repo**, ví dụ
`D:\javabtap\uisptitv2`, không phải terminal đang ở `apps/api`.

## 4. Tạo và kiểm tra CENTRAL

Lần đầu, tạo cấu hình cá nhân (không chép đè nếu đã có):

```powershell
if (-not (Test-Path -LiteralPath .\db\central\config.local.psd1)) {
    Copy-Item -LiteralPath .\db\central\config.example.psd1 -Destination .\db\central\config.local.psd1
}
```

Sửa file đó bằng editor:

```powershell
@{
    SqlServer = 'localhost\PTITONE'
    DatabaseName = 'PTITONE_CENTRAL'
    TrustServerCertificate = $true
}
```

`DatabaseName` chỉ nhận `PTITONE_CENTRAL` hoặc tên có hậu tố viết hoa/số/gạch
dưới như `PTITONE_CENTRAL_T2`. Runner dùng Windows Authentication (`-E`), không
đọc mật khẩu. `TrustServerCertificate = $true` dành cho chứng chỉ tự ký trong
môi trường dev; vẫn yêu cầu mã hóa kết nối (`-N`). Khi máy chủ có chứng chỉ
được máy khách tin cậy, đổi thành `$false`.

```powershell
# Chỉ xem server, DB và file đích; chưa kết nối SQL Server.
.\db\central\run.ps1 -Action CreateDatabase -WhatIf

# Khi đích đã đúng, tạo DB rỗng rồi kiểm tra.
.\db\central\run.ps1 -Action CreateDatabase
.\db\central\run.ps1 -Action VerifyDatabase
```

Script bootstrap kết nối database hệ thống `master` để phát lệnh tạo
`PTITONE_CENTRAL`; không tạo bảng ứng dụng trong `master` và không nối
`PTITONE_MASTER`. DB mới dùng `Vietnamese_CI_AS`, RCSI bật, recovery `SIMPLE`
cho dev/demo; file data/log theo đường dẫn mặc định của instance.

**Kết quả cần thấy:** đúng tên server/DB, `ONLINE`, `Vietnamese_CI_AS`, RCSI = 1,
`SIMPLE`. DB mới có số bảng người dùng = 0 là bình thường, T2 chưa thêm schema.
Runner dừng với lỗi nếu SQL thất bại. Chạy lại chỉ kiểm cấu hình DB đã tồn tại;
không xóa DB, không nạp lại seed, không tự sửa DB lệch cấu hình.

Nếu lần tạo đầu bị gián đoạn sau `CREATE DATABASE`, DB có thể đã tồn tại nhưng
chưa đủ thiết lập. T1 kiểm tra trong SSMS, hoàn tất thiết lập trong thời gian
không có ứng dụng kết nối rồi chạy VerifyDatabase lại. Không cần DROP DB.

**Xem trước không cần cấu hình cá nhân:**

```powershell
.\db\central\run.ps1 -Action CreateDatabase -ConfigPath .\db\central\config.example.psd1 -WhatIf
```

## 5. T1: DB tích hợp, quyền và bàn giao

T1 chạy tạo DB ngay trên máy giữ SQL Server bằng quyền quản trị local. Khi
T5 cần kết nối từ máy khác, bật TCP/IP, chốt một cổng cố định và cho phép
kết nối qua mạng LAN/VPN của nhóm. Máy T1 phải bật trong thời gian tích hợp.
Bàn giao `host`, `port`, `database`, chế độ xác thực và cách kiểm kết nối.
Không mặc định named instance đang nghe ở cổng 1433; kiểm tra cấu hình thực tế.

Runner bootstrap chỉ dùng Windows Authentication. Máy khác chỉ chạy được
runner vào DB chung nếu tài khoản Windows của họ được SQL Server xác thực
và cấp quyền. Mô hình workgroup qua VPN không tự có điều kiện này. T1 quản
trị local; T5 có thể dùng SQL Authentication riêng cho JDBC sau khi T1 bật
Mixed Mode và cấp login. Không dùng chung tài khoản `sa` để chạy ứng dụng.

Trong SSMS, **Security → Logins → New Login**, tạo login riêng, tự đặt mật khẩu
và gửi qua kênh riêng. Trong **User Mapping**, ánh xạ vào đúng CENTRAL với
default schema `dbo`. Tài khoản SQL này khác tài khoản SV/GV/Admin trong ứng dụng.

| Loại tài khoản | Quyền và phạm vi |
|---|---|
| Quản trị T1 | Tạo DB/login/user, cấp quyền, backup/restore |
| Migration T2/T5 | DDL/DML cần cho migration trên đúng CENTRAL; T1 cấp theo script thực tế, không mặc định sysadmin |
| Chạy API T5 | Chỉ quyền đọc/ghi/EXECUTE cần cho repository/thủ tục; không cấp `db_owner`, `db_ddladmin`, tạo/xóa DB |

Sau khi T2 có đối tượng thật, T1/T2 thêm script cấp quyền cụ thể theo repository
hoặc thủ tục mà API dùng. Khung hiện chưa tự tạo login/user/role hoặc giả định
quyền trên các bảng chưa tồn tại. T5 kiểm bằng chính tài khoản API, gồm cả
trường hợp thao tác DDL bị từ chối.

Trước một đợt migration DB chung, T1 backup ra file riêng và kiểm khôi phục
trên DB thử nghiệm. `SIMPLE` không cung cấp chuỗi log backup để phục hồi tới
từng thời điểm. Ghi bản migration/seed tương ứng với backup; không commit
`.bak`, file dữ liệu hoặc mật khẩu vào Git.

## 6. T2: triển khai schema/migration/seed

1. Bắt đầu ở [migrations/README.md](migrations/README.md). Tái sử dụng định nghĩa
   bảng nghiệp vụ phù hợp từ thiết kế, gộp vào một DB; không chạy script site
   đang chờ snapshot.
2. Thống nhất với T5 bảng/tên cột/kiểu dữ liệu/khóa, ánh xạ tài khoản và quyền,
   môn tiên quyết, học kỳ, lớp/lịch, đăng ký/điểm. Giữ mã cơ sở chủ quản và
   mã cơ sở mở lớp để phục vụ Phần 2.
3. Viết migration có phiên bản. Chốt một công cụ quản lý migration với T5
   (mẫu cấu hình bên dưới theo Flyway). Runner CENTRAL chỉ bootstrap/verify,
   không phải migration engine và không tự quét/chạy tất cả file SQL.
4. Thêm [seed](seed/README.md), ca đúng/sai và truy vấn đối soát; chạy chủ động.
5. Thêm ca kiểm trong [tests](tests/README.md); bàn giao kết quả dựng từ DB rỗng
   và chạy lại cùng migration, cùng phiên bản seed.

Đừng đánh dấu ENV-04/F00 hoàn tất chỉ vì tạo được database rỗng. T1 phải dựng
lại được schema/seed theo hướng dẫn, T5 phải truy vấn và rollback thật.

## 7. T5: cấu hình backend một DB

Backend hiện chỉ có Spring Web; `/api/health` là liveness, không kiểm DB.
Các file trong `examples/` là **mẫu tham khảo, chưa được backend đọc**.

Khi triển khai ENV-05:

1. Thêm JDBC starter, Microsoft SQL Server JDBC driver và Flyway starter/module
   SQL Server tương thích Spring Boot đang ghim trong `apps/api/pom.xml`.
2. Đóng gói SQL nguồn của T2 bằng Maven resources, giữ resource ứng dụng hiện có:

```xml
<resources>
    <resource>
        <directory>src/main/resources</directory>
    </resource>
    <resource>
        <directory>${project.basedir}/../../db/central/migrations</directory>
        <targetPath>db/migration</targetPath>
        <filtering>false</filtering>
        <includes><include>*.sql</include></includes>
    </resource>
</resources>
```

Đặt đoạn trên trong `<build>` cùng các plugin hiện có. Không đưa bootstrap,
seed hoặc file `.example` vào migration resource.

3. Tham khảo [application-central.properties.example](examples/application-central.properties.example)
   để tạo profile `application-central.properties` trong resources của API.
   Bật profile `central` qua IDE hoặc biến `SPRING_PROFILES_ACTIVE`.
4. Cấp các biến trong [mẫu môi trường](examples/.env.example) qua run configuration
   của IDE hoặc môi trường tiến trình. URL ví dụ cho máy local, **thay cổng bằng
   cổng SQL thực tế T1 đã cấu hình**:

```text
PTITONE_DB_URL=jdbc:sqlserver://localhost:1433;databaseName=PTITONE_CENTRAL;encrypt=true;trustServerCertificate=true
PTITONE_DB_USERNAME=<login SQL chạy API do T1 cấp>
PTITONE_DB_PASSWORD=<mật khẩu riêng>
PTITONE_MIGRATE_ON_START=false
SPRING_PROFILES_ACTIVE=central
```

DB chung: thay `localhost` bằng host/IP VPN của T1. Không ghi mật khẩu vào URL.
`trustServerCertificate=true` chỉ là lựa chọn dev với chứng chỉ tự ký; khi
chứng chỉ đã được tin cậy thì dùng `false`. Frontend không nhận biến kết nối DB.

5. Sau khi T2 có migration, chọn một lần chạy migration có chủ đích: bật
   `PTITONE_MIGRATE_ON_START=true` và cấu hình đầy đủ `spring.flyway.url/user/password`
   như ghi chú trong mẫu, dùng tài khoản migration T1 cấp. URL migration và
   DataSource nghiệp vụ phải trỏ **cùng một CENTRAL**. Sau đó chạy API thường
   với migration tắt và chỉ giữ tài khoản API. T2/T5 có thể chốt công cụ chạy
   migration riêng thay cho startup, nhưng vẫn dùng cùng nguồn SQL/checksum.

Mẫu tắt migration mặc định để API không tự thay schema DB chung. Đây chưa
phải profile dùng được ngay: thiếu dependency, migration và quyền thì chưa
thể nghiệm thu. Không bật `baseline-on-migrate`/`clean` để xử lý lỗi tùy tiện.

Spring Boot không tự nạp `apps/api/.env`. Đã có `scripts/dev-api.ps1` nạp file
này vào môi trường tiến trình rồi gọi Maven Wrapper. Copy
`apps/api/.env.example` sang `apps/api/.env` nếu chưa có, điền giá trị riêng
rồi chạy `.\scripts\dev-api.ps1` từ gốc repo. Biến tiến trình có sẵn được ưu
tiên; giá trị rỗng được bỏ qua. Script không thực thi nội dung `.env` hay in
giá trị ra log. Dùng `-ValidateOnly` để kiểm cú pháp mà không chạy ứng dụng.
Xem [quy tắc nạp và cấu hình IntelliJ](../../apps/api/README.md#nạp-biến-môi-trường-khi-dev-t5).
Chạy trực tiếp `mvnw.cmd spring-boot:run` hoặc nút Run của IDE vẫn cần cấp
biến qua môi trường/run configuration. Loader chỉ hoàn tất cơ chế nạp biến;
JDBC, profile `central`, migration và kiểm chứng ENV-05 vẫn cần triển khai.

**Bàn giao ENV-05:** kết nối đúng DB; truy vấn bảng thật; transaction rollback
đúng; sai mật khẩu/thiếu quyền báo lỗi; quyền nghiệp vụ được kiểm qua API;
`/api/health` trả UP riêng lẻ không chứng minh các điều trên.

## 8. Lỗi thường gặp và kiểm chứng

| Hiện tượng | Kiểm tra |
|---|---|
| Thiếu `config.local.psd1` | Copy mẫu, sửa đúng server/DB rồi chạy `-WhatIf` |
| Không tìm thấy `sqlcmd` | Cài bản ODBC, mở terminal mới và kiểm PATH |
| Login failed / không tạo được DB | SSMS thử đúng Windows account; T1 kiểm quyền trên instance đích |
| Chứng chỉ hoặc timeout | Đúng instance/port, Engine/TCP hoạt động, cấu hình trust phù hợp; với máy khác kiểm LAN/VPN |
| DB tồn tại nhưng lệch cấu hình | T1 kiểm collation/RCSI/recovery; runner không tự sửa hay ngắt session |
| API UP nhưng không có bảng | Health chỉ là liveness; T2 chưa migrate hoặc T5 chưa nối JDBC |
| Seed/migration chưa có | Đây là phần T2/T5 phải hoàn thiện; không lấy file `.example` làm script nghiệp vụ |

Kiểm tra offline toàn bộ SQL/runner: `powershell -NoProfile -File db/tests/Test-Scripts.ps1`.
ScriptDom không thay thế kiểm SQL Server runtime. Mỗi người ghi riêng kết quả
đã chạy, server/DB, commit, phiên bản migration và ca thử; không đưa secret vào log.

Nhánh theo README: T1 có thể dùng `feature/db-central-environment`, T2 dùng
`feature/db-central-schema`, T5 dùng `feature/api-central-datasource`;
PR vào `dev`, rồi nhóm tích hợp lên `main`.

## 9. Khi chuyển sang Phần 2

CENTRAL là DB nghiệp vụ của Phần 1 và nguồn đối chiếu tính đúng. Phần 2 sẽ
phân chia danh mục về MASTER, hồ sơ về cơ sở chủ quản, lớp/đăng ký/điểm về
cơ sở mở lớp theo thiết kế. Không mặc định đổi tên toàn CENTRAL thành MASTER
hay site; cần kế hoạch migration và đối soát riêng.

## Tài liệu tham khảo

- [Thiết kế và quyết định D20](../../docs/PTIT-One-Thiet-Ke.md)
- [Khung backend và việc tiếp theo](../../docs/PTIT-One-Backend-Khoi-Dong.md)
- [Microsoft: tùy chọn sqlcmd](https://learn.microsoft.com/en-us/sql/tools/sqlcmd/sqlcmd-utility)
- [Microsoft: URL kết nối JDBC](https://learn.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url)
- [Spring Boot: Flyway và khởi tạo DB](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
