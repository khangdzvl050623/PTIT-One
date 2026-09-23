/* =====================================================================
   PTIT One — 00-create-databases.sql
   ---------------------------------------------------------------------
   Tạo các database theo topology đã chốt ở mục C0 của tài liệu thiết kế.

   CHẠY Ở ĐÂU : trên MỖI MÁY CHỦ, đúng MỘT lần
                  .\run.ps1 -Script 00-create-databases.sql -On MASTER
                  .\run.ps1 -Script 00-create-databases.sql -On HN
                  .\run.ps1 -Script 00-create-databases.sql -On DN

   Mỗi lần chạy tạo TẤT CẢ database mà config.ps1 gán cho máy đó.
   Với phương án 3 máy, lần chạy -On MASTER tạo cả PTITONE_MASTER lẫn
   PTITONE_HCM vì hai database này cùng nằm trên SRV-HCM.

   CHẠY LẠI ĐƯỢC: có, mọi thao tác đều được bọc IF NOT EXISTS.

   ⚠️ Tên database và tên máy đến từ db/config.ps1, KHÔNG hardcode ở đây.
   ===================================================================== */

SET NOCOUNT ON;
GO

/* ===================================================================
   1. TẠO MỌI DATABASE THUỘC INSTANCE NÀY

   $(DbList) do run.ps1 tính sẵn: tất cả database mà config.ps1 gán cho
   đúng máy chủ đang kết nối.

   Vì sao phân giải ở PowerShell chứ không so tên máy trong T-SQL:
     - Named instance: @@SERVERNAME trả về 'MAY\SITE_HN' còn cấu hình có
       thể ghi '.\SITE_HN' — so chuỗi sẽ sai
     - So khớp chuỗi con dễ nhầm 'SRV-HN' với 'SRV-HN2'
     - Alias và CNAME làm mọi phép so tên trở nên không đáng tin

   Nhờ vậy CHẠY MỘT LẦN trên mỗi máy là tạo đủ. Máy vừa giữ Master vừa
   giữ HCM sẽ tạo cả hai, không cần chạy thêm lần thứ hai.
   =================================================================== */
DECLARE @dbList NVARCHAR(1000) = N'$(DbList)';
DECLARE @db     SYSNAME,
        @sql    NVARCHAR(MAX);

IF NULLIF(LTRIM(RTRIM(@dbList)), N'') IS NULL
BEGIN
    RAISERROR(N'Bien DbList rong. Hay chay qua run.ps1, dung goi sqlcmd truc tiep.', 16, 1);
    SET NOEXEC ON;
END

DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT LTRIM(RTRIM(value)) FROM STRING_SPLIT(@dbList, ',')
     WHERE LTRIM(RTRIM(value)) <> N'';

OPEN cur;
FETCH NEXT FROM cur INTO @db;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF DB_ID(@db) IS NULL
    BEGIN
        SET @sql = N'CREATE DATABASE ' + QUOTENAME(@db)
                 + N' COLLATE $(Collation);';
        EXEC sp_executesql @sql;
        PRINT '  [+] Da tao database ' + @db;
    END
    ELSE
        PRINT '  [=] Database ' + @db + ' da ton tai, bo qua';

    FETCH NEXT FROM cur INTO @db;
END
CLOSE cur;
DEALLOCATE cur;
GO

/* ===================================================================
   3. TUỲ CHỌN BẮT BUỘC cho mọi database của dự án
   =================================================================== */
DECLARE @db  SYSNAME,
        @sql NVARCHAR(MAX);

DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT d.name
      FROM sys.databases d
      JOIN STRING_SPLIT(N'$(DbList)', ',') sp
        ON d.name = LTRIM(RTRIM(sp.value))
     WHERE d.state_desc = 'ONLINE';

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

SET NOEXEC OFF;
GO
