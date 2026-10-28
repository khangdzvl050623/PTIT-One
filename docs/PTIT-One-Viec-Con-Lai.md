# PTIT One — Việc còn lại

Chốt **24/10/2026**, deadline **30/10**. Năm yêu cầu bắt buộc đã PASS và chạy
lại được bằng một lệnh; mọi thứ dưới đây là phần chưa xong.

```
powershell -NoProfile -File db\tests\Chay-Bang-Chung.ps1 -DongBoLai
powershell -NoProfile -File db\tests\94-demo-lien-co-so.ps1
```

---

## 1. F02 qua Outbox — gấp nhất

`admin.master → POST /api/students` trả **503** `Invalid object name
'dbo.SinhVien'`. Admin Master đứng ở `MASTER`, nơi không có bảng vận hành; mà
quyết định 02/10 là *chỉ Admin Master được cấp tài khoản*. Nên hiện **không ai
cấp được hồ sơ mới ở Phần 2**.

Luồng theo thiết kế (mục C2):

```
┌ Một giao dịch tại MASTER ─────────────────┐
│  INSERT DanhBaNguoiDung (CHO_KICH_HOAT)   │
│  INSERT OutboxSuKien ('SinhVienDuocTao')  │
└──────────── COMMIT ───────────────────────┘
        │  OutboxWorker
        ├─ 1. tạo SinhVien + TaiKhoan tại cơ sở nhà   ← TRƯỚC
        └─ 2. đánh dấu SENT tại Master                ← SAU
```

`OutboxWorker` đã xử lý `DiemDaChot`, chỉ thiếu loại thứ hai. Đảo thứ tự hai
bước là mất sự kiện vĩnh viễn.

**Ước lượng:** 1 ngày.

---

## 2. Ba máy site lên VPN

Hiện replication chỉ có **một** subscriber (`PTITONE_HCM`, cùng máy với
Publisher), nên 7 giây là trễ nội bộ, **chưa phải trễ qua mạng**.

| Máy | Việc |
|---|---|
| Master | Linked Server tới 3 site · `replication\30` → `31` → `32` · `migrate\50` rồi `51` |
| HCM · HN · ĐN | Mở port 1433 + SQL Browser · Mixed Mode · tạo `ptitone_admin` (sysadmin) và `ptitone_api` · `00-create-databases.sql` · `site\10` → `11` → `12` → `13` |

Ba máy site **không** chạy seed, **không** tạo dữ liệu — dữ liệu tới từ
`migrate\50` và từ replication. Xong rồi `tests\93` tự báo đủ 3 subscriber mà
không phải sửa gì.

**Ước lượng:** 2 ngày. Đây là **đường găng** — không rút ngắn bằng thêm người.

---

## 3. Hai đường chưa ai thử

| Đường | Có code | Đã chạy thử |
|---|---|---|
| Huỷ đăng ký liên cơ sở (`POST /{maYeuCau}/cancel`) | ✅ | ❌ |
| Outbox gửi hỏng rồi thử lại (`recordFailure`) | ✅ | ❌ |

Demo `94` mới đi đường thành công. Thiết kế nêu bốn tình huống bắt buộc kiểm
cho đường huỷ: **huỷ đến trước** · **phản hồi đến muộn** · **gửi lặp** ·
**tiến trình chết giữa chừng**. Thêm pha vào `94` rẻ hơn viết script mới.

**Ước lượng:** 0,5 ngày.

---

## 4. Báo cáo và ảnh chụp

Output của `Chay-Bang-Chung.ps1` nằm ở `bang-chung\<ngày-giờ>\`, đã gitignore.
Chụp màn hình cần có: màn Tổng quan của Admin Master (thống kê liên cơ sở),
đăng nhập 5 vai, và demo `94` chạy xong.

**Ước lượng:** 2 ngày, chạy xuyên suốt.

---

## 5. Phần ➕ — cắt được nếu thiếu giờ

- **X-Ray phân tán** (mục 0.2 thiết kế) — chưa bắt đầu.
- **Frontend `lien-co-so`** — đang dựng. Backend đã sẵn:
  `POST /api/me/cross-campus` (body `maCoSoHost`, `maLopHP`),
  `GET ?maHocKy=`, `/{maYeuCau}/cancel`, `/{maYeuCau}/retry`.
- **Port `CatalogHealth`** — một trong ba port Phần 2, chưa dựng.
  (`CrossSiteQuery` và `GlobalReport` coi như đã có qua `master\08`.)

---

## Hai cái bẫy dễ vấp lại

**Chạy bộ test API xong phải `-DongBoLai`.** Test tích hợp để lại lớp đã huỷ
trong CENTRAL, mà CENTRAL là **nguồn** của `migrate\50` — không reset thì đối
soát báo `LECH` và trông y như lỗi phân mảnh.

**`run.ps1 -On HN/DN` trỏ tới máy thật của nhóm**, chưa lên VPN. Bản sao cục
bộ nằm ở instance `SPIKE2` (`tcp:localhost,14331`), chạy script bằng `sqlcmd`
trực tiếp kèm `-v`.

---

## Nói thẳng khi nghiệm thu

Nếu tới hôm báo cáo vẫn chỉ có một subscriber, **nói rõ điều đó** thay vì để
bảng tổng kết ngụ ý ba cơ sở. Con số 7 giây khi ấy là trễ nội bộ máy.
