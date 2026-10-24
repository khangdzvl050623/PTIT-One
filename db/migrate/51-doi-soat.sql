/* =====================================================================
   PTIT One — migrate/51-doi-soat.sql
   ---------------------------------------------------------------------
   ĐỐI SOÁT SAU KHI TÁCH — bắt buộc chạy, và bắt buộc đọc kết quả.

   Phép kiểm cốt lõi của phân mảnh ngang:

       tổng số dòng ở MỌI mảnh  ==  số dòng ở CENTRAL

   Lệch một dòng nghĩa là **vị từ phân mảnh sai**: hoặc có dòng rơi ra
   ngoài mọi mảnh (mất dữ liệu), hoặc có dòng lọt vào hai mảnh (nhân đôi).
   Cả hai đều là lỗi nặng, và cả hai đều KHÔNG tự lộ ra khi dùng ứng dụng
   — chỉ phép đếm này mới thấy.

   Đây cũng chính là bằng chứng nộp cho yêu cầu bắt buộc số 1.

   CHẠY Ở ĐÂU : .\run.ps1 -Script migrate\51-doi-soat.sql -On MASTER
   ===================================================================== */

SET NOCOUNT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    DECLARE @DbHienTai SYSNAME = DB_NAME();
    RAISERROR(N'Script nay CHI chay tren $(DbMaster). Database hien tai: %s', 16, 1, @DbHienTai);
    SET NOEXEC ON;
END
GO

DECLARE @bang TABLE (ThuTu INT, Ten SYSNAME);
INSERT INTO @bang VALUES
 (1,'SinhVien'), (2,'GiangVien'), (3,'TaiKhoan'), (4,'SinhVienHocKy'),
 (5,'DotDangKy'), (6,'LopHocPhan'), (7,'LichHoc'), (8,'DangKyHocPhan'),
 (9,'DangKyMonHoc'), (10,'Diem');

DECLARE @kq TABLE (
    ThuTu     INT,
    Bang      SYSNAME,
    OCentral  INT,
    TongMoiNoi INT,
    Lech      INT,
    KetLuan   VARCHAR(20));

DECLARE @khongToi TABLE (MaCoSo VARCHAR(10), ThongBao NVARCHAR(400));

DECLARE @thu INT, @ten SYSNAME, @sql NVARCHAR(MAX), @n INT, @tong INT;
DECLARE @site VARCHAR(10), @lnk SYSNAME, @db SYSNAME, @pre NVARCHAR(300);

DECLARE bg CURSOR LOCAL FAST_FORWARD FOR SELECT ThuTu, Ten FROM @bang ORDER BY ThuTu;
OPEN bg;
FETCH NEXT FROM bg INTO @thu, @ten;

WHILE @@FETCH_STATUS = 0
BEGIN
    /* --- Số dòng ở nguồn --- */
    SET @sql = N'SELECT @x = COUNT(*) FROM [$(DbCentral)].dbo.' + QUOTENAME(@ten) + N';';
    EXEC sp_executesql @sql, N'@x INT OUTPUT', @x = @n OUTPUT;

    /* --- Cộng dồn qua mọi mảnh --- */
    SET @tong = 0;

    DECLARE cs CURSOR LOCAL FAST_FORWARD FOR
        SELECT MaCoSo, TenLinkedServer, TenDatabase
          FROM dbo.CoSo WHERE DangHoatDong = 1 ORDER BY MaCoSo;
    OPEN cs;
    FETCH NEXT FROM cs INTO @site, @lnk, @db;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @pre = CASE WHEN @lnk IS NULL
                        THEN QUOTENAME(@db) + N'.'
                        ELSE QUOTENAME(@lnk) + N'.' + QUOTENAME(@db) + N'.' END;
        BEGIN TRY
            DECLARE @nSite INT;
            SET @sql = N'SELECT @x = COUNT(*) FROM ' + @pre + N'dbo.' + QUOTENAME(@ten) + N';';
            EXEC sp_executesql @sql, N'@x INT OUTPUT', @x = @nSite OUTPUT;
            SET @tong = @tong + @nSite;
        END TRY
        BEGIN CATCH
            /* Ghi một lần cho mỗi cơ sở, không lặp lại theo từng bảng. */
            IF NOT EXISTS (SELECT 1 FROM @khongToi WHERE MaCoSo = @site)
                INSERT INTO @khongToi VALUES (@site, ERROR_MESSAGE());
        END CATCH
        FETCH NEXT FROM cs INTO @site, @lnk, @db;
    END
    CLOSE cs; DEALLOCATE cs;

    INSERT INTO @kq (ThuTu, Bang, OCentral, TongMoiNoi, Lech, KetLuan)
    VALUES (@thu, @ten, @n, @tong, @tong - @n,
            CASE WHEN @tong = @n THEN 'KHOP' ELSE 'LECH' END);

    FETCH NEXT FROM bg INTO @thu, @ten;
