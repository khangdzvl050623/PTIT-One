# Schema vận hành tại mỗi cơ sở

Phạm vi hiện tại: **15 bảng theo C1/I6**, không phải 12 bảng trong bàn giao:

| Nhóm | Bảng |
|---|---|
| Phân mảnh ngang (5) | SinhVien, GiangVien, TaiKhoan, DotDangKy, LopHocPhan |
| Dẫn xuất (3) | LichHoc, DangKyHocPhan, Diem |
| Học vụ tại Home (3) | SinhVienHocKy, DangKyMonHoc, LichHocMirror |
| Liên cơ sở (4) | YeuCauHocLienCoSo, KetQuaXuLyYeuCau, BangDiemMirror, OutboxSuKien |

## Dùng chung script giữa máy HCM, HN và DN

**Cả nhóm dùng cùng bộ script SQL và thống nhất `db/config.ps1` chứa topology
của tất cả máy.** Không cần viết một bản SQL riêng cho từng cơ sở.
Tên server trong cấu hình phải là tên SQL Server instance thực tế mà các
máy kết nối được qua mạng/VPN; `SRV-HCM`, `SRV-HN`, `SRV-DN` hiện là tên mẫu.
Nếu dùng named instance thì khai báo dạng `TEN-MAY\TEN-INSTANCE`.

Ví dụ máy của bạn giữ cả Master và HCM, máy bạn cùng nhóm giữ HN:

| Vai trò trong config | SQL Server instance | Database đích |
|---|---|---|
| `MASTER` | Máy của bạn — cùng giá trị với `HCM` | `PTITONE_MASTER` |
| `HCM` | Máy của bạn — cùng giá trị với `MASTER` | `PTITONE_HCM` |
| `HN` | Máy bạn cùng nhóm tại Hà Nội | `PTITONE_HN` |
| `DN` | Máy của thành viên phụ trách Đà Nẵng | `PTITONE_DN` |

Chạy từ thư mục `db/` trên từng máy để tạo database:

```powershell
# Máy giữ Master + HCM: tạo cả PTITONE_MASTER và PTITONE_HCM
.\run.ps1 -Script 00-create-databases.sql -On MASTER

# Máy HN: tạo PTITONE_HN
.\run.ps1 -Script 00-create-databases.sql -On HN
```

Đây chỉ là bước tạo database. Sau khi dựng Master/replication và snapshot
đã áp dụng đủ bảng tham chiếu, cùng file `site/10-schema-vanhanh.sql` được
chạy với `-On HCM` tại HCM hoặc `-On HN` tại HN; các bước 11/12 cũng tương tự.
**Cấu trúc bảng dùng chung; dữ liệu vận hành thuộc từng mảnh cơ sở.**

`-On` chọn **đích kết nối từ config**, không tự nhận diện máy đang chạy lệnh.
Cũng có thể chạy từ một máy quản trị tới site khác nếu mạng/VPN thông và
tài khoản Windows hiện tại có quyền kết nối (`run.ps1` dùng `-E`). Không đổi
tất cả địa chỉ thành `localhost` trên từng máy vì sẽ mất ánh xạ toàn hệ thống.

SSMS dùng để quản trị database đã tạo trong SQL Server. Mở SSMS hoặc chạy
backend không tự thực thi các file trong `db/`; hiện phải chủ động gọi runner.
Thư mục `master/` và script tạo publication/push subscription chỉ chạy tại
Master theo [thứ tự cài replication](../replication/README.md), không chạy ở
mọi site. Riêng script dọn metadata Subscriber chạy tại từng Subscriber.

## Chạy

Từ `db/`, **sau khi snapshot đã áp dụng đủ 9 bảng tham chiếu** ở từng site:

Vòng lặp dưới đây kết nối lần lượt tới cả ba site; dùng khi máy quản trị
và tài khoản hiện tại truy cập được cả ba. Nếu mỗi người cài trên máy mình,
chạy ba lệnh bên trong với `-On HCM`, `-On HN` hoặc `-On DN` tương ứng.

```powershell
foreach ($site in 'HCM', 'HN', 'DN') {
    .\run.ps1 -Script site\10-schema-vanhanh.sql -On $site
    .\run.ps1 -Script site\11-rangbuoc.sql -On $site
    .\run.ps1 -Script site\12-chimuc.sql -On $site
}
```

Thêm `-WhatIf` vào mỗi lệnh để chỉ xem lệnh kết nối. Runner truyền `-d`
để chọn đúng database ngay lúc kết nối; các script tiếp tục kiểm tra đích.
Trong SSMS phải bật **SQLCMD Mode** và cấp biến từ `config.ps1`.

