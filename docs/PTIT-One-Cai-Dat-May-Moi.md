# PTIT One — cài SQL Server cho máy HN hoặc ĐN

> 🚦 **Đang làm Phần 1? Đây không phải tài liệu của bạn.**
> Cài SQL Server cho Phần 1 xem [hướng dẫn cài Phần 1](PTIT-One-Cai-Dat-Phan-1.md)
> — đơn giản hơn, không cần Replication, Agent hay VPN. Trang này dành cho
> lúc dựng hạ tầng phân tán ở Phần 2.

> Khôi phục ngày 24/09/2026 từ bản lưu local `da70d64`. Tài liệu này mô tả
> hạ tầng Master/site của phần phân tán; không phải script khởi tạo DB tập
> trung cho backend Phần 1. Lần khôi phục chỉ kiểm tra offline, chưa chạy SQL.

Cả nhóm dùng **cùng bộ script SQL**. Tên máy trong `db/config.ps1` và tham số
`-On` quyết định script chạy ở đâu. Mỗi máy giữ dữ liệu của cơ sở được giao.
Hướng dẫn này là quy trình cần thực hiện, **chưa được chạy thử trên máy HN/ĐN**.

Thiết kế chuẩn: [Phần F](PTIT-One-Thiet-Ke.md).
Trạng thái máy đã cài: [HCM kiêm Master](PTIT-One-Setup-HCM.md).

## 1. Cài phần mềm trên máy mới

1. Dùng SQL Server **Developer Edition**, thống nhất phiên bản trong nhóm.
   Máy HCM hiện dùng SQL Server 2019. Với bộ cài CAB 2019, đặt hai file
   `SQLServer2019-DEV-x64-ENU.exe` và `.box` cạnh nhau; chạy `.exe` để giải nén
   sang thư mục trên D, rồi mở `setup.exe` trong thư mục đã giải nén.
2. **Installation → New SQL Server stand-alone installation**.
   Chọn Developer; **Feature Selection** chỉ cần **Database Engine Services**
   và **SQL Server Replication**. Instance root directory: `D:\MSSQL` nếu có D.
3. **Instance Configuration:** Named instance `PTITONE`, Instance ID `PTITONE`.
   Nhiều máy cùng dùng tên instance này được vì tên máy khác nhau. Nếu máy mới
   đã có instance `PTITONE`, kiểm tra bản đó trước, không cài đè.
4. **Server Configuration:** Engine và Agent = **Automatic**. Browser cần
   chạy nếu dùng cơ chế tìm cổng của named instance. Giữ tài khoản dịch vụ mặc
   định trong giai đoạn cài local; cấu hình tài khoản replication ở bước VPN.
5. **Collation → Customize:** Windows collation designator = **Vietnamese**,
   chỉ tích **Accent-sensitive**. Sau OK phải hiện `Vietnamese_CI_AS`.
6. **Database Engine Configuration:** bấm **Add Current User**. Phần F của
   dự án yêu cầu **Mixed Mode** khi triển khai toàn nhóm: tự đặt mật khẩu `sa`
   mạnh và lưu riêng, không đưa vào Git/chat. Dùng Windows Authentication để
   quản trị local; ứng dụng và replication sẽ được cấp tài khoản/quyền riêng.
7. **Data Directories / TempDB:** đặt data, log, backup và TempDB trên ổ D.
   `ConfigurationFile.ini` và một số thành phần cài đặt còn ở C là bình thường.
8. **Install → Complete:** Engine và Replication phải cùng báo **Succeeded**.
   Cài SSMS nếu máy chưa có.

Máy HCM hiện mới bật Windows Authentication. Bước Mixed Mode/quyền kết nối
từ xa vẫn nằm trong phần triển khai VPN chưa thực hiện.

## 2. Kiểm tra local bằng SSMS

Kết nối `localhost\PTITONE`, Windows Authentication. Với chứng chỉ tự ký của
instance phát triển local: giữ Encrypt = Mandatory, chọn Trust Server Certificate.
Mở **New Query** và chạy:

```sql
SELECT @@SERVERNAME AS TenServer,
       SERVERPROPERTY('Edition') AS Edition,
       SERVERPROPERTY('Collation') AS Collation,
       SERVERPROPERTY('InstanceDefaultDataPath') AS DataPath,
       SERVERPROPERTY('InstanceDefaultLogPath') AS LogPath;
```

Ghi lại chính xác `TenServer`, ví dụ `MAY-BAN-HN\PTITONE`, để cập nhật config.
Kiểm tra Developer, `Vietnamese_CI_AS`, đường dẫn data/log trên ổ đã chọn.

## 3. Lấy repo và cấu hình topology dùng chung

Máy mới cần Git, PowerShell và công cụ `sqlcmd`. Kiểm tra `sqlcmd -?` chạy được.
Lấy cùng phiên bản repo đã thống nhất với nhóm. Chỉnh `db/config.ps1`:

