/* =====================================================================
   PTIT One — master/07-bao-cao-phan-tan.sql
   ---------------------------------------------------------------------
   ⭐ YÊU CẦU BẮT BUỘC SỐ 5 — TRUY VẤN PHÂN TÁN.

   Bốn báo cáo toàn hệ thống. Dữ liệu nằm rải ở các cơ sở và KHÔNG được
   nhân bản về đây (sĩ số đổi từng giây, mirror về là cũ ngay), nên phải
   hỏi từng site đúng lúc cần.

   HAI KỸ THUẬT, DÙNG ĐÚNG CHỖ — theo khuyến nghị ở D2:

     OPENQUERY    cho báo cáo KHÔNG tham số. Chuỗi truy vấn gửi nguyên
                  sang site, site tự lập plan và tự gom, chỉ trả về kết
                  quả cuối. Nhược: không nhận biến.

     EXEC ... AT  cho báo cáo CÓ tham số. Cũng là pass-through, nhưng
                  tham số hoá được nên không phải nối chuỗi do người dùng
                  nhập vào.

   Tên gọi đúng trong báo cáo: đẩy WHERE/GROUP BY xuống nơi chứa dữ liệu
   là **aggregate pushdown**, một chiến lược tối ưu hoá truy vấn phân tán.

   ⚠️ KHÔNG mặc định OPENQUERY luôn nhanh hơn. Khi kết quả remote vốn đã
      lớn, hoặc optimizer đã đẩy được predicate tốt, four-part name có thể
      hoà hoặc thắng. Đo bằng db\tests\92-do-truy-van-phan-tan.sql.

   ⚠️ BÊN TRONG OPENQUERY PHẢI GHI RÕ TÊN DATABASE. Truy vấn chạy trong
      database MẶC ĐỊNH của login ở đầu xa, không phải database ta nghĩ.
      Đây đúng lỗi đã vấp ở db\spike\01-kiem-dtc.sql.

   CHẠY Ở ĐÂU : CHỈ trên PTITONE_MASTER
                  .\run.ps1 -Script master\07-bao-cao-phan-tan.sql -On MASTER
   ===================================================================== */

SET NOCOUNT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    DECLARE @DbHienTai SYSNAME = DB_NAME();
    RAISERROR(N'Script nay CHI chay tren $(DbMaster). Database hien tai: %s',
              16, 1, @DbHienTai);
    SET NOEXEC ON;
END
GO

/* =====================================================================
   dbo.fn_TienToCoSo — tiền tố bốn phần của một cơ sở
   ---------------------------------------------------------------------
   Mọi thủ tục dưới đây lấy tên server và database TỪ BẢNG CoSo, không
   bao giờ nhận từ tham số. Mã cơ sở do người dùng gửi chỉ được dùng để
   TRA bảng; tra không ra thì dừng. Đó là lớp chặn SQL injection.
   ===================================================================== */
CREATE OR ALTER FUNCTION dbo.fn_TienToCoSo (@MaCoSo VARCHAR(10))
RETURNS NVARCHAR(300)
AS
BEGIN
    DECLARE @r NVARCHAR(300);
    SELECT @r = CASE WHEN TenLinkedServer IS NULL
                     THEN QUOTENAME(TenDatabase) + N'.dbo.'
                     ELSE QUOTENAME(TenLinkedServer) + N'.'
                        + QUOTENAME(TenDatabase) + N'.dbo.' END
      FROM dbo.CoSo
     WHERE MaCoSo = @MaCoSo AND DangHoatDong = 1;
    RETURN @r;
END
GO
PRINT '  [+] dbo.fn_TienToCoSo';
GO

/* =====================================================================
   BÁO CÁO 1 — Sĩ số đăng ký một môn trên TOÀN hệ thống
   ---------------------------------------------------------------------
   Aggregate thuần. Đây là báo cáo minh hoạ pushdown rõ nhất: mỗi site tự
   đếm trên dữ liệu của mình rồi trả về MỘT dòng, thay vì gửi hàng nghìn
   dòng đăng ký thô qua mạng để Master đếm hộ.

   Có tham số (@MaMonHoc, @MaHocKy) nên dùng EXEC ... AT.
   ===================================================================== */
