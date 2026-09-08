/* =====================================================================
   PTIT One — replication/31-publication.sql
   ---------------------------------------------------------------------
   Dựng KHUNG publication cho dữ liệu tham chiếu.

   CHẠY Ở ĐÂU : CHỈ trên máy Master, sau khi 30-distributor.sql đã xong
                  .\run.ps1 -Script replication\31-publication.sql -On MASTER

   ⚠️ PHẦN KHAI BÁO ARTICLE CHƯA LÀM ĐƯỢC.
      sp_addarticle gắn trực tiếp vào đối tượng nguồn, nên nó phụ thuộc
      schema. Schema nghiệp vụ chỉ chốt được sau khi đối chiếu file Excel
      phân công đề tài của giảng viên.

      Mục 3 dưới đây để sẵn khung và danh sách bảng dự kiến, đang bị
      comment. Bỏ comment sau khi có schema.

   CHẠY LẠI ĐƯỢC: có.

   ⚠️ Gỡ ra thì chạy 39-go-publication.sql, đừng xoá tay trong SSMS.
   ===================================================================== */

SET NOCOUNT ON;
GO

/* ---------------------------------------------------------------------
   0. CHẶN CHẠY NHẦM MÁY VÀ NHẦM DATABASE
   --------------------------------------------------------------------- */
IF UPPER(@@SERVERNAME) <> UPPER('$(SrvMaster)')
   AND CHARINDEX(UPPER('$(SrvMaster)'), UPPER(@@SERVERNAME)) = 0
BEGIN
    RAISERROR(N'Script nay CHI chay tren may Master ($(SrvMaster)). May hien tai: %s', 16, 1, @@SERVERNAME);
    SET NOEXEC ON;
END
GO

IF DB_ID(N'$(DbMaster)') IS NULL
BEGIN
    RAISERROR(N'Chua co database $(DbMaster). Chay 00-create-databases.sql truoc.', 16, 1);
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
   3. ⚠️ KHAI BÁO ARTICLE — CHỜ SCHEMA

   sp_addarticle tham chiếu trực tiếp tới bảng nguồn, nên phần này KHÔNG
   viết trước được. Chỉ bỏ comment sau khi:
     (a) Đã đối chiếu file Excel phân công đề tài của giảng viên
     (b) db/master/01-schema-thamchieu.sql đã chạy xong

   Bảy bảng dự kiến (mục C1, nhóm 1 — tên có thể đổi theo đề tài):

       CoSo                  cấu hình topology, có TenLinkedServer + TenDatabase
       Khoa
       ChuongTrinhDaoTao
       MonHoc                bảng bị đọc nhiều nhất hệ thống
       MonHocTienQuyet
       HocKy                 ⚠️ CHỈ lịch chung toàn trường.
                                DotDangKy là bảng CỤC BỘ, KHÔNG nhân bản
       DanhBaNguoiDung       danh bạ định vị — nền tảng Location Transparency

   ⚠️ TaiKhoanMaster KHÔNG nhân bản: nó chỉ tồn tại ở Master.

   Khuôn mẫu cho mỗi bảng:
   ---------------------------------------------------------------------
   EXEC sp_addarticle
        @publication      = N'$(PublicationName)',
        @article          = N'<TenBang>',
        @source_owner     = N'dbo',
        @source_object    = N'<TenBang>',
        @type             = N'logbased',
        @schema_option    = 0x000000000803509F,
        @ins_cmd          = N'CALL sp_MSins_dbo<TenBang>',
        @upd_cmd          = N'SCALL sp_MSupd_dbo<TenBang>',
        @del_cmd          = N'CALL sp_MSdel_dbo<TenBang>',
        @force_invalidate_snapshot = 1;
   ---------------------------------------------------------------------

   ⚠️ HAI ĐIỀU PHẢI NHỚ KHI BỎ COMMENT:

   1. Thứ tự article phải tôn trọng khoá ngoại. Bảng cha trước bảng con:
        CoSo → Khoa → ChuongTrinhDaoTao → MonHoc → MonHocTienQuyet
             → HocKy → DanhBaNguoiDung

   2. Trigger ở Subscriber PHẢI khai báo NOT FOR REPLICATION, nếu không
      nó sẽ chặn chính Distribution Agent và replication chết với triệu
      chứng nhìn không liên quan gì tới trigger. Xem db/site/13-trigger.sql.
   ===================================================================== */
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
