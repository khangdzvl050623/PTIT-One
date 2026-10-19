# PTIT One — Kế hoạch Phần 2: CSDL phân tán

**Ngày chốt kế hoạch:** 09/10/2026.
**Trạng thái Phần 1:** đã xong và đã merge vào nhánh `dev`.

**Mục tiêu tài liệu:** trả lời *làm theo thứ tự nào*, khi Phần 1 đã xong trước
Phần 2. Tài liệu này **không thay** [tài liệu thiết kế](PTIT-One-Thiet-Ke.md) —
thiết kế vẫn là nguồn sự thật duy nhất về kiến trúc, lược đồ và quyết định.

> Lộ trình 8 tuần ở **Phần H** của thiết kế viết theo thứ tự **cũ** (hạ tầng
> trước, ứng dụng sau). Nhóm đã đảo thứ tự đó bằng quyết định **D20** ngày
> 24/09/2026. Vì vậy tuần 5–6 của Phần H **đã xong rồi**; tuần 1–4 mới là phần
> sắp làm.

**Tóm tắt một câu:** ba tuần, bảy giai đoạn `P0`–`P6`, xếp theo rủi ro chứ
không theo thứ tự tài liệu, kiểm soát bằng năm mốc `M0`–`M4`.

### Quy ước nhãn

Bản trước dùng `G0`–`G4` cho mốc kiểm soát, trùng với `G4`/`G5`/`G7` vốn là
**mục của tài liệu thiết kế**. Hai loại nhãn nay tách hẳn:

| Loại | Nhãn mới | Nhãn cũ | Ghi chú |
|---|---|---|---|
| Giai đoạn | `P0`–`P6` | "Giai đoạn 0–6" | ý nghĩa không đổi |
| Mốc kiểm soát | `M0`–`M4` | `G0`–`G4` | ý nghĩa không đổi |
| Mục tài liệu thiết kế | `B1`–`B6`, `F1`–`F7`, `G1`–`G7`, `C2`, `D9`… | giữ nguyên | **luôn là tham chiếu sang thiết kế**, không phải mốc |

---

## 1. Tóm tắt nhanh

1. **Đã xong:** toàn bộ Phần 1 — `apps/api` 85 endpoint và `apps/web` chạy
   trên một database `PTITONE_CENTRAL`, schema `V1`–`V10`.
2. **Đã xong luôn một yêu cầu bắt buộc:** yêu cầu **#4 (tương tranh)** — có
   sẵn ca kiểm thử 100 luồng tranh 30 chỗ.
3. **Đang chặn tiến độ:** chưa biết **nhân bản** và **giao dịch phân tán** có
   chạy được giữa các máy hay không. Hai thứ này quyết định yêu cầu #2 và #3.
4. **Phát hiện làm đổi thứ tự kế hoạch:** `db/site` và `db/central` đã lệch
   nhau theo cả hai chiều (§4). Triển khai `db/site` nguyên trạng thì
   `apps/api` gãy ngay.
5. **Thiếu một việc chưa ai nghĩ tới:** chưa có script tách dữ liệu từ
   `CENTRAL` sang Master và ba site.
6. **Topology 4 máy giữ nút + 1 máy không nút:** `SRV-MASTER` riêng cho
   Người 1; ba site do ba người còn lại tự chọn; máy thứ năm không giữ nút
   (§8.1). `db/config.ps1` **phải sửa** vì đang ở mặc định 3 máy (§3.1).
7. **Ba việc làm ngay:** chạy spike (`P0`) · hoà giải schema (`P1`) · dựng kho
   screenshot.
8. **Mốc gần nhất:** `M0` ngày **10/10** — spike A và B PASS, chạy **solo**
   trên máy Người 1 nên không chờ ai. Lưu ý `M0` PASS **chưa** chứng minh
   VPN thông (§6.1).
9. **Việc phải làm đúng thứ tự trong hai ngày đầu:** chốt Phần 1 **trước**, rồi
   mới bật Distributor và MS DTC — vì `CENTRAL` đang nằm trên đúng instance
   sẽ thành `SRV-MASTER` (§9, rủi ro 6).

---

## 2. Trạng thái hiện tại

### 2.1 Phần 1 — đã xong và đang chạy

| Hạng mục | Trạng thái | Bằng chứng / ghi chú |
|---|---|---|
| `apps/api`, `apps/web` | ✅ Hoàn thành | 85 endpoint, chạy trên một database `PTITONE_CENTRAL` |
| Schema `db/central` | ✅ Đã triển khai | `V1`–`V10`, đang dùng |
| Yêu cầu bắt buộc **#4** — tương tranh | ✅ Hoàn thành | `EnrollmentConcurrencyIntegrationTest` — 100 luồng tranh 30 chỗ |

### 2.2 Đã viết nhưng **chưa chạy lần nào**

| Hạng mục | Trạng thái | Bằng chứng / ghi chú |
|---|---|---|
| `db/master` | ⚠️ Viết xong, chưa chạy | Chỉ mới kiểm cú pháp |
| `db/site` | ⚠️ Viết xong, chưa chạy | Chỉ mới kiểm cú pháp |
| `db/replication` | ⚠️ Viết xong, chưa chạy | Chỉ mới kiểm cú pháp |

`db/tests/Test-Scripts.ps1` chỉ kiểm **cú pháp T-SQL** và tự ghi trong kết quả:
*"CHUA kiem chung SQL Server runtime"*. Nghĩa là chưa có gì chạy thật trên
SQL Server.

### 2.3 Chưa bắt đầu

| Hạng mục | Trạng thái | Bằng chứng / ghi chú |
|---|---|---|
| `SiteContext`, `RoutingDataSource` | ❌ Chưa có | Tầng định tuyến theo cơ sở ở phía ứng dụng |
| Ba port (`CrossSiteQuery`, `GlobalReport`, `CatalogHealth`) | ❌ Chưa có | — |
| Outbox worker | ❌ Chưa có | — |
| X-Ray | ❌ Chưa có | — |
| Script tách dữ liệu `CENTRAL` → Master + 3 site | ❌ Chưa có | `db/master/04-seed-danhmuc.sql` chỉ seed danh mục, không chuyển dữ liệu thật |

---

## 3. Quyết định đã chốt

> Bảng này **chỉ** chứa quyết định đã chốt. Câu hỏi chưa trả lời nằm ở §10.

| Mã | Quyết định | Ngày | Ảnh hưởng đến kế hoạch |
|---|---|---|---|
| **D12** | Triển khai **3 cơ sở** (HCM · HN · ĐN) + Master = **4 nút logic** | 09/10/2026 | `P2` phải dựng subscription cho 3 site |
| **D15** | **4 máy.** `SRV-MASTER` là nút **riêng**, do **Người 1** giữ. Ba site `HCM` · `HN` · `ĐN` nằm trên ba máy của **ba người còn lại, tự chọn nhau**. Máy người thứ năm **không giữ nút** | 09/10/2026 | Tuần 1 cần 4 máy online trong buổi dựng subscription. `M0` vẫn chạy được solo trên máy Người 1 (§3.1) |
| — | **Tách `PTITONE_CENTRAL`** thành `MASTER` + 3 site; `apps/api` chạy lại trên bản phân tán | 09/10/2026 | Sinh ra hai việc mới trong `P1`: hoà giải schema và script tách dữ liệu |
| — | **Phần 1 chốt hết ngày 10/10.** Từ 11/10 `central` **thôi làm database chạy ứng dụng** | 09/10/2026 | Không còn ràng buộc "giữ central để demo Phần 1" — xem §3.2 |

