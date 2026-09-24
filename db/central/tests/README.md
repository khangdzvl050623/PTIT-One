# Kiểm tra CENTRAL

`00-verify-database.sql` chỉ đọc để kiểm tra đích kết nối, collation, RCSI,
recovery và số bảng. DB rỗng vẫn có thể qua bước này; chưa đủ nghiệm thu F00.

Từ gốc repo, sau khi T1 đã tạo DB và cấu hình `config.local.psd1`:

```powershell
.\db\central\run.ps1 -Action VerifyDatabase -WhatIf
.\db\central\run.ps1 -Action VerifyDatabase
```

T2 bổ sung test schema/seed/ràng buộc; T5 bổ sung test truy vấn, quyền và
rollback qua JDBC trên SQL Server. Ca thay đổi dữ liệu chạy trên CENTRAL
test riêng. Hủy/đăng ký đồng thời cần kiểm số chỗ, tín chỉ và chống trùng.

Kiểm tra offline toàn bộ SQL/runner, không kết nối DB:

```powershell
powershell -NoProfile -File db/tests/Test-Scripts.ps1
```

Test dùng ScriptDom từ SSMS; có thể truyền `-ScriptDomPath` tới DLL tương ứng.
Khi thêm `.sql`, cập nhật danh sách trong test trên để file không bị bỏ sót.
PASS offline chưa chứng minh SQL Server runtime, migration hay nghiệp vụ.
