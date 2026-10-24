/* =====================================================================
   PTIT One — tests/92-do-truy-van-phan-tan.sql        (yêu cầu 5)
   ---------------------------------------------------------------------
   CHẠY:  .\run.ps1 -Script tests\92-do-truy-van-phan-tan.sql -On MASTER

   Đo cùng MỘT câu thống kê bằng bốn cách, trên lớp học phần ở cơ sở HN.

   Bằng chứng quyết định là SoDongQuaMang, KHÔNG phải thời gian: ba cách
   pushdown kéo 1 dòng, cách tệ kéo cả bảng. Với dữ liệu demo (vài chục
   dòng) thời gian nằm trong khoảng nhiễu — ghi lại để đối chiếu khi có
   dữ liệu thật, không dùng để kết luận cách nào nhanh hơn.

   PASS = bốn cách cho CÙNG một kết quả. Đó là phần khẳng định được;
   phần còn lại là số đo.
   ===================================================================== */

SET NOCOUNT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    RAISERROR(N'Chi chay tren $(DbMaster).', 16, 1);
    SET NOEXEC ON;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.servers WHERE name = N'$(LnkHN)')
BEGIN
    RAISERROR(N'Chua co Linked Server $(LnkHN). Chay replication\20-linked-server.sql truoc.', 16, 1);
    SET NOEXEC ON;
END
GO

DECLARE @SoLan INT = 5;

/* Môn/kỳ phải là cặp HN THỰC SỰ có lớp, nếu không cả bốn cách cùng trả 0
   và số đo trông như PASS mà chẳng đo gì. Hỏi thẳng HN cho chắc. */
