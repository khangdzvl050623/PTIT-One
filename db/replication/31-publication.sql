/* =====================================================================
   PTIT One — replication/31-publication.sql
   ---------------------------------------------------------------------
   Dựng KHUNG publication cho dữ liệu tham chiếu.

   CHẠY Ở ĐÂU : CHỈ trên máy Master, sau khi 30-distributor.sql đã xong
                  .\run.ps1 -Script replication\31-publication.sql -On MASTER

   ĐIỀU KIỆN TRƯỚC KHI CHẠY: db/master/01..03 đã chạy xong, vì
   sp_addarticle tham chiếu trực tiếp tới bảng nguồn.

   Script tự kiểm từng bảng có tồn tại chưa trước khi khai báo article.

   CHẠY LẠI ĐƯỢC: có.

   ⚠️ Gỡ ra thì chạy 39-go-publication.sql, đừng xoá tay trong SSMS.
   ===================================================================== */

SET NOCOUNT ON;
GO

/* ---------------------------------------------------------------------
   0. CHẶN CHẠY NHẦM MÁY VÀ NHẦM DATABASE
   --------------------------------------------------------------------- */
/* Bat bien: may Master la may CO database Master.
   Khong so ten may — xem giai thich o 30-distributor.sql. */
IF DB_ID(N'$(DbMaster)') IS NULL
BEGIN
    RAISERROR(N'Khong tim thay $(DbMaster) tren instance nay (%s). Script chi chay tren may Master; chay 00-create-databases.sql truoc.',
              16, 1, @@SERVERNAME);
    SET NOEXEC ON;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.servers WHERE is_distributor = 1)
BEGIN
    RAISERROR(N'Chua cau hinh Distributor. Chay replication\30-distributor.sql truoc.', 16, 1);
    SET NOEXEC ON;
END
GO

/* ---------------------------------------------------------------------
   1. BẬT KHẢ NĂNG PUBLISH CHO DATABASE MASTER

   ⚠️ Publisher là database VAI TRÒ MASTER, KHÔNG phải database vận hành
      của cơ sở HCM. Đây chính là điểm tách bạch ở mục C0: Master là một
      vai trò, không phải một cơ sở.
   --------------------------------------------------------------------- */
USE [$(DbMaster)];
GO

IF NOT EXISTS (SELECT 1 FROM sys.databases
                WHERE name = N'$(DbMaster)' AND is_published = 1)
BEGIN
    EXEC sp_replicationdboption
         @dbname      = N'$(DbMaster)',
         @optname     = N'publish',
         @value       = N'true';
    PRINT '  [+] Da bat publish cho $(DbMaster)';
END
ELSE
    PRINT '  [=] $(DbMaster) da bat publish, bo qua';
GO

/* ---------------------------------------------------------------------
   2. TẠO PUBLICATION

   Transactional Replication, một chiều, liên tục.

   ⚠️ @retention PHẢI BẰNG @max_distretention đã đặt ở 30-distributor.sql.
      Thời gian một máy site tắt tối đa = MIN(hai giá trị). Đặt lệch nhau
      thì con số nhỏ hơn mới là giới hạn thật.
   --------------------------------------------------------------------- */
USE [$(DbMaster)];
GO

IF NOT EXISTS (SELECT 1 FROM syspublications WHERE name = N'$(PublicationName)')
BEGIN
    EXEC sp_addpublication
         @publication          = N'$(PublicationName)',
         @description          = N'PTIT One - du lieu tham chieu va danh ba dinh vi',
         @status               = N'active',
         @repl_freq            = N'continuous',      -- transactional
         @sync_method          = N'concurrent',
         @independent_agent    = N'true',
         @allow_push           = N'true',
         @allow_pull           = N'false',           -- chi dung push subscription
         @allow_anonymous      = N'false',
         @immediate_sync       = N'false',
         @retention            = $(SubRetention),    -- = DistRetention
         @enabled_for_internet = N'false';
    PRINT '  [+] Da tao publication $(PublicationName), retention = $(SubRetention) gio';
END
ELSE
BEGIN
    EXEC sp_changepublication
         @publication = N'$(PublicationName)',
         @property    = N'retention',
         @value       = $(SubRetention),
         @force_invalidate_snapshot = 0;
    PRINT '  [~] Publication da co, da cap nhat retention = $(SubRetention) gio';
END
GO

/* Snapshot Agent — sinh ảnh chụp ban đầu cho subscriber */
USE [$(DbMaster)];
GO

IF NOT EXISTS (SELECT 1 FROM msdb.dbo.sysjobs
                WHERE name LIKE N'%$(PublicationName)%Snapshot%')
BEGIN
    EXEC sp_addpublication_snapshot
         @publication   = N'$(PublicationName)',
         @frequency_type = 1;          -- 1 = chay mot lan, goi thu cong khi can
    PRINT '  [+] Da tao Snapshot Agent cho $(PublicationName)';
