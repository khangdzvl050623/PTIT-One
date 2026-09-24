/* PTIT One — chi muc cho duong truy van cu the.
   -On HCM/HN/DN, sau 10 va 11. Chua co benchmark, KHONG tuyen bo da toi uu.
   PK da phuc vu: danh sach lop, lich theo lop, quyen mon/lich theo SV+ky.
   Ba bang UUID de heap + PK NONCLUSTERED theo C9, khong lay UUID lam clustered. */
:ON ERROR EXIT
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
IF N'$(SiteTarget)' NOT IN (N'HCM', N'HN', N'DN')
   OR DB_NAME() <> N'$(DbTarget)' OR DB_NAME() = N'$(DbMaster)'
    THROW 51100, N'Chi chay tren dung Subscriber (-On HCM/HN/DN).', 1;
IF EXISTS (SELECT 1 FROM (VALUES (N'SinhVien'), (N'GiangVien'), (N'TaiKhoan'), (N'DotDangKy'), (N'LopHocPhan'), (N'LichHoc'), (N'DangKyHocPhan'), (N'Diem'), (N'SinhVienHocKy'), (N'DangKyMonHoc'), (N'LichHocMirror'), (N'YeuCauHocLienCoSo'), (N'KetQuaXuLyYeuCau'), (N'BangDiemMirror'), (N'OutboxSuKien')
    ) r(TableName) WHERE OBJECT_ID(N'dbo.' + r.TableName, N'U') IS NULL)
    THROW 51102, N'Chay 10-schema-vanhanh.sql truoc.', 1;
GO

-- Tra danh muc lop theo hoc ky + mon (J3).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.LopHocPhan')
    AND name = N'IX_LopHocPhan_HocKy_MonHoc')
    CREATE NONCLUSTERED INDEX IX_LopHocPhan_HocKy_MonHoc
        ON dbo.LopHocPhan (MaHocKy, MaMonHoc)
        INCLUDE (TrangThai, SoLuongDaDangKy, SoLuongToiDa, HinhThucHoc, ChoPhepLienCoSo);
GO
-- Bang diem/dang ky theo sinh vien: PK Host bat dau bang MaLopHP, can chieu nguoc.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.DangKyHocPhan')
    AND name = N'IX_DangKyHocPhan_SinhVien')
    CREATE NONCLUSTERED INDEX IX_DangKyHocPhan_SinhVien
        ON dbo.DangKyHocPhan (MaSinhVien, MaLopHP);
GO
-- Worker C10 chi doc su kien chua gui, theo thoi diem tao.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.OutboxSuKien')
    AND name = N'IX_OutboxSuKien_ChoGui')
    CREATE NONCLUSTERED INDEX IX_OutboxSuKien_ChoGui
        ON dbo.OutboxSuKien (ThoiDiemTao)
        INCLUDE (EventId, SoLanThu) WHERE TrangThai = 'PENDING';
GO
PRINT '  [ok] Da tao 3 chi muc truy van. Can do actual plan khi co du lieu G1.';
GO
