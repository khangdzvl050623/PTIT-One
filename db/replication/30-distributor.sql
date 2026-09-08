/* =====================================================================
   PTIT One — replication/30-distributor.sql
   ---------------------------------------------------------------------
   Cấu hình Distributor (local distributor, đặt ngay trên Publisher).

   CHẠY Ở ĐÂU : CHỈ trên máy Master
                  .\run.ps1 -Script replication\30-distributor.sql -On MASTER

   ĐIỀU KIỆN TRƯỚC KHI CHẠY
     1. SQL Server Agent đang chạy, Startup Type = Automatic
     2. Tài khoản chạy Agent PHẢI đọc/ghi được $(SnapshotFolder)
        ⚠️ Virtual account 'NT Service\SQLSERVERAGENT' KHÔNG xác thực được
           ra share máy khác trong môi trường workgroup. Phải tạo một tài
           khoản Windows TRÙNG username + TRÙNG password trên MỌI máy và
           cho Agent chạy bằng tài khoản đó.
     3. Thư mục $(SnapshotFolder) đã tồn tại và đã được chia sẻ

   CHẠY LẠI ĐƯỢC: có — kiểm tra trạng thái trước mọi thao tác.

   ⚠️ GỠ RA thì chạy 39-go-distributor.sql, đừng sửa tay.
   ===================================================================== */

SET NOCOUNT ON;
GO

/* ---------------------------------------------------------------------
   0. CHẶN CHẠY NHẦM MÁY
   --------------------------------------------------------------------- */
IF CHARINDEX(UPPER('$(SrvMaster)'), UPPER(@@SERVERNAME)) = 0
   AND UPPER(@@SERVERNAME) <> UPPER('$(SrvMaster)')
BEGIN
    RAISERROR(N'Script nay CHI duoc chay tren may Master ($(SrvMaster)). May hien tai: %s',
              16, 1, @@SERVERNAME);
    SET NOEXEC ON;
END
GO

/* ---------------------------------------------------------------------
   1. KIỂM TRA SQL SERVER AGENT
      Không có Agent thì không có Snapshot/Log Reader/Distribution Agent,
      tức là KHÔNG CÓ replication ở bất kỳ dạng nào.
   --------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.dm_server_services
                WHERE servicename LIKE N'SQL Server Agent%'
                  AND status_desc = N'Running')
BEGIN
    RAISERROR(N'SQL Server Agent CHUA CHAY. Bat Agent va dat Startup Type = Automatic roi chay lai.',
              16, 1);
    SET NOEXEC ON;
END
ELSE
    PRINT '  [ok] SQL Server Agent dang chay';
GO

/* ---------------------------------------------------------------------
   2. KIỂM TRA @@SERVERNAME KHỚP TÊN MÁY THẬT
      Replication lưu TÊN MÁY, không lưu IP. Nếu máy từng bị đổi tên thì
      @@SERVERNAME còn giữ tên cũ và replication sẽ hỏng theo cách rất khó truy.
   --------------------------------------------------------------------- */
IF @@SERVERNAME IS NULL OR @@SERVERNAME <> CAST(SERVERPROPERTY('ServerName') AS SYSNAME)
BEGIN
    RAISERROR(N'@@SERVERNAME (%s) khac ten may that (%s). Sua bang sp_dropserver / sp_addserver roi KHOI DONG LAI SQL Server.',
              16, 1, @@SERVERNAME, CAST(SERVERPROPERTY('ServerName') AS NVARCHAR(128)));
    SET NOEXEC ON;
END
ELSE
    PRINT '  [ok] @@SERVERNAME khop ten may that: ' + @@SERVERNAME;
GO

/* ---------------------------------------------------------------------
   3. ĐĂNG KÝ DISTRIBUTOR
      Dùng local distributor: Distributor nằm ngay trên Publisher.
      Tách Distributor ra máy riêng là thêm một máy phải bật liên tục,
      không đổi lấy lợi ích nào ở quy mô này.
   --------------------------------------------------------------------- */
DECLARE @distributor SYSNAME = @@SERVERNAME;

IF NOT EXISTS (SELECT 1 FROM sys.servers
                WHERE is_distributor = 1 AND name = @distributor)
BEGIN
    EXEC sp_adddistributor @distributor = @distributor;
    PRINT '  [+] Da dang ky Distributor: ' + @distributor;