| Khóa | Server | Database |
|---|---|---|
| `MASTER` | `DESKTOP-85V5Q0S\PTITONE` | `PTITONE_MASTER` |
| `HCM` | `DESKTOP-85V5Q0S\PTITONE` | `PTITONE_HCM` |
| `HN` | Tên thực từ `@@SERVERNAME` của máy HN | `PTITONE_HN` |
| `DN` | Tên thực từ `@@SERVERNAME` của máy ĐN | `PTITONE_DN` |

Không đổi tất cả server thành `localhost`: làm vậy sẽ gán database của mọi
cơ sở cho cùng một máy. Cả nhóm phải thống nhất cùng bản topology; không cần
đổi tên instance thành HCM/HN/ĐN vì vai trò đã nằm trong config.

Từ **PowerShell tại gốc repo trên máy HN**, xem trước rồi chạy:

```powershell
.\db\run.ps1 -Script 00-create-databases.sql -On HN -WhatIf
.\db\run.ps1 -Script 00-create-databases.sql -On HN
```

Máy ĐN thay `-On HN` bằng `-On DN`. Máy HCM kiêm Master dùng `-On MASTER`,
một lần tạo đủ hai database. Runner dùng tài khoản Windows đang đăng nhập;
tài khoản đó phải có quyền tạo database trên instance đích.

**Tới đây chỉ có database rỗng.** SSMS không tự chạy file SQL; backend cũng
chưa có cơ chế tự tạo bảng. Phải chạy các script theo bước tiếp theo.

## 4. Nối các máy và tạo bảng theo đúng vai trò

| Nơi thực hiện | Công việc tiếp theo |
|---|---|
| Mọi máy | Vào cùng Radmin VPN; phân giải đúng tên máy thật; bật TCP/IP và thống nhất cổng SQL cố định theo Phần F. Chỉ cho phép kết nối từ mạng VPN |
| Máy HCM/Master | Chuẩn bị share snapshot và tài khoản dịch vụ; dựng Distributor, chạy `master/01..04`, tạo Publication bằng `31` |
| Máy HCM/Master | Tạo push subscription bằng `32` tới database site đã sẵn sàng; **không chạy `32` trên máy HN/ĐN** |
| Từng máy site | Chờ snapshot chuyển đủ 9 bảng tham chiếu; kiểm tra dữ liệu đồng bộ trước khi chạy `site/10..12` |
| Từng máy site | Cài trigger, role và thủ tục nghiệp vụ khi các script tương ứng được hoàn thiện |
| Cả nhóm | Cấu hình Linked Server/MS DTC theo F4b/F5 và chạy các kịch bản kiểm chứng Phần F/G |

Tài khoản dịch vụ, quyền share/SQL và firewall phải được cấu hình theo
[hướng dẫn replication](../db/replication/README.md), không chỉ kiểm tra ping.
Named instance dùng SQL Browser để tìm cổng thì cần đường kết nối Browser
trong VPN; phương án khác là cấu hình client dùng cổng cố định đã thống nhất.

**Chọn subscriber đã sẵn sàng:** runner hiện hỗ trợ `-Subscribers`. Chạy
trên máy HCM/Master, xem trước trước khi thực thi:

```powershell
.\db\run.ps1 -Script replication\32-subscription.sql -On MASTER -Subscribers HN -WhatIf
.\db\run.ps1 -Script replication\32-subscription.sql -On MASTER -Subscribers HN
```

Dùng `DN` cho máy ĐN hoặc `HCM` khi chỉ kiểm chứng subscriber cục bộ.
Bỏ `-Subscribers` sẽ chọn cả ba site; chỉ làm khi cả ba đã sẵn sàng.
Không chạy `master/01..04` trên site để tự tạo bản sao thay cho snapshot.

Sau khi snapshot đã được kiểm chứng, chạy trên máy HN:

```powershell
.\db\run.ps1 -Script site\10-schema-vanhanh.sql -On HN
.\db\run.ps1 -Script site\11-rangbuoc.sql -On HN
.\db\run.ps1 -Script site\12-chimuc.sql -On HN
```

Ba script này tạo **15 bảng vận hành/hỗ trợ**, cùng ràng buộc và chỉ mục;
9 bảng tham chiếu nhận từ Master là nhóm bảng riêng. Máy ĐN thay `HN` bằng `DN`.
Chỉ đánh dấu hoàn tất sau khi kiểm chứng trên máy thật; trạng thái viết xong
script không thay thế kết quả chạy.

## 5. Tách Master sau này

Có thể chuyển riêng database Master sang máy thứ tư theo D15.
Xem [các bước và giới hạn migration](PTIT-One-Setup-HCM.md#sau-này-tách-master-sang-máy-riêng).
Việc chuyển cần backup/restore, cập nhật địa chỉ và dựng lại replication;
không cần thiết kế lại các bảng vận hành.
