# Replication — hướng dẫn chạy

> Thư mục này dựng **Transactional Replication một chiều** từ `PTITONE_MASTER`
> ra mọi cơ sở vận hành. Đây là **yêu cầu bắt buộc số 2** của giảng viên.
>
> ⚠️ Đây cũng là hạng mục **rủi ro cao nhất** của cả dự án. Đọc hết trang này
> trước khi gõ lệnh đầu tiên — mọi cái bẫy đã biết đều nằm ở đây.

---

## 1. Trước khi bắt đầu — bốn điều kiện

Thiếu bất kỳ điều nào thì replication **sẽ hỏng**, và triệu chứng thường
không chỉ về đúng nguyên nhân.

| # | Điều kiện | Kiểm tra thế nào |
|---|---|---|
| 1 | **SQL Server Developer Edition** trên mọi máy | `SELECT SERVERPROPERTY('Edition');` — ⚠️ Express **không làm được Publisher** và **không có SQL Server Agent** |
| 2 | **SQL Server Agent** đang chạy, Startup Type = `Automatic` | Services → `SQL Server Agent (MSSQLSERVER)` |
| 3 | **`@@SERVERNAME` khớp tên máy thật** | `SELECT @@SERVERNAME, SERVERPROPERTY('ServerName');` — hai giá trị phải giống nhau |
| 4 | **Thư mục snapshot là UNC share, mọi máy vào được** | Từ **máy site khác**, mở Explorer gõ `\\SRV-HCM\repldata` — phải vào được |

### ⚠️ Cái bẫy nặng nhất: tài khoản chạy SQL Server Agent

Nếu Agent chạy bằng virtual account `NT Service\SQLSERVERAGENT`, nó **không
xác thực được ra thư mục chia sẻ trên máy khác** trong môi trường workgroup
(không có domain).

**Cách xử lý:** tạo **cùng một tài khoản Windows cục bộ, trùng username và
trùng password, trên MỌI máy** (pass-through authentication của workgroup),
rồi cho SQL Server Agent chạy bằng tài khoản đó.

```
Services → SQL Server Agent → Properties → Log On → This account
```

Sau khi đổi, **khởi động lại Agent**.

---

## 2. Chạy trên máy nào

```
                    ┌───────────────────────────────┐
                    │  SRV-HCM   (máy Master)       │
                    │                               │
                    │  00-create-databases.sql      │  -On MASTER
                    │  30-distributor.sql           │  -On MASTER
                    │  31-publication.sql           │  -On MASTER
                    │  32-subscription.sql          │  -On MASTER  ⭐ chạy từ đây
                    └───────────────────────────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              ▼                     ▼                     ▼
      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
      │   SRV-HCM    │      │   SRV-HN     │      │   SRV-DN     │
      │  PTITONE_HCM │      │  PTITONE_HN  │      │  PTITONE_DN  │
      │              │      │              │      │              │
      │ 00-create-databases.sql chay tren TUNG may            │
      └──────────────┘      └──────────────┘      └──────────────┘
```

> `32-subscription.sql` chạy **trên máy Master**, không phải trên từng
> subscriber — vì dùng **push subscription**, Distribution Agent nằm ở
> Distributor và tự đẩy dữ liệu sang.

---

## 3. Thứ tự thực hiện

Mọi lệnh chạy từ thư mục `db/`, dùng runner để bơm cấu hình từ `config.ps1`:

