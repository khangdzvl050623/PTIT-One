/* =====================================================================
   PTIT One — replication/20-linked-server.sql
   ---------------------------------------------------------------------
   Tạo Linked Server từ Master tới các cơ sở ở xa, và ghi tên link vào
   `dbo.CoSo` để bốn báo cáo phân tán biết đường đi.

   CHẠY:  .\run.ps1 -Script replication\20-linked-server.sql -On MASTER

   Linked Server là cơ chế SQL Server → SQL Server. KHÔNG có endpoint,
   KHÔNG qua HTTP, và tầng ứng dụng không đụng tới nó: `apps/api` nối
   thẳng bốn DataSource qua JDBC. Linked Server chỉ phục vụ các truy vấn
   GOM TỪ NHIỀU SITE chạy bên trong SQL Server (master\07, master\08,
   tests\92).

   ⚠️ `rpc out` BẮT BUỘC bật. Thiếu nó thì `EXEC (@q, …) AT [SRV_HN]`
      hỏng, trong khi `OPENQUERY` vẫn chạy — nên lỗi chỉ lộ ra ở đúng
      những báo cáo CÓ tham số.

   ⚠️ Cơ sở nằm NGAY TRÊN instance này thì KHÔNG tạo link: `TenLinkedServer`
      để NULL, và báo cáo tự chạy cục bộ. Tạo link trỏ về chính mình là
      thêm một vòng mạng cho dữ liệu vốn nằm sẵn ở đây.
   ===================================================================== */

SET NOCOUNT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    RAISERROR(N'Chi chay tren $(DbMaster).', 16, 1);
    SET NOEXEC ON;
END
GO

DECLARE @site TABLE (MaCoSo VARCHAR(10), Lnk SYSNAME, Srv NVARCHAR(200),
                     Db SYSNAME, Dn SYSNAME NULL, Mk NVARCHAR(200) NULL);

INSERT INTO @site (MaCoSo, Lnk, Srv, Db, Dn, Mk) VALUES
    ('HCM', N'$(LnkHCM)', N'$(SrvHCM)', N'$(DbHCM)',
            NULLIF(N'$(SubLoginHCM)', N''), NULLIF(N'$(SubPwdHCM)', N'')),
    ('HN',  N'$(LnkHN)',  N'$(SrvHN)',  N'$(DbHN)',
            NULLIF(N'$(SubLoginHN)',  N''), NULLIF(N'$(SubPwdHN)',  N'')),
    ('DN',  N'$(LnkDN)',  N'$(SrvDN)',  N'$(DbDN)',
            NULLIF(N'$(SubLoginDN)',  N''), NULLIF(N'$(SubPwdDN)',  N''));

DECLARE @ma VARCHAR(10), @lnk SYSNAME, @srv NVARCHAR(200), @db SYSNAME,
        @dn SYSNAME, @mk NVARCHAR(200), @sql NVARCHAR(MAX);

DECLARE c CURSOR LOCAL FAST_FORWARD FOR
    SELECT MaCoSo, Lnk, Srv, Db, Dn, Mk FROM @site ORDER BY MaCoSo;
OPEN c;
FETCH NEXT FROM c INTO @ma, @lnk, @srv, @db, @dn, @mk;

