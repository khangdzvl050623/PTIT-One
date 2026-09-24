/* PTIT One — go publication sau khi da go het subscription.
   Chay: .\run.ps1 -Script replication\39-go-publication.sql -On MASTER
   Giu bang nguon va du lieu; khong go publication khac. Chay lai duoc. */
:ON ERROR EXIT
SET NOCOUNT ON;
IF N'$(SiteTarget)' <> N'MASTER' OR DB_NAME() <> N'$(DbMaster)'
    THROW 51000, N'Chi chay tren DbMaster voi -On MASTER.', 1;

DECLARE @result int;
IF OBJECT_ID(N'dbo.syspublications') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.syssubscriptions s
        JOIN dbo.sysarticles a ON a.artid = s.artid
        JOIN dbo.syspublications p ON p.pubid = a.pubid
        WHERE p.name = N'$(PublicationName)')
        THROW 51004, N'Con subscription. Chay 39-go-subscription.sql truoc.', 1;

    IF EXISTS (SELECT 1 FROM dbo.syspublications WHERE name = N'$(PublicationName)')
    BEGIN
        EXEC @result = sys.sp_droppublication @publication = N'$(PublicationName)';
        IF @result <> 0 THROW 51005, N'Go publication that bai.', 1;
    END;
    IF EXISTS (SELECT 1 FROM dbo.syspublications)
    BEGIN
        PRINT '  [=] Con publication khac: giu publish va Distributor.';
        RETURN;
    END;
END;

IF EXISTS (SELECT 1 FROM sys.databases WHERE name = DB_NAME() AND is_published = 1)
BEGIN
    EXEC @result = sys.sp_replicationdboption @dbname = N'$(DbMaster)',
        @optname = N'publish', @value = N'false';
    IF @result <> 0 THROW 51006, N'Tat publish that bai.', 1;
END;
PRINT '  [ok] Da go publication va tat publish. Bang nghiep vu van con.';
GO