CREATE OR ALTER PROCEDURE dbo.sp_BaoCaoSiSoMon
    @MaMonHoc VARCHAR(20),
    @MaHocKy  VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;

    /* Site khong phan hoi thi GHI LAI roi di tiep, khong lam chet ca bao
       cao. Mot co so tat may khong duoc phep xoa so lieu cua ba co so con
       lai — do la ca kiem thu G4. */
    DECLARE @loi TABLE (MaCoSo VARCHAR(10), ThongBao NVARCHAR(400));

    DECLARE @kq TABLE (MaCoSo VARCHAR(10), SoLop INT, SoLuot INT, SucChua INT);

    DECLARE @ma VARCHAR(10), @lnk SYSNAME, @db SYSNAME,
            @trong NVARCHAR(MAX), @ngoai NVARCHAR(MAX);

    DECLARE cs CURSOR LOCAL FAST_FORWARD FOR
        SELECT MaCoSo, TenLinkedServer, TenDatabase
          FROM dbo.CoSo WHERE DangHoatDong = 1 ORDER BY MaCoSo;
    OPEN cs;
    FETCH NEXT FROM cs INTO @ma, @lnk, @db;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        /* Gom NGAY TẠI SITE. Qua mạng chỉ còn đúng một dòng. */
        SET @trong = N'
            SELECT COUNT(*), ISNULL(SUM(l.SoLuongDaDangKy),0), ISNULL(SUM(l.SoLuongToiDa),0)
              FROM ' + QUOTENAME(@db) + N'.dbo.LopHocPhan l
             WHERE l.MaMonHoc = ? AND l.MaHocKy = ? AND l.TrangThai <> ''DA_HUY'';';

        BEGIN TRY
            IF @lnk IS NULL
            BEGIN
                /* Cơ sở cục bộ: không có Linked Server để đi qua. */
                DECLARE @cucBo NVARCHAR(MAX) = REPLACE(@trong, N'?', N'@p');
                SET @cucBo = N'
                    SELECT COUNT(*), ISNULL(SUM(l.SoLuongDaDangKy),0), ISNULL(SUM(l.SoLuongToiDa),0)
                      FROM ' + QUOTENAME(@db) + N'.dbo.LopHocPhan l
                     WHERE l.MaMonHoc = @mon AND l.MaHocKy = @ky AND l.TrangThai <> ''DA_HUY'';';
                INSERT INTO @kq (SoLop, SoLuot, SucChua)
                EXEC sp_executesql @cucBo, N'@mon VARCHAR(20), @ky VARCHAR(20)',
                     @mon = @MaMonHoc, @ky = @MaHocKy;
            END
            ELSE
            BEGIN
                /* EXEC ... AT nhận tham số theo vị trí của dấu `?`, nên giá trị
                   do người dùng nhập KHÔNG bị nối vào chuỗi. */
                SET @ngoai = N'EXEC (@q, @mon, @ky) AT ' + QUOTENAME(@lnk) + N';';
                INSERT INTO @kq (SoLop, SoLuot, SucChua)
                EXEC sp_executesql @ngoai,
                     N'@q NVARCHAR(MAX), @mon VARCHAR(20), @ky VARCHAR(20)',
                     @q = @trong, @mon = @MaMonHoc, @ky = @MaHocKy;
            END

            UPDATE @kq SET MaCoSo = @ma WHERE MaCoSo IS NULL;
        END TRY
        BEGIN CATCH
            INSERT INTO @loi (MaCoSo, ThongBao) VALUES (@ma, ERROR_MESSAGE());
        END CATCH
        FETCH NEXT FROM cs INTO @ma, @lnk, @db;
    END
    CLOSE cs; DEALLOCATE cs;

    SELECT MaCoSo        AS [Co so],
           SoLop         AS [So lop],
           SoLuot        AS [Luot dang ky],
           SucChua       AS [Suc chua],
           CASE WHEN SucChua = 0 THEN NULL
                ELSE CAST(100.0 * SoLuot / SucChua AS DECIMAL(5,1)) END AS [Lap day %]
      FROM @kq
     ORDER BY MaCoSo;

    SELECT N'TOAN HE THONG'     AS [Pham vi],
           SUM(SoLop)           AS [So lop],
           SUM(SoLuot)          AS [Luot dang ky],
           SUM(SucChua)         AS [Suc chua]
      FROM @kq;

    IF EXISTS (SELECT 1 FROM @loi)
        SELECT MaCoSo AS [Co so KHONG phan hoi], ThongBao AS [Ly do] FROM @loi;