### 3.1 Topology 4 máy — cấu hình và cách kiểm chứng sớm

| Nút | Máy | Người giữ |
|---|---|---|
| `SRV-MASTER` → `PTITONE_MASTER` | máy trung tâm, **riêng** | **Người 1** (Hạ tầng 1) |
| `SRV-HCM` → `PTITONE_HCM` | một trong ba máy còn lại | ba người **tự chọn** |
| `SRV-HN` → `PTITONE_HN` | một trong ba máy còn lại | ba người **tự chọn** |
| `SRV-DN` → `PTITONE_DN` | một trong ba máy còn lại | ba người **tự chọn** |
| — | máy thứ năm | **không giữ nút** |

**Phải sửa `db/config.ps1` trước khi dựng:** file đang để mặc định phương án 3
máy, `MASTER` và `HCM` trỏ cùng một instance.

```
MASTER = 'SRV-MASTER'        ← đang là 'DESKTOP-85V5Q0S\PTITONE'
HCM    = '<máy người chọn HCM>'   ← đang là 'DESKTOP-85V5Q0S\PTITONE'
HN     = 'SRV-HN'
DN     = 'SRV-DN'
```

Và `Replication.SnapshotFolder` phải là **UNC share trên máy `SRV-MASTER`** —
máy chạy Distributor — chứ không phải đường dẫn local. Comment trong file gọi
đây là *"lỗi số một giết các nhóm"*.

**Kiểm chứng sớm mà không cần 4 máy.** `M0` ngày 10/10 **không** phụ thuộc vào
ai khác: Người 1 chạy spike A và B ngay trên máy mình, với hai database trên
cùng một instance làm publisher/subscriber giả. Đó là phương án `I2` dùng làm
**cách thử**, không phải topology thật.

⚠️ **Điều `M0` KHÔNG chứng minh được:** replication và DTC trong cùng một máy
gần như luôn thành công. `M0` PASS chỉ chứng minh **cấu hình SQL và MS DTC
đúng**, chưa chứng minh **VPN và tường lửa thông**. Phần đó do `M1` chịu, khi
máy site đầu tiên lên.

