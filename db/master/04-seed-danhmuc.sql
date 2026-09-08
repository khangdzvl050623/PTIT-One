/* =====================================================================
   PTIT One — master/04-seed-danhmuc.sql
   ---------------------------------------------------------------------
   Dữ liệu danh mục ban đầu.

   CHẠY Ở ĐÂU : CHỈ trên PTITONE_MASTER
                  .\run.ps1 -Script master\04-seed-danhmuc.sql -On MASTER

   CHẠY LẠI ĐƯỢC : có — dùng INSERT ... WHERE NOT EXISTS.
     ⚠️ Không dùng MERGE (quy ước dự án). Ở đây không có tương tranh nên
        lý do là tính nhất quán của quy ước, không phải rủi ro khoá.

   🔶 GIẢ ĐỊNH cần nhóm đối chiếu quy chế thật của trường:
      khung giờ tiết, mã môn, số tín chỉ, danh sách môn tiên quyết.
      Đây là dữ liệu mẫu để chạy được, KHÔNG phải quy chế PTIT.
   ===================================================================== */

SET NOCOUNT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    RAISERROR(N'Script nay CHI chay tren $(DbMaster). Database hien tai: %s',
              16, 1, DB_NAME());
    SET NOEXEC ON;
END
GO

/* =====================================================================
   1. CoSo — lấy thẳng từ db/config.ps1

   ⚠️ Dòng này PHẢI khớp với config.ps1. Nếu lệch, sp_ChuyenCoSoSinhVien
      sẽ dựng sai tên bốn phần và giao dịch phân tán thất bại.
      Vì vậy ở đây CẬP NHẬT LẠI mỗi lần chạy, không chỉ chèn nếu thiếu.
   ===================================================================== */

/* --- HCM: cùng máy với Master, không đi qua Linked Server --- */
UPDATE dbo.CoSo
   SET TenCoSo = N'Cơ sở TP. Hồ Chí Minh', ThanhPho = N'TP. Hồ Chí Minh',
       TenLinkedServer = NULL, TenDatabase = N'$(DbHCM)'
 WHERE MaCoSo = 'HCM';
INSERT INTO dbo.CoSo (MaCoSo, TenCoSo, ThanhPho, TenLinkedServer, TenDatabase)
SELECT 'HCM', N'Cơ sở TP. Hồ Chí Minh', N'TP. Hồ Chí Minh', NULL, N'$(DbHCM)'
 WHERE NOT EXISTS (SELECT 1 FROM dbo.CoSo WHERE MaCoSo = 'HCM');

/* --- HN --- */
UPDATE dbo.CoSo
   SET TenCoSo = N'Cơ sở Hà Nội', ThanhPho = N'Hà Nội',
       TenLinkedServer = N'$(LnkHN)', TenDatabase = N'$(DbHN)'
 WHERE MaCoSo = 'HN';
INSERT INTO dbo.CoSo (MaCoSo, TenCoSo, ThanhPho, TenLinkedServer, TenDatabase)
SELECT 'HN', N'Cơ sở Hà Nội', N'Hà Nội', N'$(LnkHN)', N'$(DbHN)'
 WHERE NOT EXISTS (SELECT 1 FROM dbo.CoSo WHERE MaCoSo = 'HN');

/* --- DN --- */
UPDATE dbo.CoSo
   SET TenCoSo = N'Cơ sở Đà Nẵng', ThanhPho = N'Đà Nẵng',
       TenLinkedServer = N'$(LnkDN)', TenDatabase = N'$(DbDN)'
 WHERE MaCoSo = 'DN';
INSERT INTO dbo.CoSo (MaCoSo, TenCoSo, ThanhPho, TenLinkedServer, TenDatabase)
SELECT 'DN', N'Cơ sở Đà Nẵng', N'Đà Nẵng', N'$(LnkDN)', N'$(DbDN)'
 WHERE NOT EXISTS (SELECT 1 FROM dbo.CoSo WHERE MaCoSo = 'DN');

PRINT '  [+] CoSo';
GO

/* =====================================================================
   2. KhungGioTiet 🔶

   ⚠️ Bảng này quyết định việc kiểm trùng lịch liên cơ sở có ý nghĩa hay
      không. Ba cơ sở BẮT BUỘC dùng chung khung giờ — đó là lý do nó nằm
      ở Master và được nhân bản, thay vì để mỗi nơi tự quy ước.
   ===================================================================== */