END
GO
PRINT '  [+] dbo.sp_BaoCaoSiSoMon';
GO

/* =====================================================================
   BÁO CÁO 2 — Phân bố điểm theo cơ sở cho một môn
   ---------------------------------------------------------------------
   GROUP BY nhiều chiều. Mỗi site tự phân nhóm điểm của mình; qua mạng
   chỉ còn vài dòng thống kê chứ không phải từng dòng điểm.
   ===================================================================== */
CREATE OR ALTER PROCEDURE dbo.sp_BaoCaoPhanBoDiem
    @MaMonHoc VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;

    /* Site khong phan hoi thi GHI LAI roi di tiep, khong lam chet ca bao
       cao. Mot co so tat may khong duoc phep xoa so lieu cua ba co so con
       lai — do la ca kiem thu G4. */
    DECLARE @loi TABLE (MaCoSo VARCHAR(10), ThongBao NVARCHAR(400));

    DECLARE @kq TABLE (MaCoSo VARCHAR(10), Nhom VARCHAR(20), SoLuong INT);
    DECLARE @ma VARCHAR(10), @lnk SYSNAME, @db SYSNAME, @trong NVARCHAR(MAX);

    DECLARE cs CURSOR LOCAL FAST_FORWARD FOR
        SELECT MaCoSo, TenLinkedServer, TenDatabase
          FROM dbo.CoSo WHERE DangHoatDong = 1 ORDER BY MaCoSo;
    OPEN cs;
    FETCH NEXT FROM cs INTO @ma, @lnk, @db;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @trong = N'
            SELECT CASE WHEN d.DiemTongKet IS NULL THEN ''CHUA CO DIEM''
                        WHEN d.DiemTongKet >= 8.5  THEN ''GIOI''
                        WHEN d.DiemTongKet >= 7.0  THEN ''KHA''
                        WHEN d.DiemTongKet >= 5.0  THEN ''TRUNG BINH''
                        ELSE ''KHONG DAT'' END,
                   COUNT(*)
              FROM ' + QUOTENAME(@db) + N'.dbo.Diem d
              JOIN ' + QUOTENAME(@db) + N'.dbo.LopHocPhan l ON l.MaLopHP = d.MaLopHP
             WHERE l.MaMonHoc = ?
             GROUP BY CASE WHEN d.DiemTongKet IS NULL THEN ''CHUA CO DIEM''
                           WHEN d.DiemTongKet >= 8.5  THEN ''GIOI''
                           WHEN d.DiemTongKet >= 7.0  THEN ''KHA''
                           WHEN d.DiemTongKet >= 5.0  THEN ''TRUNG BINH''
                           ELSE ''KHONG DAT'' END;';

        BEGIN TRY
            IF @lnk IS NULL
            BEGIN
                DECLARE @cucBo2 NVARCHAR(MAX) = REPLACE(@trong, N'l.MaMonHoc = ?', N'l.MaMonHoc = @mon');
                INSERT INTO @kq (Nhom, SoLuong)
                EXEC sp_executesql @cucBo2, N'@mon VARCHAR(20)', @mon = @MaMonHoc;
            END
            ELSE
            BEGIN
                DECLARE @ngoai2 NVARCHAR(MAX) = N'EXEC (@q, @mon) AT ' + QUOTENAME(@lnk) + N';';
                INSERT INTO @kq (Nhom, SoLuong)
                EXEC sp_executesql @ngoai2, N'@q NVARCHAR(MAX), @mon VARCHAR(20)',
                     @q = @trong, @mon = @MaMonHoc;
            END

            UPDATE @kq SET MaCoSo = @ma WHERE MaCoSo IS NULL;
        END TRY
        BEGIN CATCH
            INSERT INTO @loi (MaCoSo, ThongBao) VALUES (@ma, ERROR_MESSAGE());
        END CATCH
        FETCH NEXT FROM cs INTO @ma, @lnk, @db;
    END
    CLOSE cs; DEALLOCATE cs;

    SELECT MaCoSo AS [Co so], Nhom AS [Xep loai], SoLuong AS [So sinh vien]
      FROM @kq
     ORDER BY MaCoSo,
              CASE Nhom WHEN 'GIOI' THEN 1 WHEN 'KHA' THEN 2
                        WHEN 'TRUNG BINH' THEN 3 WHEN 'KHONG DAT' THEN 4 ELSE 5 END;

    IF EXISTS (SELECT 1 FROM @loi)
        SELECT MaCoSo AS [Co so KHONG phan hoi], ThongBao AS [Ly do] FROM @loi;
