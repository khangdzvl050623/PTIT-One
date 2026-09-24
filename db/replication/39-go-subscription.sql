/* PTIT One — go PUSH subscription tai Publisher, giu nguyen du lieu.
   Chay: .\run.ps1 -Script replication\39-go-subscription.sql -On MASTER
   Sau do chay 39-go-subscriber.sql tren TUNG site de don metadata con lai.
   Chi go 3 dich trong config va dung PublicationName; khong dung subscriber=all.
   Chay lai duoc. Khong boc cac thu tuc replication trong user transaction. */
:ON ERROR EXIT
SET NOCOUNT ON;
IF N'$(SiteTarget)' <> N'MASTER' OR DB_NAME() <> N'$(DbMaster)'
    THROW 51000, N'Chi chay tren DbMaster voi -On MASTER.', 1;

IF OBJECT_ID(N'dbo.syspublications') IS NULL
BEGIN
    PRINT '  [=] Khong con publication metadata.';
    RETURN;
END;

DECLARE @targets TABLE (OrderNo int, SiteCode varchar(10), ServerName sysname, DatabaseName sysname);
INSERT @targets VALUES
    (1, 'HCM', N'$(SrvHCM)', N'$(DbHCM)'),
    (2, 'HN', N'$(SrvHN)', N'$(DbHN)'),
    (3, 'DN', N'$(SrvDN)', N'$(DbDN)');

/* Gỡ đúng những site được chọn (run.ps1 -Subscribers). Mặc định cả ba.
   @targets giữ NGUYÊN cả ba dòng — bước kiểm chứng cuối script cần biết
   đâu là đích hợp lệ theo config, kể cả đích không gỡ lần này. */
DECLARE @chon TABLE (SiteCode varchar(10) PRIMARY KEY);
INSERT @chon SELECT LTRIM(RTRIM(value)) FROM STRING_SPLIT(N'$(SubList)', ',')
 WHERE LTRIM(RTRIM(value)) <> N'';
IF NOT EXISTS (SELECT 1 FROM @targets t JOIN @chon c ON c.SiteCode = t.SiteCode)
    THROW 51015, N'Danh sach Subscriber rong. Kiem tra tham so -Subscribers cua run.ps1.', 1;

DECLARE @server sysname, @database sysname, @result int;
DECLARE subscriptions CURSOR LOCAL FAST_FORWARD FOR
    SELECT t.ServerName, t.DatabaseName FROM @targets t
      JOIN @chon c ON c.SiteCode = t.SiteCode
     ORDER BY t.OrderNo;
OPEN subscriptions;
FETCH NEXT FROM subscriptions INTO @server, @database;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF EXISTS (
        SELECT 1 FROM dbo.syssubscriptions s
        JOIN dbo.sysarticles a ON a.artid = s.artid
        JOIN dbo.syspublications p ON p.pubid = a.pubid
        JOIN master.sys.servers v ON v.server_id = s.srvid
        WHERE p.name = N'$(PublicationName)'
          AND v.name = @server AND s.dest_db = @database)
    BEGIN
        EXEC @result = sys.sp_dropsubscription
            @publication = N'$(PublicationName)', @article = N'all',
            @subscriber = @server, @destination_db = @database;
        IF @result <> 0 THROW 51001, N'Go subscription that bai.', 1;
        PRINT N'  [-] ' + @server + N'.' + @database;
    END
    ELSE PRINT N'  [=] Khong con subscription: ' + @server + N'.' + @database;
    FETCH NEXT FROM subscriptions INTO @server, @database;
END;
CLOSE subscriptions;
DEALLOCATE subscriptions;

/* Còn subscription NGOÀI ba đích trong config → dừng lại, vì gỡ publication
   sau đó sẽ tác động tới một site nhóm không biết là site nào. */
IF EXISTS (
    SELECT 1 FROM dbo.syssubscriptions s
    JOIN dbo.sysarticles a ON a.artid = s.artid
    JOIN dbo.syspublications p ON p.pubid = a.pubid
    JOIN master.sys.servers v ON v.server_id = s.srvid
    WHERE p.name = N'$(PublicationName)'
      AND NOT EXISTS (SELECT 1 FROM @targets t
                       WHERE t.ServerName = v.name AND t.DatabaseName = s.dest_db))
    THROW 51002, N'Publication con subscription ngoai config. Kiem tra topology truoc khi go publication.', 1;

/* Còn đích thuộc config nhưng lần này không chọn gỡ: hợp lệ, chỉ nhắc. */
IF EXISTS (SELECT 1 FROM dbo.syssubscriptions s
    JOIN dbo.sysarticles a ON a.artid = s.artid
    JOIN dbo.syspublications p ON p.pubid = a.pubid
    WHERE p.name = N'$(PublicationName)')
    PRINT '  [!] Van con subscription cua site KHONG chon lan nay. Chua go duoc publication.';
ELSE
    PRINT '  [ok] Khong con subscription cua publication. Don metadata tai tung Subscriber.';
GO
