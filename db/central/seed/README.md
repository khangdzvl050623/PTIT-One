# Seed — T2 phụ trách

Chưa có dữ liệu mẫu trong khung này. Sau khi schema và hợp đồng tài khoản
được chốt, T2 thêm script seed chạy chủ động và ghi rõ thứ tự/lệnh chạy tại đây.

- Có SV, GV, quản trị; các ca đạt/chưa đạt/chưa có điểm môn tiên quyết,
  lớp còn chỗ/hết chỗ, lịch trùng và đợt đăng ký đóng/mở.
- Dùng mã fixture ổn định; chạy lại không tạo bản ghi trùng hoặc xóa dữ liệu
  người khác. Không reset toàn DB; chỉ dọn đúng fixture khi thực sự cần.
- Mật khẩu ứng dụng phải theo cơ chế hash T5 chọn. Không đưa mật khẩu SQL
  hoặc dữ liệu cá nhân thật vào seed.
- Ghi phiên bản migration yêu cầu, số dòng dự kiến và truy vấn đối soát.
- Seed có kiểm tra đúng DB CENTRAL, xử lý lỗi/rollback và ca parse offline
  tương ứng trong `db/tests/Test-Scripts.ps1`.

T1 chỉ nạp vào DB tích hợp theo đợt bàn giao đã thống nhất. T5 không gọi seed
demo mặc định lúc ứng dụng khởi động.

## Đã có

| File | Cần migration | Nội dung |
|---|---|---|
| `10-auth-seed.sql` | V1 | 3 cơ sở + 10 tài khoản demo (SV/GV/Admin cơ sở/Admin Master, có ca `CHO_KICH_HOAT`, `NGUNG`, khác cơ sở). Mật khẩu chung `PtitOne@2026`; bảng tài khoản ở đầu file |
| `20-hoc-vu-seed.sql` | V2, sau `10` | Danh mục, học kỳ 2026-1, hồ sơ SV/GV khớp tài khoản, đợt mở/đóng, 10 lớp có lịch; ca tiên quyết đạt/trượt/chưa có, lớp đầy, trùng lịch, liên cơ sở. Danh sách ca ở đầu file |

```powershell
sqlcmd -S "localhost\PTITONE" -d PTITONE_CENTRAL -E -C -b -f 65001 -i db\central\seed\10-auth-seed.sql
sqlcmd -S "localhost\PTITONE" -d PTITONE_CENTRAL -E -C -b -f 65001 -i db\central\seed\20-hoc-vu-seed.sql
```

Chạy lại không nhân đôi (kỳ vọng auth `3 10 9 1`; seed học vụ in bảng đối soát
bộ đếm, cột `Lech` phải bằng 0). Hash sinh bằng `PasswordConfig` của
API; `SeedPasswordHashTest` đỏ nếu encoder đổi mà seed không đổi theo.