END
ELSE
    PRINT '  [=] Snapshot Agent da ton tai, bo qua';
GO

/* =====================================================================
   3. KHAI BÁO ARTICLE — TÁM bảng tham chiếu

   ⚠️ THỨ TỰ TÔN TRỌNG KHOÁ NGOẠI, bảng cha trước bảng con:
        CoSo → Khoa → ChuongTrinhDaoTao → MonHoc → CTDT_MonHoc
             → MonHocTienQuyet → HocKy → KhungGioTiet → DanhBaNguoiDung

   ⚠️ TaiKhoanMaster KHÔNG có trong danh sách này — mật khẩu quản trị
      không có lý do gì để nằm trên ba máy. Đừng thêm vào.

   ⚠️ DotDangKy cũng KHÔNG nhân bản — nó là bảng CỤC BỘ của từng cơ sở.
      Nếu nhân bản, Subscriber chỉ đọc nên mỗi cơ sở sẽ không tự mở được
      lịch đăng ký của mình.

   @schema_option 0x000000000803509F: tạo schema bảng, khai báo khoá chính,
   kèm ràng buộc và chỉ mục ở Subscriber.
   ===================================================================== */
USE [$(DbMaster)];
GO

DECLARE @bang TABLE (ThuTu INT, Ten SYSNAME);
INSERT INTO @bang (ThuTu, Ten) VALUES
    (1, N'CoSo'),
    (2, N'Khoa'),
    (3, N'ChuongTrinhDaoTao'),
    (4, N'MonHoc'),
    (5, N'CTDT_MonHoc'),
    (6, N'MonHocTienQuyet'),
    (7, N'HocKy'),
    (8, N'KhungGioTiet'),
    (9, N'DanhBaNguoiDung');

DECLARE @ten SYSNAME;
DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT Ten FROM @bang ORDER BY ThuTu;

OPEN cur;
FETCH NEXT FROM cur INTO @ten;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF OBJECT_ID(N'dbo.' + QUOTENAME(@ten), N'U') IS NULL
        RAISERROR(N'Chua co bang %s. Chay master\01..03 truoc.', 16, 1, @ten);
    ELSE IF NOT EXISTS (SELECT 1 FROM sysarticles WHERE name = @ten)
    BEGIN
        EXEC sp_addarticle
             @publication   = N'$(PublicationName)',
             @article       = @ten,
             @source_owner  = N'dbo',
             @source_object = @ten,
             @type          = N'logbased',
             @schema_option = 0x000000000803509F,
             @force_invalidate_snapshot = 1;
        PRINT '  [+] article ' + @ten;
    END
    ELSE
        PRINT '  [=] article ' + @ten;

    FETCH NEXT FROM cur INTO @ten;
END
CLOSE cur;
DEALLOCATE cur;
GO

/* ---------------------------------------------------------------------
   Sinh snapshot ban đầu.
   ⚠️ Job này ghi vào $(SnapshotFolder). Nếu tài khoản chạy SQL Server
      Agent không ghi được vào share đó, job sẽ thất bại ở đây — xem
      db/replication/README.md muc 1.
   --------------------------------------------------------------------- */
DECLARE @job SYSNAME;
SELECT TOP 1 @job = name FROM msdb.dbo.sysjobs
 WHERE name LIKE N'%$(PublicationName)%Snapshot%';

IF @job IS NOT NULL
BEGIN
    EXEC msdb.dbo.sp_start_job @job_name = @job;
    PRINT '  [>] Da khoi dong Snapshot Agent: ' + @job;
    PRINT '      Theo doi trong Replication Monitor cho toi khi xong.';
END
ELSE
    PRINT '  [!] Khong tim thay Snapshot Agent job';
GO

/* ---------------------------------------------------------------------
   4. KIỂM CHỨNG
   --------------------------------------------------------------------- */
PRINT '';
PRINT '  ------------------------------------------------------------';
PRINT '  Publication hien co:';
GO

USE [$(DbMaster)];
GO
SELECT  name                AS [Publication],
        status              AS [Status],
        repl_freq           AS [ReplFreq_0laLienTuc],
        retention           AS [RetentionGio],
        allow_push          AS [ChoPush]
  FROM  syspublications;
GO

PRINT '';
PRINT '  So article dang co (se la 0 cho toi khi bo comment muc 3):';
GO
USE [$(DbMaster)];
GO
SELECT COUNT(*) AS [SoArticle] FROM sysarticles;
GO

PRINT '';
PRINT '  Buoc tiep theo:';
PRINT '    - Con cho schema : bo comment muc 3 sau khi co file Excel de tai';
PRINT '    - Da co article  : replication\32-subscription.sql';
PRINT '    - Kiem tra tong the: xem db/replication/README.md';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