DECLARE @chon TABLE (MaMonHoc VARCHAR(20), MaHocKy VARCHAR(20), SoLop INT);
INSERT @chon
SELECT * FROM OPENQUERY([$(LnkHN)], '
    SELECT TOP 1 MaMonHoc, MaHocKy, SoLop = COUNT(*)
      FROM [$(DbHN)].dbo.LopHocPhan
     WHERE TrangThai <> ''DA_HUY''
     GROUP BY MaMonHoc, MaHocKy
     ORDER BY COUNT(*) DESC') q;

DECLARE @MaMonHoc VARCHAR(20), @MaHocKy VARCHAR(20);
SELECT @MaMonHoc = MaMonHoc, @MaHocKy = MaHocKy FROM @chon;

IF @MaMonHoc IS NULL
BEGIN
    RAISERROR(N'HN khong co lop nao. Chay migrate\50-tach-du-lieu.sql truoc.', 16, 1);
    RETURN;
END

/* Tra lại ở Master trước khi nối vào chuỗi OPENQUERY bên dưới — nhánh đó
   KHÔNG nhận tham số nên buộc phải nối. Đây là lớp chặn SQL injection. */
IF NOT EXISTS (SELECT 1 FROM dbo.MonHoc WHERE MaMonHoc = @MaMonHoc)
BEGIN
    RAISERROR(N'Mon %s khong co trong danh muc Master.', 16, 1, @MaMonHoc);
    RETURN;
END

DECLARE @kq TABLE (
    ThuTu       INT,
    ChienLuoc   VARCHAR(16),
    SoLop       INT,
    SoLuot      INT,
    SucChua     INT,
    SoDongQuaMang VARCHAR(20),
    MicroTB     BIGINT);

DECLARE @i INT, @t0 DATETIME2(7), @micro BIGINT, @sql NVARCHAR(MAX),
        @lop INT, @luot INT, @chua INT, @tongDong INT;

DECLARE @loc NVARCHAR(400) =
    N'MaMonHoc = @mon AND MaHocKy = @ky AND TrangThai <> ''DA_HUY''';
DECLARE @out NVARCHAR(200) =
    N'@mon VARCHAR(20), @ky VARCHAR(20), @o1 INT OUTPUT, @o2 INT OUTPUT, @o3 INT OUTPUT';

/* --- 1. Four-part name ------------------------------------------------
   Viết gọn, tham số hoá tự nhiên. Nhưng optimizer MỚI là nơi quyết định
   đẩy được bao nhiêu xuống remote, nên số dòng qua mạng không biết trước. */
SET @micro = 0;
SET @i = 1;
WHILE @i <= @SoLan
BEGIN
    SET @t0 = SYSUTCDATETIME();
    SET @sql = N'
        SELECT @o1 = COUNT(*),
               @o2 = ISNULL(SUM(SoLuongDaDangKy), 0),
               @o3 = ISNULL(SUM(SoLuongToiDa), 0)
          FROM [$(LnkHN)].' + QUOTENAME(N'$(DbHN)') + N'.dbo.LopHocPhan
         WHERE ' + @loc + N';';
    EXEC sp_executesql @sql, @out,
         @mon = @MaMonHoc, @ky = @MaHocKy,
         @o1 = @lop OUTPUT, @o2 = @luot OUTPUT, @o3 = @chua OUTPUT;
    SET @micro += DATEDIFF(MICROSECOND, @t0, SYSUTCDATETIME());
    SET @i += 1;
END
INSERT @kq VALUES (1, 'FOUR_PART', @lop, @luot, @chua, 'optimizer quyet', @micro / @SoLan);

/* --- 2. OPENQUERY -----------------------------------------------------
   Pass-through: remote tự parse, lập plan, gom. Qua mạng còn 1 dòng.
   ⚠️ KHÔNG nhận biến — @MaMonHoc phải NỐI vào chuỗi. Ba lớp nháy đơn bên
      dưới là giá phải trả, và là lý do @MaMonHoc được tra bảng ở trên
      thay vì nhận thẳng từ tham số. */
SET @micro = 0;
SET @i = 1;
WHILE @i <= @SoLan
BEGIN
    SET @t0 = SYSUTCDATETIME();
    SET @sql = N'
        SELECT @o1 = SoLop, @o2 = SoLuot, @o3 = SucChua
          FROM OPENQUERY([$(LnkHN)], ''
                SELECT SoLop   = COUNT(*),
                       SoLuot  = ISNULL(SUM(SoLuongDaDangKy), 0),
                       SucChua = ISNULL(SUM(SoLuongToiDa), 0)
                  FROM [$(DbHN)].dbo.LopHocPhan
                 WHERE MaMonHoc = ''''' + @MaMonHoc + N'''''
                   AND MaHocKy  = ''''' + @MaHocKy + N'''''
                   AND TrangThai <> ''''DA_HUY'''' '') q;';
    EXEC sp_executesql @sql, @out,
         @mon = @MaMonHoc, @ky = @MaHocKy,
         @o1 = @lop OUTPUT, @o2 = @luot OUTPUT, @o3 = @chua OUTPUT;
    SET @micro += DATEDIFF(MICROSECOND, @t0, SYSUTCDATETIME());
    SET @i += 1;
END
INSERT @kq VALUES (2, 'OPENQUERY', @lop, @luot, @chua, '1', @micro / @SoLan);

/* --- 3. EXEC … AT ----------------------------------------------------
   Pass-through CÓ tham số hoá: giá trị đi theo vị trí dấu `?`, không nối
   vào chuỗi. Đây là mặc định cho báo cáo có tham số. */
SET @micro = 0;
SET @i = 1;
WHILE @i <= @SoLan
BEGIN
    SET @t0 = SYSUTCDATETIME();
    DECLARE @trong NVARCHAR(MAX) = N'
        SELECT COUNT(*), ISNULL(SUM(SoLuongDaDangKy), 0), ISNULL(SUM(SoLuongToiDa), 0)
          FROM [$(DbHN)].dbo.LopHocPhan
         WHERE MaMonHoc = ? AND MaHocKy = ? AND TrangThai <> ''DA_HUY'';';
    DECLARE @mot TABLE (SoLop INT, SoLuot INT, SucChua INT);
    DELETE @mot;
    INSERT @mot
    EXEC sp_executesql N'EXEC (@q, @mon, @ky) AT [$(LnkHN)];',
         N'@q NVARCHAR(MAX), @mon VARCHAR(20), @ky VARCHAR(20)',
         @q = @trong, @mon = @MaMonHoc, @ky = @MaHocKy;
    SELECT @lop = SoLop, @luot = SoLuot, @chua = SucChua FROM @mot;
    SET @micro += DATEDIFF(MICROSECOND, @t0, SYSUTCDATETIME());
    SET @i += 1;
END
INSERT @kq VALUES (3, 'EXEC_AT', @lop, @luot, @chua, '1', @micro / @SoLan);

/* --- 4. Mốc "cách tệ" ------------------------------------------------
   Kéo dòng thô về rồi mới lọc và gom tại Master. Cùng kết quả, nhưng số
   dòng qua mạng là CẢ BẢNG. Đây là thứ cần tránh, giữ lại để đối chiếu. */
SELECT @tongDong = SoLop FROM OPENQUERY([$(LnkHN)],
    'SELECT SoLop = COUNT(*) FROM [$(DbHN)].dbo.LopHocPhan') q;

SET @micro = 0;
SET @i = 1;
WHILE @i <= @SoLan
BEGIN
    SET @t0 = SYSUTCDATETIME();
    SET @sql = N'
        SELECT @o1 = COUNT(*),
               @o2 = ISNULL(SUM(q.SoLuongDaDangKy), 0),
               @o3 = ISNULL(SUM(q.SoLuongToiDa), 0)
          FROM OPENQUERY([$(LnkHN)], ''
                SELECT MaMonHoc, MaHocKy, TrangThai, SoLuongDaDangKy, SoLuongToiDa
                  FROM [$(DbHN)].dbo.LopHocPhan'') q
         WHERE q.MaMonHoc = @mon AND q.MaHocKy = @ky AND q.TrangThai <> ''DA_HUY'';';
    EXEC sp_executesql @sql, @out,
         @mon = @MaMonHoc, @ky = @MaHocKy,
         @o1 = @lop OUTPUT, @o2 = @luot OUTPUT, @o3 = @chua OUTPUT;
    SET @micro += DATEDIFF(MICROSECOND, @t0, SYSUTCDATETIME());
    SET @i += 1;
END
INSERT @kq VALUES (4, 'KEO_THO', @lop, @luot, @chua,
                   CAST(@tongDong AS VARCHAR(20)), @micro / @SoLan);

/* --- Kết quả ---------------------------------------------------------- */
PRINT '';
PRINT '=== Do truy van phan tan: mon ' + @MaMonHoc + ', ky ' + @MaHocKy
      + ', ' + CAST(@SoLan AS VARCHAR(10)) + ' lan/chien luoc ===';

SELECT ChienLuoc            AS [Chien luoc],
       SoLop                AS [So lop],
       SoLuot               AS [Luot DK],
       SucChua              AS [Suc chua],
       SoDongQuaMang        AS [Dong qua mang],
       MicroTB / 1000.0     AS [ms trung binh]
  FROM @kq
 ORDER BY ThuTu;

PRINT '';
IF EXISTS (SELECT 1 FROM @kq
            WHERE SoLop   <> (SELECT SoLop   FROM @kq WHERE ThuTu = 1)
               OR SoLuot  <> (SELECT SoLuot  FROM @kq WHERE ThuTu = 1)
               OR SucChua <> (SELECT SucChua FROM @kq WHERE ThuTu = 1))
BEGIN
    SELECT [Lech] = ChienLuoc, SoLop, SoLuot, SucChua FROM @kq ORDER BY ThuTu;
    THROW 51920, N'TRUY VAN PHAN TAN: LECH. Bon chien luoc cho ket qua khac nhau.', 1;
END

PRINT '  TRUY VAN PHAN TAN: PASS';
PRINT '  Bon chien luoc cung ket qua. Pushdown: 1 dong qua mang; KEO_THO: '
      + CAST(@tongDong AS VARCHAR(20)) + ' dong.';

/* =====================================================================
   PHA 2 — đo CÓ khối lượng

   Dữ liệu demo chỉ vài chục dòng nên pha 1 không cho thấy tương phản nào:
   1 dòng so với 2 dòng thì mạng nào cũng chịu được. Pha này dựng một bảng
   tạm 50.000 dòng tại HN để chênh lệch hiện ra bằng số, rồi XOÁ nó đi.

   Bảng tạm KHÔNG nằm trong publication và tự xoá ở cuối pha.
   ===================================================================== */
DECLARE @SoDongTao INT = 50000;

EXEC [$(LnkHN)].[$(DbHN)].sys.sp_executesql N'
    IF OBJECT_ID(''dbo.DoTaiLopHocPhan'', ''U'') IS NOT NULL
        DROP TABLE dbo.DoTaiLopHocPhan;
    SELECT TOP (50000)
           MaLopHP         = CAST(NEWID() AS VARCHAR(80)),
           MaMonHoc        = ''DOTAI'',
           MaHocKy         = ''2026-1'',
           TrangThai       = ''MO'',
           SoLuongDaDangKy = CAST(ABS(CHECKSUM(NEWID())) % 40 AS INT),
           SoLuongToiDa    = 40
      INTO dbo.DoTaiLopHocPhan
      FROM sys.all_objects a CROSS JOIN sys.all_objects b;';

DECLARE @kq2 TABLE (ThuTu INT, ChienLuoc VARCHAR(16), SoLop INT, SoLuot INT,
                    SoDongQuaMang INT, MicroTB BIGINT);

/* ⚠️ Hai câu đo dưới đây PHẢI đi qua sp_executesql, không viết OPENQUERY
   thẳng. OPENQUERY được phân giải lúc BIÊN DỊCH cả batch, tức TRƯỚC khi
   lệnh tạo bảng ở trên kịp chạy — viết thẳng thì batch chết ngay với
   "Deferred prepare could not be completed / Invalid object name", và
   thông báo đó không hề chỉ về phía thứ tự biên dịch. */
DECLARE @outPush NVARCHAR(100) = N'@o1 INT OUTPUT, @o2 INT OUTPUT';

SET @micro = 0;
SET @i = 1;
WHILE @i <= @SoLan
BEGIN
    SET @t0 = SYSUTCDATETIME();
    EXEC sp_executesql N'
        SELECT @o1 = SoLop, @o2 = SoLuot FROM OPENQUERY([$(LnkHN)], ''
            SELECT SoLop  = COUNT(*),
                   SoLuot = ISNULL(SUM(SoLuongDaDangKy), 0)
              FROM [$(DbHN)].dbo.DoTaiLopHocPhan
             WHERE MaMonHoc = ''''DOTAI'''' AND TrangThai <> ''''DA_HUY'''' '') q;',
        @outPush, @o1 = @lop OUTPUT, @o2 = @luot OUTPUT;
    SET @micro += DATEDIFF(MICROSECOND, @t0, SYSUTCDATETIME());
    SET @i += 1;
END
INSERT @kq2 VALUES (1, 'PUSHDOWN', @lop, @luot, 1, @micro / @SoLan);

/* Cách tệ: kéo dòng thô về rồi mới gom tại Master. */
SET @micro = 0;
SET @i = 1;
WHILE @i <= @SoLan
BEGIN
    SET @t0 = SYSUTCDATETIME();
    EXEC sp_executesql N'
        SELECT @o1 = COUNT(*), @o2 = ISNULL(SUM(q.SoLuongDaDangKy), 0)
          FROM OPENQUERY([$(LnkHN)], ''
                SELECT MaMonHoc, TrangThai, SoLuongDaDangKy
                  FROM [$(DbHN)].dbo.DoTaiLopHocPhan'') q
         WHERE q.MaMonHoc = ''DOTAI'' AND q.TrangThai <> ''DA_HUY'';',
        @outPush, @o1 = @lop OUTPUT, @o2 = @luot OUTPUT;
    SET @micro += DATEDIFF(MICROSECOND, @t0, SYSUTCDATETIME());
    SET @i += 1;
END
INSERT @kq2 VALUES (2, 'KEO_THO', @lop, @luot, @SoDongTao, @micro / @SoLan);

EXEC [$(LnkHN)].[$(DbHN)].sys.sp_executesql
     N'DROP TABLE IF EXISTS dbo.DoTaiLopHocPhan;';

PRINT '';
PRINT '=== Pha 2: ' + CAST(@SoDongTao AS VARCHAR(10)) + ' dong tai HN ===';

SELECT ChienLuoc        AS [Chien luoc],
       SoLop            AS [So lop],
       SoLuot           AS [Luot DK],
       SoDongQuaMang    AS [Dong qua mang],
       MicroTB / 1000.0 AS [ms trung binh]
  FROM @kq2
 ORDER BY ThuTu;

PRINT '';
IF (SELECT COUNT(DISTINCT SoLop) FROM @kq2) <> 1
   OR (SELECT COUNT(DISTINCT SoLuot) FROM @kq2) <> 1
    THROW 51921, N'PHA 2: LECH. Pushdown va keo tho cho ket qua khac nhau.', 1;

DECLARE @msPush DECIMAL(10,3) = (SELECT MicroTB / 1000.0 FROM @kq2 WHERE ThuTu = 1),
        @msKeo  DECIMAL(10,3) = (SELECT MicroTB / 1000.0 FROM @kq2 WHERE ThuTu = 2);

PRINT '  PHA 2: PASS — cung ket qua, '
      + CAST(@SoDongTao AS VARCHAR(10)) + ' dong so voi 1 dong qua mang.';
PRINT '  Pushdown ' + CAST(@msPush AS VARCHAR(20)) + ' ms, keo tho '
      + CAST(@msKeo AS VARCHAR(20)) + ' ms.';
GO

SET NOEXEC OFF;
GO
