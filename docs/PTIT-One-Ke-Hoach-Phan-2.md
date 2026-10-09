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
| — | **Tách `PTITONE_CENTRAL` thành `MASTER` + 3 site**; Phần 1 chạy lại trên bản phân tán. `central` nghỉ sau Giai đoạn 1 | 09/10/2026 |

Đổi `db/config.ps1`: `MASTER = 'SRV-MASTER'` — chỗ đó đã dự trù sẵn phương án
4 máy, không phải sửa gì khác.

### 3b. Tách `central`, không bỏ Phần 1

`PTITONE_CENTRAL` **không** tồn tại song song tới cuối. Nó được **tách** thành
`PTITONE_MASTER` + ba database cơ sở, và `apps/api` chạy lại nguyên vẹn trên
bản phân tán đó. Phần 1 là vốn đã có, không phải thứ để bỏ lại.

Hai việc kéo theo, đều nằm ở Giai đoạn 1:

1. **Hoà giải schema** — port đủ `V4`–`V10` sang `db/site` / `db/master`, vì
   thiếu chúng thì `apps/api` gãy (§2).
2. **Script tách dữ liệu** — `db/migrate/`, chuyển dữ liệu đang có trong
   `CENTRAL` sang Master và ba site **theo đúng vị từ phân mảnh**. Hiện
   `db/` chưa có script nào làm việc này; `db/master/04-seed-danhmuc.sql` chỉ
   seed danh mục, không chuyển dữ liệu thật.

**Vì sao không giữ hai bản song song:** duy trì hai schema cùng lúc chính là
cách chúng đã trôi khỏi nhau (§2). Giữ thêm một bản thứ hai là bảo đảm lặp
lại đúng lỗi đó.

Máy dân dụng dựng **hai named instance** (`MASTER` + một site) là lập trình
được bình thường, không cần VPN.

---

## 4. Bảy giai đoạn, xếp theo rủi ro

> Không xếp theo thứ tự tài liệu. Việc nào **có thể làm hỏng cả đồ án** thì
> làm trước, kể cả khi nó không phải việc dễ nhất.

### Giai đoạn 0 · Spike — **cổng chặn** · 09–10/10

Hai thứ có thể giết yêu cầu **#2** và **#3**. Chi tiết và cách đọc kết quả:
[`db/spike/README.md`](../db/spike/README.md).

- **A — Replication:** `INSERT` ở MASTER → ≤10s thấy ở site
- **B — MS DTC:** `BEGIN DISTRIBUTED TRANSACTION` qua Linked Server, **có ca ROLLBACK**

Chạy trên **hai named instance cùng một máy trước**, rồi mới qua VPN. Lý do:
tách được "sai cấu hình SQL" khỏi "mạng không thông" — hai nguyên nhân mà chạy
thẳng qua VPN sẽ lẫn vào nhau.

**Không đi tiếp khi chưa có kết quả.** Hỏng thì rẽ theo bảng ở cuối
`db/spike/README.md`.

### Giai đoạn 1 · Hoà giải schema + tách dữ liệu · 09–17/10

Việc này **không cần hạ tầng**, chạy song song với Giai đoạn 0 ngay từ ngày đầu.

1. Dùng **Ownership Matrix (C2)** quyết chỗ đứng của 4 bảng Phần 1 mới thêm.
   Gợi ý: `MaKichHoat`/`MaXacThuc` đi theo `TaiKhoan` → **per-site**;
   `ThongBao` thuộc module `notification` → **per-site**. Cần chốt bằng C2 chứ
   đừng quyết theo cảm giác.
2. Port các cột `V8`–`V10` (lý lịch, ảnh, `NguoiCap`) vào `db/site`.
3. Đưa 4 bảng phân tán (`OutboxSuKien`, `YeuCauHocLienCoSo`,
   `KetQuaXuLyYeuCau`, `BangDiemMirror`) vào tầm nhìn của `apps/api`.
4. **`db/migrate/` — script tách dữ liệu** `CENTRAL` → `MASTER` + 3 site theo
   đúng vị từ phân mảnh. Chưa có script nào làm việc này.
   Ba điểm dễ sai, kiểm bằng câu đối soát sau khi chạy:
   - `DangKyHocPhan` đi theo **`LopHocPhan`** (cơ sở mở lớp), **không** theo
     `SinhVien` — đây là lỗi đã từng mắc, ghi rõ trong `AGENTS.md`
   - `Diem` là dẫn xuất **bậc 2**, đi theo `DangKyHocPhan`
   - Danh mục dùng chung (`MonHoc`, `Khoa`, `HocKy`, `ChuongTrinhDaoTao`) về
     **Master**, không nhân bản tay xuống site — để replication làm
