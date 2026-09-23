# PTIT One Web

React 19 + TypeScript + Vite + SCSS Modules. Frontend của **PTIT One**.

> Frontend đặt tại `apps/web`, backend tại `apps/api`, theo cấu trúc monorepo
> trong [tài liệu thiết kế, mục J2](../../docs/PTIT-One-Thiet-Ke.md).
> Mở terminal hoặc IDE tại `apps/web` để chạy các lệnh dưới đây.

```bash
npm ci
npm run dev       # http://localhost:5173
npm run build
```

API được dựng ở `apps/api`, mặc định cổng 8080. Vite cổng 5173 đã proxy
`/api` tới `http://127.0.0.1:8080`; truy cập `/api/health` qua Vite để kiểm
kết nối. Backend hiện là skeleton, form đăng nhập chưa nối auth.
Xem [hướng dẫn backend](../api/README.md). UI tiếp tục làm song song
theo kế hoạch Phần 1; không cần chờ cổng phân tán của lịch cũ.

## Kiến trúc: Feature-Sliced (rút gọn)

Bốn tầng, phụ thuộc **một chiều**: `app → pages → features → shared`.
**Feature không import lẫn nhau. `shared` không import feature.**

```
src/
├── app/
│   ├── router/        định tuyến, route guard theo vai trò
│   ├── providers/      provider toàn cục (router, error boundary...)
│   └── layouts/          khung trang: layout mặc định, layout admin, layout auth
│
├── pages/              màn hình — HomePage (cổng thông tin), PlaceholderPage (logo mặc định), NotFoundPage
│
├── shared/
│   ├── ui/             design system: đã có Logo · Icon · Panel — còn Button · Table · Field · Dialog · Badge
│   ├── api/             client fetch duy nhất — gắn JWT · map lỗi · bóc _xray
│   ├── hooks/            useAuth · useSite · useAsync
│   ├── lib/                format ngày · tiết học · điểm
│   ├── config/               định danh trường/cơ sở
│   ├── constants/              route names, copy tĩnh
│   └── types/                   type dùng chung toàn app
│
├── features/
│   ├── auth/            đăng nhập · giữ JWT   (đã có LoginForm)
│   ├── thong-bao/         thông báo + học phí trang chủ (NoticeSpotlight · NoticeList · NoticeRow)
│   ├── thong-ke/               thống kê truy cập (AccessStats)
│   ├── lich-hoc/         thời khoá biểu hợp nhất
│   ├── dang-ky/            tìm lớp · đăng ký · trạng thái chỗ trống
│   ├── lien-co-so/           duyệt lớp site khác · gửi yêu cầu · theo dõi DANG_XU_LY / DANG_HUY
│   ├── bang-diem/               điểm local + BangDiemMirror, có nhãn LastSyncedAt
│   ├── nhap-diem/                 màn hình giảng viên
│   ├── danh-muc/                    admin Master
│   ├── bao-cao/                       thống kê toàn hệ thống
│   └── xray/                            ⭐ panel + phòng điều khiển + so sánh chiến lược
│
├── assets/               ảnh, icon, font — import qua barrel *.ts (không import thẳng vào component)
└── styles/                 _tokens.scss (màu, spacing, breakpoint) · _type.scss (type scale, dạng mixin)
```

Mỗi feature có `components/` (UI riêng của feature) và `hooks/`
(state/logic riêng của feature), cộng `index.ts` làm barrel export công khai —
phần còn lại của app chỉ import qua barrel đó, không đào sâu vào nội bộ
feature.

Component đặt tên `PascalCase.tsx`, style đi kèm `PascalCase.module.scss`
(CSS Modules). Giá trị màu/khoảng cách/cỡ chữ lấy từ token trong `styles/`,
không viết thẳng số vào component.

**Chỉ light mode.** Không khai báo `prefers-color-scheme: dark` — token màu
đặt một lần ở `:root` trong `global.scss`. Khi nào cần dark mode thì thêm nhánh
ghi đè token, không sửa từng component.

Dữ liệu mẫu của màn hình đặt trong `features/<tên>/data/` (chưa nối API) —
component không nhúng sẵn mảng dữ liệu hay chuỗi hiển thị dài.

## Quy ước

| Mẫu | Ở đâu |
|---|---|
| API client là adapter duy nhất | `shared/api` — không component nào gọi `fetch` trực tiếp |
| Overlay X-Ray xuyên suốt | `features/xray` đọc `_xray` từ **mọi** response, feature khác không cần biết |
| Design tokens | `styles/` — khai báo một chỗ, không lặp giá trị trong từng component |
| Import nội bộ | alias `@/...` (khai báo ở `vite.config.ts` + `tsconfig.app.json`), không leo `../..` |
| Ảnh tĩnh | import qua `assets/index.ts`, không đường dẫn chuỗi rải rác |
| Màu/nền logo | `Logo` nhận `variant="tile"` khi nền phía sau đậm (header/footer đỏ) |

Xem thêm mục **J2. Frontend** và **J4. Thứ tự dựng** trong
[tài liệu thiết kế](../../docs/PTIT-One-Thiet-Ke.md) về tổ chức mã nguồn.
Thứ tự triển khai hiện tại theo [Phần 1 và các nhánh tiếp theo](../../docs/PTIT-One-Backend-Khoi-Dong.md);
lịch phân tán cũ không chặn công việc UI/API trên một DB tập trung.