```powershell
# --- Bước 0: tạo database, chạy MỘT lần trên TỪNG MÁY CHỦ ---
# Mỗi lần chạy tạo TẤT CẢ database mà config.ps1 gán cho máy đó.
# Phương án 3 máy: lệnh đầu tạo cả PTITONE_MASTER lẫn PTITONE_HCM,
# vì hai database này cùng nằm trên SRV-HCM.
.\run.ps1 -Script 00-create-databases.sql -On MASTER
.\run.ps1 -Script 00-create-databases.sql -On HN
.\run.ps1 -Script 00-create-databases.sql -On DN

# --- Bước 1: Distributor, chỉ trên Master ---
.\run.ps1 -Script replication\30-distributor.sql -On MASTER

# --- Bước 2: Publication, chỉ trên Master ---
.\run.ps1 -Script replication\31-publication.sql -On MASTER

#   ⚠️ DỪNG Ở ĐÂY nếu chưa có schema.
#      Article phải khai báo sau khi có db/master/01-schema-thamchieu.sql.
#      Xem mục 3 trong 31-publication.sql.

# --- Bước 3: Subscription, chạy TỪ Master ---
.\run.ps1 -Script replication\32-subscription.sql -On MASTER
```

Muốn xem lệnh `sqlcmd` mà không thực thi: thêm `-WhatIf`.

### ⭐ Thứ tự subscription: cục bộ trước, VPN sau

`32-subscription.sql` tạo subscription cho `PTITONE_HCM` **trước** — đây là
subscriber **cùng instance**, không đi qua VPN.

Đó không phải chi tiết ngẫu nhiên: nó **tách hai loại lỗi hoàn toàn khác nhau**.

| Subscription cục bộ | Kết luận |
|---|---|
| ✅ Chạy được | Publication đúng → lỗi tiếp theo (nếu có) là **lỗi mạng** |
| ❌ Hỏng | Publication sai → sửa publication, đừng đụng tới VPN hay firewall |

Không có bước đệm này thì mọi lỗi trông giống nhau, và nhóm sẽ mất cả buổi
để đoán xem hỏng ở publication hay ở mạng.

---

## 4. Kiểm chứng

### Replication Monitor

```
SSMS → chuột phải Replication → Launch Replication Monitor
```

Mọi mục phải xanh. Nếu có dấu ✗, bấm vào để xem chi tiết lỗi của agent.

### Kiểm bằng T-SQL

```sql
-- Publication da co bao nhieu article?
USE PTITONE_MASTER;
SELECT p.name AS Publication, COUNT(a.artid) AS SoArticle
  FROM syspublications p
  LEFT JOIN sysarticles a ON a.pubid = p.pubid
 GROUP BY p.name;

-- Cac subscription va trang thai
USE distribution;
EXEC sp_replmonitorhelpsubscription @publisher = @@SERVERNAME;

-- Con bao nhieu lenh chua gui toi subscriber?
EXEC sp_replmonitorsubscriptionpendingcmds
     @publisher            = @@SERVERNAME,
     @publisher_db         = 'PTITONE_MASTER',
     @publication          = 'PUB_ThamChieu',
     @subscriber           = 'SRV-HN',
     @subscriber_db        = 'PTITONE_HN',
     @subscription_type    = 0;   -- 0 = push
```

### Đo độ trễ nhân bản bằng tracer token

Đây là cách **chính thức** để đo replication lag, và là số liệu cho
benchmark **B6**:

```sql
USE PTITONE_MASTER;
DECLARE @id INT;
EXEC sp_posttracertoken @publication = 'PUB_ThamChieu', @tracer_token_id = @id OUTPUT;

-- doi vai giay roi chay:
EXEC sp_helptracertokenhistory
     @publication = 'PUB_ThamChieu', @tracer_id = @id;
```

Cột `distributor_latency` và `subscriber_latency` chính là độ trễ cần đưa
vào báo cáo.

### Kiểm chứng end-to-end (dùng cho spike tuần 1)

```sql
-- Tren MASTER
INSERT INTO <BangThamChieu> (...) VALUES (...);

-- Doi <= 10 giay, roi tren HN:
SELECT * FROM <BangThamChieu> WHERE ...;   -- phai thay dong vua them
```

**Tiêu chí PASS của spike tuần 1:** `INSERT` tại Master → **≤ 10 giây** sau
`SELECT` tại HN thấy được dòng đó.

---