END
ELSE
    PRINT '  [=] Distributor da duoc dang ky, bo qua';
GO

/* ---------------------------------------------------------------------
   4. TẠO DISTRIBUTION DATABASE

   ⚠️ HAI THAM SỐ RETENTION — chỗ dễ sai nhất của cả file này.

      @max_distretention : thời gian lệnh được GIỮ trong distribution db
      (ở 31-publication) @retention : thời gian subscription HẾT HẠN

      Thời gian một máy site có thể tắt tối đa = MIN(hai giá trị).
      Đặt lệch nhau thì con số nhỏ hơn mới là giới hạn thật — nếu lệnh đã
      bị dọn, subscription dù chưa hết hạn cũng KHÔNG CÒN GÌ để bắt kịp,
      buộc phải khởi tạo lại snapshot.

      Cả hai đặt = $(DistRetention) giờ.
   --------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE name = N'distribution')
BEGIN
    EXEC sp_adddistributiondb
         @database                = N'distribution',
         @security_mode           = 1,              -- Windows Authentication
         @max_distretention       = $(DistRetention),
         @min_distretention       = 0,
         @history_retention       = 48;
    PRINT '  [+] Da tao distribution database, retention = $(DistRetention) gio';
END
ELSE
BEGIN
    /* Đã có sẵn thì chỉ chỉnh lại retention cho khớp config */
    EXEC sp_changedistributiondb
         @database = N'distribution',
         @property = N'max_distretention',
         @value    = $(DistRetention);
    PRINT '  [~] distribution database da co, da cap nhat retention = $(DistRetention) gio';
END
GO

/* ---------------------------------------------------------------------
   5. ĐĂNG KÝ PUBLISHER VỚI DISTRIBUTOR

   ⚠️ @working_directory BẮT BUỘC là UNC share.
      KHÔNG để mặc định 'C:\Program Files\...\ReplData' — máy Subscriber
      không thể với tới đường dẫn local của máy khác.
      ĐÂY LÀ LỖI SỐ MỘT GIẾT CÁC NHÓM.

      Share phải nằm trên máy chạy DISTRIBUTOR (không phải Publisher —
      ở đây trùng nhau, nhưng nhớ nguyên tắc khi tách máy).
   --------------------------------------------------------------------- */
DECLARE @publisher SYSNAME = @@SERVERNAME;

IF NOT EXISTS (SELECT 1 FROM msdb.dbo.MSdistpublishers WHERE name = @publisher)
BEGIN
    EXEC sp_adddistpublisher
         @publisher        = @publisher,
         @distribution_db  = N'distribution',
         @security_mode    = 1,
         @working_directory = N'$(SnapshotFolder)',
         @trusted          = N'false',
         @thirdparty_flag  = 0,
         @publisher_type   = N'MSSQLSERVER';
    PRINT '  [+] Da dang ky Publisher, snapshot folder = $(SnapshotFolder)';
END
ELSE
    PRINT '  [=] Publisher da duoc dang ky voi Distributor, bo qua';
GO

/* ---------------------------------------------------------------------
   6. KIỂM CHỨNG
   --------------------------------------------------------------------- */
PRINT '';
PRINT '  ------------------------------------------------------------';
PRINT '  Cau hinh Distributor hien tai:';
GO

SELECT  p.name              AS [Publisher],
        p.distribution_db   AS [DistributionDB],
        p.working_directory AS [SnapshotFolder]
  FROM  msdb.dbo.MSdistpublishers p;

SELECT  name                AS [DistributionDB],
        max_distretention   AS [DistRetentionGio],
        min_distretention   AS [MinRetentionGio],
        history_retention   AS [HistoryRetentionGio]
  FROM  msdb.dbo.MSdistributiondbs;
GO

PRINT '';
PRINT '  ⚠️ KIEM TRA THU CONG TRUOC KHI DI TIEP:';
PRINT '     1. Tu MOT MAY SITE KHAC, mo Windows Explorer va go:';
PRINT '           $(SnapshotFolder)';
PRINT '        Phai vao duoc. Neu khong, subscription se that bai o buoc snapshot.';
PRINT '     2. Tai khoan chay SQL Server Agent phai la tai khoan Windows';
PRINT '        trung username + trung password tren MOI may (moi truong workgroup).';
PRINT '';
PRINT '  Buoc tiep theo: replication\31-publication.sql (van tren may Master)';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