INSERT INTO dbo.KhungGioTiet (SoTiet, GioBatDau, GioKetThuc, Buoi)
SELECT v.SoTiet, v.GioBatDau, v.GioKetThuc, v.Buoi
  FROM (VALUES
        ( 1, '07:00', '07:50', 'SANG'),
        ( 2, '08:00', '08:50', 'SANG'),
        ( 3, '09:00', '09:50', 'SANG'),
        ( 4, '10:00', '10:50', 'SANG'),
        ( 5, '11:00', '11:50', 'SANG'),
        ( 6, '13:00', '13:50', 'CHIEU'),
        ( 7, '14:00', '14:50', 'CHIEU'),
        ( 8, '15:00', '15:50', 'CHIEU'),
        ( 9, '16:00', '16:50', 'CHIEU'),
        (10, '17:00', '17:50', 'CHIEU'),
        (11, '18:00', '18:50', 'TOI'),
        (12, '19:00', '19:50', 'TOI')
       ) AS v(SoTiet, GioBatDau, GioKetThuc, Buoi)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.KhungGioTiet k WHERE k.SoTiet = v.SoTiet);

PRINT '  [+] KhungGioTiet';
GO

/* =====================================================================
   3. Khoa 🔶
   ===================================================================== */
INSERT INTO dbo.Khoa (MaKhoa, TenKhoa)
SELECT v.MaKhoa, v.TenKhoa
  FROM (VALUES
        ('CNTT', N'Công nghệ thông tin'),
        ('VT',   N'Viễn thông'),
        ('ATTT', N'An toàn thông tin'),
        ('DPT',  N'Công nghệ đa phương tiện'),
        ('QTKD', N'Quản trị kinh doanh')
       ) AS v(MaKhoa, TenKhoa)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.Khoa k WHERE k.MaKhoa = v.MaKhoa);

PRINT '  [+] Khoa';
GO

/* =====================================================================
   4. ChuongTrinhDaoTao 🔶
   ===================================================================== */
INSERT INTO dbo.ChuongTrinhDaoTao (MaCTDT, TenCTDT, MaKhoa, TongTinChi)
SELECT v.MaCTDT, v.TenCTDT, v.MaKhoa, v.TongTinChi
  FROM (VALUES
        ('CN-CNTT-2022', N'Cử nhân Công nghệ thông tin',   'CNTT', 130),
        ('KS-ATTT-2022', N'Kỹ sư An toàn thông tin',       'ATTT', 145),
        ('CN-DPT-2022',  N'Cử nhân Công nghệ đa phương tiện','DPT', 130)
       ) AS v(MaCTDT, TenCTDT, MaKhoa, TongTinChi)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.ChuongTrinhDaoTao c WHERE c.MaCTDT = v.MaCTDT);

PRINT '  [+] ChuongTrinhDaoTao';
GO

/* =====================================================================
   5. MonHoc 🔶
   ===================================================================== */
INSERT INTO dbo.MonHoc (MaMonHoc, TenMonHoc, SoTinChi, MaKhoa)
SELECT v.MaMonHoc, v.TenMonHoc, v.SoTinChi, v.MaKhoa
  FROM (VALUES
        ('BAS1150', N'Giải tích 1',                      3, 'CNTT'),
        ('BAS1151', N'Giải tích 2',                      3, 'CNTT'),
        ('BAS1203', N'Đại số tuyến tính',                3, 'CNTT'),
        ('INT1154', N'Tin học cơ sở 1',                  3, 'CNTT'),
        ('INT1155', N'Tin học cơ sở 2',                  3, 'CNTT'),
        ('INT1306', N'Cấu trúc dữ liệu và giải thuật',   3, 'CNTT'),
        ('INT1313', N'Lập trình hướng đối tượng',        3, 'CNTT'),
        ('INT1332', N'Cơ sở dữ liệu',                    3, 'CNTT'),
        ('INT1339', N'Cơ sở dữ liệu phân tán',           3, 'CNTT'),
        ('INT1358', N'Mạng máy tính',                    3, 'CNTT'),
        ('INT1434', N'Công nghệ phần mềm',               3, 'CNTT'),
        ('INT1445', N'Nhập môn trí tuệ nhân tạo',        3, 'CNTT'),
        ('ATT1234', N'Cơ sở an toàn thông tin',          3, 'ATTT'),
        ('DPT1201', N'Nhập môn đa phương tiện',          3, 'DPT')
       ) AS v(MaMonHoc, TenMonHoc, SoTinChi, MaKhoa)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.MonHoc m WHERE m.MaMonHoc = v.MaMonHoc);

