# PTIT One — Kế hoạch Phần 2 (CSDL phân tán)

Chốt ngày **09/10/2026**, sau khi Phần 1 đã merge vào `dev`.

> Tài liệu này **không thay** [thiết kế](PTIT-One-Thiet-Ke.md) — thiết kế vẫn là
> nguồn sự thật duy nhất. Nó chỉ trả lời một câu mà thiết kế không trả lời được:
> *làm theo thứ tự nào, khi Phần 1 đã xong trước Phần 2.*
>
> Lộ trình 8 tuần ở **Phần H** của thiết kế viết theo thứ tự **cũ** — dựng hạ
> tầng trước, ứng dụng sau. Nhóm đã đảo thứ tự đó (quyết định **D20**, ngày
> 24/09/2026). Nên tuần 5–6 của Phần H **đã xong rồi**, còn tuần 1–4 mới là
> phần sắp làm.

---

## 1. Đang đứng ở đâu

### Đã xong (Phần 1)

| | Trạng thái |
|---|---|
| `apps/api` · `apps/web` | 85 endpoint, chạy trên **một** database `PTITONE_CENTRAL` |
| Schema `db/central` | `V1`–`V10`, đã triển khai và đang dùng |
| Yêu cầu bắt buộc **#4** — tương tranh | ✅ **đã xong** — `EnrollmentConcurrencyIntegrationTest`, 100 luồng tranh 30 chỗ |

### Đã viết nhưng **chưa chạy lần nào**

`db/master`, `db/site`, `db/replication` có đủ script. `Test-Scripts.ps1` chỉ
kiểm **cú pháp** và tự ghi *"CHUA kiem chung SQL Server runtime"*.

### Chưa có gì

`apps/api` **không có** `SiteContext`, `RoutingDataSource`, ba port, Outbox
worker, hay X-Ray. Tầng phân tán ở phía ứng dụng là trang giấy trắng.

---

## 2. ⚠️ Hai schema đã trôi khỏi nhau

Đây là phát hiện làm đổi thứ tự kế hoạch, và nó **không nằm trong tài liệu
thiết kế** vì thiết kế viết trước khi Phần 1 bắt đầu.

| | `db/site` (Phần 2) | `db/central` (Phần 1, đang chạy) |
|---|---|---|
| `MaKichHoat`, `MaXacThuc`, `ThongBao` | ✗ | ✓ (`V4`–`V6`) |
| `AnhDaiDien`, `SoCCCD`, `EmailDaXacMinh`, `NguoiCap` | ✗ | ✓ (`V6`, `V8`–`V10`) |
| `OutboxSuKien`, `YeuCauHocLienCoSo`, `KetQuaXuLyYeuCau`, `BangDiemMirror` | ✓ | ✗ |

**Hệ quả:** triển khai `db/site` nguyên trạng thì `apps/api` **gãy ngay** —
mất kích hoạt tài khoản, xác minh email, thông báo, lý lịch, ảnh đại diện.

Phải hoà giải trước khi chạm vào hạ tầng. Chi tiết ở §4, Giai đoạn 1.

---

## 3. Quyết định mới

| Mã | Quyết định | Ngày |
|---|---|---|
| **D15** | **4 máy — `SRV-MASTER` là nút riêng**, không colocate trên SRV-HCM | 09/10/2026 |
| **D12** | Triển khai **3 cơ sở** (HCM · HN · ĐN) + Master = 4 nút | 09/10/2026 |
| — | `PTITONE_CENTRAL` **nghỉ sau Giai đoạn 1**, nhưng giữ chạy tới đó để còn demo Phần 1 | 09/10/2026 |

Đổi `db/config.ps1`: `MASTER = 'SRV-MASTER'` — chỗ đó đã dự trù sẵn phương án
4 máy, không phải sửa gì khác.

**Vì sao cho `central` nghỉ:** duy trì hai schema song song chính là cách
chúng đã trôi khỏi nhau (§2). Giữ thêm một bản thứ hai là bảo đảm lặp lại
đúng lỗi đó. Sau Giai đoạn 1, máy dân dụng dựng **hai named instance**
(`MASTER` + một site) là làm việc được.

---

## 4. Bảy giai đoạn, xếp theo rủi ro

> Không xếp theo thứ tự tài liệu. Việc nào **có thể làm hỏng cả đồ án** thì
> làm trước, kể cả khi nó không phải việc dễ nhất.

### Giai đoạn 0 · Spike — **cổng chặn** · 2–3 ngày

Hai thứ có thể giết yêu cầu **#2** và **#3**. Chi tiết và cách đọc kết quả:
[`db/spike/README.md`](../db/spike/README.md).

- **A — Replication:** `INSERT` ở MASTER → ≤10s thấy ở site
- **B — MS DTC:** `BEGIN DISTRIBUTED TRANSACTION` qua Linked Server, **có ca ROLLBACK**

Chạy trên **hai named instance cùng một máy trước**, rồi mới qua VPN. Lý do:
tách được "sai cấu hình SQL" khỏi "mạng không thông" — hai nguyên nhân mà chạy
thẳng qua VPN sẽ lẫn vào nhau.

**Không đi tiếp khi chưa có kết quả.** Hỏng thì rẽ theo bảng ở cuối
`db/spike/README.md`.

### Giai đoạn 1 · Hoà giải schema · 2–3 ngày

Việc này **không cần hạ tầng**, làm song song với Giai đoạn 0 được.

