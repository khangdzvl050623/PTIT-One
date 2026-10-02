/* PTIT One — seed/10-auth-seed.sql (CENTRAL, dữ liệu DEMO)
   Yêu cầu: migration V1__auth_slice đã áp dụng.
   Chạy chủ động, KHÔNG tự chạy lúc API khởi động:
     sqlcmd -S "localhost\PTITONE" -d PTITONE_CENTRAL -E -C -b -f 65001 -i db\central\seed\10-auth-seed.sql

   Chạy lại không nhân đôi: chỉ INSERT dòng còn thiếu, không sửa/xóa dòng có sẵn.

   Mật khẩu demo của MỌI tài khoản: PtitOne@2026
   Hash sinh bằng chính PasswordConfig của API ({argon2id-v1}, m=19456,t=2,p=1).
   SeedPasswordHashTest kiểm hash này khớp encoder — đổi encoder mà quên
   seed thì build đỏ, thay vì login sai 100% lúc demo.

   Tài khoản                 Vai trò        Cơ sở  Trạng thái     Mục đích
   B26DCCN001                SINH_VIEN      HCM    HOAT_DONG      SV chuẩn
   B26DCCN002                SINH_VIEN      HN     HOAT_DONG      fixture khác cơ sở
   B26DCCN003                SINH_VIEN      HCM    CHO_KICH_HOAT  phải bị từ chối
   B26DCCN004                SINH_VIEN      HCM    NGUNG          phải bị từ chối
   B25DCCN001                SINH_VIEN      HCM    HOAT_DONG      có điểm 2025-1 (ca tiên quyết, xem 20-hoc-vu-seed)
   GVHCM001                  GIANG_VIEN     HCM    HOAT_DONG
   GVHN001                   GIANG_VIEN     HN     HOAT_DONG
   admin.hcm                 ADMIN_CO_SO    HCM    HOAT_DONG
   admin.hn                  ADMIN_CO_SO    HN     HOAT_DONG
   admin.master              ADMIN_MASTER   —      HOAT_DONG      tài khoản ở TaiKhoanMaster */

SET NOCOUNT ON;
SET XACT_ABORT ON;
-- sqlcmd mặc định QUOTED_IDENTIFIER OFF; ghi vào bảng có filtered index sẽ bị từ chối.
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

IF DB_NAME() NOT LIKE N'PTITONE[_]CENTRAL%'
    THROW 51000, N'Seed chi chay tren PTITONE_CENTRAL*.', 1;
IF OBJECT_ID(N'dbo.TokenLamMoi', N'U') IS NULL
    THROW 51001, N'Chua co schema auth. Chay migration V1__auth_slice truoc.', 1;

DECLARE @Hash varchar(255) =
    '{argon2id-v1}$argon2id$v=19$m=19456,t=2,p=1$LFrRsEWWlFRst06Tm+1BEw$SaafhAGeiJBOojmoK+rW5TqJampuEBmiyqYAoxGPpq8';

BEGIN TRANSACTION;

INSERT INTO dbo.CoSo (MaCoSo, TenCoSo, ThanhPho, TenLinkedServer, TenDatabase)
SELECT v.MaCoSo, v.TenCoSo, v.ThanhPho, NULL, v.TenDatabase
  FROM (VALUES ('HCM', N'Cơ sở TP. Hồ Chí Minh', N'TP. Hồ Chí Minh', N'PTITONE_HCM'),
               ('HN',  N'Cơ sở Hà Nội',          N'Hà Nội',          N'PTITONE_HN'),
               ('DN',  N'Cơ sở Đà Nẵng',         N'Đà Nẵng',         N'PTITONE_DN'))
       v (MaCoSo, TenCoSo, ThanhPho, TenDatabase)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.CoSo c WHERE c.MaCoSo = v.MaCoSo);

DECLARE @DanhBa TABLE (
    TenDangNhap varchar(50), MaCoSo varchar(10), LoaiNguoiDung varchar(20),
    MaThucThe varchar(20), TrangThai varchar(20));
INSERT INTO @DanhBa VALUES
    ('B26DCCN001',   'HCM', 'SINH_VIEN',    'B26DCCN001', 'HOAT_DONG'),
    ('B26DCCN002',   'HN',  'SINH_VIEN',    'B26DCCN002', 'HOAT_DONG'),
    ('B26DCCN003',   'HCM', 'SINH_VIEN',    'B26DCCN003', 'CHO_KICH_HOAT'),
    ('B26DCCN004',   'HCM', 'SINH_VIEN',    'B26DCCN004', 'NGUNG'),
    ('B25DCCN001',   'HCM', 'SINH_VIEN',    'B25DCCN001', 'HOAT_DONG'),
    ('GVHCM001',     'HCM', 'GIANG_VIEN',   'GVHCM001',   'HOAT_DONG'),
    ('GVHN001',      'HN',  'GIANG_VIEN',   'GVHN001',    'HOAT_DONG'),
    ('admin.hcm',    'HCM', 'ADMIN_CO_SO',  NULL,         'HOAT_DONG'),
    ('admin.hn',     'HN',  'ADMIN_CO_SO',  NULL,         'HOAT_DONG'),
    ('admin.master', NULL,  'ADMIN_MASTER', NULL,         'HOAT_DONG');

INSERT INTO dbo.DanhBaNguoiDung (TenDangNhap, MaCoSo, LoaiNguoiDung, MaThucThe, TrangThai)
SELECT s.TenDangNhap, s.MaCoSo, s.LoaiNguoiDung, s.MaThucThe, s.TrangThai
  FROM @DanhBa s
 WHERE NOT EXISTS (SELECT 1 FROM dbo.DanhBaNguoiDung d WHERE d.TenDangNhap = s.TenDangNhap);

INSERT INTO dbo.TaiKhoan (TenDangNhap, MatKhauHash, VaiTro, MaThucThe, MaCoSo)
SELECT s.TenDangNhap, @Hash, s.LoaiNguoiDung, s.MaThucThe, s.MaCoSo
  FROM @DanhBa s
 WHERE s.LoaiNguoiDung <> 'ADMIN_MASTER'
   AND NOT EXISTS (SELECT 1 FROM dbo.TaiKhoan t WHERE t.TenDangNhap = s.TenDangNhap);

INSERT INTO dbo.TaiKhoanMaster (TenDangNhap, MatKhauHash, HoTen)
SELECT s.TenDangNhap, @Hash, N'Quản trị Phòng Đào tạo (demo)'
  FROM @DanhBa s
 WHERE s.LoaiNguoiDung = 'ADMIN_MASTER'
   AND NOT EXISTS (SELECT 1 FROM dbo.TaiKhoanMaster m WHERE m.TenDangNhap = s.TenDangNhap);

COMMIT TRANSACTION;

-- Đối soát: kỳ vọng 3 cơ sở, 10 danh bạ, 9 TaiKhoan, 1 TaiKhoanMaster (khi DB chỉ có fixture này).
SELECT (SELECT COUNT(*) FROM dbo.CoSo)            AS CoSo,
       (SELECT COUNT(*) FROM dbo.DanhBaNguoiDung) AS DanhBa,
       (SELECT COUNT(*) FROM dbo.TaiKhoan)        AS TaiKhoan,
       (SELECT COUNT(*) FROM dbo.TaiKhoanMaster)  AS TaiKhoanMaster;