PRINT '  [+] MonHoc';
GO

/* =====================================================================
   6. MonHocTienQuyet 🔶

   Điều kiện tiên quyết được kiểm TẠI HOME vì bảng điểm nằm ở Home —
   ví dụ điển hình của "push computation to data".
   ===================================================================== */
INSERT INTO dbo.MonHocTienQuyet (MaMonHoc, MaMonTienQuyet)
SELECT v.MaMonHoc, v.MaMonTienQuyet
  FROM (VALUES
        ('BAS1151', 'BAS1150'),   -- Giải tích 2      <- Giải tích 1
        ('INT1155', 'INT1154'),   -- Tin học cơ sở 2  <- Tin học cơ sở 1
        ('INT1306', 'INT1155'),   -- CTDL & GT        <- Tin học cơ sở 2
        ('INT1313', 'INT1155'),   -- OOP              <- Tin học cơ sở 2
        ('INT1332', 'INT1306'),   -- Cơ sở dữ liệu    <- CTDL & GT
        ('INT1339', 'INT1332'),   -- CSDL phân tán    <- Cơ sở dữ liệu
        ('INT1434', 'INT1313')    -- Công nghệ PM     <- OOP
       ) AS v(MaMonHoc, MaMonTienQuyet)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.MonHocTienQuyet t
                    WHERE t.MaMonHoc = v.MaMonHoc
                      AND t.MaMonTienQuyet = v.MaMonTienQuyet);

PRINT '  [+] MonHocTienQuyet';
GO

/* =====================================================================
   7. CTDT_MonHoc 🔶 — chương trình CNTT gồm những môn nào
   ===================================================================== */
INSERT INTO dbo.CTDT_MonHoc (MaCTDT, MaMonHoc, HocKyGoiY, BatBuoc)
SELECT v.MaCTDT, v.MaMonHoc, v.HocKyGoiY, v.BatBuoc
  FROM (VALUES
        ('CN-CNTT-2022', 'BAS1150', 1, 1),
        ('CN-CNTT-2022', 'BAS1203', 1, 1),
        ('CN-CNTT-2022', 'INT1154', 1, 1),
        ('CN-CNTT-2022', 'BAS1151', 2, 1),
        ('CN-CNTT-2022', 'INT1155', 2, 1),
        ('CN-CNTT-2022', 'INT1306', 3, 1),
        ('CN-CNTT-2022', 'INT1313', 3, 1),
        ('CN-CNTT-2022', 'INT1332', 4, 1),
        ('CN-CNTT-2022', 'INT1358', 4, 1),
        ('CN-CNTT-2022', 'INT1339', 5, 1),   -- CSDL phân tán
        ('CN-CNTT-2022', 'INT1434', 5, 1),
        ('CN-CNTT-2022', 'INT1445', 6, 0),   -- tự chọn
        ('KS-ATTT-2022', 'BAS1150', 1, 1),
        ('KS-ATTT-2022', 'INT1154', 1, 1),
        ('KS-ATTT-2022', 'INT1155', 2, 1),
        ('KS-ATTT-2022', 'INT1332', 4, 1),
        ('KS-ATTT-2022', 'ATT1234', 4, 1),
        ('CN-DPT-2022',  'INT1154', 1, 1),
        ('CN-DPT-2022',  'DPT1201', 1, 1)
       ) AS v(MaCTDT, MaMonHoc, HocKyGoiY, BatBuoc)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.CTDT_MonHoc c
                    WHERE c.MaCTDT = v.MaCTDT AND c.MaMonHoc = v.MaMonHoc);

PRINT '  [+] CTDT_MonHoc';
GO

/* =====================================================================
   8. HocKy 🔶

   NgayBatDau là mốc suy ra SỐ TUẦN dùng trong LichHoc / LichHocMirror.
   Ba cơ sở phải dùng chung mốc này, nếu không TuanBatDau / TuanKetThuc
   giữa hai site không so được với nhau.
   ===================================================================== */