1. Dùng **Ownership Matrix (C2)** quyết chỗ đứng của 4 bảng Phần 1 mới thêm.
   Gợi ý: `MaKichHoat`/`MaXacThuc` đi theo `TaiKhoan` → **per-site**;
   `ThongBao` thuộc module `notification` → **per-site**. Cần chốt bằng C2 chứ
   đừng quyết theo cảm giác.
2. Port các cột `V8`–`V10` (lý lịch, ảnh, `NguoiCap`) vào `db/site`.
3. Đưa 4 bảng phân tán vào tầm nhìn của `apps/api`.
4. Chạy `db/tests/Test-Scripts.ps1` — nó chặn mọi `.sql` không được khai báo.

**Xong giai đoạn này mới cho `central` nghỉ.**

### Giai đoạn 2 · Cài đặt vật lý (Phần F) · 1–1.5 tuần

VPN · SQL Server · Agent · **MS DTC (F4b)** · Linked Server hình sao từ
SRV-HCM · Publication trên MASTER · Subscription: **HCM trước** (cục bộ, dễ
kiểm chứng) rồi mới HN và ĐN qua VPN.

⚠️ **Chụp màn hình từng bước, ngay khi làm**, vào `docs/screenshots/<số>-<tên>/`.
Đây là phần nặng điểm nhất và bằng chứng không dựng lại được sau.

### Giai đoạn 3 · Năm yêu cầu bắt buộc (mục 3.7) · 1 tuần

| # | Việc | Trạng thái |
|---|---|---|
| 1 | Phân mảnh ngang — triển khai `db/site`, chia dữ liệu | script có, chưa chạy |
| 2 | Replication một chiều | sau Giai đoạn 2 |
| 3 | **`sp_ChuyenCoSoSinhVien` (2PC) + demo rollback khi tắt site đích** | ⚠️ **chưa có gì** |
| 4 | Tương tranh 100 luồng / 30 chỗ | ✅ **đã xong ở Phần 1** — chạy lại trên bản phân tán |
| 5 | `OPENQUERY` thống kê toàn hệ thống | thiết kế xong, chưa cài |

Kèm: trigger T1–T3 (**`NOT FOR REPLICATION`**), database role, demo phân quyền.

> ✅ **Xong giai đoạn này là ~75% điểm đã an toàn.** Mọi thứ sau đây là phần thêm.

### Giai đoạn 4 · Ứng dụng phân tán · 1.5–2 tuần

- `SiteContext` + `RoutingDataSource` — **một giao dịch = một site**, tuyệt đối
- Ba port: `CrossSiteQuery`, `GlobalReport`, `CatalogHealth` — không port thứ tư
- Saga đăng ký liên cơ sở + idempotency + compensating transaction
- Outbox + worker + write-back điểm: **upsert mirror TRƯỚC, đánh dấu `SENT` SAU**

### Giai đoạn 5 · X-Ray + đo đạc · 1 tuần

Trace từng request · phòng điều khiển · so sánh ba chiến lược truy vấn ·
benchmark B1–B6 · thí nghiệm Saga vs 2PC (G5) · demo deadlock (G7) · sáu kịch
bản sự cố (G4).

### Giai đoạn 6 · Báo cáo, slide, tổng duyệt

---

## 5. Năm rủi ro lớn nhất

| # | Rủi ro | Giảm bằng |
|---|---|---|
| 1 | **Replication qua VPN** — chính tài liệu gọi đây là *"hạng mục rủi ro cao nhất"* | Spike A trước mọi thứ; phương án I2 sẵn sàng |
| 2 | **Hai schema lệch nhau** (§2) | Giai đoạn 1, trước khi chạm hạ tầng |
| 3 | **`@Transactional` không bao giờ trải hai DataSource.** Mọi repository Phần 1 viết với một DataSource — chuyển sang routing là sửa sâu, không phải thêm một lớp | Làm `SiteContext` sớm trong Giai đoạn 4, chuyển từng module |
| 4 | **Nhóm 3 người, thiết kế phân vai cho 5** — mà ~30% điểm nằm ở tài liệu | Phải có **một người làm tài liệu gần như toàn thời gian**; đây là cách hỏng bài phổ biến nhất |
| 5 | **4 máy phải cùng online đúng buổi hẹn** | Chốt 2–3 buổi cố định mỗi tuần (I2b); mỗi máy online ít nhất một lần mỗi cửa sổ retention |

---

## 6. Việc còn treo, cần chốt với giảng viên

Lấy từ **I4** của thiết kế, chỉ giữ mục còn hiệu lực:

- [ ] ⭐ **Xác nhận năm yêu cầu bắt buộc** — nhất là #3, để chắc
      `sp_ChuyenCoSoSinhVien` là hiện thực được chấp nhận
- [ ] **Tài liệu hướng dẫn Replication của giảng viên** — quyết cách làm F6, xác nhận D9
- [ ] **Trọng số bonus của phần mềm** (X-Ray, `apps/web`) — quyết đáng đầu tư bao nhiêu cho Giai đoạn 5
- [ ] **Số liệu quy mô thật** thay giả định 🔶 ở A4/B2
- [ ] Chốt người giữ máy từng site. Tiêu chí cho `SRV-MASTER`: **ít bị mang đi lại
      nhất, ưu tiên máy để bàn + đĩa khá** — không phải máy yếu nhất
