# PTIT One API

Khung backend Spring Boot **4.1.1**, Java **21**, Maven Wrapper **3.9.16**.
Mở thư mục này trong IntelliJ hoặc VS Code; wrapper là cách build chung.

Hiện có khung module nghiệp vụ và `GET /api/health`. Endpoint này chỉ kiểm ứng dụng HTTP
đang chạy, không xác nhận DB hoạt động. Chưa có đăng nhập, Security, nghiệp vụ,
JDBC, migration hoặc kết nối database. Khởi động skeleton không đọc/ghi DB.

## Chạy trên Windows

Cài JDK 21, đặt `JAVA_HOME` vào thư mục JDK, rồi mở terminal tại `apps/api`:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd spring-boot:run
```

Lần đầu wrapper cần mạng để tải Maven/dependency. Không cần Maven cài toàn máy.
Ứng dụng mặc định ở `http://localhost:8080`; có thể đổi cổng bằng biến môi trường
`SERVER_PORT`. Chạy wrapper trực tiếp không nạp file `.env`; dùng script bên
dưới nếu muốn nạp file. Skeleton không cần secret để chạy.

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Kết quả: `{"service":"ptit-one-api","status":"UP"}`.
Các endpoint nghiệp vụ chưa triển khai, ví dụ `/api/me`, trả 404.

Trên Linux/macOS dùng `./mvnw` thay cho `.\mvnw.cmd`.

## Nạp biến môi trường khi dev (T5)

Tại gốc repo, tạo file riêng tư một lần, không chép đè file đã có:

```powershell
if (-not (Test-Path -LiteralPath .\apps\api\.env)) {
    Copy-Item -LiteralPath .\apps\api\.env.example -Destination .\apps\api\.env
}
# Sửa .env bằng editor, rồi chạy:
.\scripts\dev-api.ps1
```

Script dùng Windows PowerShell 5.1 hoặc PowerShell 7, tự chuyển về `apps/api`
để gọi wrapper và trả lại thư mục/biến môi trường cũ khi kết thúc. Có thể gọi
bằng đường dẫn tuyệt đối từ bất kỳ thư mục nào. JDK 21 vẫn cần được cấu hình
qua `JAVA_HOME`/PATH. Không sửa biến môi trường User/Machine.

```powershell
# Chỉ kiểm cú pháp, không in giá trị, không chạy Maven hay kết nối DB:
.\scripts\dev-api.ps1 -ValidateOnly
# Nạp env rồi kiểm JDK mà Maven thực sự dùng:
.\scripts\dev-api.ps1 -MavenArguments '--version'
# Truyền nhiều đối số từ PowerShell:
.\scripts\dev-api.ps1 -MavenArguments @('clean', 'verify')
# File khác: đường dẫn tương đối tính từ thư mục terminal hiện tại:
.\scripts\dev-api.ps1 -EnvFile .\apps\api\.env.local
```

Quy tắc nạp:

- Biến đã có trong môi trường tiến trình được ưu tiên hơn `.env`; file không
  ghi đè chúng. Dòng giá trị rỗng được bỏ qua, không xóa biến đã có.
- Không có `.env` mặc định vẫn chạy skeleton bằng môi trường hiện tại.
  Nếu chỉ định `-EnvFile` mà file không tồn tại thì dừng báo lỗi.
- File UTF-8, mỗi dòng `NAME=value`; bỏ qua dòng trống và dòng bắt đầu bằng `#`.
  Tên biến dùng chữ ASCII, số, `_`, không bắt đầu bằng số; không được trùng tên.
- Có thể bọc giá trị trong `'...'` hoặc `"..."` để giữ khoảng trắng đầu/cuối.
  Chỉ bỏ cặp nháy ngoài; `$`, `#`, `;`, `=`, `\`, `%` và backtick bên trong
  được giữ nguyên. Không thay `${VAR}`, không giải mã `\n`, không thực thi lệnh.
  Không hỗ trợ `export`, giá trị nhiều dòng hay chú thích cuối dòng.
- Sai cú pháp dừng trước khi chạy Maven, chỉ báo số dòng, không in nội dung.
  Script không log giá trị; tránh bật log debug/config của ứng dụng khi có secret.
- Script trả mã thoát của Maven. `.env` và `.env.local` đã được Git bỏ qua;
  chỉ commit `.env.example` không chứa giá trị thật.

Ví dụ thử cơ chế nạp ngay với skeleton: ghi `SERVER_PORT=8081` trong `.env`,
chạy script rồi gọi `http://localhost:8081/api/health`. Nếu terminal đã có
`SERVER_PORT`, giá trị terminal được ưu tiên. Đổi proxy Vite nếu chạy web cùng.

