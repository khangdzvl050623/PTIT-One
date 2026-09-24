/* PTIT One — go LOCAL Distributor, buoc CUOI sau subscription/publication.
   Chay: .\run.ps1 -Script replication\39-go-distributor.sql -On MASTER
   Xoa database distribution va job replication, KHONG xoa DB nghiep vu.
   Tu choi neu con Publisher/DB replication khac. Khong force/no_checks=1.
   Chay lai duoc, ke ca lan truoc dung giua cac buoc. */
:ON ERROR EXIT
SET NOCOUNT ON;
IF N'$(SiteTarget)' <> N'MASTER' OR DB_NAME() <> N'$(DbMaster)'
    THROW 51000, N'Chi chay tren DbMaster voi -On MASTER.', 1;
USE master;

IF NOT EXISTS (SELECT 1 FROM sys.servers WHERE is_distributor = 1)
BEGIN
    PRINT '  [=] Khong con Distributor.';
    RETURN;
END;
DECLARE @distributor sysname, @probeResult int;
EXEC @probeResult = sys.sp_helpdistributor @distributor = @distributor OUTPUT;
IF @probeResult <> 0 OR @distributor IS NULL OR @distributor <> @@SERVERNAME
    THROW 51007, N'Script chi ho tro local Distributor theo topology hien tai.', 1;
IF EXISTS (SELECT 1 FROM sys.databases WHERE is_published = 1 OR is_merge_published = 1)
    THROW 51008, N'Con database publish/merge publish. Go publication truoc.', 1;
IF EXISTS (SELECT 1 FROM msdb.dbo.MSdistpublishers WHERE name <> @@SERVERNAME)
    THROW 51009, N'Distributor dang phuc vu Publisher khac; khong tu dong go.', 1;
IF EXISTS (SELECT 1 FROM msdb.dbo.MSdistributiondbs WHERE name <> N'distribution')
    THROW 51010, N'Co distribution database ngoai cau hinh; khong tu dong go.', 1;

DECLARE @result int, @publisher sysname = @@SERVERNAME;
IF EXISTS (SELECT 1 FROM msdb.dbo.MSdistpublishers WHERE name = @publisher)
BEGIN
    EXEC @result = sys.sp_dropdistpublisher @publisher = @publisher, @no_checks = 0;
    IF @result <> 0 THROW 51011, N'Go Publisher khoi Distributor that bai.', 1;
END;
IF EXISTS (SELECT 1 FROM msdb.dbo.MSdistributiondbs WHERE name = N'distribution')
BEGIN
    EXEC @result = sys.sp_dropdistributiondb @database = N'distribution';
    IF @result <> 0 THROW 51012, N'Go distribution database that bai.', 1;
END;
EXEC @result = sys.sp_dropdistributor @no_checks = 0, @ignore_distributor = 0;
IF @result <> 0 THROW 51013, N'Go Distributor that bai.', 1;
IF EXISTS (SELECT 1 FROM sys.servers WHERE is_distributor = 1)
    THROW 51014, N'Distributor van con sau khi go.', 1;
PRINT '  [ok] Da go local Distributor. Co the dung lai tu 30 -> 31 -> 32.';
GO
