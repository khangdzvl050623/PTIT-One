/* =====================================================================
   PTIT One — replication/32-subscription.sql
   ---------------------------------------------------------------------
   Tạo PUSH subscription cho mọi CSDL vận hành.

   CHẠY Ở ĐÂU : CHỈ trên máy Master
                  .\run.ps1 -Script replication\32-subscription.sql -On MASTER

   ⚠️ Chạy TỪ Master, KHÔNG chạy trên từng subscriber. Vì dùng push
      subscription, Distribution Agent nằm ở Distributor và tự đẩy sang.

   ⭐ THỨ TỰ: subscriber CỤC BỘ trước, subscriber qua VPN sau.
      Đây không phải chi tiết ngẫu nhiên — xem giải thích ở mục 2 dưới.

   ĐIỀU KIỆN: 31-publication.sql đã chạy và publication đã có article.
   CHẠY LẠI ĐƯỢC: có.
   ===================================================================== */

SET NOCOUNT ON;
GO

/* ---------------------------------------------------------------------
   1. TIỀN ĐIỀU KIỆN
   --------------------------------------------------------------------- */
IF DB_ID(N'$(DbMaster)') IS NULL
BEGIN
    RAISERROR(N'Khong tim thay $(DbMaster) tren instance nay (%s). Script chi chay tren may Master.',
              16, 1, @@SERVERNAME);
    SET NOEXEC ON;
END
GO

USE [$(DbMaster)];
GO

IF NOT EXISTS (SELECT 1 FROM syspublications WHERE name = N'$(PublicationName)')
BEGIN
    RAISERROR(N'Chua co publication $(PublicationName). Chay replication\31-publication.sql truoc.', 16, 1);
    SET NOEXEC ON;
END
GO

/* Publication rỗng thì subscription sẽ tạo được nhưng không đồng bộ gì cả —
   bắt lỗi ngay ở đây thay vì để nhóm ngồi đợi Replication Monitor. */
USE [$(DbMaster)];
GO
DECLARE @soArticle INT = (SELECT COUNT(*) FROM sysarticles);
IF @soArticle = 0
BEGIN
    RAISERROR(N'Publication chua co article nao. Chay master\01..03 roi 31-publication.sql truoc.', 16, 1);
    SET NOEXEC ON;
END
ELSE
    PRINT '  [ok] Publication co ' + CAST(@soArticle AS VARCHAR(10)) + ' article';
GO

/* =====================================================================
   2. DANH SÁCH SUBSCRIBER — cục bộ TRƯỚC, VPN SAU

   ⭐ VÌ SAO THỨ TỰ NÀY QUAN TRỌNG:

   Subscriber cục bộ (PTITONE_HCM) nằm CÙNG INSTANCE với Publisher, nên
   nó KHÔNG đi qua VPN. Chạy nó trước tách được hai loại lỗi hoàn toàn
   khác nhau:

     ✅ Cục bộ chạy được  → publication ĐÚNG.
                            Lỗi tiếp theo (nếu có) là LỖI MẠNG.
     ❌ Cục bộ hỏng       → publication SAI.
                            Sửa publication, đừng đụng VPN hay firewall.

   Không có bước đệm này thì mọi lỗi trông giống nhau, và nhóm sẽ mất cả
   buổi để đoán xem hỏng ở publication hay ở mạng.
   ===================================================================== */
USE [$(DbMaster)];
GO

DECLARE @sub TABLE (
    ThuTu   INT,
    Srv     SYSNAME,
    Db      SYSNAME,
    GhiChu  NVARCHAR(50)
);

INSERT INTO @sub (ThuTu, Srv, Db, GhiChu) VALUES
    (1, N'$(SrvHCM)', N'$(DbHCM)', N'cuc bo — cung instance'),
    (2, N'$(SrvHN)',  N'$(DbHN)',  N'qua VPN'),
    (3, N'$(SrvDN)',  N'$(DbDN)',  N'qua VPN');

DECLARE @srv SYSNAME, @db SYSNAME, @ghiChu NVARCHAR(50);

DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT Srv, Db, GhiChu FROM @sub ORDER BY ThuTu;

OPEN cur;
FETCH NEXT FROM cur INTO @srv, @db, @ghiChu;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM dbo.syssubscriptions s
          JOIN master.sys.servers   v ON v.server_id = s.srvid
         WHERE v.name = @srv AND s.dest_db = @db)
    BEGIN
        /* --- Đăng ký subscription --- */
        EXEC sp_addsubscription
             @publication       = N'$(PublicationName)',
             @subscriber        = @srv,
             @destination_db    = @db,
             @subscription_type = N'push',
             @sync_type         = N'automatic',
             @article           = N'all',
             /* ⚠️ read only: Subscriber KHÔNG đẩy ngược thay đổi về Publisher.
                Đây là nhân bản MỘT CHIỀU (quyết định D1) — không phải merge,
                nên không có bài toán giải quyết xung đột nào cả. */
             @update_mode       = N'read only',
             @subscriber_type   = 0;

        /* --- Distribution Agent, chạy liên tục --- */
        EXEC sp_addpushsubscription_agent
             @publication              = N'$(PublicationName)',
             @subscriber               = @srv,
             @subscriber_db            = @db,
             @subscriber_security_mode = 1,      -- Windows Authentication
             @frequency_type           = 64,     -- 64 = tự khởi động khi Agent chạy
             @enabled_for_syncmgr      = N'False';

        PRINT '  [+] subscription ' + @srv + '.' + @db + '   (' + @ghiChu + ')';
    END
    ELSE
        PRINT '  [=] subscription ' + @srv + '.' + @db + ' da ton tai';

    FETCH NEXT FROM cur INTO @srv, @db, @ghiChu;
