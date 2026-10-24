/* =====================================================================
   PTIT One — master/06-sp-chuyen-co-so.sql
   ---------------------------------------------------------------------
   ⭐ YÊU CẦU BẮT BUỘC SỐ 3 — GIAO DỊCH PHÂN TÁN (2PC) qua MS DTC.

   Chuyển một sinh viên từ cơ sở này sang cơ sở khác. Ba CSDL cùng tham
   gia một giao dịch: cơ sở CŨ, cơ sở MỚI, và Master (danh bạ định vị).

   VÌ SAO LÀ 2PC CHỨ KHÔNG PHẢI SAGA:
   Không có trạng thái trung gian nào an toàn — sinh viên không thể "nửa
   ở HCM nửa ở HN". Thao tác này hiếm (vài lần mỗi kỳ) và không tranh
   chấp (mỗi sinh viên một dòng riêng), nên chịu được chi phí giữ khoá
   xuyên mạng. Đăng ký học phần thì ngược lại hoàn toàn — xem D8.

   CHẠY Ở ĐÂU : CHỈ trên PTITONE_MASTER
                  .\run.ps1 -Script master\06-sp-chuyen-co-so.sql -On MASTER

   ⚠️ ĐIỀU KIỆN HẠ TẦNG
   - MS DTC đã cấu hình ở Master và CẢ HAI cơ sở (F4b).
   - Master có Linked Server tới MỌI cơ sở. Khác với topology hình sao từ
     SRV-HCM dùng cho bốn báo cáo: thủ tục này điều phối hai site bất kỳ,
     nên nơi điều phối phải với được tất cả.
   - `CoSo.TenLinkedServer` và `CoSo.TenDatabase` đã khai đúng.
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

IF OBJECT_ID(N'dbo.CoSo', N'U') IS NULL OR OBJECT_ID(N'dbo.DanhBaNguoiDung', N'U') IS NULL
BEGIN
    RAISERROR(N'Thieu CoSo hoac DanhBaNguoiDung. Chay master\01 va 02 truoc.', 16, 1);
    SET NOEXEC ON;
END
GO

CREATE OR ALTER PROCEDURE dbo.sp_ChuyenCoSoSinhVien
    @MaSinhVien VARCHAR(20),
    @CoSoCu     VARCHAR(10),
    @CoSoMoi    VARCHAR(10)
AS
BEGIN
    /* ⚠️ BẮT BUỘC. Thiếu dòng này, một lỗi ở site xa có thể chỉ huỷ câu
       lệnh chứ không huỷ giao dịch, và 2PC commit một nửa. */
    SET XACT_ABORT ON;
    SET NOCOUNT ON;

    /* ================================================================
       GIAI ĐOẠN 0 — THAM SỐ
       Mã cơ sở KHÔNG bao giờ được nối thẳng vào chuỗi SQL. Chúng chỉ
       được dùng để TRA bảng `CoSo`; tên server và database lấy ra từ đó,
       rồi bọc QUOTENAME. Mã không có trong `CoSo` thì dừng ngay.
       ================================================================ */
    IF @CoSoCu = @CoSoMoi
        THROW 51000, N'Co so cu va co so moi trung nhau.', 1;

    DECLARE @PrefixCu  NVARCHAR(300),
            @PrefixMoi NVARCHAR(300);

    SELECT @PrefixCu = CASE WHEN TenLinkedServer IS NULL
                            THEN QUOTENAME(TenDatabase) + N'.dbo.'
                            ELSE QUOTENAME(TenLinkedServer) + N'.'
                               + QUOTENAME(TenDatabase) + N'.dbo.' END
      FROM dbo.CoSo
     WHERE MaCoSo = @CoSoCu AND DangHoatDong = 1;

    SELECT @PrefixMoi = CASE WHEN TenLinkedServer IS NULL
                             THEN QUOTENAME(TenDatabase) + N'.dbo.'
                             ELSE QUOTENAME(TenLinkedServer) + N'.'
                                + QUOTENAME(TenDatabase) + N'.dbo.' END
      FROM dbo.CoSo
     WHERE MaCoSo = @CoSoMoi AND DangHoatDong = 1;

    IF @PrefixCu IS NULL OR @PrefixMoi IS NULL
        THROW 51001, N'Ma co so khong ton tai hoac da ngung hoat dong.', 1;

    DECLARE @sql NVARCHAR(MAX),
            @dem INT;

    /* ================================================================
       GIAI ĐOẠN 1 — TIỀN ĐIỀU KIỆN, chạy NGOÀI giao dịch

       Kiểm ở ngoài để không giữ khoá phân tán trong lúc đọc. Có cửa sổ
       đua giữa kiểm và giao dịch, nhưng `DanhBaNguoiDung.TrangThai` đổi
       sang DANG_CHUYEN bên trong giao dịch mới là chốt chặn thật.
       ================================================================ */
    SET @sql = N'SELECT @n = COUNT(*) FROM ' + @PrefixCu + N'SinhVien
                  WHERE MaSinhVien = @sv;';
    EXEC sp_executesql @sql, N'@sv VARCHAR(20), @n INT OUTPUT',
         @sv = @MaSinhVien, @n = @dem OUTPUT;
    IF @dem <> 1
        THROW 51002, N'Khong tim thay sinh vien o co so cu.', 1;

    SET @sql = N'SELECT @n = COUNT(*) FROM ' + @PrefixMoi + N'SinhVien
                  WHERE MaSinhVien = @sv;';
    EXEC sp_executesql @sql, N'@sv VARCHAR(20), @n INT OUTPUT',
         @sv = @MaSinhVien, @n = @dem OUTPUT;
    IF @dem <> 0
        THROW 51003, N'Sinh vien da ton tai o co so moi.', 1;

    /* Yêu cầu liên cơ sở còn treo: chuyển đi lúc này sẽ bỏ rơi một saga
       đang chạy dở ở site khác. */
    SET @sql = N'SELECT @n = COUNT(*) FROM ' + @PrefixCu + N'YeuCauHocLienCoSo
                  WHERE MaSinhVien = @sv AND TrangThai IN (''DANG_XU_LY'', ''DANG_HUY'');';
    EXEC sp_executesql @sql, N'@sv VARCHAR(20), @n INT OUTPUT',
         @sv = @MaSinhVien, @n = @dem OUTPUT;
    IF @dem > 0
        THROW 51004, N'Con yeu cau lien co so chua xu ly xong.', 1;

    /* Sự kiện Outbox chưa gửi: nếu xoá sinh viên bây giờ, worker sẽ gửi
       một sự kiện trỏ tới bản ghi không còn ở đó. */
    SET @sql = N'SELECT @n = COUNT(*) FROM ' + @PrefixCu + N'OutboxSuKien
                  WHERE KhoaThucThe = @sv AND TrangThai = ''PENDING'';';
    EXEC sp_executesql @sql, N'@sv VARCHAR(20), @n INT OUTPUT',
         @sv = @MaSinhVien, @n = @dem OUTPUT;
    IF @dem > 0
        THROW 51005, N'Con su kien Outbox chua gui.', 1;

    /* ================================================================
       GIAI ĐOẠN 2 — GIAO DỊCH PHÂN TÁN

       Thứ tự bắt buộc: CHÉP sang mới → XOÁ ở cũ → cập nhật danh bạ.
       Chép trước thì nếu đứt giữa chừng, bản gốc vẫn còn nguyên.

       ⚠️ `DangKyHocPhan` và `Diem` KHÔNG đi theo sinh viên. Chúng được
          phân mảnh dẫn xuất theo `LopHocPhan`, tức theo cơ sở MỞ LỚP.
          Em ấy đã học lớp đó ở cơ sở nào thì kết quả nằm lại ở đó. Đây
          chính là lý do `DangKyHocPhan` không có khoá ngoại sang
          `SinhVien` — xem chú thích trong db/site/10.
       ================================================================ */
    BEGIN DISTRIBUTED TRANSACTION;

        /* --- (a) CHÉP sang cơ sở mới -------------------------------- */
        SET @sql =
            N'INSERT INTO ' + @PrefixMoi + N'SinhVien
                     (MaSinhVien, HoTen, NgaySinh, MaCoSoNha, MaCTDT, TrangThai,
                      SoTinChiTichLuy, SoMonLienCoSo, GioiTinh, DienThoai, SoCCCD,
                      EmailCaNhan, NoiSinh, DanToc, TonGiao, HoKhau, AnhDaiDien)
              SELECT  MaSinhVien, HoTen, NgaySinh, @moi, MaCTDT, TrangThai,
                      SoTinChiTichLuy, SoMonLienCoSo, GioiTinh, DienThoai, SoCCCD,
                      EmailCaNhan, NoiSinh, DanToc, TonGiao, HoKhau, AnhDaiDien
                FROM  ' + @PrefixCu + N'SinhVien
               WHERE  MaSinhVien = @sv;

              INSERT INTO ' + @PrefixMoi + N'TaiKhoan
                     (TenDangNhap, MatKhauHash, VaiTro, MaThucThe, MaCoSo,
                      Email, EmailDaXacMinh, ThoiDiemXacMinhEmail)
              SELECT  TenDangNhap, MatKhauHash, VaiTro, MaThucThe, @moi,
                      Email, EmailDaXacMinh, ThoiDiemXacMinhEmail
                FROM  ' + @PrefixCu + N'TaiKhoan
               WHERE  MaThucThe = @sv AND VaiTro = ''SINH_VIEN'';

              /* Ba bảng dưới dùng SELECT * CÓ CHỦ ĐÍCH: không cột nào cần
                 đổi giá trị, và mọi cơ sở đều dựng từ CÙNG bộ db/site nên
                 hình dạng bảng khớp nhau theo thiết kế. Liệt kê tay thì
                 thủ tục mục ngay lần thêm cột kế tiếp — đã vấp đúng lỗi đó
                 với TranTinChi và MaCoSoHost. */

              /* Bộ đếm tín chỉ theo kỳ đi theo sinh viên: nó là ràng buộc
                 của NGƯỜI, không của lớp. Để lại thì cơ sở mới mất trần
                 tín chỉ đã dùng. */
              INSERT INTO ' + @PrefixMoi + N'SinhVienHocKy
              SELECT * FROM ' + @PrefixCu + N'SinhVienHocKy
               WHERE MaSinhVien = @sv;

              INSERT INTO ' + @PrefixMoi + N'DangKyMonHoc
              SELECT * FROM ' + @PrefixCu + N'DangKyMonHoc
               WHERE MaSinhVien = @sv;

              /* Lịch sử yêu cầu liên cơ sở đi theo người để cơ sở mới còn
                 tra cứu được. Đã chặn các yêu cầu đang treo ở giai đoạn 1. */
              INSERT INTO ' + @PrefixMoi + N'YeuCauHocLienCoSo
              SELECT * FROM ' + @PrefixCu + N'YeuCauHocLienCoSo
               WHERE MaSinhVien = @sv;';

        EXEC sp_executesql @sql,
             N'@sv VARCHAR(20), @moi VARCHAR(10)',
             @sv = @MaSinhVien, @moi = @CoSoMoi;

        /* --- (b) XOÁ ở cơ sở cũ ------------------------------------
           Xoá con trước cha, theo đúng chiều khoá ngoại. Đảo thứ tự là
           vi phạm ràng buộc, và vì XACT_ABORT bật nên cả giao dịch phân
           tán sẽ bị huỷ.

           Phiên đăng nhập và các mã xác thực KHÔNG chép sang: chuyển cơ
           sở phải làm mọi phiên cũ mất hiệu lực. `BangDiemMirror` cũng
           không chép — nó là read model, dựng lại ở giai đoạn 3 từ nguồn
           sự thật là `Diem` ở các site Host. */
        SET @sql =
            N'DELETE FROM ' + @PrefixCu + N'TokenLamMoi
               WHERE MaPhien IN (SELECT MaPhien FROM ' + @PrefixCu + N'PhienDangNhap
                                  WHERE TenDangNhap IN (SELECT TenDangNhap
                                                          FROM ' + @PrefixCu + N'TaiKhoan
                                                         WHERE MaThucThe = @sv));
              DELETE FROM ' + @PrefixCu + N'PhienDangNhap
               WHERE TenDangNhap IN (SELECT TenDangNhap FROM ' + @PrefixCu + N'TaiKhoan
                                      WHERE MaThucThe = @sv);
              DELETE FROM ' + @PrefixCu + N'MaXacThuc
               WHERE TenDangNhap IN (SELECT TenDangNhap FROM ' + @PrefixCu + N'TaiKhoan
                                      WHERE MaThucThe = @sv);
              DELETE FROM ' + @PrefixCu + N'MaKichHoat
               WHERE TenDangNhap IN (SELECT TenDangNhap FROM ' + @PrefixCu + N'TaiKhoan
                                      WHERE MaThucThe = @sv);

              DELETE FROM ' + @PrefixCu + N'DangKyMonHoc       WHERE MaSinhVien = @sv;
              DELETE FROM ' + @PrefixCu + N'SinhVienHocKy      WHERE MaSinhVien = @sv;
              DELETE FROM ' + @PrefixCu + N'YeuCauHocLienCoSo  WHERE MaSinhVien = @sv;
              DELETE FROM ' + @PrefixCu + N'BangDiemMirror     WHERE MaSinhVien = @sv;
              DELETE FROM ' + @PrefixCu + N'TaiKhoan           WHERE MaThucThe  = @sv;
              DELETE FROM ' + @PrefixCu + N'SinhVien           WHERE MaSinhVien = @sv;';

        EXEC sp_executesql @sql, N'@sv VARCHAR(20)', @sv = @MaSinhVien;

        /* --- (c) Danh bạ định vị tại Master -------------------------
           Tăng `PhienBanTaiKhoan` để mọi JWT cũ hết hiệu lực ngay, không
           cần danh sách thu hồi: token mang theo số phiên bản lúc cấp,
           tầng ứng dụng đối chiếu và từ chối nếu lệch. */
        UPDATE dbo.DanhBaNguoiDung
           SET MaCoSo           = @CoSoMoi,
               PhienBanTaiKhoan = PhienBanTaiKhoan + 1,
               NgayCapNhat      = SYSUTCDATETIME()
         WHERE MaThucThe        = @MaSinhVien
           AND LoaiNguoiDung    = 'SINH_VIEN';

        IF @@ROWCOUNT <> 1
            THROW 51006, N'Khong tim thay dung mot dong danh ba.', 1;

    COMMIT TRANSACTION;

    /* ================================================================
       GIAI ĐOẠN 3 — HẬU XỬ LÝ, chạy RIÊNG và idempotent

       Cố ý để NGOÀI giao dịch phân tán. Đây đều là dữ liệu dẫn xuất,
       dựng lại được từ nguồn sự thật, nên không đáng kéo dài thời gian
       giữ khoá hai pha:
         - dựng lại BangDiemMirror tại cơ sở mới từ `Diem` ở các site Host
         - tính lại SinhVien.SoMonLienCoSo
       Chạy lại nhiều lần phải ra cùng kết quả.
       ================================================================ */
END
GO

PRINT '  [+] dbo.sp_ChuyenCoSoSinhVien';
GO

PRINT '';
PRINT '  ------------------------------------------------------------';
PRINT '  Kiem thu: db\tests\91-demo-chuyen-co-so.sql';
PRINT '  Can MS DTC o ca ba may va Linked Server tu Master toi moi co so.';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
