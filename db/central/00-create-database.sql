-- T1/T2: run through central/run.ps1 with an administrator Windows account.
-- Creates an EMPTY database only. Existing databases are checked, never reset.
:ON ERROR EXIT
SET NOCOUNT ON;

IF DB_NAME() <> N'master'
    THROW 51000, N'Bootstrap must connect to the system master database.', 1;

DECLARE @database nvarchar(128) = N'$(CentralDatabase)';
IF (@database <> N'PTITONE_CENTRAL' AND @database NOT LIKE N'PTITONE[_]CENTRAL[_]%')
    OR @database COLLATE Latin1_General_100_BIN2 LIKE N'%[^A-Z0-9_]%'
    THROW 51001, N'Only PTITONE_CENTRAL and its suffixed development databases are allowed.', 1;

IF DB_ID(@database) IS NULL
BEGIN
    DECLARE @sql nvarchar(max) = N'CREATE DATABASE ' + QUOTENAME(@database)
        + N' COLLATE Vietnamese_CI_AS;';
    EXEC sys.sp_executesql @sql;
    -- New DB only. NO_WAIT fails rather than disconnecting other sessions.
    SET @sql = N'ALTER DATABASE ' + QUOTENAME(@database)
        + N' SET READ_COMMITTED_SNAPSHOT ON WITH NO_WAIT;';
    EXEC sys.sp_executesql @sql;
    SET @sql = N'ALTER DATABASE ' + QUOTENAME(@database) + N' SET RECOVERY SIMPLE;';
    EXEC sys.sp_executesql @sql;
    PRINT N'Created empty CENTRAL database. Business schema and seed are still pending.';
END
ELSE
    PRINT N'Database already exists. Checking configuration without modifying it.';

IF NOT EXISTS (
    SELECT 1 FROM sys.databases
    WHERE name = @database AND state_desc = N'ONLINE'
        AND collation_name = N'Vietnamese_CI_AS'
        AND is_read_committed_snapshot_on = 1
        AND recovery_model_desc = N'SIMPLE'
)
    THROW 51002, N'Unexpected DB settings. T1 must inspect state/collation/RCSI/recovery; no automatic reset.', 1;

SELECT @@SERVERNAME AS SqlServer, name AS DatabaseName, state_desc,
       collation_name, is_read_committed_snapshot_on, recovery_model_desc
FROM sys.databases WHERE name = @database;
GO