`10` và `11` dùng giao dịch cục bộ cùng `:ON ERROR EXIT`, lỗi thì ngừng
script; khi kết nối sqlcmd đóng, giao dịch chưa commit được rollback.
Chạy lại bỏ qua đối tượng đã tồn tại, không phải cơ chế tự sửa schema lệch.
Thay đổi schema đã triển khai cần migration có kiểm soát theo J3.

## Các quyết định khi hiện thực

- `DangKyHocPhan` chỉ FK tới lớp ở Host và danh mục cơ sở; **không FK tới
  SinhVien**. `Diem` FK kép tới đăng ký. Các mã lớp từ xa không FK tới lớp cục bộ.
- `KetQuaXuLyYeuCau` không FK tới ghi danh/lớp/SV: phải ghi được kết quả
  từ chối lớp không tồn tại và kết quả hủy đến trước đăng ký.
- UUID do ứng dụng cấp, PK **NONCLUSTERED**, không dùng `IDENTITY`.
- `PhongHoc` nằm ở từng `LichHoc` theo C1 nhóm 3b, không lặp cột trên lớp.
  `LopHocPhan.PhienBanLich` hiện thực việc Host đối chiếu phiên bản ở D3.
- `LichHocMirror` dùng `PhienBanLich` làm phiên bản và có thời điểm/trạng thái
  đồng bộ theo C10. Phải ghi mirror ngay trong giao dịch giữ quyền tại Home.
- Chưa đóng cứng công thức điểm, trần 24 tín chỉ hoặc enum trạng thái
  SV/lớp/đợt/SyncStatus vì tài liệu chưa chốt đầy đủ. `TranTinChi` phải cấp rõ.
- CHECK bảo vệ sức chứa/tín chỉ không thay thế `sp_getapplock`, UPDATE có
  điều kiện, xử lý hủy và đối soát bộ đếm trong thủ tục/ứng dụng.

### Điểm cần nhóm làm rõ trong thiết kế đã chốt

I6 yêu cầu **PK và unique filtered index cùng bộ ba cột** trên `DangKyMonHoc`.
Script giữ cả hai đúng tài liệu. Tuy nhiên, PK đã chặn dòng thứ hai của cùng
SV/kỳ/môn **ở mọi trạng thái**; filtered index không cho phép chèn thêm dòng
sau hủy. Muốn đăng ký lại cùng kỳ phải tái sử dụng dòng hiện có theo luồng
nghiệp vụ, hoặc nhóm phải duyệt thay đổi khóa nếu cần giữ lịch sử nhiều lần
đăng ký. Không tự đổi schema để diễn giải lại yêu cầu này.

PK lịch ở I6 cũng chỉ cho một dòng trên lớp/thứ/tiết bắt đầu (mirror thêm
SV/kỳ/môn); không lưu được hai khoảng tuần riêng có cùng bộ khóa đó.

## Trạng thái và kiểm chứng

| File | Trạng thái |
|---|---|
| `10-schema-vanhanh.sql` | ✅ **Đã chạy thật trên `PTITONE_HCM`** — 15 bảng, PK/FK, default |
| `11-rangbuoc.sql` | ✅ **Đã chạy thật trên `PTITONE_HCM`** — CHECK, unique filtered index |
| `12-chimuc.sql` | ✅ **Đã chạy thật trên `PTITONE_HCM`** — 3 chỉ mục; chưa benchmark |
| `13-trigger.sql` | Chưa viết — bảo vệ phân mảnh, replica và điểm đã khóa |
| `14-role-grant.sql` | Chưa viết — role và DENY trên bảng nhân bản |
| `15-thutuc-dangky.sql` | Chưa viết — giao dịch đăng ký và tương tranh |

Kiểm tra offline từ gốc repo:

```powershell
powershell -NoProfile -File db/tests/Test-Scripts.ps1
```

Đã PASS bộ phân tích **Microsoft ScriptDom (T-SQL 160)**, biến SQLCMD và
database runner: 15 tổ hợp script/site, thêm 5 trường hợp hồi quy runner.
**Chưa chạy DDL trên SQL Server**, chưa kiểm tra FK/CHECK bằng dữ liệu thật,
chưa chứng minh chạy lại, rollback, replication hay tương tranh ở runtime.

Khi có môi trường: chạy 10→11→12 hai lần trên cả ba site; thử dữ liệu hợp lệ,
sinh viên khách tại Host, điểm không có đăng ký, mã yêu cầu trùng, sai trạng
thái, vượt sĩ số và vượt trần tín chỉ. Ca sai phải bị CSDL từ chối. Tiếp theo
là 13→14→15 và kiểm chứng Phần F; chưa đủ cơ sở báo PASS cài đặt vật lý.

Không áp dụng lại snapshot có thao tác DROP bảng tham chiếu khi các FK
vận hành đang tham chiếu tới chúng: cần lập migration/reinitialization riêng.
Các script gỡ replication không tự gỡ FK hoặc xóa dữ liệu vận hành.
