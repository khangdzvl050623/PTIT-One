-- Read-only environment check. This is NOT a business schema acceptance test.
:ON ERROR EXIT
SET NOCOUNT ON;
IF DB_NAME() <> N'$(CentralDatabase)'
    THROW 51010, N'Wrong database connection.', 1;
IF DB_NAME() <> N'PTITONE_CENTRAL' AND DB_NAME() NOT LIKE N'PTITONE[_]CENTRAL[_]%'
    THROW 51011, N'Expected a CENTRAL database.', 1;

IF NOT EXISTS (
    SELECT 1 FROM sys.databases
    WHERE database_id = DB_ID() AND state_desc = N'ONLINE'
        AND collation_name = N'Vietnamese_CI_AS'
        AND is_read_committed_snapshot_on = 1
        AND recovery_model_desc = N'SIMPLE'
)
    THROW 51012, N'Unexpected state/collation/RCSI/recovery. Ask T1 to inspect.', 1;

SELECT @@SERVERNAME AS SqlServer, DB_NAME() AS DatabaseName,
       ORIGINAL_LOGIN() AS LoginName, USER_NAME() AS DatabaseUser;
SELECT name, state_desc, collation_name, is_read_committed_snapshot_on,
       recovery_model_desc FROM sys.databases WHERE database_id = DB_ID();
SELECT COUNT(*) AS UserTableCount FROM sys.tables WHERE is_ms_shipped = 0;
PRINT N'Environment check finished. Schema, seed, app permissions and business tests require separate acceptance.';
GO