END
CLOSE cur;
DEALLOCATE cur;
GO

/* =====================================================================
   3. SINH LẠI SNAPSHOT CHO SUBSCRIPTION VỪA TẠO

   ⚠️ CHỖ NÀY DỄ MẤT CẢ BUỔI NẾU BỎ QUA.

   Publication đặt @immediate_sync = 'false' (xem 31-publication.sql).
   Với thiết lập đó, ảnh chụp sinh ra ở bước 31 CHỈ dùng được cho những
   subscription ĐÃ TỒN TẠI lúc nó được sinh. Subscription tạo sau — tức
   toàn bộ ba subscription ở mục 2 — sẽ đứng nguyên ở trạng thái chưa
   khởi tạo và KHÔNG có dữ liệu, dù Replication Monitor không báo lỗi đỏ.

   Nên phải khởi động lại Snapshot Agent SAU KHI có subscription.
   ===================================================================== */
USE [$(DbMaster)];
GO

DECLARE @job SYSNAME;
SELECT TOP 1 @job = name FROM msdb.dbo.sysjobs
 WHERE name LIKE N'%$(PublicationName)%Snapshot%';

IF @job IS NULL
    PRINT '  [!] Khong tim thay Snapshot Agent job — chay lai 31-publication.sql';
ELSE
BEGIN
    /* sp_start_job nem loi neu job DANG chay. Do khong phai su co — bat
       lay va bao cao, dung de sqlcmd -b dung ca script. */
    BEGIN TRY
        EXEC msdb.dbo.sp_start_job @job_name = @job;
        PRINT '  [>] Da khoi dong lai Snapshot Agent: ' + @job;
    END TRY
    BEGIN CATCH
        PRINT '  [=] Khong khoi dong lai duoc (thuong la job dang chay): '
              + ERROR_MESSAGE();
    END CATCH
END
GO

/* =====================================================================
   4. KIỂM CHỨNG
   ===================================================================== */
PRINT '';
PRINT '  ------------------------------------------------------------';
PRINT '  Subscription hien co:';
GO

USE [$(DbMaster)];
GO
SELECT  v.name           AS [Subscriber],
        s.dest_db        AS [DatabaseDich],
        CASE s.status WHEN 0 THEN N'Inactive'
                      WHEN 1 THEN N'Subscribed'
                      WHEN 2 THEN N'Active'
                      ELSE CAST(s.status AS NVARCHAR(10)) END AS [TrangThai],
        CASE s.update_mode WHEN 0 THEN N'read only'
                           ELSE N'CO GHI NGUOC — SAI THIET KE!' END AS [ChieuGhi]
  FROM  dbo.syssubscriptions s
  JOIN  master.sys.servers   v ON v.server_id = s.srvid
 GROUP BY v.name, s.dest_db, s.status, s.update_mode
 ORDER BY v.name;
GO

/* ⚠️ Nhân bản phải là MỘT CHIỀU. Nếu có subscription nào ghi ngược thì
   thiết kế đã bị phá — bắt ngay ở đây thay vì phát hiện lúc demo. */
USE [$(DbMaster)];
GO
IF EXISTS (SELECT 1 FROM dbo.syssubscriptions WHERE update_mode <> 0)
    RAISERROR(N'CO SUBSCRIPTION GHI NGUOC. Thiet ke yeu cau nhan ban MOT CHIEU (D1).', 16, 1);
ELSE
    PRINT '  [ok] Moi subscription deu la read only — nhan ban mot chieu';
GO

PRINT '';
PRINT '  ⚠️ SUBSCRIPTION MOI TAO CHUA CO DU LIEU NGAY.';
PRINT '     Phai doi Snapshot Agent sinh xong anh chup, roi Distribution';
PRINT '     Agent day sang. Theo doi:';
PRINT '        SSMS -> chuot phai Replication -> Launch Replication Monitor';
PRINT '';
PRINT '  Kiem chung end-to-end (tieu chi PASS cua spike tuan 1):';
PRINT '     1. Tren $(DbMaster):  INSERT mot dong vao dbo.Khoa';
PRINT '     2. Doi <= 10 giay';
PRINT '     3. Tren $(DbHN)    :  SELECT thay dong do';
PRINT '';
PRINT '  Do do tre chinh thuc (so lieu cho benchmark B6):';
PRINT '     EXEC sp_posttracertoken @publication = ''$(PublicationName)'';';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