INSERT INTO dbo.HocKy (MaHocKy, TenHocKy, NamHoc, NgayBatDau, NgayKetThuc)
SELECT v.MaHocKy, v.TenHocKy, v.NamHoc, v.NgayBatDau, v.NgayKetThuc
  FROM (VALUES
        ('2025-1',  N'Học kỳ 1 năm học 2025-2026', '2025-2026', '2025-09-08', '2026-01-11'),
        ('2025-2',  N'Học kỳ 2 năm học 2025-2026', '2025-2026', '2026-01-19', '2026-05-24'),
        ('2025-HE', N'Học kỳ hè 2025-2026',        '2025-2026', '2026-06-01', '2026-07-26')
       ) AS v(MaHocKy, TenHocKy, NamHoc, NgayBatDau, NgayKetThuc)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.HocKy h WHERE h.MaHocKy = v.MaHocKy);

PRINT '  [+] HocKy';
GO

/* =====================================================================
   9. Tài khoản Admin Master

   ⚠️ Mật khẩu mẫu để chạy thử. ĐỔI NGAY trước khi mở API ra internet.
      Chuỗi dưới đây là băm BCrypt của 'ChangeMe!2026'.
      Tầng ứng dụng phải tự sinh băm, script này chỉ để có tài khoản
      đăng nhập lần đầu.
   ===================================================================== */
INSERT INTO dbo.DanhBaNguoiDung
       (TenDangNhap, MaCoSo, LoaiNguoiDung, MaThucThe, TrangThai)
SELECT 'admin.master', NULL, 'ADMIN_MASTER', NULL, 'HOAT_DONG'
 WHERE NOT EXISTS (SELECT 1 FROM dbo.DanhBaNguoiDung
                    WHERE TenDangNhap = 'admin.master');

INSERT INTO dbo.TaiKhoanMaster (TenDangNhap, MatKhauHash, VaiTro, HoTen)
SELECT 'admin.master',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       'ADMIN_MASTER',
       N'Phòng đào tạo trung tâm'
 WHERE NOT EXISTS (SELECT 1 FROM dbo.TaiKhoanMaster
                    WHERE TenDangNhap = 'admin.master');

PRINT '  [+] Tai khoan admin.master  (mat khau mau: ChangeMe!2026 — DOI NGAY)';
GO

/* =====================================================================
   KIỂM CHỨNG
   ===================================================================== */
PRINT '';
PRINT '  ------------------------------------------------------------';
GO

SELECT 'CoSo'              AS [Bang], COUNT(*) AS [SoDong] FROM dbo.CoSo
UNION ALL SELECT 'Khoa',              COUNT(*) FROM dbo.Khoa
UNION ALL SELECT 'ChuongTrinhDaoTao', COUNT(*) FROM dbo.ChuongTrinhDaoTao
UNION ALL SELECT 'MonHoc',            COUNT(*) FROM dbo.MonHoc
UNION ALL SELECT 'CTDT_MonHoc',       COUNT(*) FROM dbo.CTDT_MonHoc
UNION ALL SELECT 'MonHocTienQuyet',   COUNT(*) FROM dbo.MonHocTienQuyet
UNION ALL SELECT 'HocKy',             COUNT(*) FROM dbo.HocKy
UNION ALL SELECT 'KhungGioTiet',      COUNT(*) FROM dbo.KhungGioTiet
UNION ALL SELECT 'DanhBaNguoiDung',   COUNT(*) FROM dbo.DanhBaNguoiDung
UNION ALL SELECT 'TaiKhoanMaster',    COUNT(*) FROM dbo.TaiKhoanMaster;
GO

/* Đối chiếu CoSo với cấu hình đang dùng — lệch là hỏng sp_ChuyenCoSoSinhVien */
PRINT '';
PRINT '  Doi chieu CoSo voi config.ps1:';
GO
SELECT MaCoSo, TenDatabase, ISNULL(TenLinkedServer, N'(cuc bo)') AS LinkedServer
  FROM dbo.CoSo ORDER BY MaCoSo;
GO

PRINT '';
PRINT '  ⚠️ 🔶 Du lieu tren la MAU de chay duoc, KHONG phai quy che PTIT.';
PRINT '     Nhom phai doi chieu quy che that: khung gio tiet, ma mon,';
PRINT '     so tin chi, danh sach mon tien quyet.';
PRINT '';
PRINT '  Buoc tiep theo:';
PRINT '    - Mo khoa muc 3 trong replication\31-publication.sql (8 article)';
PRINT '    - Roi toi db\site\ cho cac CSDL van hanh';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
