/* =====================================================================
   PTIT One — 00-create-databases.sql
   ---------------------------------------------------------------------
   Tạo các database theo topology đã chốt ở mục C0 của tài liệu thiết kế.

   CHẠY Ở ĐÂU : trên MỖI máy chủ, một lần
                  .\run.ps1 -Script 00-create-databases.sql -On MASTER
                  .\run.ps1 -Script 00-create-databases.sql -On HN
                  .\run.ps1 -Script 00-create-databases.sql -On DN

   Script chỉ tạo những database THUỘC VỀ máy đang chạy, nên chạy nhầm
   máy cũng không sinh ra database thừa.

   CHẠY LẠI ĐƯỢC: có, mọi thao tác đều được bọc IF NOT EXISTS.

   ⚠️ Tên database và tên máy đến từ db/config.ps1, KHÔNG hardcode ở đây.
   ===================================================================== */

SET NOCOUNT ON;
GO

/* ---------------------------------------------------------------------
   Thủ tục cục bộ: tạo một database nếu chưa có, rồi đặt các tuỳ chọn
   bắt buộc của dự án.
   --------------------------------------------------------------------- */
DECLARE @sql NVARCHAR(MAX);

/* ===================================================================
   1. DATABASE VAI TRÒ MASTER
      Chỉ chứa 7 bảng tham chiếu + danh bạ định vị + tài khoản Admin Master.
      TUYỆT ĐỐI không chứa dữ liệu vận hành (xem C0).
      Chỉ tạo khi máy đang chạy chính là máy Master.
   =================================================================== */
IF UPPER(@@SERVERNAME) = UPPER('$(SrvMaster)')
   OR CHARINDEX(UPPER('$(SrvMaster)'), UPPER(@@SERVERNAME)) > 0
BEGIN
    IF DB_ID(N'$(DbMaster)') IS NULL
    BEGIN
        SET @sql = N'CREATE DATABASE [$(DbMaster)] COLLATE $(Collation);';
        EXEC sp_executesql @sql;
        PRINT '  [+] Da tao database $(DbMaster)';
    END
    ELSE
        PRINT '  [=] Database $(DbMaster) da ton tai, bo qua';
END
ELSE
    PRINT '  [ ] May nay khong phai Master ($(SrvMaster)), bo qua $(DbMaster)';
GO

/* ===================================================================
   2. DATABASE VẬN HÀNH của cơ sở tương ứng máy đang chạy
      Mỗi máy chỉ tạo mảnh của chính nó.
   =================================================================== */
DECLARE @sql NVARCHAR(MAX);

IF DB_ID(N'$(DbTarget)') IS NULL
BEGIN
    SET @sql = N'CREATE DATABASE [$(DbTarget)] COLLATE $(Collation);';
    EXEC sp_executesql @sql;
    PRINT '  [+] Da tao database $(DbTarget)  (co so $(SiteTarget))';
END
ELSE
    PRINT '  [=] Database $(DbTarget) da ton tai, bo qua';
GO

/* ===================================================================
   3. TUỲ CHỌN BẮT BUỘC cho mọi database của dự án
   =================================================================== */
DECLARE @db  SYSNAME,
        @sql NVARCHAR(MAX);

DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT name FROM sys.databases
     WHERE name IN (N'$(DbMaster)', N'$(DbTarget)')
       AND state_desc = 'ONLINE';

OPEN cur;
FETCH NEXT FROM cur INTO @db;
WHILE @@FETCH_STATUS = 0
BEGIN
    /* READ_COMMITTED_SNAPSHOT — quyết định D11.
       Người xem "còn mấy chỗ" không bị chặn bởi người đang đăng ký.
       Vẫn đúng đắn vì câu UPDATE có điều kiện đọc lại dưới lock. */
    IF EXISTS (SELECT 1 FROM sys.databases
                WHERE name = @db AND is_read_committed_snapshot_on = 0)
    BEGIN
        SET @sql = N'ALTER DATABASE ' + QUOTENAME(@db)
                 + N' SET READ_COMMITTED_SNAPSHOT ON WITH ROLLBACK IMMEDIATE;';
        EXEC sp_executesql @sql;
        PRINT '  [+] ' + @db + ': bat READ_COMMITTED_SNAPSHOT';
    END

    /* SIMPLE recovery — môi trường lab, không sao lưu log.
       Transactional Replication vẫn hoạt động: Log Reader giữ lại các bản ghi
       log chưa xử lý, nên log không bị cắt mất trước khi nhân bản xong. */
    IF EXISTS (SELECT 1 FROM sys.databases
                WHERE name = @db AND recovery_model_desc <> 'SIMPLE')
    BEGIN
        SET @sql = N'ALTER DATABASE ' + QUOTENAME(@db)
                 + N' SET RECOVERY SIMPLE;';
        EXEC sp_executesql @sql;
        PRINT '  [+] ' + @db + ': recovery model = SIMPLE';
    END

    /* Tự động cập nhật thống kê — cần cho benchmark truy vấn phân tán,
       vì optimizer dựa vào thống kê để quyết định đẩy predicate xuống remote. */
    SET @sql = N'ALTER DATABASE ' + QUOTENAME(@db)
             + N' SET AUTO_UPDATE_STATISTICS ON;';
    EXEC sp_executesql @sql;

    FETCH NEXT FROM cur INTO @db;
END
CLOSE cur;
DEALLOCATE cur;
GO

/* ===================================================================
   4. TÓM TẮT
   =================================================================== */
PRINT '';
PRINT '  ------------------------------------------------------------';
PRINT '  Database hien co tren may nay:';
GO

SELECT  name                        AS [Database],
        collation_name              AS [Collation],
        recovery_model_desc         AS [Recovery],
        is_read_committed_snapshot_on AS [RCSI],
        state_desc                  AS [TrangThai]
  FROM  sys.databases
 WHERE  name LIKE 'PTITONE[_]%'
 ORDER BY name;
GO

PRINT '  Buoc tiep theo:';
PRINT '    - Tren may Master : replication\30-distributor.sql';
PRINT '    - Cho toan bo      : xem db/replication/README.md';
PRINT '  ------------------------------------------------------------';
GO