⚠️ **`SRV-HCM` nằm trên máy người khác — ai lấy site này cần biết trước.**
Theo thiết kế, `SRV-HCM` là **tâm của Linked Server hình sao**: nơi phát
`OPENQUERY` (yêu cầu #5) và nơi khởi tạo giao dịch phân tán (yêu cầu #3). Người
giữ `HCM` phải có mặt trong các buổi Người 1 cấu hình Linked Server và MS DTC,
hoặc mở sẵn quyền truy cập từ xa. Ba người tự chọn site, nhưng nên tính yếu tố
này vào.

### 3.2 `central` dừng ngày nào — và vẫn phải giữ bản dữ liệu

Phần 1 chốt hết **10/10**. Từ **11/10**, `PTITONE_CENTRAL` không còn là database
mà `apps/api` trỏ vào.

> **Giả định tôi đang dùng, cần anh xác nhận:** "không dùng central nữa" là
> *thôi chạy ứng dụng trên nó*, **không phải xoá nó**. `db/migrate` cần đọc
> `CENTRAL` làm nguồn để tách dữ liệu (§4.6), nên bản dữ liệu phải còn nguyên
> tới khi **đối soát số dòng khớp**. Chỉ sau đó mới backup rồi gỡ.

### 3.3 Vì sao tách `central` thay vì giữ song song

`PTITONE_CENTRAL` **không** tồn tại song song tới cuối đồ án. Nó được **tách**
thành `PTITONE_MASTER` và ba database cơ sở, rồi `apps/api` chạy lại nguyên vẹn
trên bản phân tán đó. Phần 1 là vốn đã có, không phải thứ để bỏ lại.

Lý do không giữ hai bản: duy trì hai schema cùng lúc **chính là cách chúng đã
trôi khỏi nhau** (§4). Giữ thêm một bản thứ hai là bảo đảm lặp lại đúng lỗi đó.

Máy dân dụng dựng **nhiều database trên một instance**, hoặc nhiều named
instance, là chuyện bình thường — không cần VPN để kiểm chứng.

---

## 4. Vấn đề schema cần xử lý trước

### 4.1 Chuyện gì đã xảy ra

`db/site` được viết **trước** khi làm Phần 1. Trong lúc làm Phần 1, nhóm thêm
nhiều bảng và cột vào `db/central` (các migration `V4`–`V10`) mà không thêm
tương ứng vào `db/site`. Ngược lại, `db/site` có sẵn bốn bảng phục vụ phân tán
mà `db/central` không có.

Kết quả: hai schema lệch nhau **theo cả hai chiều**. Phát hiện này **không nằm
trong tài liệu thiết kế**, vì thiết kế viết trước khi Phần 1 bắt đầu.

### 4.2 Bảng đối chiếu

| | `db/site` (Phần 2) | `db/central` (Phần 1, đang chạy) |
|---|---|---|
| `MaKichHoat`, `MaXacThuc`, `ThongBao` | ✗ | ✓ (`V4`–`V6`) |
| `AnhDaiDien`, `SoCCCD`, `EmailDaXacMinh`, `NguoiCap` | ✗ | ✓ (`V6`, `V8`–`V10`) |
| `OutboxSuKien`, `YeuCauHocLienCoSo`, `KetQuaXuLyYeuCau`, `BangDiemMirror` | ✓ | ✗ |

### 4.3 Hệ quả nếu triển khai `db/site` nguyên trạng

`apps/api` **gãy ngay**: mất kích hoạt tài khoản, xác minh email, thông báo,
lý lịch và ảnh đại diện. Vì vậy việc hoà giải phải xong **trước** khi chuyển
ứng dụng sang chạy phân tán.

### 4.4 Việc đã chốt trong hoà giải schema

1. Port các cột `V8`–`V10` (lý lịch, ảnh đại diện, `NguoiCap`) vào `db/site`.
2. Đưa bốn bảng phân tán (`OutboxSuKien`, `YeuCauHocLienCoSo`,
   `KetQuaXuLyYeuCau`, `BangDiemMirror`) vào tầm nhìn của `apps/api`.
3. Chạy `db/tests/Test-Scripts.ps1` — script này chặn mọi tệp `.sql` chưa được
   khai báo trong danh sách kiểm.

### 4.5 Việc **chưa quyết**, phải chốt theo Ownership Matrix (C2)

> ⚠️ **Cần xác nhận.** Bốn bảng Phần 1 mới thêm đứng ở Master hay ở từng cơ
> sở? Bản gốc có ghi *gợi ý* — `MaKichHoat`/`MaXacThuc` đi theo `TaiKhoan` nên
> per-site, `ThongBao` thuộc module `notification` nên per-site — nhưng cũng
> ghi rõ **"cần chốt bằng C2 chứ đừng quyết theo cảm giác"**. Đây là đề xuất,
> chưa phải quyết định.

### 4.6 Tách dữ liệu `CENTRAL` → Master + 3 site

Cần viết mới thư mục **`db/migrate/`**. Hiện chưa có script nào làm việc này.

**Ba điểm dễ sai:**

| Bảng | Đi theo | Ghi chú |
|---|---|---|
| `DangKyHocPhan` | **`LopHocPhan`** (cơ sở **mở lớp**) | **Không** đi theo `SinhVien`. Đây là lỗi đã từng mắc, ghi rõ trong `AGENTS.md` |
| `Diem` | `DangKyHocPhan` | Dẫn xuất **bậc 2** |
| `MonHoc`, `Khoa`, `HocKy`, `ChuongTrinhDaoTao` | **Master** | Không nhân bản tay xuống site — để replication làm |

**Điều kiện đối soát bắt buộc sau khi chạy:** tổng số dòng từng bảng ở ba site
**cộng lại phải bằng** số dòng ở `CENTRAL`. Lệch một dòng nghĩa là vị từ phân
mảnh sai.

---

## 5. Kế hoạch triển khai theo thứ tự phụ thuộc

> Xếp theo **rủi ro**, không theo thứ tự tài liệu: việc nào có thể làm hỏng cả
> đồ án thì làm trước, kể cả khi nó không phải việc dễ nhất.
> **Ngày tháng không lặp ở đây** — lịch chính thức duy nhất nằm ở §6.

### P0 · Spike — cổng chặn

| | |
|---|---|
| **Mục tiêu** | Biết sớm nhân bản và giao dịch phân tán có chạy được không |
| **Việc cần làm** | **A — Replication:** `INSERT` ở Master → ≤10 giây thấy ở site.<br>**B — MS DTC:** `BEGIN DISTRIBUTED TRANSACTION` qua Linked Server, **có ca ROLLBACK**. Chi tiết: [`db/spike/README.md`](../db/spike/README.md) |
| **Đầu vào / phụ thuộc** | **Chỉ máy Người 1** — chạy thử với hai database trên cùng một instance. Không chờ máy nào khác, không cần VPN |
| **Người phụ trách** | Hạ tầng 1 |
| **Điều kiện hoàn thành** | Cả A và B đều PASS → đạt mốc `M0` |
| **Rủi ro / rẽ nhánh** | Chạy **cùng máy trước**, qua VPN sau — để tách "sai cấu hình SQL" khỏi "mạng không thông". Hỏng thì rẽ theo bảng cuối `db/spike/README.md` |

**Không đi tiếp khi chưa có kết quả `P0`.**

### P1 · Hoà giải schema và tách dữ liệu

| | |
|---|---|
| **Mục tiêu** | `db/site` chạy được `apps/api`, và dữ liệu `CENTRAL` nằm đúng mảnh |
| **Việc cần làm** | Xem §4.4 (đã chốt), §4.5 (chưa quyết), §4.6 (tách dữ liệu) |
| **Đầu vào / phụ thuộc** | **Không cần hạ tầng** — chạy song song với `P0` ngay từ ngày đầu |
| **Người phụ trách** | Hạ tầng 2 |
| **Điều kiện hoàn thành** | `Test-Scripts.ps1` xanh **và** đối soát số dòng khớp (§4.6) |
| **Rủi ro / rẽ nhánh** | Ownership bốn bảng chưa chốt (§4.5) có thể làm phải sửa lại schema |

### P2 · Cài đặt vật lý (Phần F của thiết kế)

| | |
|---|---|
| **Mục tiêu** | Bốn nút nói chuyện được với nhau, nhân bản chạy |
| **Việc cần làm** | VPN · SQL Server · SQL Server Agent · **MS DTC (F4b)** · Linked Server hình sao từ SRV-HCM · Publication trên Master · Subscription theo thứ tự **HCM trước** (cục bộ, dễ kiểm chứng), rồi HN và ĐN qua VPN |
| **Đầu vào / phụ thuộc** | `P0` PASS |
| **Người phụ trách** | Hạ tầng 1 |
| **Điều kiện hoàn thành** | Nhân bản chạy giữa Master và cả 3 site → mốc `M1` |
| **Rủi ro / rẽ nhánh** | Đây là phần **nặng điểm nhất**. Trượt `M1` → thu về 2 site; thiết kế viết cho N cơ sở nên không phải sửa tài liệu |

⚠️ **Chụp màn hình từng bước, ngay khi làm**, lưu vào
`docs/screenshots/<số>-<tên bước>/`. Bằng chứng này **không dựng lại được sau**.

### P3 · Năm yêu cầu bắt buộc (mục 3.7 của đề bài)

| | |
|---|---|
| **Mục tiêu** | Hoàn tất phần chấm điểm chính |
| **Việc cần làm** | Năm yêu cầu — xem bảng đầy đủ ở §7. Kèm trigger T1–T3 (**`NOT FOR REPLICATION`**), database role, demo phân quyền qua SSMS |
| **Đầu vào / phụ thuộc** | `P1` (dữ liệu đã tách) + `P2` (hạ tầng chạy) |
| **Người phụ trách** | Hạ tầng 1 + Hạ tầng 2; yêu cầu #4 do Ứng dụng chạy lại |
| **Điều kiện hoàn thành** | Cả năm yêu cầu có bằng chứng → mốc `M2` |
| **Rủi ro / rẽ nhánh** | Yêu cầu **#3** phụ thuộc MS DTC và **chưa có một dòng code nào** — rủi ro cao nhất của giai đoạn |

> ✅ **Xong `P3` là ~75% điểm đã an toàn.** Mọi thứ sau đây là phần thêm.

### P4 · Ứng dụng phân tán

| | |
|---|---|
| **Mục tiêu** | `apps/api` chạy trên bản phân tán thay vì `CENTRAL` |
| **Việc cần làm** | `SiteContext` + `RoutingDataSource` — **một giao dịch = một site**, tuyệt đối.<br>Ba port: `CrossSiteQuery`, `GlobalReport`, `CatalogHealth` — **không có port thứ tư**.<br>Saga đăng ký liên cơ sở + idempotency + compensating transaction.<br>Outbox + worker + write-back điểm: **upsert mirror TRƯỚC, đánh dấu `SENT` SAU** |
| **Đầu vào / phụ thuộc** | `P1` cho schema; dựng `SiteContext` được trên **hai database cục bộ** trước khi VPN xong |
| **Người phụ trách** | Ứng dụng |
| **Điều kiện hoàn thành** | `apps/api` chạy trên bản phân tán, `central` tắt được |
| **Rủi ro / rẽ nhánh** | `@Transactional` không bao giờ trải hai DataSource — xem rủi ro 3 ở §9 |

### P5 · X-Ray và đo đạc

| | |
|---|---|
| **Mục tiêu** | Làm các cơ chế phân tán **nhìn thấy được** và có số đo |
| **Việc cần làm** | Trace từng request · phòng điều khiển · so sánh ba chiến lược truy vấn · benchmark `B1`–`B6` · thí nghiệm Saga vs 2PC (`G5`) · demo deadlock (`G7`) · sáu kịch bản sự cố (`G4`) |
| **Đầu vào / phụ thuộc** | `P3` và `P4` |
| **Người phụ trách** | Ứng dụng (X-Ray) · Hạ tầng 2 (benchmark, kịch bản sự cố) |
| **Điều kiện hoàn thành** | Có số đo cho Phần G của báo cáo |
| **Rủi ro / rẽ nhánh** | Trượt `M3` → cắt X-Ray khỏi báo cáo, giữ phần chạy được |

### P6 · Báo cáo, slide, tổng duyệt

| | |
|---|---|
| **Mục tiêu** | Nộp được |
| **Việc cần làm** | Báo cáo · slide · kịch bản demo · hoàn thiện bốn lược đồ · tổng duyệt 2 lần |
| **Đầu vào / phụ thuộc** | Kho screenshot đã đầy đủ từ `P2` trở đi |
| **Người phụ trách** | Tài liệu + Sơ đồ; tổng duyệt cả nhóm |
| **Điều kiện hoàn thành** | Mốc `M4` |
| **Rủi ro / rẽ nhánh** | Tuần cuối **không có chỗ** để dựng lại bằng chứng của tuần 1 |

---

## 6. Lịch làm việc 09/10 – 31/10/2026

> **Đây là lịch chính thức duy nhất.** Các mục khác dẫn chiếu về đây, không lặp
> lại ngày tháng.

### 6.1 Năm mốc kiểm soát

Lịch xếp quanh mốc chứ không quanh đầu việc: tới mốc mà chưa đạt thì **đổi
phương án ngay hôm đó**, không dồn sang tuần sau.

| Mốc | Hạn | Đạt nghĩa là | Trượt thì |
|---|---|---|---|
| **M0** | **10/10** | Spike A và B đều PASS, chạy thử **trên máy Người 1** | Trượt ở đây là lỗi cấu hình SQL hoặc MS DTC, không phải lỗi mạng — xem bảng cuối `db/spike/README.md` |
| **M1** | **17/10** | Nhân bản chạy giữa `SRV-MASTER` và **cả 3 site qua VPN** | Thu về 2 site; thiết kế viết cho N cơ sở nên không phải sửa tài liệu |
| **M2** | **24/10** | Năm yêu cầu bắt buộc xong — **~75% điểm an toàn** | Dừng `P4`–`P5`, dồn toàn bộ người vào đây |
| **M3** | **28/10** | Báo cáo có bản đầy đủ, screenshot đủ | Cắt X-Ray khỏi báo cáo, giữ phần chạy được |
| **M4** | **31/10** | Tổng duyệt lần 2 xong | — |

⚠️ **`M0` không chứng minh được VPN.** Spike chạy trong cùng một máy thì
replication và DTC gần như luôn thành công. `M0` PASS nghĩa là **cấu hình SQL
đúng**; việc **mạng có thông hay không** chỉ biết ở `M1`, khi máy `HN` lên.
Đừng coi `M0` PASS là đã hết rủi ro hạ tầng.

### 6.2 Tuần 1 · 09/10 – 17/10 — hạ tầng và tách dữ liệu

Năm cột chạy **song song**. Chỉ cột "Hạ tầng 1" là đường găng.

| Ngày | Hạ tầng 1 | Hạ tầng 2 | Tài liệu | Sơ đồ | Ứng dụng |
|---|---|---|---|---|---|
| 09–10/10 | **Spike A + B trên máy mình** → **M0** | Hoà giải schema: port `V4`–`V10` sang `db/site` / `db/master` | Dựng kho screenshot, chụp từ lệnh đầu tiên | Đối chiếu ERD với schema sau hoà giải | Dựng 2 database cục bộ (`MASTER` + `HCM`) |
| 11–13/10 | VPN · SQL Server · Agent · **MS DTC (F4b)** · Linked Server | Viết **`db/migrate/`** — script tách dữ liệu `CENTRAL` → Master + 3 site · cài SQL Server trên máy `HN` | Viết `F1`–`F5` theo đúng việc vừa làm | Lược đồ phân mảnh · ánh xạ · định vị | `SiteContext` + `RoutingDataSource` |
| 14–17/10 | Publication trên `SRV-MASTER` · Subscription **`HCM` trước**, rồi `HN`, `ĐN` — cả ba qua VPN → **M1** | **Chạy thử** `db/migrate` trên máy mình, đối soát số dòng | Viết `F6` | Sơ đồ đồng bộ hoá | Chuyển repository sang routing, từng module |

⚠️ **Bốn vai không chờ Hạ tầng 1.** Hạ tầng 2 bắt đầu `P1` ngay ngày đầu;
Ứng dụng dựng `SiteContext` trên hai database cục bộ trước khi VPN xong.

> **Đã làm rõ — `db/migrate` chạy hai lần.** 14–17/10 là **chạy thử trên máy
> Hạ tầng 2** (có `CENTRAL` khôi phục từ backup làm nguồn, ba database cơ sở
> cục bộ làm đích) để kiểm vị từ phân mảnh và câu đối soát. 18–19/10 mới là
> **chạy thật** lên ba site sau khi `db/site` đã triển khai. Đây là cách đọc
> tôi đang áp dụng cho §6.3 và §8.4 — nếu nhóm muốn chạy một lần thì phải dời
> toàn bộ việc này sang tuần 2.

### 6.3 Tuần 2 · 18/10 – 24/10 — năm yêu cầu bắt buộc

| Ngày | Việc | Yêu cầu | Phụ trách |
|---|---|---|---|
| 18–19/10 | Triển khai `db/site` lên cả 3 cơ sở, rồi **chạy thật** `db/migrate` để dữ liệu vào đúng mảnh | **#1 phân mảnh** | Hạ tầng 2 |
| 18–20/10 | Trigger T1–T3 **`NOT FOR REPLICATION`** · database role · demo phân quyền qua SSMS | — | Hạ tầng 2 |
| 19–20/10 | Nhân bản chạy ổn định, đo độ trễ, demo `db/tests/90-demo-nhan-ban.sql` | **#2 nhân bản** | Hạ tầng 1 |
| **20–22/10** | **`sp_ChuyenCoSoSinhVien` (2PC) + demo rollback khi tắt site đích** | **#3 giao dịch phân tán** | Hạ tầng 1 + 2 |
| 21–22/10 | `OPENQUERY` thống kê toàn hệ thống, so với truy vấn four-part | **#5 truy vấn phân tán** | Hạ tầng 1 |
| 22/10 | Chạy lại 100 luồng / 30 chỗ **trên bản phân tán** | **#4 tương tranh** | Ứng dụng |
| 18–24/10 | Ba port · saga liên cơ sở · Outbox + write-back điểm (`P4`) | — | Ứng dụng |
| 18–24/10 | Viết `F7` và Phần G từ kết quả chạy thật | — | Tài liệu |

> **#3 là mục rủi ro nhất tuần này.** Nó phụ thuộc MS DTC và là mục duy nhất
> trong năm mục chưa có một dòng code nào. Xếp vào **giữa tuần** chứ không phải
> cuối tuần, để còn chỗ xoay.

> ⚠️ **Cần xác nhận — yêu cầu #2 hoàn thành ngày nào.** Mốc `M1` (17/10) đòi
> "nhân bản chạy giữa Master và cả 3 site", nhưng dòng 19–20/10 ở bảng trên
> vẫn ghi "nhân bản chạy ổn định… #2".
> **Hai chỗ liên quan:** §6.1 mốc `M1` và §6.3 dòng 19–20/10.
> **Câu hỏi:** `M1` là "chạy được lần đầu" còn 19–20/10 là "chạy ổn định và có
> số đo", hay hai mốc này trùng việc?

**24/10 → mốc `M2`.** Đến đây ~75% điểm đã an toàn.

### 6.4 Tuần 3 · 25/10 – 31/10 — X-Ray, đo đạc, báo cáo

| Ngày | Việc | Phụ trách |
|---|---|---|
| 25–27/10 | X-Ray: trace từng request · phòng điều khiển · so sánh ba chiến lược | Ứng dụng |
| 25–28/10 | Benchmark `B1`–`B6` · sáu kịch bản sự cố (`G4`) · thí nghiệm Saga vs 2PC (`G5`) · demo deadlock (`G7`) | Hạ tầng 2 |
| 28/10 | Báo cáo bản đầy đủ → **M3** | Tài liệu |
| 28–30/10 | Slide · kịch bản demo · hoàn thiện bốn lược đồ | Tài liệu + Sơ đồ |
| 30–31/10 | **Tổng duyệt 2 lần**, cả nhóm cùng online → **M4** | Cả nhóm |

### 6.5 Hai điều kiện lịch này phụ thuộc

1. **Buổi làm việc chung cố định** — cần chốt 3 buổi mỗi tuần, cả 5 người
   online. Dựng replication, đo benchmark và tổng duyệt đều cần nhiều máy bật
   cùng lúc (mục `I2b` của thiết kế). Không có lịch cố định thì tuần 1 trôi
   mất một nửa.
   **Chỉ `M0` là không cần buổi chung:** spike chạy thử solo trên máy Người 1.
   Từ 11/10 trỏ đi thì **cần 4 máy giữ nút cùng online** để dựng VPN và
   subscription — đây là ràng buộc lịch nặng nhất của tuần 1.
2. **Screenshot chụp ngay khi làm.** Tuần 3 không có chỗ để dựng lại bằng
   chứng của tuần 1.

---

## 7. Năm yêu cầu bắt buộc

| # | Yêu cầu | Trạng thái hiện tại | Việc còn lại | Hạn | Phụ trách |
|---|---|---|---|---|---|
| 1 | **Phân mảnh ngang** | Script có, chưa chạy | Triển khai `db/site` lên 3 cơ sở, dữ liệu đã tách vào đúng mảnh | 19/10 | Hạ tầng 2 |
| 2 | **Nhân bản một chiều** | Script có, chưa chạy | Dựng Publication + Subscription, đo độ trễ, demo | 20/10 | Hạ tầng 1 |
| 3 | **Giao dịch phân tán (2PC)** | ⚠️ **Chưa có gì** | Viết `sp_ChuyenCoSoSinhVien` + demo rollback khi tắt site đích | 22/10 | Hạ tầng 1 + 2 |
| 4 | **Tình huống tương tranh** | ✅ **Đã xong ở Phần 1** | **Chạy lại trên bản phân tán** — 100 luồng / 30 chỗ | 22/10 | Ứng dụng |
| 5 | **Truy vấn phân tán** | Thiết kế xong, chưa cài | `OPENQUERY` thống kê toàn hệ thống, so với truy vấn four-part | 22/10 | Hạ tầng 1 |

Cả năm phải xong trước mốc **`M2` ngày 24/10**.

---

## 8. Phân máy và phân người

Phân vai gốc ở **Phần H** của thiết kế vẫn dùng được, nhưng khối lượng đã đổi:
Phần A–C của báo cáo và bốn lược đồ **đã viết xong** trong tài liệu thiết kế.
Hai vai "Tài liệu" và "Sơ đồ" vì vậy chuyển từ *sáng tác* sang *đối chiếu với
hệ thống thật và ghép bằng chứng*. Nhẹ hơn về chữ, nhưng chỉ làm được **sau**
khi hạ tầng chạy, nên không dồn được vào tuần cuối.

> **Hai cách đọc lịch, không trùng nhau.** §6 là lịch **theo ngày** — ngày đó
> cả nhóm đang làm gì. §8.3–§8.7 là cam kết **theo tuần** cho từng người —
> cuối tuần người đó phải đưa ra được cái gì. Mục này **không ghi ngày lẻ**;
> ngày lẻ chỉ có ở §6.

### 8.1 Bảng phân công — một dòng mỗi người

> In bảng này ra, hoặc dán vào nhóm chat. Mỗi người chỉ cần đọc **dòng của
> mình**, rồi mở mục riêng ở §8.3–§8.7 để biết cuối từng tuần phải đưa ra cái gì.

| Người | Vai | Máy phụ trách | Nút / database | Việc chính | Bàn giao |
|---|---|---|---|---|---|
| ☐ *điền tên* — **Người 1** | **Hạ tầng 1** | **Máy trung tâm, riêng** | `SRV-MASTER` → `PTITONE_MASTER` | Spike A+B · VPN · SQL Server Agent · MS DTC · Linked Server · Publication/Subscription · `sp_ChuyenCoSoSinhVien` (2PC) · `OPENQUERY` | `M0` **10/10** · `M1` **17/10** · yêu cầu **#2, #3, #5** |
| ☐ *điền tên* | **Hạ tầng 2** | Một trong ba máy site — **tự chọn** | ☐ `HCM` / `HN` / `ĐN` | Hoà giải schema · `db/migrate` · triển khai `db/site` · trigger `NOT FOR REPLICATION` · database role · sinh dữ liệu lớn · benchmark | Schema hoà giải (hết tuần 1) · bảng đối soát dữ liệu · yêu cầu **#1** · số đo Phần G |
| ☐ *điền tên* | **Ứng dụng** | Một trong ba máy site — **tự chọn** | ☐ `HCM` / `HN` / `ĐN` | `SiteContext` + `RoutingDataSource` · ba port · saga liên cơ sở · Outbox + write-back điểm · X-Ray · kịch bản demo | `apps/api` chạy trên 3 site (hết tuần 2) · yêu cầu **#4** chạy lại · X-Ray |
| ☐ *điền tên* | **Tài liệu** | Một trong ba máy site — **tự chọn** | ☐ `HCM` / `HN` / `ĐN` | Kho screenshot (**giữ xuyên suốt**) · `F1`–`F7` · Phần G · đối chiếu Phần A–C · báo cáo · slide | Báo cáo bản đầy đủ **28/10** (`M3`) |
| ☐ *điền tên* | **Sơ đồ** | **Máy thứ năm — không giữ nút** | — | ERD · lược đồ phân mảnh · ánh xạ phân mảnh · định vị · đồng bộ hoá · kiến trúc triển khai | Bốn lược đồ **khớp hệ thống thật** |

**Ba site do ba người tự chọn.** Chốt xong thì tick vào cột `Nút / database` ở
trên và cập nhật §8.2. Một lưu ý khi chọn: **`HCM` là site nặng việc nhất** —
tâm Linked Server hình sao, nơi phát `OPENQUERY` và nơi khởi tạo giao dịch phân
tán, nên người giữ `HCM` phải có mặt trong các buổi Người 1 cấu hình (§3.1).

**Ai chờ ai — một dòng:** Hạ tầng 1 **không chờ ai**, là đường găng · Hạ tầng 2
chờ Hạ tầng 1 **chỉ từ tuần 2** · Ứng dụng chờ Hạ tầng 2 (schema) rồi Hạ tầng 1
(3 site thật) · Tài liệu **bám Hạ tầng 1 từng buổi** · Sơ đồ chờ Hạ tầng 2
(schema cuối). Hạn chót từng việc bàn giao: **§8.8**.

⚠️ **Tuần 1 không ai ngồi chờ hạ tầng.** Hạ tầng 2 làm `P1` ngay ngày đầu;
Ứng dụng dựng `SiteContext` trên hai database cục bộ; Sơ đồ đối chiếu ERD;
Tài liệu dựng kho screenshot. Để bốn người chờ một người dựng mạng là cách
lãng phí tuần đầu.

### 8.2 Bốn nút — ai giữ máy nào

| Nút | Máy | Người giữ | Vai trò trong hệ thống | Nút này tắt thì mất gì |
|---|---|---|---|---|
| `SRV-MASTER` → `PTITONE_MASTER` | máy trung tâm, **riêng** | **Người 1** (Hạ tầng 1) | Publisher + Distributor. Giữ danh mục: `MonHoc`, `Khoa`, `HocKy`, `ChuongTrinhDaoTao` | Nhân bản dừng, không sửa được danh mục. Site vẫn đọc bản sao cũ nên demo **đọc** không chết |
| `SRV-HCM` → `PTITONE_HCM` | máy site | ☐ **tự chọn** | Tâm của Linked Server hình sao · nơi phát `OPENQUERY` · nơi khởi tạo 2PC · site đông sinh viên nhất | Mất truy vấn phân tán (#5) **và** 2PC (#3). Nút quan trọng nhất sau Master |
| `SRV-HN` → `PTITONE_HN` | máy site | ☐ **tự chọn** | Site thứ hai · **site đích** trong demo chuyển cơ sở | Tắt nút này là *một bước của demo rollback 2PC*, không phải sự cố |
| `SRV-DN` → `PTITONE_DN` | máy site | ☐ **tự chọn** | Site thứ ba · chứng minh thiết kế chạy với N > 2 cơ sở | Thu về 2 site; `M1` vẫn đạt ở mức tối thiểu |
| — | máy thứ năm | vai **Sơ đồ** | Không giữ nút. Chỉ cần quyền đọc để đối chiếu lược đồ với schema thật | — |

**Tên người:** điền vào cột `Người` ở §8.1. Các mục còn lại gọi theo
nhãn vai, để đổi người thì chỉ phải sửa một chỗ.

### 8.3 Hạ tầng 1 (Người 1) — giữ `SRV-MASTER`

| Tuần | Việc | **Cuối tuần phải xong** |
|---|---|---|
| **Tuần 1**<br>09–17/10 | Sửa `db/config.ps1` sang topology 4 máy (§3.1) · spike A + B ngay trên máy mình · cài VPN · bật MS DTC ở Master và ở từng site · UNC share cho snapshot · publication trên `SRV-MASTER` · subscription `HCM` trước, rồi `HN`, `ĐN` · Linked Server `SRV_HN`, `SRV_DN` | `M0` PASS **10/10** (solo trên máy mình) · nhân bản từ `SRV-MASTER` xuống **cả ba site qua VPN** · `M1` 17/10 |
| **Tuần 2**<br>18–24/10 | `sp_ChuyenCoSoSinhVien` (2PC) + demo rollback bằng cách **tắt nút `HN`** · `OPENQUERY` thống kê toàn hệ thống, so với truy vấn four-part · đo độ trễ nhân bản | Yêu cầu **#2**, **#3**, **#5** có bằng chứng chạy thật · `M2` 24/10 |
| **Tuần 3**<br>25–31/10 | Hỗ trợ 6 kịch bản sự cố (`G4`) · tổng duyệt | Hệ thống chạy được suốt 2 buổi tổng duyệt · `M4` 31/10 |

**Ai chờ mình:** Hạ tầng 2 (VPN + Linked Server để triển khai lên ba site) ·
Ứng dụng (3 site thật để chạy `RoutingDataSource`) · Tài liệu (chụp `F1`–`F6`).

### 8.4 Hạ tầng 2 — giữ một site (tự chọn)

| Tuần | Việc | **Cuối tuần phải xong** |
|---|---|---|
| **Tuần 1**<br>09–17/10 | Hoà giải schema: port `V4`–`V10` vào `db/site` và `db/master` · viết `db/migrate/` · **chạy thử** `db/migrate` trên máy mình · cài SQL Server + Agent trên máy site của mình, mở firewall | `Test-Scripts.ps1` xanh · `db/migrate` chạy thử xong, **tổng số dòng 3 mảnh = `CENTRAL`** · máy site của mình sẵn sàng nhận subscription |
| **Tuần 2**<br>18–24/10 | Triển khai `db/site` lên cả ba site · chạy `db/migrate` **thật** · trigger T1–T3 với `NOT FOR REPLICATION` · database role + demo `DENY` qua SSMS · cùng Hạ tầng 1 viết `sp_ChuyenCoSoSinhVien` | Yêu cầu **#1** có bằng chứng · bảng đối soát số dòng từng bảng từng site · trigger và `DENY` chạy ở Subscriber **mà Distribution Agent không chết** |
| **Tuần 3**<br>25–31/10 | Sinh dữ liệu lớn · benchmark `B1`–`B6` · 6 kịch bản sự cố (`G4`) · thí nghiệm Saga vs 2PC (`G5`) · demo deadlock (`G7`) | Số đo Phần G ở **dạng bảng dán được thẳng vào báo cáo**, không phải log thô |

**Ai chờ mình:** Ứng dụng (schema sau hoà giải) · Sơ đồ (ERD cuối) · Hạ tầng 1
(`sp_ChuyenCoSoSinhVien`).

### 8.5 Ứng dụng — giữ một site (tự chọn)

| Tuần | Việc | **Cuối tuần phải xong** |
|---|---|---|
| **Tuần 1**<br>09–17/10 | Dựng `PTITONE_MASTER` + `PTITONE_HCM` **cục bộ trên máy mình** để code không chờ VPN · `SiteContext` · `RoutingDataSource` · chuyển repository sang routing **theo từng module** | `SiteContext` + `RoutingDataSource` chạy với 2 database cục bộ · **ít nhất 1 module** đã chuyển xong và test xanh |
| **Tuần 2**<br>18–24/10 | Ba port `CrossSiteQuery` / `GlobalReport` / `CatalogHealth` · saga đăng ký liên cơ sở + idempotency + bù trừ · Outbox + worker + write-back điểm (**upsert mirror trước, `SENT` sau**) · chạy lại 100 luồng / 30 chỗ trên bản phân tán | Yêu cầu **#4** chạy lại **xanh trên bản phân tán** · `apps/api` nối được 3 site thật, `@Transactional` không nơi nào trải 2 site |
| **Tuần 3**<br>25–31/10 | X-Ray: trace từng request, phòng điều khiển, so sánh 3 chiến lược truy vấn · kịch bản demo trên giao diện | X-Ray chạy được trong buổi tổng duyệt. Trượt `M3` → cắt X-Ray khỏi báo cáo, giữ phần chạy được |

**Ai chờ mình:** Tài liệu (chụp demo trên giao diện) · cả nhóm (buổi tổng duyệt).

### 8.6 Tài liệu — giữ một site (tự chọn)

| Tuần | Việc | **Cuối tuần phải xong** |
|---|---|---|
| **Tuần 1**<br>09–17/10 | Dựng `docs/screenshots/<số>-<tên bước>/` · **ngồi cùng buổi với Hạ tầng 1**, chụp từng bước ngay khi làm · viết `F1`–`F6` theo đúng việc vừa thấy | Kho screenshot có mục cho **mọi bước đã làm trong tuần** · `F1`–`F6` có bản nháp đầy đủ |
| **Tuần 2**<br>18–24/10 | Viết `F7` (thử các giao tác) · khung Phần G từ kết quả chạy thật · chụp bằng chứng cho từng yêu cầu bắt buộc | **Mỗi yêu cầu trong 5 yêu cầu** có ít nhất 1 screenshot + 1 đoạn giải thích |
| **Tuần 3**<br>25–31/10 | Ghép báo cáo đầy đủ · slide · đối chiếu Phần A–C với hệ thống thật | `M3` **28/10**: báo cáo bản đầy đủ, không còn chỗ trống chờ số đo |

**Ai chờ mình:** cả nhóm — không ai dựng lại được screenshot của tuần 1 vào
tuần 3.

### 8.7 Sơ đồ — không giữ nút

| Tuần | Việc | **Cuối tuần phải xong** |
|---|---|---|
| **Tuần 1**<br>09–17/10 | Đối chiếu ERD hiện tại với schema sau hoà giải · lược đồ phân mảnh · lược đồ ánh xạ phân mảnh · lược đồ định vị | **3 lược đồ** có bản nháp **theo schema đã hoà giải**, không theo schema cũ |
| **Tuần 2**<br>18–24/10 | Sơ đồ đồng bộ hoá (luồng nhân bản + Outbox) · sơ đồ kiến trúc triển khai (4 máy giữ nút + 1 máy không nút) · cập nhật theo thay đổi thật | **Bốn lược đồ** đủ bản dùng được |
| **Tuần 3**<br>25–31/10 | Bản cuối theo hệ thống đang chạy · xuất hình cho slide | Lược đồ trong báo cáo **khớp với hệ thống thật**, kể cả việc ai giữ site nào |

### 8.8 Bàn giao — ai chờ ai, hạn chót

| Người giao | Giao cái gì | Cho ai | Hạn |
|---|---|---|---|
| Hạ tầng 1 | `M0` PASS — biết DTC và nhân bản chạy được | cả nhóm | **10/10** |
| Hạ tầng 2 | schema `db/site` đã hoà giải | Ứng dụng · Sơ đồ | hết tuần 1 |
| Hạ tầng 1 | VPN thông + Linked Server | Hạ tầng 2 | hết tuần 1 |
| Hạ tầng 1 | nhân bản chạy 3 site (`M1`) | Tài liệu (chụp `F6`) · Hạ tầng 2 | **17/10** |
| Hạ tầng 2 | `db/migrate` chạy thật + bảng đối soát | Ứng dụng (dữ liệu thật) · Tài liệu | đầu tuần 2 |
| Ứng dụng | `apps/api` nối 3 site | Tài liệu (chụp demo) · cả nhóm | hết tuần 2 |
| Hạ tầng 2 | số đo benchmark dạng bảng | Tài liệu | **27/10** |
| Tài liệu | kho screenshot đầy đủ + báo cáo | cả nhóm | **28/10** |

---

## 9. Rủi ro và phương án giảm thiểu

| # | Rủi ro | Vì sao đáng lo | Cách giảm thiểu | Dấu hiệu cần chuyển phương án |
|---|---|---|---|---|
| 1 | **Replication qua VPN** | Chính tài liệu thiết kế gọi đây là *"hạng mục rủi ro cao nhất"* | Spike A trước mọi thứ, **trong cùng một máy** để tách lỗi SQL khỏi lỗi mạng | `M1` trượt → thu về 2 site. Trượt nữa thì rẽ phương án `I2`: gom các nút về ít máy hơn, chấp nhận mất tính “nhiều máy thật” |
| 2 | **Hai schema lệch nhau** (§4) | Triển khai `db/site` nguyên trạng thì `apps/api` gãy ngay | Làm `P1` trước khi chạm hạ tầng | Đối soát số dòng (§4.6) không khớp |
| 3 | **`@Transactional` không bao giờ trải hai DataSource** | Mọi repository Phần 1 viết với **một** DataSource. Chuyển sang routing là sửa sâu, không phải thêm một lớp | Làm `SiteContext` sớm trong `P4`, chuyển **từng module** | Hết tuần 2 mà chưa chuyển xong module đầu tiên |
| 4 | **Tài liệu bị để tới tuần cuối** | ~30% điểm nằm ở đó, và screenshot **không dựng lại được sau** | Một người bám tài liệu + kho screenshot **xuyên suốt**, không kiêm code | Hết `P2` mà kho screenshot còn trống |
| 5 | **Bốn máy giữ nút phải cùng online đúng buổi hẹn** | Dựng replication và đo benchmark đều cần nhiều máy bật cùng lúc | `M0` chỉ cần **1 máy**, nên tuần 1 khởi động được ngay. Từ 11/10 chốt 2–3 buổi cố định mỗi tuần (`I2b`) cho **4 máy giữ nút**; mỗi máy online ít nhất một lần mỗi cửa sổ retention | Một buổi dựng hạ tầng phải hoãn vì thiếu máy |
| 6 | **`CENTRAL` nằm trên instance sẽ thành `SRV-MASTER`** | `db/central/config.local.psd1` trỏ `localhost\PTITONE` trên máy Người 1 — cũng là máy sẽ chạy Publisher + Distributor. Bật Distributor, cấu hình MS DTC và khởi động lại SQL Server Agent trên instance đó sẽ **ngắt** mọi thứ đang chạy trên `CENTRAL` | Chốt Phần 1 xong **trước** rồi mới chạm vào instance; sau 10/10 thì `CENTRAL` chỉ còn là nguồn đọc cho `db/migrate` (§3.2) | Phải khởi động lại instance trong lúc còn người đang demo Phần 1 |

---

## 10. Việc cần xác nhận

> Các mục ở đây **chưa phải quyết định**. Không dùng chúng như đã chốt.

### 10.1 Cần giảng viên trả lời

Lấy từ mục `I4` của thiết kế, chỉ giữ mục còn hiệu lực:

- [ ] ⭐ **Xác nhận năm yêu cầu bắt buộc** — nhất là #3, để chắc
      `sp_ChuyenCoSoSinhVien` là hiện thực được chấp nhận
- [ ] **Tài liệu hướng dẫn Replication của giảng viên** — quyết cách làm `F6`,
      xác nhận `D9`
- [ ] **Trọng số bonus của phần mềm** (X-Ray, `apps/web`) — quyết đáng đầu tư
      bao nhiêu cho `P5`
- [ ] **Số liệu quy mô thật** thay cho giả định ở `A4`/`B2`

### 10.2 Cần nhóm chốt

**Đã chốt ngày 09/10 — không còn phải bàn:** vai và việc của từng
người (§8.1) · topology **4 máy giữ nút + 1 máy không nút**, `SRV-MASTER`
riêng cho Người 1 (§3.1) · vai Sơ đồ là máy không giữ nút (§8.2) ·
`central` thôi chạy ứng dụng sau 10/10 (§3.2).

Còn lại:

- [ ] ⚠️ **Ba người tự chọn site** (§8.1) — Hạ tầng 2, Ứng dụng, Tài liệu,
      mỗi người một trong `HCM` / `HN` / `ĐN`. Chốt xong tick vào §8.1 và
      điền vào §8.2. Lưu ý `HCM` là site nặng việc nhất (§3.1)
- [ ] ⚠️ **`CENTRAL` giữ hay xoá sau 10/10** (§3.2). Giả định đang dùng: giữ
      nguyên bản dữ liệu làm nguồn cho `db/migrate`, chỉ gỡ sau khi đối soát
      số dòng khớp
- [ ] **Ownership bốn bảng Phần 1** theo `C2` (§4.5) — per-site hay Master
- [ ] **Tên thật 5 người** để điền vào cột `Người` ở §8.1
- [ ] **Cấu hình 4 máy giữ nút** — máy nào để bàn, RAM, đĩa trống. Cần để
      biết benchmark `B1`–`B6` chạy trên máy nào thì số đo có nghĩa
- [ ] **Ai đã cài sẵn gì** — SQL Server Developer, SQL Server Agent, SSMS, VPN
      (`D9`: Radmin, Tailscale dự phòng). Ai chưa cài thì tuần 1 phải trừ giờ
- [ ] **Ai viết được T-SQL**, ai chỉ làm tài liệu/slide — ảnh hưởng trực tiếp
      đến việc giao `db/migrate` và `sp_ChuyenCoSoSinhVien`
- [ ] **Windows bản nào trên 4 máy giữ nút** (Home / Pro) — Home không vào domain
      được, MS DTC phải đi đường *No Authentication Required* (`F4b`)
- [ ] **Lịch buổi làm việc chung** — 3 buổi/tuần, cả 5 người (§6.5)
- [ ] **Yêu cầu #2 hoàn thành ngày nào** (§6.3) — `M1` 17/10 hay 20/10?
- [ ] **Hai con số phần trăm.** Tài liệu ghi "~75% điểm" cho năm yêu cầu bắt
      buộc (§5, `P3`) và "~30% điểm" cho tài liệu (§9, rủi ro 4). Cộng lại vượt
      100%, nên hai con số cắt theo hai cách khác nhau. **Câu hỏi:** chúng
      chồng lấn thế nào, và barem thật của giảng viên là gì?

---

## 11. Bảng thuật ngữ

### Mã tham chiếu sang tài liệu thiết kế

| Mã | Nghĩa |
|---|---|
| `A4`, `B2` | Mục nêu giả định về quy mô (số sinh viên, số lượt đăng ký) |
| `B1`–`B6` | Nhóm mục phân tích; `B1`–`B6` trong §6.4 chỉ bộ benchmark |
| `C2` | **Ownership Matrix** — bảng quy định mỗi bảng dữ liệu do site nào sở hữu |
| `D9` | Quyết định về VPN (Radmin, Tailscale dự phòng) |
| `D12` | Quyết định số cơ sở triển khai |
| `D15` | Quyết định Master colocate hay chạy máy riêng |
| `D20` | Quyết định đảo thứ tự: Phần 1 làm ứng dụng trước, Phần 2 phân tán sau |
| `F1`–`F6` | Các bước **cài đặt vật lý**: VPN, kết nối server, SQL Server, Agent, Linked Server, Publication/Subscription |
| `F4b` | Bước cấu hình **MS DTC** trong Phần F |
| `F7` | Mục "thử các giao tác" (mục 3.7 của đề bài) |
| `G4` | Mục **kiểm thử sự cố** — sáu kịch bản |
| `G5` | Mục **thí nghiệm đối chứng Saga vs 2PC** |
| `G7` | Mục **demo deadlock** |
| `I2` | **Kế hoạch dự phòng cho replication** — gom nhiều nút về ít máy hơn (nhiều database trên một instance, hoặc nhiều named instance). Topology chính là 4 máy; `I2` chỉ dùng khi `M1` trượt, và dùng làm **cách chạy thử spike** ở `M0` |
| `I2b` | Mục nêu hệ thống **không cần chạy 24/7**; chỉ cần cả nhóm online cùng lúc trong buổi đã hẹn, và mỗi máy online ít nhất một lần mỗi cửa sổ retention |
| `I4` | Mục **việc còn treo** trong thiết kế |

### Thuật ngữ kỹ thuật

| Thuật ngữ | Nghĩa |
|---|---|
| **2PC** | Two-Phase Commit — giao dịch phân tán hai pha, bảo đảm nhiều database cùng commit hoặc cùng rollback |
| **MS DTC** | Microsoft Distributed Transaction Coordinator — dịch vụ Windows điều phối 2PC giữa các SQL Server |
| **Saga** | Cách ghi nhiều site **không** dùng khoá phân tán: mỗi bước commit riêng, hỏng thì chạy bước bù (compensating transaction) |
| **Outbox** | Bảng ghi sự kiện cần gửi đi, kèm worker đọc và gửi. Thứ tự bắt buộc: **upsert mirror trước, đánh dấu `SENT` sau** |
| **OPENQUERY** | Cách gọi truy vấn chạy **tại** server từ xa qua Linked Server, trả về kết quả đã gom — khác truy vấn four-part vốn kéo nhiều dòng về |
| **Linked Server** | Khai báo trong SQL Server cho phép một server truy vấn sang server khác |
| **Publication / Subscription** | Hai đầu của cơ chế replication: nơi phát và nơi nhận |
| **`NOT FOR REPLICATION`** | Tuỳ chọn khi tạo trigger ở Subscriber; thiếu nó thì trigger chặn chính Distribution Agent và replication chết |
| **X-Ray** | Công cụ tự viết hiển thị đường đi của mỗi request qua hệ thống: chạm site nào, bao nhiêu dòng qua mạng, thời gian từng site |
| **`SiteContext`** | Lớp giữ thông tin "request này thuộc cơ sở nào", để tầng dữ liệu chọn đúng database |
| **`RoutingDataSource`** | DataSource chọn kết nối theo `SiteContext`. Ràng buộc tuyệt đối: **một giao dịch chỉ chạm một site** |
| **Ba port** | `CrossSiteQuery`, `GlobalReport`, `CatalogHealth` — ba ranh giới trừu tượng duy nhất cho phần phân tán; không có port thứ tư |
| **Phân mảnh dẫn xuất** | Bảng con nằm cùng mảnh với bảng cha. `DangKyHocPhan` theo `LopHocPhan`; `Diem` theo `DangKyHocPhan` (bậc 2) |
| **Named instance** | Nhiều bản SQL Server chạy trên cùng một máy Windows, phân biệt bằng tên |
| **Retention window** | Khoảng thời gian subscription còn hiệu lực nếu không đồng bộ; quá hạn thì phải khởi tạo lại |
