# PTIT One — Việc còn lại

Cập nhật **29/10/2026**, deadline **30/10**. Chạy lại toàn bộ bằng chứng:

```
powershell -NoProfile -File db\tests\Chay-Bang-Chung.ps1 -DongBoLai
powershell -NoProfile -File db\tests\94-demo-lien-co-so.ps1
powershell -NoProfile -File db\tests\95-demo-cap-ho-so.ps1
powershell -NoProfile -File db\tests\96-kiem-api-phan-tan.ps1
```

`94`–`96` cần API ở profile `central,distributed`. `96` gọi cả 85 endpoint
bằng đúng vai — lần chạy 29/10: 116/116 OK, chạy lặp không đổi dữ liệu.

D13 (phân quyền tầng CSDL): `site/14` + `master/10` dựng 4 vai trò theo B3
và 2 trigger toàn vẹn; demo `.\run.ps1 -Script tests\97-demo-phan-quyen.sql -On HCM`
→ 13/13 ca, kèm thông báo từ chối thật của SQL Server để chụp màn hình.

## Đã xong 29/10 (dựng trọn trên một máy, ba named instance)

| Hạng mục | Kết quả |
|---|---|
| 5 yêu cầu bắt buộc | 5/5 PASS · nhân bản **3 subscriber**, trễ cao nhất 8 s |
| F02 qua Outbox Master (SV + GV) | `95` PASS · gửi lặp idempotent · kích hoạt rồi đăng nhập được |
| Huỷ liên cơ sở, 4 tình huống | `94` pha 1b–1e PASS |
| Outbox gửi hỏng rồi thử lại | `94` pha 3 PASS |
| Bộ test API | 194 test, 0 lỗi |

Lỗi tìm ra và đã sửa khi dựng mới:

| Lỗi | Hậu quả |
|---|---|
| `retry` yêu cầu `DANG_HUY` gửi lại lệnh **đăng ký** | Host ghi danh mồ côi, mất chỗ vĩnh viễn |
| `retry` gửi `MaCoSoNhaSV`/`HoTen` = NULL | Retry luôn hỏng đúng lúc cần nó; thiếu kiểm chủ yêu cầu |
| Kích hoạt / quên / đặt lại mật khẩu không định tuyến | Chạy ở Master → 503, không ai kích hoạt được ở Phần 2 |
| `TaiKhoanMaster.MatKhauHash NOT NULL` | `master/04` hỏng trên máy mới |
| `migrate/50` không chờ nhân bản danh mục | FK `HocKy` hỏng lần chạy đầu |
| `SinhVien.TrangThai` ở site thiếu DEFAULT | Cấp hồ sơ ở site hỏng |
| `refresh`/`logout` luôn chạy ở Master | Mọi người dùng bị đăng xuất sau 15 phút |
| Đổi mật khẩu / đăng xuất mọi nơi ghi danh bạ nhân bản ở site | 503 với mọi người dùng |
| Admin Master đổi email, cấp lại mã, cấp lại mật khẩu | Chạy ở Master, không thấy `TaiKhoan` |
| Admin Master: danh sách GV/lớp, xoá môn, đặt tiên quyết | Đọc bảng chỉ có ở site → dựng port `CrossSiteQuery` |
| Admin Master soạn thông báo | Master thiếu `ThongBao` → `master/09` + Outbox `ThongBaoDaGui` về các cơ sở |

## Còn lại

| Việc | Ghi chú |
|---|---|
| Ba máy thật lên VPN | Bản một máy đo trễ **trong máy**, không phải qua mạng — nói rõ khi nghiệm thu |
| Ảnh chụp | Tổng quan Admin Master, đăng nhập 5 vai, `94`/`95` chạy xong |
| ➕ X-Ray, frontend `lien-co-so`, port `CatalogHealth` | Cắt được |
| Thông báo theo lớp có SV liên cơ sở | Chỉ tới cơ sở mở lớp; SV khách đọc hộp thư ở cơ sở nhà nên không thấy — đã có từ trước, không riêng Master |
| Xoá môn ở Master | Kiểm qua các cơ sở rồi mới xoá; một cơ sở mở lớp chen giữa sẽ chặn Distribution Agent (FK). Xoá môn hiếm nên chấp nhận |

## Bẫy

**Chạy bộ test API xong phải `-DongBoLai`.** Test tích hợp để lại dữ liệu trong
CENTRAL, mà CENTRAL là nguồn của `migrate/50` — không reset thì đối soát `LECH`.
