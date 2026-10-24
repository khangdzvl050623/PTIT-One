/* =====================================================================
   PTIT One — master/08-thong-ke-hoc-vu.sql
   ---------------------------------------------------------------------
   Thống kê học vụ TOÀN HỆ THỐNG cho màn "Tổng quan" của Admin Master.

   CHẠY:  .\run.ps1 -Script master\08-thong-ke-hoc-vu.sql -On MASTER

   Vì sao không dùng lại bốn báo cáo ở master/07: chúng trả hình dạng
   khác. `sp_BaoCaoSiSoMon` bắt buộc có @MaMonHoc và chỉ cho 4 trong 8
   con số màn này cần — thiếu SoSinhVien, DaKhoa, DaCongBo. Hai thủ tục
   dưới đây dùng CÙNG kỹ thuật (EXEC ... AT, gom tại site) nhưng đúng
   hình dạng ReportRepository đọc.

   ⚠️ SoSinhVien là TỔNG số sinh viên phân biệt TẠI MỖI cơ sở. Một sinh
      viên học lớp ở hai cơ sở sẽ được đếm hai lần. Hôm nay con số vẫn
      đúng vì đăng ký liên cơ sở chưa nối; khi nối thì phải gom định danh
      về Master mới đếm phân biệt được.
   ===================================================================== */

SET NOCOUNT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    RAISERROR(N'Chi chay tren $(DbMaster).', 16, 1);
    SET NOEXEC ON;
END
GO

/* =====================================================================
   sp_ThongKeTongQuan — tám con số của màn Tổng quan
   @MaCoSo NULL = toàn hệ thống; khác NULL = đúng một cơ sở.
   ===================================================================== */
CREATE OR ALTER PROCEDURE dbo.sp_ThongKeTongQuan
    @MaHocKy  VARCHAR(20),
    @MaCoSo   VARCHAR(10) = NULL,
    @MaMonHoc VARCHAR(20) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @kq TABLE (MaCoSo VARCHAR(10), SoLop INT, LuotDangKy INT, SoSinhVien INT,
                       TongSucChua INT, TongDaDangKy INT, SoLopDay INT, DaKhoa INT, DaCongBo INT);
    DECLARE @loi TABLE (MaCoSo VARCHAR(10), ThongBao NVARCHAR(400));

    DECLARE @ma VARCHAR(10), @lnk SYSNAME, @db SYSNAME, @trong NVARCHAR(MAX), @goi NVARCHAR(MAX);

    DECLARE cs CURSOR LOCAL FAST_FORWARD FOR
        SELECT MaCoSo, TenLinkedServer, TenDatabase
          FROM dbo.CoSo
         WHERE DangHoatDong = 1 AND (@MaCoSo IS NULL OR MaCoSo = @MaCoSo)
         ORDER BY MaCoSo;
    OPEN cs;
    FETCH NEXT FROM cs INTO @ma, @lnk, @db;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        /* Gom NGAY TẠI SITE: qua mạng chỉ còn một dòng mỗi cơ sở.
           Tập lớp định nghĩa một lần ở CTE Lop, giống hệt ReportRepository
           — các con số khác nhau ở cách đếm, không ở tập lớp. */
        SET @trong = N'
            WITH Lop AS (
                SELECT MaLopHP, MaMonHoc, SoLuongToiDa, SoLuongDaDangKy, TrangThai
                  FROM ' + QUOTENAME(@db) + N'.dbo.LopHocPhan
                 WHERE MaHocKy = ? AND TrangThai IN (''MO'', ''DA_KHOA'')
                   AND (? IS NULL OR MaMonHoc = ?)
            ), GhiDanh AS (
                SELECT d.MaLopHP, d.MaSinhVien
                  FROM ' + QUOTENAME(@db) + N'.dbo.DangKyHocPhan d
                  JOIN Lop l ON l.MaLopHP = d.MaLopHP
                 WHERE d.TrangThai IN (''DANG_XU_LY'', ''DA_DANG_KY'', ''DANG_HUY'')
            ), DiemLop AS (
                SELECT dm.MaLopHP, COUNT(*) AS SoDong,
                       SUM(CASE WHEN dm.NgayCongBo IS NULL THEN 1 ELSE 0 END) AS SoNhap
                  FROM ' + QUOTENAME(@db) + N'.dbo.Diem dm
                  JOIN Lop l ON l.MaLopHP = dm.MaLopHP
                 GROUP BY dm.MaLopHP
            )
            SELECT (SELECT COUNT(*) FROM Lop),
                   (SELECT COUNT(*) FROM GhiDanh),
                   (SELECT COUNT(DISTINCT MaSinhVien) FROM GhiDanh),
                   (SELECT ISNULL(SUM(SoLuongToiDa), 0) FROM Lop),
                   (SELECT ISNULL(SUM(SoLuongDaDangKy), 0) FROM Lop),
                   (SELECT COUNT(*) FROM Lop WHERE SoLuongDaDangKy >= SoLuongToiDa),
                   (SELECT COUNT(*) FROM Lop WHERE TrangThai = ''DA_KHOA''),
                   (SELECT COUNT(*) FROM Lop l JOIN DiemLop x ON x.MaLopHP = l.MaLopHP
                     WHERE l.TrangThai = ''MO'' AND x.SoDong > 0 AND x.SoNhap = 0);';

        BEGIN TRY
            IF @lnk IS NULL
            BEGIN
                /* Cơ sở cục bộ: không có Linked Server để đi qua. */
                SET @goi = REPLACE(REPLACE(@trong, N'MaHocKy = ?', N'MaHocKy = @ky'),
                                   N'(? IS NULL OR MaMonHoc = ?)', N'(@mon IS NULL OR MaMonHoc = @mon)');
                INSERT INTO @kq (SoLop, LuotDangKy, SoSinhVien, TongSucChua, TongDaDangKy,
                                 SoLopDay, DaKhoa, DaCongBo)
                EXEC sp_executesql @goi, N'@ky VARCHAR(20), @mon VARCHAR(20)',
                     @ky = @MaHocKy, @mon = @MaMonHoc;
            END
            ELSE
            BEGIN
                /* ⚠️ Tham số đầu của sp_executesql phải là BIẾN hoặc hằng,
                   không phải biểu thức nối chuỗi — nối thẳng thì lỗi cú pháp
                   báo ở dòng khác hẳn chỗ sai. */
                SET @goi = N'EXEC (@q, @ky, @mon, @mon) AT ' + QUOTENAME(@lnk) + N';';
                INSERT INTO @kq (SoLop, LuotDangKy, SoSinhVien, TongSucChua, TongDaDangKy,
                                 SoLopDay, DaKhoa, DaCongBo)
                EXEC sp_executesql @goi,
                     N'@q NVARCHAR(MAX), @ky VARCHAR(20), @mon VARCHAR(20)',
                     @q = @trong, @ky = @MaHocKy, @mon = @MaMonHoc;
            END
            UPDATE @kq SET MaCoSo = @ma WHERE MaCoSo IS NULL;
        END TRY
        BEGIN CATCH
            /* Một cơ sở tắt máy không được xoá sổ số liệu của các cơ sở còn
               lại — ghi lại rồi đi tiếp. */
            INSERT INTO @loi (MaCoSo, ThongBao) VALUES (@ma, ERROR_MESSAGE());
        END CATCH
        FETCH NEXT FROM cs INTO @ma, @lnk, @db;
    END
    CLOSE cs; DEALLOCATE cs;

    SELECT ISNULL(SUM(SoLop), 0)        AS SoLop,
           ISNULL(SUM(LuotDangKy), 0)   AS LuotDangKy,
           ISNULL(SUM(SoSinhVien), 0)   AS SoSinhVien,
           ISNULL(SUM(TongSucChua), 0)  AS TongSucChua,
           ISNULL(SUM(TongDaDangKy), 0) AS TongDaDangKy,
           ISNULL(SUM(SoLopDay), 0)     AS SoLopDay,
           ISNULL(SUM(DaKhoa), 0)       AS DaKhoa,
           ISNULL(SUM(DaCongBo), 0)     AS DaCongBo,
           (SELECT COUNT(*) FROM @loi)  AS SoCoSoLoi
      FROM @kq;