5. Chạy `db/tests/Test-Scripts.ps1` — nó chặn mọi `.sql` không được khai báo.
6. Đối soát: tổng số dòng từng bảng ở 3 site **cộng lại phải bằng** số dòng ở
   `CENTRAL`. Lệch một dòng là vị từ phân mảnh sai.

**Xong giai đoạn này mới cho `central` nghỉ** — và `apps/api` chạy lại nguyên
vẹn trên bản phân tán.

### Giai đoạn 2 · Cài đặt vật lý (Phần F) · 11–17/10

VPN · SQL Server · Agent · **MS DTC (F4b)** · Linked Server hình sao từ
SRV-HCM · Publication trên MASTER · Subscription: **HCM trước** (cục bộ, dễ
kiểm chứng) rồi mới HN và ĐN qua VPN.

⚠️ **Chụp màn hình từng bước, ngay khi làm**, vào `docs/screenshots/<số>-<tên>/`.
Đây là phần nặng điểm nhất và bằng chứng không dựng lại được sau.

### Giai đoạn 3 · Năm yêu cầu bắt buộc (mục 3.7) · 18–24/10

| # | Việc | Trạng thái |
|---|---|---|
| 1 | Phân mảnh ngang — triển khai `db/site`, chia dữ liệu | script có, chưa chạy |
| 2 | Replication một chiều | sau Giai đoạn 2 |
| 3 | **`sp_ChuyenCoSoSinhVien` (2PC) + demo rollback khi tắt site đích** | ⚠️ **chưa có gì** |
| 4 | Tương tranh 100 luồng / 30 chỗ | ✅ **đã xong ở Phần 1** — chạy lại trên bản phân tán |
| 5 | `OPENQUERY` thống kê toàn hệ thống | thiết kế xong, chưa cài |

Kèm: trigger T1–T3 (**`NOT FOR REPLICATION`**), database role, demo phân quyền.

> ✅ **Xong giai đoạn này là ~75% điểm đã an toàn.** Mọi thứ sau đây là phần thêm.

### Giai đoạn 4 · Ứng dụng phân tán · 11–24/10 (song song)

- `SiteContext` + `RoutingDataSource` — **một giao dịch = một site**, tuyệt đối
- Ba port: `CrossSiteQuery`, `GlobalReport`, `CatalogHealth` — không port thứ tư
- Saga đăng ký liên cơ sở + idempotency + compensating transaction
- Outbox + worker + write-back điểm: **upsert mirror TRƯỚC, đánh dấu `SENT` SAU**

### Giai đoạn 5 · X-Ray + đo đạc · 25–28/10

Trace từng request · phòng điều khiển · so sánh ba chiến lược truy vấn ·
benchmark B1–B6 · thí nghiệm Saga vs 2PC (G5) · demo deadlock (G7) · sáu kịch
bản sự cố (G4).

### Giai đoạn 6 · Báo cáo, slide, tổng duyệt · 28–31/10

---

## 5. Lịch ba tuần — 09/10 → 31/10/2026

Toàn bộ bảy giai đoạn, không cắt. Lịch này **chặt**, nên nó được xếp quanh
bốn mốc kiểm soát chứ không phải quanh đầu việc: tới mốc mà chưa đạt thì đổi
phương án ngay hôm đó, đừng dồn sang tuần sau.

### Mốc kiểm soát

| Mốc | Hạn | Đạt nghĩa là | Trượt thì |
|---|---|---|---|
| **G0** | **10/10** | Spike A + B đều PASS | Rẽ phương án I2 **trong ngày** — named instance trên một máy |
| **G1** | **17/10** | Nhân bản chạy giữa Master và cả 3 site | Thu về 2 site; thiết kế viết cho N nên không phải sửa tài liệu |
| **G2** | **24/10** | ✅ Năm yêu cầu bắt buộc xong — **~75% điểm an toàn** | Dừng Giai đoạn 4–5, dồn toàn bộ người vào đây |
| **G3** | **28/10** | Báo cáo có bản đầy đủ, screenshot đủ | Cắt X-Ray khỏi báo cáo, giữ phần chạy được |
| **G4** | **31/10** | Tổng duyệt lần 2 xong | — |

