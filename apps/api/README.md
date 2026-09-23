# PTIT One API

Khung backend Spring Boot **4.1.1**, Java **21**, Maven Wrapper **3.9.16**.
Mở thư mục này trong IntelliJ hoặc VS Code; wrapper là cách build chung.

Hiện có cấu trúc tầng và `GET /api/health`. Endpoint này chỉ kiểm ứng dụng HTTP
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
`SERVER_PORT`. Skeleton không nạp file `.env` và không cần secret để chạy.

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Kết quả: `{"service":"ptit-one-api","status":"UP"}`.
Các endpoint nghiệp vụ chưa triển khai, ví dụ `/api/me`, trả 404.

Trên Linux/macOS dùng `./mvnw` thay cho `.\mvnw.cmd`.

## Kiểm tra và đóng gói

```powershell
.\mvnw.cmd verify
java -jar target/ptit-one-api-0.0.1-SNAPSHOT.jar
```

Smoke test khởi động server ở cổng ngẫu nhiên và gọi HTTP thật, không cần DB.
`target/` được Git bỏ qua. JAR hiện chỉ phục vụ backend; chưa chứa frontend.

## Kết nối frontend khi dev

Frontend ở `apps/web`, cùng cấp với `apps/api`. Xem
[hướng dẫn frontend](../web/README.md). Chạy `npm ci` rồi `npm run dev` tại đó; Vite ở 5173 và proxy `/api` tới backend
8080. Gọi `http://localhost:5173/api/health` để kiểm đường đi qua proxy.
Frontend nên gọi URL tương đối `/api/...`; không cần bật CORS rộng.

Nếu đổi cổng backend, sửa đích proxy tương ứng trong `apps/web/vite.config.ts`.
Form đăng nhập hiện tại vẫn là giao diện mẫu, chưa xác thực với API.

## Ranh giới các tầng

```text
vn.ptit.one/
├── PtitOneApplication.java
├── domain/              quy tắc nghiệp vụ, không import Spring/JDBC
├── application/         use case điều phối nghiệp vụ
├── infrastructure/      repository, cấu hình DB và security khi triển khai
└── interfaces/rest/     controller và DTO HTTP
```

Các package chưa có nghiệp vụ được lưu bằng `package-info.java`, không tạo
service/interface giả. Controller nghiệp vụ đi qua application; SQL nằm trong
repository. Chưa dựng routing, saga, Outbox, mirror hoặc port phân tán.

Bước tiếp theo là schema `PTITONE_CENTRAL` rồi JDBC/migration với **một
DataSource**; chỉ thêm khi có DB và contract phù hợp. Xem
[ghi chú triển khai và nhánh tiếp theo](../../docs/PTIT-One-Backend-Khoi-Dong.md).