END
GO
PRINT '  [+] dbo.sp_ThongKeTongQuan';
GO

/* =====================================================================
   sp_ThongKeTheoMon — một dòng mỗi môn, gộp mọi cơ sở trong phạm vi
   ===================================================================== */
CREATE OR ALTER PROCEDURE dbo.sp_ThongKeTheoMon
    @MaHocKy  VARCHAR(20),
    @MaCoSo   VARCHAR(10) = NULL,
    @NguongDat DECIMAL(3,1) = 4.0
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @kq TABLE (MaMonHoc VARCHAR(20), SoLop INT, LuotDangKy INT, TongSucChua INT,
                       TongDaDangKy INT, Dat INT, Truot INT,
                       K1 INT, K2 INT, K3 INT, K4 INT, K5 INT);

    DECLARE @ma VARCHAR(10), @lnk SYSNAME, @db SYSNAME, @trong NVARCHAR(MAX), @goi NVARCHAR(MAX);

    DECLARE cs CURSOR LOCAL FAST_FORWARD FOR
        SELECT MaCoSo, TenLinkedServer, TenDatabase
          FROM dbo.CoSo
         WHERE DangHoatDong = 1 AND (@MaCoSo IS NULL OR MaCoSo = @MaCoSo)
         ORDER BY MaCoSo;
    OPEN cs;
    FETCH NEXT FROM cs INTO @ma, @lnk, @db;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        /* Qua mạng chỉ còn vài chục dòng (một dòng mỗi môn), không phải
           từng dòng điểm. */
        SET @trong = N'
            WITH Lop AS (
                SELECT MaLopHP, MaMonHoc, SoLuongToiDa, SoLuongDaDangKy
                  FROM ' + QUOTENAME(@db) + N'.dbo.LopHocPhan
                 WHERE MaHocKy = ? AND TrangThai IN (''MO'', ''DA_KHOA'')
            ), GhiDanh AS (
                SELECT l.MaMonHoc
                  FROM ' + QUOTENAME(@db) + N'.dbo.DangKyHocPhan d
                  JOIN Lop l ON l.MaLopHP = d.MaLopHP
                 WHERE d.TrangThai IN (''DANG_XU_LY'', ''DA_DANG_KY'', ''DANG_HUY'')
            ), K AS (
                SELECT l.MaMonHoc,
                       SUM(CASE WHEN d.DiemTongKet >= ? THEN 1 ELSE 0 END) AS Dat,
                       SUM(CASE WHEN d.DiemTongKet <  ? THEN 1 ELSE 0 END) AS Truot,
                       SUM(CASE WHEN d.DiemTongKet < 4.0 THEN 1 ELSE 0 END) AS K1,
                       SUM(CASE WHEN d.DiemTongKet >= 4.0 AND d.DiemTongKet < 5.5 THEN 1 ELSE 0 END) AS K2,
                       SUM(CASE WHEN d.DiemTongKet >= 5.5 AND d.DiemTongKet < 7.0 THEN 1 ELSE 0 END) AS K3,
                       SUM(CASE WHEN d.DiemTongKet >= 7.0 AND d.DiemTongKet < 8.5 THEN 1 ELSE 0 END) AS K4,
                       SUM(CASE WHEN d.DiemTongKet >= 8.5 THEN 1 ELSE 0 END) AS K5
                  FROM ' + QUOTENAME(@db) + N'.dbo.Diem d
                  JOIN Lop l ON l.MaLopHP = d.MaLopHP
                 WHERE d.NgayCongBo IS NOT NULL AND d.DiemTongKet IS NOT NULL
                 GROUP BY l.MaMonHoc
            )
            SELECT c.MaMonHoc, c.SoLop, ISNULL(g.Luot, 0), c.TongSucChua, c.TongDaDangKy,
                   ISNULL(k.Dat, 0), ISNULL(k.Truot, 0),
                   ISNULL(k.K1, 0), ISNULL(k.K2, 0), ISNULL(k.K3, 0), ISNULL(k.K4, 0), ISNULL(k.K5, 0)
              FROM (SELECT MaMonHoc, COUNT(*) AS SoLop,
                           SUM(SoLuongToiDa) AS TongSucChua, SUM(SoLuongDaDangKy) AS TongDaDangKy
                      FROM Lop GROUP BY MaMonHoc) c
              LEFT JOIN (SELECT MaMonHoc, COUNT(*) AS Luot FROM GhiDanh GROUP BY MaMonHoc) g
                     ON g.MaMonHoc = c.MaMonHoc
              LEFT JOIN K k ON k.MaMonHoc = c.MaMonHoc;';

        BEGIN TRY
            IF @lnk IS NULL
            BEGIN
                SET @goi = REPLACE(REPLACE(@trong, N'MaHocKy = ?', N'MaHocKy = @ky'),
                                   N'>= ? THEN', N'>= @ng THEN');
                SET @goi = REPLACE(@goi, N'<  ? THEN', N'<  @ng THEN');
                INSERT INTO @kq
                EXEC sp_executesql @goi, N'@ky VARCHAR(20), @ng DECIMAL(3,1)',
                     @ky = @MaHocKy, @ng = @NguongDat;
            END
            ELSE
            BEGIN
                SET @goi = N'EXEC (@q, @ky, @ng, @ng) AT ' + QUOTENAME(@lnk) + N';';
                INSERT INTO @kq
                EXEC sp_executesql @goi,
                     N'@q NVARCHAR(MAX), @ky VARCHAR(20), @ng DECIMAL(3,1)',
                     @q = @trong, @ky = @MaHocKy, @ng = @NguongDat;
            END
        END TRY
        BEGIN CATCH
            /* Bỏ qua cơ sở không phản hồi, xem ghi chú ở sp_ThongKeTongQuan. */
        END CATCH
        FETCH NEXT FROM cs INTO @ma, @lnk, @db;
    END
    CLOSE cs; DEALLOCATE cs;

    /* TenMonHoc lấy ở Master — danh mục là dữ liệu tham chiếu, không cần
       hỏi site. */
    SELECT k.MaMonHoc, m.TenMonHoc,
           SUM(k.SoLop) AS SoLop, SUM(k.LuotDangKy) AS LuotDangKy,
           SUM(k.TongSucChua) AS TongSucChua, SUM(k.TongDaDangKy) AS TongDaDangKy,
           SUM(k.Dat) AS Dat, SUM(k.Truot) AS Truot,
           SUM(k.K1) AS K1, SUM(k.K2) AS K2, SUM(k.K3) AS K3, SUM(k.K4) AS K4, SUM(k.K5) AS K5
      FROM @kq k
      JOIN dbo.MonHoc m ON m.MaMonHoc = k.MaMonHoc
     GROUP BY k.MaMonHoc, m.TenMonHoc
     ORDER BY k.MaMonHoc;
END
GO
PRINT '  [+] dbo.sp_ThongKeTheoMon';
GO

SET NOEXEC OFF;
GO