### Tuần 1 · 09/10 – 17/10 — hạ tầng và tách dữ liệu

| Ngày | Hạ tầng 1 | Hạ tầng 2 | Tài liệu | Sơ đồ | Ứng dụng |
|---|---|---|---|---|---|
| 09–10/10 | **Spike A + B** ← G0 | Hoà giải schema: port `V4`–`V10` sang `db/site`/`db/master` | Dựng kho screenshot, chụp từ lệnh đầu tiên | Đối chiếu ERD với schema sau hoà giải | Dựng 2 named instance cục bộ |
| 11–13/10 | VPN · SQL Server · Agent · **MS DTC (F4b)** · Linked Server | **`db/migrate/` — script tách dữ liệu** `CENTRAL` → Master + 3 site | Viết F1–F5 theo đúng việc vừa làm | Lược đồ phân mảnh · ánh xạ · định vị | `SiteContext` + `RoutingDataSource` |
| 14–17/10 | Publication trên Master · Subscription **HCM trước**, rồi HN, ĐN ← G1 | Chạy `db/migrate`, đối soát số dòng từng site | Viết F6 | Sơ đồ đồng bộ hoá | Chuyển repository sang routing, **`central` nghỉ** |

⚠️ **Bốn người không chờ Hạ tầng 1.** Hạ tầng 2 làm được ngay từ ngày đầu, và
Ứng dụng dựng `SiteContext` trên hai named instance cục bộ trước khi VPN xong.

### Tuần 2 · 18/10 – 24/10 — năm yêu cầu bắt buộc

| Ngày | Việc | Yêu cầu | Ai |
|---|---|---|---|
| 18–19/10 | Triển khai `db/site` lên cả 3 cơ sở, dữ liệu đã tách vào đúng mảnh | **#1 phân mảnh** | Hạ tầng 2 |
| 18–20/10 | Trigger T1–T3 **`NOT FOR REPLICATION`** · database role · demo phân quyền qua SSMS | — | Hạ tầng 2 |
| 19–20/10 | Nhân bản chạy ổn định, đo độ trễ, demo `90-demo-nhan-ban.sql` | **#2 nhân bản** | Hạ tầng 1 |
| **20–22/10** | **`sp_ChuyenCoSoSinhVien` (2PC) + demo rollback khi tắt site đích** | **#3 giao dịch phân tán** | Hạ tầng 1 + 2 |
| 21–22/10 | `OPENQUERY` thống kê toàn hệ thống, so với four-part | **#5 truy vấn phân tán** | Hạ tầng 1 |
| 22/10 | Chạy lại 100 luồng / 30 chỗ **trên bản phân tán** | **#4 tương tranh** | Ứng dụng |
| 18–24/10 | 3 port · saga liên cơ sở · Outbox + write-back điểm | Giai đoạn 4 | Ứng dụng |
| 18–24/10 | Viết F7 và Phần G từ kết quả thật | — | Tài liệu |

> **#3 là mục rủi ro nhất của cả tuần.** Nó phụ thuộc MS DTC, và là mục duy
> nhất trong năm mục chưa có một dòng code nào. Xếp nó vào giữa tuần chứ không
> phải cuối tuần, để còn chỗ xoay.

**24/10 — G2.** Đến đây ~75% điểm đã an toàn. Mọi thứ sau là điểm cộng.

### Tuần 3 · 25/10 – 31/10 — X-Ray, đo đạc, báo cáo

| Ngày | Việc | Ai |
|---|---|---|
| 25–27/10 | X-Ray: trace từng request · phòng điều khiển · so sánh ba chiến lược | Ứng dụng |
| 25–28/10 | Benchmark B1–B6 · G4 sáu kịch bản sự cố · **G5 thí nghiệm Saga vs 2PC** · G7 deadlock | Hạ tầng 2 |
| 28/10 | ← **G3** Báo cáo bản đầy đủ | Tài liệu |
| 28–30/10 | Slide · kịch bản demo · hoàn thiện bốn lược đồ | Tài liệu + Sơ đồ |
| 30–31/10 | **Tổng duyệt 2 lần**, cả nhóm cùng online ← G4 | Cả nhóm |

### Hai điều lịch này phụ thuộc

1. **Buổi làm việc chung cố định** — chốt 3 buổi/tuần cả 5 người online. Dựng
   replication, đo benchmark và tổng duyệt đều cần nhiều máy bật cùng lúc
   (I2b). Không có lịch cố định thì tuần 1 trôi mất một nửa.