END
GO
PRINT '  [+] dbo.sp_BaoCaoPhanBoDiem';
GO

/* =====================================================================
   BÁO CÁO 3 ⭐ — Sinh viên học liên cơ sở
   ---------------------------------------------------------------------
   Đây là báo cáo THUYẾT PHỤC NHẤT cho yêu cầu số 5, vì nó là phép nối
   chéo site THẬT: dòng đăng ký nằm ở database của cơ sở MỞ LỚP, còn hồ
   sơ sinh viên nằm ở database của cơ sở NHÀ. Ba báo cáo kia chỉ là phép
   gom, mô phỏng bằng cách khác được; cái này thì không.

   Chạy hai lượt, và đó chính là một **semi-join phân tán**:
     Lượt 1 — hỏi mỗi site: ai đang học ở đây mà nhà ở nơi khác?
     Lượt 2 — chỉ với những cơ sở nhà thực sự xuất hiện ở lượt 1, lấy về
              trạng thái hồ sơ. Không hỏi cơ sở nào không liên quan.

   Không tham số nên dùng OPENQUERY.
   ===================================================================== */
CREATE OR ALTER PROCEDURE dbo.sp_BaoCaoSinhVienLienCoSo
AS
BEGIN
    SET NOCOUNT ON;

    /* Site khong phan hoi thi GHI LAI roi di tiep, khong lam chet ca bao
       cao. Mot co so tat may khong duoc phep xoa so lieu cua ba co so con
       lai — do la ca kiem thu G4. */
    DECLARE @loi TABLE (MaCoSo VARCHAR(10), ThongBao NVARCHAR(400));

    DECLARE @ghiDanh TABLE (
        MaCoSoHost   VARCHAR(10),
        MaCoSoNhaSV  VARCHAR(10),
        MaSinhVien   VARCHAR(20),
        HoTenSinhVien NVARCHAR(150),
        MaLopHP      VARCHAR(80),
        MaMonHoc     VARCHAR(20),
        MaHocKy      VARCHAR(20));

    DECLARE @hoSo TABLE (
        MaCoSoNha    VARCHAR(10),
        MaSinhVien   VARCHAR(20),
        TrangThai    VARCHAR(20),
        SoTinChiTichLuy SMALLINT);

    DECLARE @ma VARCHAR(10), @lnk SYSNAME, @db SYSNAME, @sql NVARCHAR(MAX);

    /* ---- Lượt 1: hỏi từng site về ghi danh của sinh viên khách ---- */
    DECLARE cs CURSOR LOCAL FAST_FORWARD FOR
        SELECT MaCoSo, TenLinkedServer, TenDatabase
          FROM dbo.CoSo WHERE DangHoatDong = 1 ORDER BY MaCoSo;
    OPEN cs;
    FETCH NEXT FROM cs INTO @ma, @lnk, @db;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        BEGIN TRY
            IF @lnk IS NULL
                SET @sql = N'
                    SELECT dk.MaCoSoNhaSV, dk.MaSinhVien, dk.HoTenSinhVien,
                           dk.MaLopHP, l.MaMonHoc, l.MaHocKy
                      FROM ' + QUOTENAME(@db) + N'.dbo.DangKyHocPhan dk
                      JOIN ' + QUOTENAME(@db) + N'.dbo.LopHocPhan   l ON l.MaLopHP = dk.MaLopHP
                     WHERE dk.MaCoSoNhaSV <> ''' + @ma + N''' AND dk.TrangThai = ''DA_DANG_KY'';';
            ELSE
                /* Chuỗi bên trong OPENQUERY phải nhân đôi dấu nháy đơn. */
                SET @sql = N'
                    SELECT * FROM OPENQUERY(' + QUOTENAME(@lnk) + N', ''
                        SELECT dk.MaCoSoNhaSV, dk.MaSinhVien, dk.HoTenSinhVien,
                               dk.MaLopHP, l.MaMonHoc, l.MaHocKy
                          FROM ' + QUOTENAME(@db) + N'.dbo.DangKyHocPhan dk
                          JOIN ' + QUOTENAME(@db) + N'.dbo.LopHocPhan   l ON l.MaLopHP = dk.MaLopHP
                         WHERE dk.MaCoSoNhaSV <> ''''' + @ma + N''''' AND dk.TrangThai = ''''DA_DANG_KY''''
                    '');';

            INSERT INTO @ghiDanh (MaCoSoNhaSV, MaSinhVien, HoTenSinhVien, MaLopHP, MaMonHoc, MaHocKy)
            EXEC sp_executesql @sql;

            UPDATE @ghiDanh SET MaCoSoHost = @ma WHERE MaCoSoHost IS NULL;
        END TRY
        BEGIN CATCH
            INSERT INTO @loi (MaCoSo, ThongBao) VALUES (@ma, ERROR_MESSAGE());
        END CATCH
        FETCH NEXT FROM cs INTO @ma, @lnk, @db;
    END
    CLOSE cs; DEALLOCATE cs;

    /* ---- Lượt 2: chỉ hỏi những cơ sở NHÀ có mặt ở lượt 1 ---- */
    DECLARE cs2 CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT c.MaCoSo, c.TenLinkedServer, c.TenDatabase
          FROM dbo.CoSo c
         WHERE c.DangHoatDong = 1
           AND EXISTS (SELECT 1 FROM @ghiDanh g WHERE g.MaCoSoNhaSV = c.MaCoSo);
    OPEN cs2;
    FETCH NEXT FROM cs2 INTO @ma, @lnk, @db;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        BEGIN TRY
            IF @lnk IS NULL
                SET @sql = N'SELECT MaSinhVien, TrangThai, SoTinChiTichLuy
                               FROM ' + QUOTENAME(@db) + N'.dbo.SinhVien;';
            ELSE
                SET @sql = N'SELECT * FROM OPENQUERY(' + QUOTENAME(@lnk) + N', ''
                                SELECT MaSinhVien, TrangThai, SoTinChiTichLuy
                                  FROM ' + QUOTENAME(@db) + N'.dbo.SinhVien'');';

            INSERT INTO @hoSo (MaSinhVien, TrangThai, SoTinChiTichLuy)
            EXEC sp_executesql @sql;

            UPDATE @hoSo SET MaCoSoNha = @ma WHERE MaCoSoNha IS NULL;
        END TRY
        BEGIN CATCH
            INSERT INTO @loi (MaCoSo, ThongBao) VALUES (@ma, ERROR_MESSAGE());
        END CATCH
        FETCH NEXT FROM cs2 INTO @ma, @lnk, @db;
    END
    CLOSE cs2; DEALLOCATE cs2;

    /* ---- Nối hai mảnh: đây là phần JOIN chéo site ---- */
    SELECT g.MaSinhVien        AS [Ma SV],
           g.HoTenSinhVien     AS [Ho ten],
           g.MaCoSoNhaSV       AS [Co so nha],
           g.MaCoSoHost        AS [Hoc tai],
           g.MaMonHoc          AS [Mon],
           g.MaLopHP           AS [Lop],
           g.MaHocKy           AS [Hoc ky],
           h.TrangThai         AS [Trang thai tai co so nha],
           h.SoTinChiTichLuy   AS [Tin chi tich luy]
      FROM @ghiDanh g
      LEFT JOIN @hoSo h
             ON h.MaCoSoNha = g.MaCoSoNhaSV
            AND h.MaSinhVien = g.MaSinhVien
     ORDER BY g.MaCoSoNhaSV, g.MaSinhVien, g.MaHocKy;

    /* Hồ sơ không tìm thấy ở cơ sở nhà là dấu hiệu dữ liệu lệch giữa hai
       site — đáng báo riêng chứ không nên để lẫn vào bảng trên. */
    IF EXISTS (SELECT 1 FROM @ghiDanh g
                LEFT JOIN @hoSo h ON h.MaCoSoNha = g.MaCoSoNhaSV
                                 AND h.MaSinhVien = g.MaSinhVien
                WHERE h.MaSinhVien IS NULL)
        SELECT N'CANH BAO: co ghi danh lien co so ma khong tim thay ho so o co so nha'
               AS [Canh bao];

    IF EXISTS (SELECT 1 FROM @loi)
        SELECT MaCoSo AS [Co so KHONG phan hoi], ThongBao AS [Ly do] FROM @loi;
