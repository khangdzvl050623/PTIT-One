/* =====================================================================
   PTIT One — site/13-bao-ve-subscriber.sql
   ---------------------------------------------------------------------
   Làm cho "nhân bản MỘT chiều" đúng trong thực tế, không chỉ trên giấy.

   CHẠY:  .\run.ps1 -Script site\13-bao-ve-subscriber.sql -On HCM
          (lặp lại với -On HN, -On DN)

   HAI LỚP, theo đúng thứ tự ưu tiên của thiết kế:
     1. DENY INSERT/UPDATE/DELETE cho tài khoản ỨNG DỤNG  ← lớp chính
     2. Trigger NOT FOR REPLICATION                        ← lớp phụ

   ⚠️ DENY KHÔNG chặn Distribution Agent: agent nối bằng `ptitone_admin`
      (sysadmin), mà sysadmin bỏ qua mọi phép kiểm quyền. Nếu ở máy bạn
      agent nối bằng chính tài khoản ứng dụng thì ĐỪNG chạy script này —
      sửa cấu hình subscription trước.

   ⚠️ Trigger BẮT BUỘC có NOT FOR REPLICATION. Thiếu nó thì trigger chặn
      chính Distribution Agent và replication chết với triệu chứng không
      liên quan gì tới trigger.
   ===================================================================== */

SET NOCOUNT ON;
GO

IF DB_NAME() = N'$(DbMaster)' OR DB_NAME() <> N'$(DbTarget)'
BEGIN
    RAISERROR(N'Chi chay tren Subscriber (-On HCM/HN/DN).', 16, 1);
    SET NOEXEC ON;
END
GO

DECLARE @app SYSNAME = N'$(AppUser)';
IF @app IS NULL OR @app = N'' SET @app = N'ptitone_api';

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = @app)
BEGIN
    RAISERROR(N'Khong co user %s trong database nay. Tao user ung dung truoc.', 16, 1, @app);
    RETURN;
END

DECLARE @bang TABLE (Ten SYSNAME PRIMARY KEY);
INSERT INTO @bang (Ten) VALUES
    (N'CoSo'), (N'Khoa'), (N'MonHoc'), (N'MonHocTienQuyet'),
    (N'ChuongTrinhDaoTao'), (N'CTDT_MonHoc'), (N'HocKy'), (N'KhungGioTiet'),
    (N'DanhBaNguoiDung');

DECLARE @ten SYSNAME, @sql NVARCHAR(MAX), @soBang INT = 0, @soTrigger INT = 0;

DECLARE b CURSOR LOCAL FAST_FORWARD FOR SELECT Ten FROM @bang ORDER BY Ten;
OPEN b;
FETCH NEXT FROM b INTO @ten;

WHILE @@FETCH_STATUS = 0
BEGIN
    IF OBJECT_ID(N'dbo.' + QUOTENAME(@ten), N'U') IS NULL
    BEGIN
        PRINT '  [-] ' + @ten + ': khong co o database nay, bo qua';
        FETCH NEXT FROM b INTO @ten;
        CONTINUE;
    END

    /* --- Lớp 1: DENY --- */
    SET @sql = N'DENY INSERT, UPDATE, DELETE ON dbo.' + QUOTENAME(@ten)
             + N' TO ' + QUOTENAME(@app) + N';';
    EXEC sp_executesql @sql;
    SET @soBang += 1;

    /* --- Lớp 2: trigger bia đỡ ---
       Chỉ dựng nếu chưa có. Trigger này KHÔNG bao giờ chạy cho agent nhân
       bản nhờ NOT FOR REPLICATION, nên nó chỉ bắt các đường ghi bất ngờ:
       người chạy tay bằng tài khoản quản trị, hoặc một job quên mất đây là
       bản sao. */
    DECLARE @trg SYSNAME = N'TR_' + @ten + N'_ChiDoc';
    IF OBJECT_ID(N'dbo.' + QUOTENAME(@trg), N'TR') IS NULL
    BEGIN
        SET @sql = N'
CREATE TRIGGER dbo.' + QUOTENAME(@trg) + N'
ON dbo.' + QUOTENAME(@ten) + N'
FOR INSERT, UPDATE, DELETE
NOT FOR REPLICATION
AS
BEGIN
    SET NOCOUNT ON;
    THROW 53200, N''Bang nhan ban chi doc tai Subscriber. Sua o PTITONE_MASTER roi doi nhan ban.'', 1;
END';
        EXEC sp_executesql @sql;
        SET @soTrigger += 1;
    END

    FETCH NEXT FROM b INTO @ten;
END
CLOSE b; DEALLOCATE b;

PRINT '';
PRINT '  [+] DENY tren ' + CONVERT(VARCHAR(10), @soBang) + ' bang cho ' + @app;
PRINT '  [+] Them ' + CONVERT(VARCHAR(10), @soTrigger) + ' trigger NOT FOR REPLICATION';
GO

/* =====================================================================
   KIỂM CHỨNG — không có trigger nào thiếu NOT FOR REPLICATION
   ===================================================================== */
IF EXISTS (SELECT 1 FROM sys.triggers t
            WHERE t.name LIKE 'TR[_]%[_]ChiDoc' AND t.is_not_for_replication = 0)
BEGIN
    SELECT name AS [Trigger THIEU NOT FOR REPLICATION] FROM sys.triggers
     WHERE name LIKE 'TR[_]%[_]ChiDoc' AND is_not_for_replication = 0;
    THROW 53201, N'Co trigger thieu NOT FOR REPLICATION — no se chan Distribution Agent.', 1;
END

PRINT '  [ok] Moi trigger deu co NOT FOR REPLICATION';
GO

SET NOEXEC OFF;
GO
