/* =====================================================================
   PTIT One — master/10-vai-tro.sql   (-On MASTER)
   ---------------------------------------------------------------------
   D13/B3 phía Master: danh mục và danh bạ "R/W — CHỈ tại Master".
   Cùng tên vai trò với site/14 để một người đọc hai file thấy ngay
   cùng một ma trận. Demo: tests/97.
   ===================================================================== */
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    RAISERROR(N'Script nay CHI chay tren $(DbMaster).', 16, 1);
    SET NOEXEC ON;
END
GO

IF DATABASE_PRINCIPAL_ID(N'r_AdminMaster') IS NULL CREATE ROLE r_AdminMaster;
IF DATABASE_PRINCIPAL_ID(N'u_demo_master') IS NULL CREATE USER u_demo_master WITHOUT LOGIN;
EXEC sp_addrolemember N'r_AdminMaster', N'u_demo_master';

DECLARE @b SYSNAME, @sql NVARCHAR(400);
DECLARE c CURSOR LOCAL FAST_FORWARD FOR
    SELECT Ten FROM (VALUES (N'CoSo'), (N'Khoa'), (N'MonHoc'), (N'MonHocTienQuyet'), (N'ChuongTrinhDaoTao'),
                            (N'CTDT_MonHoc'), (N'HocKy'), (N'KhungGioTiet'), (N'DanhBaNguoiDung')) t(Ten);
OPEN c; FETCH NEXT FROM c INTO @b;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.' + QUOTENAME(@b) + N' TO r_AdminMaster;';
    EXEC (@sql);
    FETCH NEXT FROM c INTO @b;
END
CLOSE c; DEALLOCATE c;

-- Hộp thư đi là của worker, không của người: chỉ đọc để theo dõi.
GRANT SELECT ON dbo.OutboxSuKien TO r_AdminMaster;
DENY INSERT, UPDATE, DELETE ON dbo.OutboxSuKien TO r_AdminMaster;
PRINT '  [+] r_AdminMaster: R/W danh muc + danh ba tai Master';
GO

SET NOEXEC OFF;
GO
