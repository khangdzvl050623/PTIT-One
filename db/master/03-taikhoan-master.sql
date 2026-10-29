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
    DECLARE @DbHienTai SYSNAME = DB_NAME();
    RAISERROR(N'Script nay CHI chay tren $(DbMaster). Database hien tai: %s',
              16, 1, @DbHienTai);
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
           Dùng BCrypt/Argon2 ở tầng ứng dụng; cột này chỉ chứa kết quả.
           NULL = chưa có mật khẩu (04 seed như vậy, migrate/50 chép hash sang). */
        MatKhauHash   VARCHAR(255)  NULL,

        VaiTro        VARCHAR(20)   NOT NULL
            CONSTRAINT DF_TaiKhoanMaster_VaiTro DEFAULT ('ADMIN_MASTER'),

        HoTen         NVARCHAR(100) NOT NULL,
        DangHoatDong  BIT           NOT NULL
            CONSTRAINT DF_TaiKhoanMaster_DangHoatDong DEFAULT (1),
        NgayTao       DATETIME2(0)  NOT NULL
            CONSTRAINT DF_TaiKhoanMaster_NgayTao DEFAULT (SYSUTCDATETIME()),

        /* Email do trường cấp. Admin Master cũng cần tự khôi phục mật
           khẩu qua thư, nên cột này KHÔNG phải tuỳ chọn về mặt nghiệp
           vụ — chỉ NULL được cho tới khi quản trị khai báo. Cấu trúc
           giữ giống hệt `TaiKhoan` ở site. */
        Email                VARCHAR(254) NULL,
        EmailDaXacMinh       BIT          NOT NULL
            CONSTRAINT DF_TaiKhoanMaster_EmailDaXacMinh DEFAULT (0),
        ThoiDiemXacMinhEmail DATETIME2(3) NULL,

        CONSTRAINT PK_TaiKhoanMaster PRIMARY KEY CLUSTERED (TenDangNhap),

        /* Đánh dấu đã xác minh mà không có địa chỉ và mốc thời gian là
           trạng thái không giải thích được. */
        CONSTRAINT CK_TaiKhoanMaster_EmailXacMinh
            CHECK (EmailDaXacMinh = 0
                OR (Email IS NOT NULL AND ThoiDiemXacMinhEmail IS NOT NULL)),

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
   BÙ CỘT CHO BẢNG ĐÃ TỒN TẠI
   ---------------------------------------------------------------------
   `IF OBJECT_ID(...) IS NULL CREATE TABLE` ở trên BỎ QUA hoàn toàn nếu
   bảng đã có từ lần chạy trước — nên mọi cột thêm vào `CREATE TABLE` sau
   đó sẽ không bao giờ được áp. Ba cột email là đúng trường hợp đó: script
   đọc thì có, database thật thì không, và lỗi chỉ hiện ra lúc đăng nhập
   dưới dạng `Invalid column name 'Email'` — một thông báo không hề chỉ về
   phía script schema.

   Đây là cùng một cái bẫy đã bù ở `db/site/10-schema-vanhanh.sql`. Thêm
   cột mới vào `CREATE TABLE` ở trên thì phải thêm vào đây.
   ===================================================================== */
IF COL_LENGTH(N'dbo.TaiKhoanMaster', N'Email') IS NULL
BEGIN
    ALTER TABLE dbo.TaiKhoanMaster ADD
        Email                VARCHAR(254) NULL,
        EmailDaXacMinh       BIT          NOT NULL
            CONSTRAINT DF_TaiKhoanMaster_EmailDaXacMinh DEFAULT (0),
        ThoiDiemXacMinhEmail DATETIME2(3) NULL,
        CONSTRAINT CK_TaiKhoanMaster_EmailXacMinh
            CHECK (EmailDaXacMinh = 0
                OR (Email IS NOT NULL AND ThoiDiemXacMinhEmail IS NOT NULL));
    PRINT '  [+] TaiKhoanMaster: bu 3 cot email';
END
ELSE PRINT '  [=] TaiKhoanMaster: du cot';

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.TaiKhoanMaster')
             AND name = N'MatKhauHash' AND is_nullable = 0)
BEGIN
    ALTER TABLE dbo.TaiKhoanMaster ALTER COLUMN MatKhauHash VARCHAR(255) NULL;
    PRINT '  [+] TaiKhoanMaster: MatKhauHash cho phep NULL';
END
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