2. **Screenshot chụp ngay khi làm.** Tuần 3 không có chỗ để dựng lại bằng
   chứng của tuần 1.

---

## 6. Năm rủi ro lớn nhất

| # | Rủi ro | Giảm bằng |
|---|---|---|
| 1 | **Replication qua VPN** — chính tài liệu gọi đây là *"hạng mục rủi ro cao nhất"* | Spike A trước mọi thứ; phương án I2 sẵn sàng |
| 2 | **Hai schema lệch nhau** (§2) | Giai đoạn 1, trước khi chạm hạ tầng |
| 3 | **`@Transactional` không bao giờ trải hai DataSource.** Mọi repository Phần 1 viết với một DataSource — chuyển sang routing là sửa sâu, không phải thêm một lớp | Làm `SiteContext` sớm trong Giai đoạn 4, chuyển từng module |
| 4 | **Tài liệu bị để tới tuần cuối** — ~30% điểm nằm ở đó, và screenshot thì không dựng lại được sau | Một người bám tài liệu + kho screenshot **xuyên suốt**, không kiêm code. Đây là cách hỏng bài phổ biến nhất |
| 5 | **4 máy phải cùng online đúng buổi hẹn** | Chốt 2–3 buổi cố định mỗi tuần (I2b); mỗi máy online ít nhất một lần mỗi cửa sổ retention |

---

## 7. Phân vai 5 người — điều chỉnh sau Phần 1

Phân vai gốc ở **Phần H** của thiết kế vẫn dùng được, nhưng khối lượng đã đổi:
Phần A–C của báo cáo và bốn lược đồ **đã viết xong** trong tài liệu thiết kế.
Hai vai "Tài liệu" và "Sơ đồ" vì vậy chuyển từ *sáng tác* sang *đối chiếu với
hệ thống thật và ghép bằng chứng* — nhẹ hơn về chữ, nhưng chỉ làm được **sau**
khi hạ tầng chạy, nên không dồn được vào cuối.

| Vai | Phần 2 làm gì | Phụ thuộc |
|---|---|---|
| **Hạ tầng 1** | Spike A+B · VPN · SQL Server · Agent · **MS DTC** · Linked Server · Publication/Subscription (F1–F6) | chặn mọi vai khác — làm trước |
| **Hạ tầng 2** | Hoà giải schema (§2, Giai đoạn 1) · triển khai `db/site` · trigger `NOT FOR REPLICATION` · role · `sp_ChuyenCoSoSinhVien` · sinh dữ liệu lớn · benchmark | Giai đoạn 1 làm được **ngay**, không chờ hạ tầng |
| **Tài liệu** | Đối chiếu Phần A–C với hệ thống thật · viết Phần F–G từ kết quả chạy · **giữ kho screenshot** | bám sát Hạ tầng 1 từng buổi |
| **Sơ đồ** | Cập nhật ERD và bốn lược đồ theo schema sau hoà giải · sơ đồ định vị · kiến trúc | sau Giai đoạn 1 |
| **Ứng dụng** | `SiteContext` · `RoutingDataSource` · 3 port · saga · Outbox · X-Ray · kịch bản demo | Giai đoạn 4–5 |

⚠️ **Hai vai không được chờ hạ tầng**: Hạ tầng 2 làm Giai đoạn 1 ngay từ ngày
đầu, và Ứng dụng có thể dựng `SiteContext` trên hai named instance cục bộ
trước khi VPN xong. Để bốn người ngồi chờ một người dựng mạng là cách lãng phí
tuần đầu.

## 8. Việc còn treo, cần chốt với giảng viên

Lấy từ **I4** của thiết kế, chỉ giữ mục còn hiệu lực:

- [ ] ⭐ **Xác nhận năm yêu cầu bắt buộc** — nhất là #3, để chắc
      `sp_ChuyenCoSoSinhVien` là hiện thực được chấp nhận
- [ ] **Tài liệu hướng dẫn Replication của giảng viên** — quyết cách làm F6, xác nhận D9
- [ ] **Trọng số bonus của phần mềm** (X-Ray, `apps/web`) — quyết đáng đầu tư bao nhiêu cho Giai đoạn 5
- [ ] **Số liệu quy mô thật** thay giả định 🔶 ở A4/B2
- [ ] Chốt người giữ máy từng site. Tiêu chí cho `SRV-MASTER`: **ít bị mang đi lại
      nhất, ưu tiên máy để bàn + đĩa khá** — không phải máy yếu nhất