END
GO
PRINT '  [+] dbo.sp_BaoCaoSinhVienLienCoSo';
GO

/* =====================================================================
   BÁO CÁO 4 — Tỉ lệ lớp đầy / còn chỗ theo cơ sở
   ---------------------------------------------------------------------
   So sánh giữa các site. Không tham số nên dùng OPENQUERY.
   ===================================================================== */
CREATE OR ALTER PROCEDURE dbo.sp_BaoCaoTiLeLopDay
AS
BEGIN
    SET NOCOUNT ON;

    /* Site khong phan hoi thi GHI LAI roi di tiep, khong lam chet ca bao
       cao. Mot co so tat may khong duoc phep xoa so lieu cua ba co so con
       lai — do la ca kiem thu G4. */
    DECLARE @loi TABLE (MaCoSo VARCHAR(10), ThongBao NVARCHAR(400));

    DECLARE @kq TABLE (MaCoSo VARCHAR(10), SoLop INT, SoLopDay INT, SoLopConCho INT);
    DECLARE @ma VARCHAR(10), @lnk SYSNAME, @db SYSNAME, @sql NVARCHAR(MAX);

    DECLARE cs CURSOR LOCAL FAST_FORWARD FOR
        SELECT MaCoSo, TenLinkedServer, TenDatabase
          FROM dbo.CoSo WHERE DangHoatDong = 1 ORDER BY MaCoSo;
    OPEN cs;
    FETCH NEXT FROM cs INTO @ma, @lnk, @db;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        BEGIN TRY
            IF @lnk IS NULL
                SET @sql = N'
                    SELECT COUNT(*),
                           SUM(CASE WHEN SoLuongDaDangKy >= SoLuongToiDa THEN 1 ELSE 0 END),
                           SUM(CASE WHEN SoLuongDaDangKy <  SoLuongToiDa THEN 1 ELSE 0 END)
                      FROM ' + QUOTENAME(@db) + N'.dbo.LopHocPhan
                     WHERE TrangThai = ''MO'';';
            ELSE
                SET @sql = N'
                    SELECT * FROM OPENQUERY(' + QUOTENAME(@lnk) + N', ''
                        SELECT COUNT(*) AS SoLop,
                               SUM(CASE WHEN SoLuongDaDangKy >= SoLuongToiDa THEN 1 ELSE 0 END) AS SoLopDay,
                               SUM(CASE WHEN SoLuongDaDangKy <  SoLuongToiDa THEN 1 ELSE 0 END) AS SoLopConCho
                          FROM ' + QUOTENAME(@db) + N'.dbo.LopHocPhan
                         WHERE TrangThai = ''''MO'''''');';

            INSERT INTO @kq (SoLop, SoLopDay, SoLopConCho) EXEC sp_executesql @sql;
            UPDATE @kq SET MaCoSo = @ma WHERE MaCoSo IS NULL;
        END TRY
        BEGIN CATCH
            INSERT INTO @loi (MaCoSo, ThongBao) VALUES (@ma, ERROR_MESSAGE());
        END CATCH
        FETCH NEXT FROM cs INTO @ma, @lnk, @db;
    END
    CLOSE cs; DEALLOCATE cs;

    SELECT MaCoSo       AS [Co so],
           SoLop        AS [Lop dang mo],
           SoLopDay     AS [Da day],
           SoLopConCho  AS [Con cho],
           CASE WHEN SoLop = 0 THEN NULL
                ELSE CAST(100.0 * SoLopDay / SoLop AS DECIMAL(5,1)) END AS [Ti le day %]
      FROM @kq
     ORDER BY MaCoSo;

    IF EXISTS (SELECT 1 FROM @loi)
        SELECT MaCoSo AS [Co so KHONG phan hoi], ThongBao AS [Ly do] FROM @loi;
END
GO
PRINT '  [+] dbo.sp_BaoCaoTiLeLopDay';
GO

PRINT '';
PRINT '  ------------------------------------------------------------';
PRINT '  Bon bao cao phan tan da san sang.';
PRINT '  Do ba chien luoc: db\tests\92-do-truy-van-phan-tan.sql';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
