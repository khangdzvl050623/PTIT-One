# PTIT One — CI Phần 1

Workflow [CI](../.github/workflows/ci.yml) kiểm tra ứng dụng khi mở/cập nhật
PR vào `dev` hoặc `main`, và khi có push vào hai nhánh này. Có thể chạy thủ
công từ tab Actions sau khi workflow đã có trên nhánh mặc định.

| Check | Môi trường | Công việc |
|---|---|---|
| `ci-api` | Ubuntu 24.04, Temurin Java 21 | Maven Wrapper `verify`: biên dịch, chạy test, đóng gói JAR |
| `ci-web` | Ubuntu 24.04, Node.js 24 | `npm ci`, ESLint, kiểm tra TypeScript và build Vite |

Hai job chạy độc lập, cache dependency theo `pom.xml`/`package-lock.json`,
timeout 15 phút/job. Khi có commit mới, lượt chạy cũ của cùng PR/nhánh bị hủy.
Không lọc theo đường dẫn để hai check luôn xuất hiện, kể cả PR chỉ sửa tài liệu.

## Phạm vi kiểm tra

CI không cần `.env`, GitHub Secrets, SQL Server hay VPN. Backend dùng profile
`default`; migration tắt và `PTITONE_DB_URL` rỗng. `AuthFlowIntegrationTest`
tự bỏ qua theo điều kiện đã có trong test vì cần database đã migrate và seed.
Các test model, mật khẩu seed, security route và HTTP smoke vẫn chạy.

CI xanh chứng minh build và các test không cần DB đã qua; chưa chứng minh
luồng auth trên SQL Server, migration hay nghiệp vụ tích hợp. Các kiểm tra đó
vẫn chạy riêng trên database kiểm thử. Frontend hiện chưa có bộ unit test;
ESLint và TypeScript không thay thế kiểm thử giao diện/nghiệp vụ.

Workflow này không deploy, không chạy migration vào database dùng chung.

## Kiểm tra tại máy trước khi mở PR

Backend, tại `apps/api` (terminal không đặt `PTITONE_DB_URL`):

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Lệnh Maven trực tiếp không đọc `.env`. Nếu terminal đã có `PTITONE_DB_URL`,
test tích hợp có thể chạy: hãy dùng terminal riêng không có biến kết nối DB
để kiểm tra cùng phạm vi với CI.

Frontend, tại `apps/web`, dùng Node.js 24:

```powershell
npm ci
npm run lint
npm run build
```

## Bật điều kiện bắt buộc trước khi merge

File workflow không tự sửa Rulesets của GitHub. Sau khi push nhánh feature
và mở PR vào `dev`, chờ `ci-api` và `ci-web` chạy thành công ít nhất một lần.
Người có quyền quản lý repository thực hiện:

1. Vào **Settings → Rules → Rulesets**.
2. Sửa ruleset cho `dev`, bật **Require status checks to pass**.
3. Thêm hai check **`ci-api`**, **`ci-web`** từ GitHub Actions.
4. Lặp lại cho ruleset của `main`; giữ nguyên quy định PR/approval hiện có.

Chỉ thêm check sau khi chúng đã xuất hiện trên GitHub; nhập sai tên hoặc yêu
cầu một check chưa chạy có thể khiến PR chờ mãi. `branch-guard` tiếp tục kiểm
nhánh nguồn của PR vào `main`, độc lập với CI ứng dụng.

Khi CI lỗi: mở PR → **Checks** → job đỏ → bước lỗi. Sửa trên nhánh feature
và push lại; workflow tự chạy lại. Không bỏ check chỉ để merge.
