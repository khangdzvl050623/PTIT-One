/* PTIT One — SPIKE: giao dịch phân tán (MS DTC) qua Linked Server.

   ĐÂY LÀ CỔNG CHẶN. Yêu cầu bắt buộc số 3 của giảng viên là một distributed
   transaction thật (`sp_ChuyenCoSoSinhVien`, mục D8). Nếu `BEGIN DISTRIBUTED
   TRANSACTION` không chạy được giữa hai máy thì yêu cầu đó KHÔNG hiện thực
   được, và phải biết điều đó trong tuần đầu chứ không phải tuần cuối.

   Script này cố tình KHÔNG đụng tới bảng nghiệp vụ nào. Nó tự dựng một bảng
   rác hai đầu, ghi hai dòng trong một giao dịch, rồi dọn sạch.

   ĐIỀU KIỆN TRƯỚC KHI CHẠY
   ------------------------
   1. MS DTC đã cấu hình ở CẢ HAI máy (xem F4b): Network DTC Access,
      Allow Inbound/Outbound, "No Authentication Required", và mở port 135
      cùng dải RPC động trên tường lửa.
   2. Linked Server đã tạo (xem F5), có bật `rpc out` và `remote proc
      transaction promotion`.
   3. Database đích đã tồn tại ở máy từ xa.

   CÁCH CHẠY
   ---------
   Chạy TẠI máy nguồn (SRV-HCM), qua runner để nhận biến SQLCMD:

     .\db\run.ps1 -Action Spike -Site HCM

   Hoặc trực tiếp:

     sqlcmd -S <SRV-HCM> -d <PTITONE_HCM> -E -N -C -b -I -f 65001 ^
            -v LinkedServer="SRV_HN" RemoteDb="PTITONE_HN" ^
            -i db/spike/01-kiem-dtc.sql

   ĐỌC KẾT QUẢ
   -----------
   PASS  → in ra `DTC: PASS` ở cuối. Cả hai dòng tồn tại rồi bị dọn.
   FAIL  → lỗi 7391 ("The operation could not be performed because OLE DB
           provider ... was unable to begin a distributed transaction") là
           triệu chứng kinh điển: DTC chưa thông. Xem F4b, đừng sửa SQL.

   FAIL thì làm gì
   ---------------
   Thử lại trên HAI NAMED INSTANCE CÙNG MỘT MÁY trước (phương án I2). DTC
   trong cùng máy gần như luôn chạy, nên nếu bản cùng máy PASS mà bản qua
   mạng FAIL thì vấn đề nằm ở tường lửa/VPN chứ không phải cấu hình SQL. */

SET XACT_ABORT ON;   -- BẮT BUỘC với giao dịch phân tán. Thiếu là lỗi im lặng.
SET NOCOUNT ON;
GO

/* ---------- Dựng bảng rác ở hai đầu ---------- */

IF OBJECT_ID('dbo.SpikeDtc') IS NULL
    CREATE TABLE dbo.SpikeDtc (
        Id     uniqueidentifier NOT NULL CONSTRAINT PK_SpikeDtc PRIMARY KEY,
        Nguon  sysname          NOT NULL,
        LucGhi datetime2(3)     NOT NULL
    );
GO

DECLARE @tao nvarchar(max) = N'
    IF OBJECT_ID(''dbo.SpikeDtc'') IS NULL
        CREATE TABLE dbo.SpikeDtc (
            Id     uniqueidentifier NOT NULL CONSTRAINT PK_SpikeDtc PRIMARY KEY,
            Nguon  sysname          NOT NULL,
            LucGhi datetime2(3)     NOT NULL
        );';
EXEC (@tao) AT [$(LnkHN)];
GO

/* ---------- Ca 1: COMMIT — hai dòng phải cùng tồn tại ---------- */

DECLARE @id uniqueidentifier = NEWID();

BEGIN DISTRIBUTED TRANSACTION;

    INSERT INTO dbo.SpikeDtc (Id, Nguon, LucGhi)
    VALUES (@id, @@SERVERNAME, SYSUTCDATETIME());

    DECLARE @ghiXa nvarchar(max) =
        N'INSERT INTO [$(DbHN)].dbo.SpikeDtc (Id, Nguon, LucGhi) VALUES (@p, @@SERVERNAME, SYSUTCDATETIME());';
    EXEC [$(LnkHN)].master.sys.sp_executesql @ghiXa, N'@p uniqueidentifier', @p = @id;

COMMIT;

DECLARE @cucBo int = (SELECT COUNT(*) FROM dbo.SpikeDtc WHERE Id = @id);
DECLARE @tuXa  int;
DECLARE @dem nvarchar(max) = N'SELECT @n = COUNT(*) FROM [$(DbHN)].dbo.SpikeDtc WHERE Id = @p;';
EXEC [$(LnkHN)].master.sys.sp_executesql @dem,
     N'@p uniqueidentifier, @n int OUTPUT', @p = @id, @n = @tuXa OUTPUT;

IF @cucBo <> 1 OR @tuXa <> 1
    THROW 50001, N'Ca COMMIT hong: hai dau khong cung co dong.', 1;

PRINT 'Ca 1 (COMMIT) OK — ghi duoc hai dau trong mot giao dich.';
GO

/* ---------- Ca 2: ROLLBACK — KHÔNG đầu nào được giữ lại dòng ----------

   Ca này mới là ca chứng minh tính nguyên tử. COMMIT thành công chỉ nói
   "gọi được qua mạng"; chỉ ROLLBACK mới nói "hai đầu cùng huỷ". */

DECLARE @id2 uniqueidentifier = NEWID();

BEGIN DISTRIBUTED TRANSACTION;

    INSERT INTO dbo.SpikeDtc (Id, Nguon, LucGhi)
    VALUES (@id2, @@SERVERNAME, SYSUTCDATETIME());

    DECLARE @ghiXa2 nvarchar(max) =
        N'INSERT INTO [$(DbHN)].dbo.SpikeDtc (Id, Nguon, LucGhi) VALUES (@p, @@SERVERNAME, SYSUTCDATETIME());';
    EXEC [$(LnkHN)].master.sys.sp_executesql @ghiXa2, N'@p uniqueidentifier', @p = @id2;

ROLLBACK;

DECLARE @conCucBo int = (SELECT COUNT(*) FROM dbo.SpikeDtc WHERE Id = @id2);
DECLARE @conTuXa  int;
DECLARE @dem2 nvarchar(max) = N'SELECT @n = COUNT(*) FROM [$(DbHN)].dbo.SpikeDtc WHERE Id = @p;';
EXEC [$(LnkHN)].master.sys.sp_executesql @dem2,
     N'@p uniqueidentifier, @n int OUTPUT', @p = @id2, @n = @conTuXa OUTPUT;

IF @conCucBo <> 0 OR @conTuXa <> 0
    THROW 50002, N'Ca ROLLBACK hong: con dong sot lai — KHONG nguyen tu.', 1;

PRINT 'Ca 2 (ROLLBACK) OK — hai dau cung huy, khong con dong nao.';
GO

/* ---------- Dọn ---------- */

DROP TABLE IF EXISTS dbo.SpikeDtc;
GO

EXEC (N'DROP TABLE IF EXISTS [$(DbHN)].dbo.SpikeDtc;') AT [$(LnkHN)];
GO

PRINT 'DTC: PASS';
GO
