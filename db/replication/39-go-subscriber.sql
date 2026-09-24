/* PTIT One — don metadata PUSH subscription tai Subscriber, GIU bang/du lieu.
   Chay SAU 39-go-subscription.sql, tren moi site voi -On HCM / HN / DN.
   Khong chay khi Distribution Agent van con day du lieu tu Publisher. */
:ON ERROR EXIT
SET NOCOUNT ON;
IF N'$(SiteTarget)' NOT IN (N'HCM', N'HN', N'DN')
   OR DB_NAME() <> N'$(DbTarget)' OR DB_NAME() = N'$(DbMaster)'
    THROW 51000, N'Chi chay tren Subscriber voi -On HCM/HN/DN.', 1;

IF OBJECT_ID(N'dbo.MSreplication_subscriptions', N'U') IS NULL
BEGIN
    PRINT '  [=] Khong con subscription metadata.';
    RETURN;
END;

DECLARE @result int;
IF EXISTS (SELECT 1 FROM dbo.MSreplication_subscriptions
    WHERE publisher = N'$(SrvMaster)' AND publisher_db = N'$(DbMaster)'
      AND publication = N'$(PublicationName)')
BEGIN
    EXEC @result = sys.sp_subscription_cleanup
        @publisher = N'$(SrvMaster)', @publisher_db = N'$(DbMaster)',
        @publication = N'$(PublicationName)';
    IF @result <> 0 THROW 51003, N'Don metadata Subscriber that bai.', 1;
END;
PRINT '  [ok] Da don metadata subscription duoc chi dinh; giu nguyen bang va du lieu.';
GO
