/* =====================================================================
   PTIT One — 91-demo-chuyen-co-so.sql
   ---------------------------------------------------------------------
   NHÌN TẬN MẮT giao dịch phân tán 2PC — yêu cầu bắt buộc số 3.

   File này KHÔNG cài đặt gì. Nó gieo một sinh viên thử, chuyển em ấy
   sang cơ sở khác, rồi ép một lần chuyển THẤT BẠI để cho thấy cả hai
   cơ sở cùng quay lui. Cuối cùng dọn sạch. Chạy lại bao nhiêu lần cũng
   ra kết quả như nhau.

   HAI MÀN, VÀ MÀN HAI MỚI LÀ MÀN QUAN TRỌNG:

     Màn 1 — COMMIT   : chứng minh ba CSDL cùng đổi.
     Màn 2 — ROLLBACK : chứng minh ba CSDL cùng KHÔNG đổi khi có lỗi.

   COMMIT thành công chỉ nói "gọi được qua mạng". Chỉ ROLLBACK mới chứng
   minh hai đầu thật sự nằm trong MỘT giao dịch — đó là thứ phân biệt
   2PC với hai lệnh ghi rời nhau.

   CHẠY Ở ĐÂU
     .\run.ps1 -Script tests\91-demo-chuyen-co-so.sql -On MASTER

   ĐIỀU KIỆN
     - MS DTC đã bật ở Master và cả hai cơ sở (F4b).
     - Master có Linked Server tới cơ sở đích.
     - Hai cơ sở đã có lược đồ db/site.
   ===================================================================== */

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/* Một batch duy nhất: biến phải sống xuyên suốt ba màn, mà GO thì xoá
   sạch biến. Vì vậy dùng THROW để chặn thay cho SET NOEXEC. */

DECLARE @sv  VARCHAR(20) = 'SVDEMO001',
        @cu  VARCHAR(10) = 'HCM',
        @moi VARCHAR(10) = 'HN';

IF DB_NAME() <> N'$(DbMaster)'
    THROW 52000, N'Demo nay chi chay tren PTITONE_MASTER (-On MASTER).', 1;

IF OBJECT_ID(N'dbo.sp_ChuyenCoSoSinhVien', N'P') IS NULL
    THROW 52001, N'Chua co sp_ChuyenCoSoSinhVien. Chay master\06 truoc.', 1;

/* Tiền tố bốn phần lấy từ chính bảng CoSo, không gõ tay — demo phải đi
   theo cấu hình thật, nếu không nó chứng minh một hệ thống khác. */
DECLARE @PreCu NVARCHAR(300), @PreMoi NVARCHAR(300);

SELECT @PreCu = CASE WHEN TenLinkedServer IS NULL
                     THEN QUOTENAME(TenDatabase) + N'.dbo.'
                     ELSE QUOTENAME(TenLinkedServer) + N'.' + QUOTENAME(TenDatabase) + N'.dbo.' END
  FROM dbo.CoSo WHERE MaCoSo = @cu;

SELECT @PreMoi = CASE WHEN TenLinkedServer IS NULL
                      THEN QUOTENAME(TenDatabase) + N'.dbo.'
                      ELSE QUOTENAME(TenLinkedServer) + N'.' + QUOTENAME(TenDatabase) + N'.dbo.' END
  FROM dbo.CoSo WHERE MaCoSo = @moi;

IF @PreCu IS NULL OR @PreMoi IS NULL
    THROW 52002, N'Thieu cau hinh TenLinkedServer/TenDatabase trong bang CoSo.', 1;

DECLARE @sql NVARCHAR(MAX);
DECLARE @anh TABLE (ViTri VARCHAR(40), SoDong INT);

/* ---------------------------------------------------------------------
   MÀN 0 — DỌN VÀ GIEO
   Dọn trước chứ không dọn sau, để lần chạy trước có chết giữa chừng thì
   lần này vẫn bắt đầu từ trạng thái sạch.
   --------------------------------------------------------------------- */
PRINT '';
PRINT '=== MAN 0: don va gieo sinh vien thu ===============================';

SET @sql = N'
    DELETE FROM ' + @PreCu  + N'SinhVienHocKy WHERE MaSinhVien = @sv;
    DELETE FROM ' + @PreCu  + N'TaiKhoan      WHERE MaThucThe  = @sv;
    DELETE FROM ' + @PreCu  + N'SinhVien      WHERE MaSinhVien = @sv;
    DELETE FROM ' + @PreMoi + N'SinhVienHocKy WHERE MaSinhVien = @sv;
    DELETE FROM ' + @PreMoi + N'TaiKhoan      WHERE MaThucThe  = @sv;
    DELETE FROM ' + @PreMoi + N'SinhVien      WHERE MaSinhVien = @sv;';