WHILE @@FETCH_STATUS = 0
BEGIN
    IF @srv IS NULL OR @srv = N'' OR @lnk IS NULL OR @lnk = N''
    BEGIN
        PRINT '  [-] ' + @ma + ': chua khai server, bo qua';
        FETCH NEXT FROM c INTO @ma, @lnk, @srv, @db, @dn, @mk;
        CONTINUE;
    END

    /* Cơ sở cục bộ: không link, để NULL cho báo cáo tự nhận ra. */
    IF UPPER(@srv) = UPPER(CONVERT(NVARCHAR(200), @@SERVERNAME))
    BEGIN
        UPDATE dbo.CoSo SET TenLinkedServer = NULL, TenDatabase = @db WHERE MaCoSo = @ma;
        PRINT '  [=] ' + @ma + ': cuc bo tren instance nay, khong tao link';
        FETCH NEXT FROM c INTO @ma, @lnk, @srv, @db, @dn, @mk;
        CONTINUE;
    END

    /* Dựng lại từ đầu cho chắc: đổi mật khẩu hay đổi máy thì link cũ sai
       mà không báo gì cho tới lúc chạy báo cáo. */
    IF EXISTS (SELECT 1 FROM sys.servers WHERE name = @lnk AND is_linked = 1)
        EXEC sp_dropserver @server = @lnk, @droplogins = 'droplogins';

    EXEC sp_addlinkedserver @server = @lnk, @srvproduct = N'',
         @provider = N'MSOLEDBSQL', @datasrc = @srv;

    IF @dn IS NULL
        /* Dùng chính danh tính Windows của phiên đang chạy. */
        EXEC sp_addlinkedsrvlogin @rmtsrvname = @lnk, @useself = 'TRUE';
    ELSE
        EXEC sp_addlinkedsrvlogin @rmtsrvname = @lnk, @useself = 'FALSE',
             @locallogin = NULL, @rmtuser = @dn, @rmtpassword = @mk;

    EXEC sp_serveroption @server = @lnk, @optname = 'rpc out',      @optvalue = 'true';
    EXEC sp_serveroption @server = @lnk, @optname = 'data access',  @optvalue = 'true';
    /* Chỉ đọc qua link này — giao dịch phân tán dùng đường riêng (master\06). */
    EXEC sp_serveroption @server = @lnk, @optname = 'remote proc transaction promotion',
         @optvalue = 'false';

    UPDATE dbo.CoSo SET TenLinkedServer = @lnk, TenDatabase = @db WHERE MaCoSo = @ma;
    PRINT '  [+] ' + @ma + ': ' + @lnk + ' -> ' + @srv;

    FETCH NEXT FROM c INTO @ma, @lnk, @srv, @db, @dn, @mk;
END
CLOSE c; DEALLOCATE c;
GO

/* =====================================================================
   KIỂM CHỨNG — mỗi link phải NỐI ĐƯỢC và `rpc out` phải bật
   ===================================================================== */
DECLARE @lnk SYSNAME, @db SYSNAME, @sql NVARCHAR(MAX), @loi INT = 0;

DECLARE k CURSOR LOCAL FAST_FORWARD FOR
    SELECT TenLinkedServer, TenDatabase FROM dbo.CoSo
     WHERE DangHoatDong = 1 AND TenLinkedServer IS NOT NULL;
OPEN k;
FETCH NEXT FROM k INTO @lnk, @db;

WHILE @@FETCH_STATUS = 0
BEGIN
    BEGIN TRY
        /* `EXEC … AT` kiểm luôn `rpc out`; OPENQUERY thi khong. */
        SET @sql = N'EXEC (N''SELECT 1'') AT ' + QUOTENAME(@lnk) + N';';
        EXEC sp_executesql @sql;

        IF NOT EXISTS (SELECT 1 FROM sys.servers s
                        JOIN sys.linked_logins l ON l.server_id = s.server_id
                       WHERE s.name = @lnk)
            RAISERROR(N'Chua co anh xa dang nhap.', 16, 1);

        PRINT '  [ok] ' + @lnk + ': noi duoc, rpc out bat';
    END TRY
    BEGIN CATCH
        PRINT '  [!!] ' + @lnk + ': ' + ERROR_MESSAGE();
        SET @loi += 1;
    END CATCH
    FETCH NEXT FROM k INTO @lnk, @db;
END
CLOSE k; DEALLOCATE k;

PRINT '';
IF @loi > 0
    THROW 53300, N'Co Linked Server khong noi duoc. Bao cao phan tan se bo qua co so do.', 1;

PRINT '  LINKED SERVER: PASS';
PRINT '  Buoc tiep theo: master\07-bao-cao-phan-tan.sql';
GO

SET NOEXEC OFF;
GO