## 5. Cấu hình retention — chỗ số học dễ sai

| Tham số | Đặt ở | Giá trị | Nghĩa là |
|---|---|---|---|
| `@max_distretention` | `30-distributor.sql` | **720 giờ** | Lệnh được **giữ** trong distribution database |
| `@retention` | `31-publication.sql` | **720 giờ** | Subscription **hết hạn** sau bấy lâu không đồng bộ |

Hai tham số điều khiển **hai cơ chế hết hạn khác nhau**. Đặt bằng nhau là
**quy ước của nhóm** cho dễ vận hành, **không phải yêu cầu của SQL Server** —
đặt lệch nhau vẫn hoàn toàn hợp lệ.

> ⚠️ **Đừng diễn đạt thành bảo đảm "máy tắt 30 ngày vẫn bắt kịp".**
>
> Nếu lệnh đã bị cleanup job dọn khỏi distribution database, subscription dù
> **chưa** hết hạn vẫn **không còn gì để bắt kịp**. Con số 720 giờ là **giới
> hạn trên**, không phải cam kết: thực tế còn phụ thuộc lịch chạy của các
> cleanup job và snapshot còn dùng được hay không.
>
> **Cách duy nhất để biết chắc là giám sát Replication Monitor hằng tuần.**

Với 8 bảng tham chiếu nhỏ và ~15 lượt ghi/ngày, giữ 720 giờ tốn không đáng kể
dung lượng — nên vẫn là lựa chọn hợp lý, chỉ cần phát biểu cho đúng.

---

## 6. Bảng lỗi thường gặp

| Thông báo | Nguyên nhân | Xử lý |
|---|---|---|
| `The process could not connect to Subscriber` | Firewall chặn 1433, hoặc VPN chưa thông | Mở TCP 1433 **trên interface VPN**, kiểm `telnet <tên máy> 1433` |
| `Cannot access the file … ReplData` | Snapshot folder đang là đường dẫn local | Đổi sang UNC share (`sp_changedistpublisher`) |
| `Login failed for user 'NT AUTHORITY\ANONYMOUS LOGON'` | Agent chạy bằng virtual account, không ra được share | Đổi Agent sang tài khoản Windows trùng tên/mật khẩu mọi máy |
| `The subscription … has expired or does not exist` | Site tắt quá retention | Khởi tạo lại subscription; kiểm lại retention ở mục 5 |
| Replication chạy được rồi **đột nhiên chết sau khi thêm trigger** | ⚠️ Trigger ở Subscriber **thiếu `NOT FOR REPLICATION`** — nó chặn chính Distribution Agent | Sửa `db/site/13-trigger.sql`, thêm `NOT FOR REPLICATION` |
| `@@SERVERNAME` trả về `NULL` hoặc tên cũ | Máy từng bị đổi tên | `sp_dropserver` + `sp_addserver … , local` rồi **khởi động lại SQL Server** |

---

## 7. Trạng thái hiện tại

| File | Trạng thái |
|---|---|
| `30-distributor.sql` | ✅ Xong — không phụ thuộc schema. ⚠️ **Chưa chạy thật trên SQL Server**, mới kiểm `-WhatIf` và biến SQLCMD |
| `31-publication.sql` | ⚠️ **Khung xong, thiếu article.** `sp_addarticle` gắn trực tiếp vào bảng nguồn nên phải chờ schema |
| `32-subscription.sql` | ⏳ Chưa viết — chỉ cần sau khi có article |
| `39-go-*.sql` | ⏳ Chưa viết — script gỡ để chạy lại từ đầu |

**Đang chặn:** file Excel phân công đề tài của giảng viên. Có nó rồi mới chốt
được tên thực thể, rồi mới khai báo được article.

📖 Ngữ cảnh đầy đủ: mục **D1** (nhân bản), **F6** (các bước wizard),
**I5** (checklist tick nhanh) trong `docs/PTIT-One-Thiet-Ke.md`.