EXEC sp_executesql @sql, N'@sv VARCHAR(20)', @sv = @sv;

DELETE FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = @sv;

INSERT INTO dbo.DanhBaNguoiDung
       (TenDangNhap, MaCoSo, LoaiNguoiDung, MaThucThe, TrangThai, PhienBanTaiKhoan)
VALUES (@sv, @cu, 'SINH_VIEN', @sv, 'HOAT_DONG', 1);

/* Chương trình đào tạo và học kỳ lấy từ danh mục ĐÃ NHÂN BẢN về site,
   nên khoá ngoại chắc chắn thoả. */
SET @sql = N'
    DECLARE @ctdt VARCHAR(20) = (SELECT TOP 1 MaCTDT  FROM ' + @PreCu + N'ChuongTrinhDaoTao ORDER BY MaCTDT);
    DECLARE @ky   VARCHAR(20) = (SELECT TOP 1 MaHocKy FROM ' + @PreCu + N'HocKy ORDER BY MaHocKy);

    INSERT INTO ' + @PreCu + N'SinhVien
           (MaSinhVien, HoTen, NgaySinh, MaCoSoNha, MaCTDT, TrangThai,
            SoTinChiTichLuy, SoMonLienCoSo)
    VALUES (@sv, N''Sinh vien demo 2PC'', ''2006-01-01'', @cu, @ctdt, ''HOAT_DONG'', 15, 0);

    INSERT INTO ' + @PreCu + N'TaiKhoan
           (TenDangNhap, MatKhauHash, VaiTro, MaThucThe, MaCoSo, EmailDaXacMinh)
    VALUES (@sv, ''{noop}demo'', ''SINH_VIEN'', @sv, @cu, 0);

    INSERT INTO ' + @PreCu + N'SinhVienHocKy
           (MaSinhVien, MaHocKy, SoTinChiDaDangKy, SoTinChiDangGiuCho, TranTinChi)
    VALUES (@sv, @ky, 15, 0, 24);';
EXEC sp_executesql @sql, N'@sv VARCHAR(20), @cu VARCHAR(10)', @sv = @sv, @cu = @cu;

PRINT '  da gieo ' + @sv + ' tai co so ' + @cu;

/* Câu đếm dùng lại ở mọi điểm kiểm tra. */
DECLARE @dem NVARCHAR(MAX) = N'
    SELECT ''' + @cu  + N' - SinhVien'',  COUNT(*) FROM ' + @PreCu  + N'SinhVien      WHERE MaSinhVien = @sv
    UNION ALL
    SELECT ''' + @cu  + N' - TaiKhoan'',  COUNT(*) FROM ' + @PreCu  + N'TaiKhoan      WHERE MaThucThe  = @sv
    UNION ALL
    SELECT ''' + @cu  + N' - TinChiKy'',  COUNT(*) FROM ' + @PreCu  + N'SinhVienHocKy WHERE MaSinhVien = @sv
    UNION ALL
    SELECT ''' + @moi + N' - SinhVien'',  COUNT(*) FROM ' + @PreMoi + N'SinhVien      WHERE MaSinhVien = @sv
    UNION ALL
    SELECT ''' + @moi + N' - TaiKhoan'',  COUNT(*) FROM ' + @PreMoi + N'TaiKhoan      WHERE MaThucThe  = @sv
    UNION ALL
    SELECT ''' + @moi + N' - TinChiKy'',  COUNT(*) FROM ' + @PreMoi + N'SinhVienHocKy WHERE MaSinhVien = @sv;';

DELETE FROM @anh;
INSERT INTO @anh EXEC sp_executesql @dem, N'@sv VARCHAR(20)', @sv = @sv;
SELECT ViTri AS [Truoc khi chuyen], SoDong FROM @anh;

SELECT MaCoSo AS [Danh ba: co so], PhienBanTaiKhoan AS [Phien ban tai khoan]
  FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = @sv;

/* ---------------------------------------------------------------------
   MÀN 1 — COMMIT
   --------------------------------------------------------------------- */
PRINT '';
PRINT '=== MAN 1: chuyen ' + @cu + ' -> ' + @moi + ' (phai THANH CONG) =========';

