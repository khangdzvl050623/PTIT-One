/* =====================================================================
   PTIT One — tests/93-do-tre-nhan-ban.sql             (yêu cầu 2)
   ---------------------------------------------------------------------
   CHẠY:  .\run.ps1 -Script tests\93-do-tre-nhan-ban.sql -On MASTER

   Đo trễ nhân bản Master → mọi Subscriber bằng tracer token.

   Khác 90-demo-nhan-ban.sql: file đó để NGƯỜI chạy từng khối trong SSMS
   và nhìn tận mắt, nên không cho kết luận tự động. File này chạy không
   cần người, in một dòng PASS/FAIL — dùng cho Chay-Bang-Chung.ps1.

   Tracer token là cơ chế có sẵn của Transactional Replication: nó đi
   đúng đường dữ liệu thật đi, nên con số nó cho là trễ THẬT, không phải
   trễ của một phép thử mô phỏng.
   ===================================================================== */

SET NOCOUNT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    RAISERROR(N'Chi chay tren $(DbMaster).', 16, 1);
    SET NOEXEC ON;
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.syspublications WHERE name = N'$(PublicationName)')
BEGIN
    RAISERROR(N'Chua co publication $(PublicationName). Chay replication\31-publication.sql truoc.', 16, 1);
    SET NOEXEC ON;
END
GO

DECLARE @HanCho INT = 60;          -- giây, trần chờ token về
DECLARE @id INT, @cho INT = 0, @soSub INT;

/* Transactional replication dung dbo.syspublications / dbo.syssubscriptions.
   `sys.publications` la view cua MERGE replication — tra "Invalid object name"
   tren mot Publisher transactional hoan toan binh thuong. */
SELECT @soSub = COUNT(DISTINCT s.srvid)
  FROM dbo.syssubscriptions s
  JOIN dbo.sysarticles a ON a.artid = s.artid
  JOIN dbo.syspublications p ON p.pubid = a.pubid
 WHERE p.name = N'$(PublicationName)';

IF @soSub = 0
BEGIN
    RAISERROR(N'Publication chua co subscription nao. Chay replication\32-subscription.sql truoc.', 16, 1);
    RETURN;
END

EXEC sys.sp_posttracertoken @publication = N'$(PublicationName)',
     @tracer_token_id = @id OUTPUT;

/* Shape phai khop CHINH XAC voi sp_helptracertokenhistory, vi INSERT..EXEC
   khong tu bo cot: distributor_latency, subscriber, subscriber_db,
   subscriber_latency, overall_latency. */
DECLARE @ls TABLE (
    distributor_latency INT NULL,
    subscriber          SYSNAME NULL,
    subscriber_db       SYSNAME NULL,
    subscriber_latency  INT NULL,
    overall_latency     INT NULL);

/* Chờ tới khi MỌI subscriber báo về, hoặc hết hạn chờ. Dừng sớm ở
   subscriber đầu tiên là tự lừa mình: một site chậm mới là thứ cần thấy. */
WHILE @cho < @HanCho
BEGIN
    DELETE @ls;
    INSERT @ls
    EXEC sys.sp_helptracertokenhistory @publication = N'$(PublicationName)',
         @tracer_id = @id;

    IF (SELECT COUNT(*) FROM @ls WHERE overall_latency IS NOT NULL) >= @soSub
        BREAK;

    WAITFOR DELAY '00:00:02';
    SET @cho += 2;
END

PRINT '';
PRINT '=== Tre nhan ban, publication $(PublicationName) ===';

SELECT subscriber                   AS [Subscriber],
       subscriber_db                AS [Database],
       distributor_latency          AS [Publisher->Dist (s)],
       subscriber_latency           AS [Dist->Sub (s)],
       overall_latency              AS [Tong (s)]
  FROM @ls
 ORDER BY subscriber, subscriber_db;

/* DECLARE khong nhan subquery lam gia tri khoi tao, va PRINT cung khong
   nhan subquery trong bieu thuc — gan bang SELECT truoc. */
DECLARE @ve INT, @treMax INT;
SELECT @ve = COUNT(*), @treMax = MAX(overall_latency)
  FROM @ls WHERE overall_latency IS NOT NULL;

PRINT '';
IF @ve < @soSub
BEGIN
    PRINT '  NHAN BAN: FAIL — ' + CAST(@ve AS VARCHAR(10)) + '/'
          + CAST(@soSub AS VARCHAR(10)) + ' subscriber bao ve sau '
          + CAST(@HanCho AS VARCHAR(10)) + ' giay.';
    PRINT '  Kiem Distribution Agent va trigger thieu NOT FOR REPLICATION.';
    THROW 51930, N'NHAN BAN: FAIL. Token khong ve du subscriber.', 1;
END

PRINT '  NHAN BAN: PASS — ' + CAST(@soSub AS VARCHAR(10))
      + ' subscriber, tre cao nhat ' + CAST(@treMax AS VARCHAR(10)) + ' giay.';
GO

SET NOEXEC OFF;
GO
