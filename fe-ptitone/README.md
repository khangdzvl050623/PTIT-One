# fe-ptitone

Vue 3 + TypeScript + Vite. Frontend của **PTIT One**.

> ⚠️ Tài liệu thiết kế đã chốt (`docs/PTIT-One-Thiet-Ke.md`, mục J2) ghi frontend
> là React + Vite tại `apps/web`. Thư mục này lệch cả hai điểm đó theo quyết
> định của nhóm: giữ Vue 3, giữ vị trí `fe-ptitone/` ở gốc repo. Nếu đổi lại
> quyết định này, cập nhật ghi chú ở đây và ở J2.

```bash
npm install
npm run dev       # http://localhost:5173
npm run build
```

## Kiến trúc: Feature-Sliced (rút gọn)

Ba tầng, phụ thuộc **một chiều**: `app → features → shared`.
**Feature không import lẫn nhau. `shared` không import feature.**

```
src/
├── app/
│   ├── router/        định tuyến, route guard theo vai trò
│   ├── providers/      plugin toàn cục (Pinia, i18n... khi cần)
│   └── layouts/          khung trang: layout mặc định, layout admin, layout auth
│
├── shared/
│   ├── ui/             design system: Button · Table · Field · Dialog · Badge
│   ├── api/             client fetch duy nhất — gắn JWT · map lỗi · bóc _xray
│   ├── composables/      useAuth · useSite · useAsync
│   ├── lib/                format ngày · tiết học · điểm
│   ├── config/               nav, định danh trường/cơ sở
│   ├── constants/              route names, copy tĩnh
│   └── types/                   type dùng chung toàn app
│
├── features/
│   ├── auth/            đăng nhập · giữ JWT
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

Mỗi feature có `components/` (UI riêng của feature) và `composables/`
(state/logic riêng của feature), cộng `index.ts` làm barrel export công khai —
phần còn lại của app chỉ import qua barrel đó, không đào sâu vào nội bộ
feature.

## Quy ước

| Mẫu | Ở đâu |
|---|---|
| API client là adapter duy nhất | `shared/api` — không component nào gọi `fetch` trực tiếp |
| Overlay X-Ray xuyên suốt | `features/xray` đọc `_xray` từ **mọi** response, feature khác không cần biết |
| Design tokens | `styles/` — khai báo một chỗ, không lặp giá trị trong từng component |
| Ảnh tĩnh | import qua `assets/*.ts`, không đường dẫn chuỗi rải rác |

Xem thêm mục **J2. Frontend** và **J4. Thứ tự dựng** trong
`docs/PTIT-One-Thiet-Ke.md` để biết feature nào dựng được từ tuần 1 (không
chờ API) và feature nào chờ tới tuần 5+.