EXEC dbo.sp_ChuyenCoSoSinhVien @MaSinhVien = @sv, @CoSoCu = @cu, @CoSoMoi = @moi;

DELETE FROM @anh;
INSERT INTO @anh EXEC sp_executesql @dem, N'@sv VARCHAR(20)', @sv = @sv;
SELECT ViTri AS [Sau khi chuyen], SoDong FROM @anh;

SELECT MaCoSo AS [Danh ba: co so], PhienBanTaiKhoan AS [Phien ban tai khoan]
  FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = @sv;

IF EXISTS (SELECT 1 FROM @anh WHERE ViTri LIKE @cu + '%'  AND SoDong <> 0)
    THROW 52003, N'MAN 1 HONG: co so cu van con du lieu.', 1;
IF EXISTS (SELECT 1 FROM @anh WHERE ViTri LIKE @moi + '%' AND SoDong <> 1)
    THROW 52004, N'MAN 1 HONG: co so moi khong nhan du du lieu.', 1;
IF (SELECT MaCoSo FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = @sv) <> @moi
    THROW 52005, N'MAN 1 HONG: danh ba chua doi co so.', 1;
IF (SELECT PhienBanTaiKhoan FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = @sv) <> 2
    THROW 52006, N'MAN 1 HONG: chua tang PhienBanTaiKhoan, JWT cu van song.', 1;

PRINT '  MAN 1 DAT: ba CSDL cung doi, va moi JWT cu da het hieu luc.';

/* ---------------------------------------------------------------------
   MÀN 2 — ROLLBACK
   Ép thất bại bằng cách xoá dòng danh bạ: thủ tục sẽ chạy hết phần chép
   và xoá ở hai site, rồi mới vấp THROW 51006 ở bước cuối. Nếu 2PC làm
   đúng việc, cả hai site phải quay lui như chưa có gì xảy ra.
   --------------------------------------------------------------------- */
PRINT '';
PRINT '=== MAN 2: chuyen nguoc ' + @moi + ' -> ' + @cu + ' (phai THAT BAI) =====';

DECLARE @banSao INT = (SELECT PhienBanTaiKhoan FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = @sv);
DELETE FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = @sv;
PRINT '  da xoa dong danh ba de ep loi o buoc cuoi cua giao dich';

DECLARE @daBatLoi BIT = 0;
BEGIN TRY
    EXEC dbo.sp_ChuyenCoSoSinhVien @MaSinhVien = @sv, @CoSoCu = @moi, @CoSoMoi = @cu;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    SET @daBatLoi = 1;
    PRINT '  da bat loi nhu mong doi: ' + ERROR_MESSAGE();
END CATCH

IF @daBatLoi = 0
    THROW 52007, N'MAN 2 HONG: thu tuc dang le phai that bai nhung lai thanh cong.', 1;

DELETE FROM @anh;
INSERT INTO @anh EXEC sp_executesql @dem, N'@sv VARCHAR(20)', @sv = @sv;
SELECT ViTri AS [Sau khi that bai], SoDong FROM @anh;

IF EXISTS (SELECT 1 FROM @anh WHERE ViTri LIKE @moi + '%' AND SoDong <> 1)
    THROW 52008, N'MAN 2 HONG: co so dich mat du lieu — KHONG nguyen tu.', 1;
IF EXISTS (SELECT 1 FROM @anh WHERE ViTri LIKE @cu + '%'  AND SoDong <> 0)
    THROW 52009, N'MAN 2 HONG: co so cu co du lieu sot lai — KHONG nguyen tu.', 1;

PRINT '  MAN 2 DAT: ca hai co so cung quay lui, khong dau nao sot du lieu.';

/* ---------------------------------------------------------------------
   DỌN
   --------------------------------------------------------------------- */
SET @sql = N'
    DELETE FROM ' + @PreMoi + N'SinhVienHocKy WHERE MaSinhVien = @sv;
    DELETE FROM ' + @PreMoi + N'TaiKhoan      WHERE MaThucThe  = @sv;
    DELETE FROM ' + @PreMoi + N'SinhVien      WHERE MaSinhVien = @sv;';
EXEC sp_executesql @sql, N'@sv VARCHAR(20)', @sv = @sv;

PRINT '';
PRINT '-------------------------------------------------------------------';
PRINT '  2PC: PASS — da don sach du lieu demo.';
PRINT '-------------------------------------------------------------------';
GO