END
CLOSE bg; DEALLOCATE bg;

/* =====================================================================
   KẾT QUẢ
   ===================================================================== */
SELECT Bang            AS [Bang],
       OCentral        AS [O CENTRAL],
       TongMoiNoi      AS [Tong cac manh],
       Lech            AS [Lech],
       KetLuan         AS [Ket luan]
  FROM @kq
 ORDER BY ThuTu;

/* Danh bạ không nằm trong phép cộng trên: nó ở Master, không phân mảnh.
   Nhưng mỗi tài khoản phải có đúng một dòng danh bạ, nếu không thì
   SiteContext không định tuyến được người đó. */
DECLARE @tkCentral INT, @danhBa INT;
DECLARE @s NVARCHAR(MAX) = N'SELECT @a = COUNT(*) FROM [$(DbCentral)].dbo.TaiKhoan;';
EXEC sp_executesql @s, N'@a INT OUTPUT', @a = @tkCentral OUTPUT;
SELECT @danhBa = COUNT(*) FROM dbo.DanhBaNguoiDung WHERE LoaiNguoiDung <> 'ADMIN_MASTER';

SELECT N'DanhBaNguoiDung tai Master' AS [Kiem rieng],
       @tkCentral                    AS [TaiKhoan o CENTRAL],
       @danhBa                       AS [Dong danh ba],
       CASE WHEN @danhBa = @tkCentral THEN 'KHOP' ELSE 'LECH' END AS [Ket luan];

IF EXISTS (SELECT 1 FROM @khongToi)
    SELECT MaCoSo AS [Co so KHONG toi duoc], ThongBao AS [Ly do] FROM @khongToi;

/* =====================================================================
   PHÁN QUYẾT

   Cơ sở không tới được thì KHÔNG kết luận là đạt — tổng sẽ thiếu đúng
   phần của cơ sở đó, và báo "LECH" ở đây là báo nhầm nguyên nhân.
   ===================================================================== */
PRINT '';
PRINT '-------------------------------------------------------------------';

IF EXISTS (SELECT 1 FROM @khongToi)
BEGIN
    PRINT '  CHUA KET LUAN DUOC: con co so khong toi duoc (xem bang tren).';
    PRINT '  Dung so lieu nay lam bang chung cho yeu cau 1.';
END
ELSE IF EXISTS (SELECT 1 FROM @kq WHERE KetLuan = 'LECH') OR @danhBa <> @tkCentral
BEGIN
    PRINT '  DOI SOAT LECH — vi tu phan manh SAI.';
    PRINT '  Lech duong = co dong lot vao hai manh. Lech am = co dong roi ra ngoai.';
    THROW 53100, N'Doi soat phan manh that bai.', 1;
END
ELSE
BEGIN
    PRINT '  PHAN MANH NGANG: PASS';
    PRINT '  Tong moi manh bang dung so dong o CENTRAL, tren tat ca cac bang.';
END

PRINT '-------------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