**Chạy bằng nút Run trong IntelliJ:** mở **Run → Edit Configurations**, chọn
cấu hình chạy API, mở **Environment variables** (qua **Modify options** nếu
đang ẩn). Nhập từng biến/giá trị bằng bảng editor, đặc biệt URL JDBC chứa dấu
`;`. Đặt JRE/SDK 21 rồi Apply/Run. Nút Run không gọi `dev-api.ps1`, nên không
tự nhận `.env` qua script; cách chung không phụ thuộc plugin của IDE.

Các biến `PTITONE_DB_*`, migration và profile `central` trong mẫu chỉ chuẩn bị
cho bước JDBC. Hiện chưa có dependency/profile DataSource; điền chúng chưa
tạo kết nối DB hoặc chạy migration. Xem [bàn giao CENTRAL](../../db/central/README.md#7-t5-cấu-hình-backend-một-db).

Kiểm thử loader offline (không cần JDK, Maven, DB hay mật khẩu thật):

```powershell
powershell -NoProfile -File scripts/tests/Test-DevApi.ps1
```

Cơ chế dựa trên [biến môi trường kế thừa của PowerShell](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_environment_variables)
và [cấu hình ngoài của Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html).

## Nối database — profile `central`

Mặc định API **không nối DB**: `DataSourceAutoConfiguration` bị loại trong
`application.properties`, nên máy chưa cài SQL Server vẫn chạy được API để
dựng frontend. Bật DB bằng profile `central`.

Điền `apps/api/.env` rồi chạy `.\scripts\dev-api.ps1` từ gốc repo:

```text
SPRING_PROFILES_ACTIVE=central
PTITONE_DB_URL=jdbc:sqlserver://localhost:14330;databaseName=PTITONE_CENTRAL;encrypt=true;trustServerCertificate=true
PTITONE_DB_USERNAME=ptitone_api
PTITONE_DB_PASSWORD=<mật khẩu riêng của bạn>
PTITONE_MIGRATE_ON_START=false
```

Cổng trong URL là cổng TCP **thật** của instance, không mặc định 1433 —
named instance thường dùng cổng động, xem
[hướng dẫn cài Phần 1](../../docs/PTIT-One-Cai-Dat-Phan-1.md).

Kiểm bằng endpoint riêng, **không** dùng `/api/health`:

```powershell
Invoke-RestMethod http://localhost:8080/api/health/db
```

```json
{ "status": "UP", "database": "PTITONE_CENTRAL", "login": "ptitone_api" }
```

Endpoint này hỏi thẳng SQL Server đang ở database nào và đăng nhập bằng tài
khoản nào, thay vì đọc lại chính cấu hình của mình — cấu hình sai thì đọc lại
cấu hình vẫn ra "đúng". Nối hỏng thì trả **503** kèm nguyên nhân, không trả UP giả.

`/api/health` vẫn chỉ là liveness và **không** nói gì về DB. Ở profile mặc
định, `/api/health/db` trả 404 đúng như thiết kế.

Migration **tắt mặc định** để API không tự đổi schema của DB dùng chung.
Nguồn SQL là `db/central/migrations/` của T2, được Maven đóng gói vào
`classpath:db/migration`; không giữ bản DDL thứ hai trong `apps/api`.

## Kiểm tra và đóng gói

```powershell
.\mvnw.cmd verify
java -jar target/ptit-one-api-0.0.1-SNAPSHOT.jar
```

Smoke test khởi động server ở cổng ngẫu nhiên và gọi HTTP thật, không cần DB.
`target/` được Git bỏ qua. JAR hiện chỉ phục vụ backend; chưa chứa frontend.

`AuthFlowIntegrationTest` chạy luồng auth thật trên SQL Server, chỉ khi có
`PTITONE_DB_URL` (DB đã migrate V1 + seed `10-auth-seed.sql`). Chạy kèm `.env`:
`.\scripts\dev-api.ps1 -MavenArguments verify` từ gốc repo. Không có DB thì
4 ca này hiện `skipped`, không phải pass.

## Auth (A0)

Cần `PTITONE_JWT_SECRET` trong `.env` khi chạy profile `central`. Contract đầy đủ
ở [kế hoạch auth](../../docs/PTIT-One-Ke-Hoach-Auth.md#5-contract-api).

| API | Ghi chú |
|---|---|
| `GET /api/auth/csrf` | Đặt cookie `XSRF-TOKEN`; mọi POST gửi lại qua header `X-XSRF-TOKEN` |
| `POST /api/auth/login` | Đặt cookie `PTITONE_AT` (15') + `PTITONE_RT` (7 ngày, HttpOnly); đổi `XSRF-TOKEN` |
| `GET /api/auth/me` | Danh tính từ DB + `expiresAt`, `accessExpiresAt` |
| `POST /api/auth/refresh` | Rotate refresh; dùng lại token cũ = replay → thu hồi cả phiên |
| `POST /api/auth/logout` | `204` sau khi thu hồi; xóa hai cookie |
| `POST /api/auth/logout-all` | Tăng `PhienBanTaiKhoan`, thu hồi mọi phiên |

Test tay: import `postman/PTIT-One-Auth.postman_collection.json`. Script của
collection tự lấy và gắn CSRF cho mọi POST; endpoint mới thêm vào collection
dùng được ngay. Không tắt CSRF trên server để tiện test.

Frontend: chỉ **một** refresh tại một thời điểm (phối hợp giữa các tab). Hai
refresh song song cùng token bị coi là replay và mất phiên. Module khác lấy người
dùng bằng `@AuthenticationPrincipal AuthenticatedUser`; không nhận `role`,
`campus`, `studentId` từ client.

## Kết nối frontend khi dev

Frontend ở `apps/web`, cùng cấp với `apps/api`. Xem
[hướng dẫn frontend](../web/README.md). Chạy `npm ci` rồi `npm run dev` tại đó; Vite ở 5173 và proxy `/api` tới backend
8080. Gọi `http://localhost:5173/api/health` để kiểm đường đi qua proxy.
Frontend nên gọi URL tương đối `/api/...`; không cần bật CORS rộng.

Nếu đổi cổng backend, sửa đích proxy tương ứng trong `apps/web/vite.config.ts`.
Form đăng nhập hiện tại vẫn là giao diện mẫu, chưa nối với API auth (AUTH-05).

## Cấu trúc theo module

Một ứng dụng Spring Boot, một DB tập trung ở Phần 1. Package gốc vẫn là
`vn.ptit.one`; class khởi động nằm trên các module để Spring scan được chúng.

```text
src/main/java/vn/ptit/one/
├── PtitOneApplication.java
├── auth/                  xác thực và tài khoản
├── student/               hồ sơ sinh viên
├── course/                môn học, lớp học phần và quan hệ tiên quyết
├── enrollment/            đăng ký, hủy đăng ký và kiểm điều kiện
├── grade/                 nhập, công bố và xem điểm
├── timetable/             lịch học và lịch giảng dạy
├── health/
│   ├── controller/        HealthController
│   └── dto/               HealthResponse
└── shared/
    ├── config/            cấu hình kỹ thuật dùng chung
    └── exception/         hợp đồng lỗi và xử lý lỗi chung
```

Hiện chỉ module `health` có chức năng chạy được. Sáu module nghiệp vụ và
`shared` được giữ bằng `package-info.java` mô tả phạm vi, chưa có nghiệp vụ.
Không tạo endpoint, service, repository hoặc interface giả để lấp thư mục.

Khi triển khai một module, thêm các package cần dùng theo bảng sau:

| Package trong module | Trách nhiệm |
|---|---|
| `controller` | HTTP, kiểm định dạng request, gọi service và trả response |
| `service` | Điều phối use case, kiểm quyền nghiệp vụ và ranh giới giao dịch |
| `repository` | SQL/JDBC, ánh xạ dữ liệu và truy cập DB của module |
| `dto` | Request/response; không trả trực tiếp đối tượng lưu trữ ra API |
| `model` | Khái niệm nghiệp vụ, trạng thái và quy tắc gắn với đối tượng |
| `policy` | Quy tắc thuần phức tạp như xét điều kiện đăng ký; thêm khi cần |

Ví dụ đường dẫn tương lai: `enrollment/service/EnrollmentService.java`,
`enrollment/policy/EnrollmentEligibilityPolicy.java`. Đây là ví dụ vị trí,
không phải các tính năng đã được cài đặt.

Quy tắc phụ thuộc:

- Controller nghiệp vụ gọi service; SQL nằm trong repository. Liveness không
  có nghiệp vụ hay truy cập DB nên trả DTO trực tiếp, không cần service rỗng.
- `model` và `policy` là Java thuần, không import Spring, JDBC, controller
  hoặc repository. Service lấy dữ liệu rồi truyền vào quy tắc để kiểm tra.
- Module chỉ dùng API công khai của module khác (service/facade được chỉ định),
  không gọi repository/controller nội bộ hoặc tạo phụ thuộc vòng.
- `shared` chứa kỹ thuật dùng chung, không import module nghiệp vụ; lỗi đặc
  thù của nghiệp vụ đặt trong module sở hữu. Không dồn nghiệp vụ vào `shared`.
- JDBC là hướng truy cập DB của Phần 1. Chỉ thêm `entity` nếu thực sự dùng JPA;
  không tạo đồng thời model/entity trùng nhau một cách mặc định.

Không còn bốn package tầng chung ở gốc. Chưa thêm Spring Modulith, routing,
saga, Outbox, mirror hoặc ba port phân tán; kế hoạch Phần 2 nằm ở C8/J1.

Bước tiếp theo là schema `PTITONE_CENTRAL` rồi JDBC/migration với **một
DataSource**; chỉ thêm khi có DB và contract phù hợp. Xem
[ghi chú triển khai và nhánh tiếp theo](../../docs/PTIT-One-Backend-Khoi-Dong.md).
