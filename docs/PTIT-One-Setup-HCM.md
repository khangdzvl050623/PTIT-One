# PTIT One — trạng thái setup máy HCM

> Khôi phục ngày 24/09/2026 từ bản lưu local `da70d64`. Các kết quả bên dưới
> là nhật ký lần kiểm chứng trước đó, không phải trạng thái DB được kiểm
> tra lại trong lần khôi phục tài liệu này. Backend Phần 1 vẫn chưa nối DB.

Ghi nhận ngày **10/09/2026**, đã kiểm tra trực tiếp bằng `sqlcmd`.
Đây là nhật ký triển khai; quyết định kỹ thuật theo
[thiết kế chính thức, mục D14/D15 và Phần F](PTIT-One-Thiet-Ke.md).

## Đã thực hiện và xác minh

| Mục | Kết quả |
|---|---|
| SQL Server | 2019 Developer Edition, phiên bản `15.0.2000.5` |
| Instance | `DESKTOP-85V5Q0S\PTITONE` |
| Dịch vụ Engine / Agent | Đang chạy, khởi động Automatic |
| Đăng nhập quản trị hiện tại | Windows Authentication; tài khoản cài đặt có quyền sysadmin |
| Collation instance và hai database | `Vietnamese_CI_AS` |
| `PTITONE_MASTER` | ONLINE, RCSI bật, recovery SIMPLE |
| `PTITONE_HCM` | ONLINE, RCSI bật, recovery SIMPLE |
| File data và log của cả hai database | `D:\MSSQL\MSSQL15.PTITONE\MSSQL\DATA\` |
| `@@SERVERNAME` | Khớp `SERVERPROPERTY('ServerName')` |

### Hạ tầng replication — đã chạy thật, không phải `-WhatIf`

| Bước | Kết quả kiểm chứng |
|---|---|
| Share snapshot | `D:\MSSQL\ReplData` chia sẻ thành `\\DESKTOP-85V5Q0S\repldata`; quyền share + NTFS cấp cho `NT SERVICE\SQLAgent$PTITONE` và `NT SERVICE\MSSQL$PTITONE` |
| `30-distributor.sql` | Local Distributor + database `distribution`, retention 720 giờ, working directory là UNC share ở trên |
| `master/01..04` | 8 bảng tham chiếu + `DanhBaNguoiDung` + `TaiKhoanMaster`; seed 3 cơ sở · 5 khoa · 3 CTĐT · 14 môn · 19 dòng CTĐT–môn · 7 tiên quyết · 3 học kỳ · 12 khung giờ |
| `31-publication.sql` | `PUB_ThamChieu`, **9 article**, `TaiKhoanMaster` đứng ngoài publication đúng thiết kế |
| `32-subscription.sql -Subscribers HCM` | 1 push subscription cục bộ, `update_mode = read only` |
| Snapshot | `A snapshot of 9 article(s) was generated` → Distribution Agent giao 4 giao dịch / 120 lệnh |
| Đối chiếu dữ liệu | **9/9 bảng khớp số dòng** giữa `PTITONE_MASTER` và `PTITONE_HCM` |
| Độ trễ nhân bản (tracer token) | `distributor_latency = 2s` · `subscriber_latency = 2s` · **overall 4s** |
| End-to-end | `INSERT` rồi `DELETE` một dòng `Khoa` tại Master → HCM thấy cả hai thay đổi. Đã dọn dòng thử, `Khoa` trở lại 5 dòng |
| `site/10..12` trên HCM | 15 bảng vận hành + CHECK/UNIQUE + 3 chỉ mục |

**Tiêu chí PASS spike tuần 1 (≤ 10 giây) đạt với subscriber cục bộ: 4 giây.**
Chưa đo được qua VPN vì HN/DN chưa lên.

### Kiểm tra ràng buộc bắt buộc trên `PTITONE_HCM`

- `UQ_DangKyMonHoc_SV_Ky_Mon` là **unique filtered index**, vị từ đúng bộ
  trạng thái đã chốt: `('DANG_XU_LY', 'DA_DANG_KY', 'DANG_HUY')`
- `SinhVienHocKy` khóa kép `MaSinhVien` + `MaHocKy`, có `SoTinChiDaDangKy`,
  `SoTinChiDangGiuCho`, `TranTinChi`
- **Không cột `IDENTITY` nào** trong 15 bảng vận hành (các cột `IDENTITY`
  còn lại đều thuộc bảng hệ thống của replication/Service Broker)

## Tự kiểm chứng trong SSMS

`db/tests/90-demo-nhan-ban.sql` — mở trong SSMS, bôi đen từng khối bấm F5.
**Không cần SQLCMD Mode, không cần chọn database.** Không cài đặt gì, chỉ
thêm một dòng `Khoa` mã `DEMO` rồi tự xoá; chạy lại bao nhiêu lần cũng được.

| Phần | Cho thấy điều gì |
|---|---|
| 1 | 9 bảng nhân bản · 1 bảng chỉ ở Master · 15 bảng chỉ ở HCM |
| 2 | Ghi vào Master → HCM chưa thấy → 15 giây sau thì thấy |
| 3 | Độ trễ bằng tracer token (số liệu cho B6) |
| 4 | **Sửa ở bản sao thì Master không đổi; rồi Master ghi đè, sửa đổi đó mất trắng** — lý do Subscriber phải chỉ đọc |
| 5 | Nhật ký Distribution Agent và số lệnh đang nằm trong `distribution` |
| 6 | Dọn dẹp, hai bên trở lại 5 khoa |

Đã chạy thật toàn bộ file: phần 2 cho `0 → 1`, phần 3 cho overall 3 giây,
phần 4 cho đúng cảnh Master ghi đè, phần 6 trả về 5/5 không sót dòng thử.

⚠️ Thời gian chờ trong file để **15 giây** chứ không phải 5: lần chạy thử
đầu tiên với 5 giây cho ra `0` ở phần 2c và HCM chưa kịp bị ghi đè ở phần 4c
— độ trễ là biến thiên, demo mà ra kết quả sai thì dạy sai.

⚠️ File này **cố ý ghi thẳng tên database**, khác quy ước của mọi script
trong `db/`. Hai lý do đã kiểm chứng: SSMS **không giữ `:setvar` giữa hai
lần F5** (chạy từng khối — đúng cách dùng của file — thì khối nào cũng báo
`Variable DbMaster is not defined`), và khi chạy qua sqlcmd thì **`:setvar`
trong file đè lên `-v` của dòng lệnh**. Nên biến SQLCMD ở đây ghim cứng giá
trị y như hardcode mà vẫn bắt bật SQLCMD Mode — chỉ thêm rắc rối. Người ở
HN/DN chỉ cần Ctrl+H đổi `PTITONE_HCM` thành `PTITONE_HN`/`PTITONE_DN`.

## Hai lỗi đã sửa trong lần chạy thật này

1. **`31` và `32` dò nhầm job Snapshot Agent.** Cả hai dùng
   `name LIKE '%<Publication>%Snapshot%'`, nhưng SQL Server đặt tên job theo
   mẫu `<Publisher>-<PublisherDB>-<Publication>-<n>` — **trong tên không có
   chữ "Snapshot"**, chữ đó chỉ nằm ở category `REPL-Snapshot`. Điều kiện
   luôn trượt, nên bước sinh lại snapshot ở mục 3 của `32` **âm thầm không
   chạy** và cả ba subscriber sẽ rỗng dữ liệu mà Replication Monitor không
   báo đỏ — đúng cái bẫy README cảnh báo. Đã đổi sang tra
   `distribution.dbo.MSsnapshot_agents` theo tên publication.

2. **`32` đăng ký cả ba site kể cả máy chưa tồn tại.** `sp_addsubscription`
   không kiểm tra máy đích có thật hay không: đăng ký tới `SRV-HN` vẫn
   "thành công" ở mức metadata rồi để lại Distribution Agent job chạy lỗi
   liên tục. Đã thêm `run.ps1 -Subscribers HCM[,HN,DN]` (mặc định cả ba);
   `32` và `39-go-subscription.sql` lọc theo danh sách này. Bước kiểm chứng
   cuối của `39-go-subscription` giờ chỉ chặn subscription **ngoài** config,
   không chặn site thuộc config mà lần này không chọn gỡ.

Kiểm tra offline sau khi sửa: **26 trường hợp SQL/site + 5 kiểm tra runner
PASS**.

## Chưa thực hiện

- `site/13-trigger.sql`, `14-role-grant.sql`, `15-thutuc-dangky.sql` chưa
  viết. ⚠️ Hệ quả trước mắt: **`PTITONE_HCM` chưa có `DENY INSERT/UPDATE/
  DELETE` trên 9 bảng nhân bản**, nên ràng buộc "Subscriber chỉ đọc" hiện
  mới do quy ước, chưa được database ép buộc.
- Kiểm chứng phân mảnh ngang, tương tranh sức chứa/tín chỉ, và luồng hủy —
  cần `15-thutuc-dangky.sql` trước.
- Subscription cho HN và DN: script đã sẵn sàng, chỉ thiếu máy thật.
- VPN, TCP/port cố định, tài khoản kết nối từ xa, Linked Server và MS DTC.
  Chưa chuyển sang Mixed Mode theo hướng dẫn triển khai toàn nhóm.
- `SRV-HN` / `SRV-DN` còn là tên mẫu trong `db/config.ps1`.
- ⚠️ **Tài khoản chạy Agent hiện là virtual account** `NT SERVICE\SQLAgent$PTITONE`.
  Đủ dùng cho subscriber cục bộ (share loopback trên cùng máy), **không đủ**
  khi HN/DN vào: môi trường workgroup cần một tài khoản Windows **trùng
  username và trùng password trên mọi máy**, xem `db/replication/README.md` mục 1.
- Dữ liệu seed là **dữ liệu mẫu để chạy được**, chưa đối chiếu quy chế PTIT
  thật (khung giờ tiết, mã môn, số tín chỉ, danh sách môn tiên quyết).
- Mật khẩu mẫu của `admin.master` (`ChangeMe!2026`) chưa đổi.

Instance mặc định `MSSQLSERVER` cũ không được dùng cho dự án này.

## Sau này tách Master sang máy riêng

**Có thể thực hiện theo D15.** Chuyển riêng `PTITONE_MASTER`; giữ
`PTITONE_HCM` tại máy HCM và giữ schema nghiệp vụ đã chốt.

Đây là kế hoạch, **chưa có script migration tự động hoặc lần diễn tập PASS**:

1. Chuẩn bị máy đích, VPN, SQL Server tương thích, collation, tài khoản và
   chốt nơi chạy Distributor cùng thư mục snapshot.
2. Tạm dừng các luồng ghi liên quan, đợi đồng bộ hết, sao lưu và ghi nhận
   trạng thái đối chiếu; giữ phương án quay về máy cũ.
3. Chuyển database Master bằng backup/restore, chuyển login/quyền cần thiết,
   cập nhật `Servers.MASTER` và các địa chỉ kết nối phụ thuộc. Chỉ đổi đường
   dẫn snapshot nếu chuyển Distributor; `Servers.HCM` vẫn trỏ máy HCM.
4. Dựng lại publication/subscription theo máy mới. Khi site đã có dữ liệu và
   FK tham chiếu, phải chuẩn bị phương án khởi tạo lại phù hợp; không áp
   snapshot có thao tác DROP bảng một cách mặc định lên site đang vận hành.
5. Kiểm tra dữ liệu, đồng bộ một chiều, định tuyến đăng nhập và giao dịch
   chuyển cơ sở/MS DTC trước khi mở lại ghi và ngừng Master cũ.

Khôi phục database nhân bản sang server khác không giữ nguyên cấu hình
replication; cần dựng lại publication/subscription.
[Microsoft Learn](https://learn.microsoft.com/en-us/sql/relational-databases/replication/administration/back-up-and-restore-replicated-databases).
