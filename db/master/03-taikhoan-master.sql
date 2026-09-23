/* =====================================================================
   PTIT One — master/03-taikhoan-master.sql
   ---------------------------------------------------------------------
   Tài khoản của Admin Master (Phòng đào tạo trung tâm).

   VÌ SAO BẢNG NÀY TỒN TẠI:
   TaiKhoan được phân mảnh ngang theo MaCoSo, mà Admin Master KHÔNG thuộc
   cơ sở nào — nên tài khoản của họ không có chỗ nào để ở. Đây là lỗ hổng
   phát hiện khi tổng quát hoá danh bạ, và đây là cách bịt.

   CHẠY Ở ĐÂU : CHỈ trên PTITONE_MASTER
                  .\run.ps1 -Script master\03-taikhoan-master.sql -On MASTER

   ⚠️ BẢNG NÀY KHÔNG NHÂN BẢN.
      Không cơ sở nào cần đọc nó, và mật khẩu quản trị không có lý do gì
      để nằm trên ba máy. Nó KHÔNG có mặt trong publication ở
      replication/31-publication.sql — đừng thêm vào.
   ===================================================================== */

SET NOCOUNT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    RAISERROR(N'Script nay CHI chay tren $(DbMaster). Database hien tai: %s',
              16, 1, DB_NAME());
    SET NOEXEC ON;
END
GO

IF OBJECT_ID(N'dbo.DanhBaNguoiDung', N'U') IS NULL
BEGIN
    RAISERROR(N'Chua co DanhBaNguoiDung. Chay master\02-danhba-nguoidung.sql truoc.', 16, 1);
    SET NOEXEC ON;
END
GO

/* =====================================================================
   TaiKhoanMaster
   ---------------------------------------------------------------------
   Cấu trúc song song với TaiKhoan ở các site, nhưng KHÔNG có cột MaCoSo
   — vì Master không phải một cơ sở.
   ===================================================================== */
IF OBJECT_ID(N'dbo.TaiKhoanMaster', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TaiKhoanMaster (
        TenDangNhap   VARCHAR(50)   NOT NULL,

        /* ⚠️ Băm mật khẩu, KHÔNG BAO GIỜ lưu mật khẩu gốc.
           Dùng BCrypt/Argon2 ở tầng ứng dụng; cột này chỉ chứa kết quả. */
        MatKhauHash   VARCHAR(255)  NOT NULL,

        VaiTro        VARCHAR(20)   NOT NULL
            CONSTRAINT DF_TaiKhoanMaster_VaiTro DEFAULT ('ADMIN_MASTER'),

        HoTen         NVARCHAR(100) NOT NULL,
        DangHoatDong  BIT           NOT NULL
            CONSTRAINT DF_TaiKhoanMaster_DangHoatDong DEFAULT (1),
        NgayTao       DATETIME2(0)  NOT NULL
            CONSTRAINT DF_TaiKhoanMaster_NgayTao DEFAULT (SYSUTCDATETIME()),

        CONSTRAINT PK_TaiKhoanMaster PRIMARY KEY CLUSTERED (TenDangNhap),

        CONSTRAINT CK_TaiKhoanMaster_VaiTro
            CHECK (VaiTro = 'ADMIN_MASTER'),

        /* Tài khoản phải có mặt trong danh bạ, nếu không đăng nhập sẽ
           không định tuyến tới đây được. FK này KHẢ THI vì cả hai bảng
           cùng nằm trong PTITONE_MASTER. */
        CONSTRAINT FK_TaiKhoanMaster_DanhBa
            FOREIGN KEY (TenDangNhap)
            REFERENCES dbo.DanhBaNguoiDung (TenDangNhap)
    );
    PRINT '  [+] dbo.TaiKhoanMaster';
END
ELSE PRINT '  [=] dbo.TaiKhoanMaster';
GO

/* =====================================================================
   KIỂM CHỨNG — nhắc lại rằng bảng này KHÔNG được nhân bản
   ===================================================================== */
PRINT '';
PRINT '  ------------------------------------------------------------';
GO

IF EXISTS (SELECT 1 FROM sys.tables
            WHERE name = N'TaiKhoanMaster' AND is_published = 1)
    RAISERROR(N'CANH BAO: TaiKhoanMaster DANG duoc nhan ban. Go no khoi publication — mat khau quan tri khong duoc nam tren cac site.', 16, 1);
ELSE
    PRINT '  [ok] TaiKhoanMaster KHONG nam trong publication (dung nhu thiet ke)';
GO

PRINT '  Buoc tiep theo: master\04-seed-danhmuc.sql';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
