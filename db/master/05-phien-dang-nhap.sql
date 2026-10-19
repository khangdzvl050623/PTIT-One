/* =====================================================================
   PTIT One — master/05-phien-dang-nhap.sql
   ---------------------------------------------------------------------
   Phiên đăng nhập và token làm mới của Admin Master.

   VÌ SAO BẢNG NÀY TỒN TẠI:
   Cùng lý do với `TaiKhoanMaster`. `PhienDangNhap` được phân mảnh ngang
   theo site vì nó đi kèm `TaiKhoan` của site đó, mà Admin Master KHÔNG
   thuộc cơ sở nào — nên phiên của họ cũng không có chỗ nào để ở. Đặt
   phiên của Admin Master vào một site bất kỳ sẽ tạo ra dòng trỏ tới một
   tài khoản không tồn tại trong database đó.

   CẤU TRÚC GIỐNG HỆT bản ở site (`db/site/10-schema-vanhanh.sql`), chỉ
   khác khóa ngoại trỏ sang `TaiKhoanMaster` thay vì `TaiKhoan`. Giữ
   nguyên tên bảng và tên cột là CÓ CHỦ ĐÍCH: `SessionRepository` viết
   SQL một lần, chạy được với cả `DS_MASTER` lẫn `DS_<site>` mà không
   phải rẽ nhánh theo nơi kết nối.

   CHẠY Ở ĐÂU : CHỈ trên PTITONE_MASTER
                  .\run.ps1 -Script master\05-phien-dang-nhap.sql -On MASTER

   ⚠️ HAI BẢNG NÀY KHÔNG NHÂN BẢN.
      Không cơ sở nào cần đọc phiên của quản trị trung tâm. Chúng KHÔNG
      có mặt trong publication ở replication/31-publication.sql —
      đừng thêm vào.
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

IF OBJECT_ID(N'dbo.TaiKhoanMaster', N'U') IS NULL
BEGIN
    RAISERROR(N'Chua co TaiKhoanMaster. Chay master\03-taikhoan-master.sql truoc.', 16, 1);
    SET NOEXEC ON;
END
GO

/* =====================================================================
   PhienDangNhap
   ---------------------------------------------------------------------
   Một phiên = một lần đăng nhập. Hạn tuyệt đối tính từ lúc tạo, không
   gia hạn theo hoạt động — làm mới token KHÔNG kéo dài phiên.

   `PhienBanLucTao` chụp lại `PhienBanNguoiDung` của danh bạ tại thời
   điểm đăng nhập. Thủ tục chuyển cơ sở tăng số phiên bản đó, nhờ vậy
   mọi phiên cũ mất hiệu lực mà không phải đi dò từng dòng.
   ===================================================================== */
IF OBJECT_ID(N'dbo.PhienDangNhap', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PhienDangNhap (
        MaPhien          uniqueidentifier NOT NULL,
        TenDangNhap      varchar(50)      NOT NULL,
        PhienBanLucTao   int              NOT NULL,
        ThoiDiemTao      datetime2(3)     NOT NULL,
        ThoiDiemHetHan   datetime2(3)     NOT NULL,
        ThoiDiemThuHoi   datetime2(3)     NULL,
        LyDoThuHoi       varchar(30)      NULL,

        /* UUID làm clustered key sẽ phân mảnh trang liên tục; để heap
           cộng PK nonclustered, theo đúng C9. */
        CONSTRAINT PK_PhienDangNhap PRIMARY KEY NONCLUSTERED (MaPhien),

        /* Trỏ sang TaiKhoanMaster, KHÔNG phải TaiKhoan — ở Master không
           có bảng TaiKhoan nào cả. */
        CONSTRAINT FK_PhienDangNhap_TaiKhoanMaster
            FOREIGN KEY (TenDangNhap)
            REFERENCES dbo.TaiKhoanMaster (TenDangNhap),

        CONSTRAINT CK_PhienDangNhap_HetHan CHECK (ThoiDiemHetHan > ThoiDiemTao),
        CONSTRAINT CK_PhienDangNhap_LyDo
            CHECK (LyDoThuHoi IN ('DANG_XUAT', 'DANG_XUAT_TAT_CA', 'PHAT_HIEN_DUNG_LAI',
                                  'DOI_MAT_KHAU', 'KHOI_PHUC_MAT_KHAU', 'THAY_DOI_TAI_KHOAN')),

        /* Thu hồi thì phải có lý do, và ngược lại. Thiếu ràng buộc này
           thì nhật ký thu hồi sẽ có dòng không giải thích được. */
        CONSTRAINT CK_PhienDangNhap_ThuHoi
            CHECK ((ThoiDiemThuHoi IS NULL AND LyDoThuHoi IS NULL)
                OR (ThoiDiemThuHoi IS NOT NULL AND LyDoThuHoi IS NOT NULL))
    );
    PRINT '  [+] dbo.PhienDangNhap';
END
ELSE PRINT '  [=] dbo.PhienDangNhap';
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
                WHERE object_id = OBJECT_ID(N'dbo.PhienDangNhap')
                  AND name = N'IX_PhienDangNhap_TenDangNhap_ConHieuLuc')
    CREATE NONCLUSTERED INDEX IX_PhienDangNhap_TenDangNhap_ConHieuLuc
        ON dbo.PhienDangNhap (TenDangNhap) INCLUDE (ThoiDiemHetHan)
        WHERE ThoiDiemThuHoi IS NULL;
GO

/* =====================================================================
   TokenLamMoi
   ---------------------------------------------------------------------
   ⚠️ CHỈ lưu SHA-256 của token, KHÔNG BAO GIỜ lưu token gốc. Lộ database
   thì kẻ tấn công vẫn không dựng lại được token để dùng.

   Làm mới token là xoay vòng: đánh dấu token cũ đã dùng rồi cấp token
   mới. Dùng lại một token đã dùng bị coi là replay và thu hồi cả phiên.
   ===================================================================== */
IF OBJECT_ID(N'dbo.TokenLamMoi', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TokenLamMoi (
        MaToken        uniqueidentifier NOT NULL,
        MaPhien        uniqueidentifier NOT NULL,
        TokenHash      binary(32)       NOT NULL,
        ThoiDiemTao    datetime2(3)     NOT NULL,
        ThoiDiemHetHan datetime2(3)     NOT NULL,
        ThoiDiemDaDung datetime2(3)     NULL,
        ThoiDiemThuHoi datetime2(3)     NULL,
        CONSTRAINT PK_TokenLamMoi PRIMARY KEY NONCLUSTERED (MaToken),
        CONSTRAINT UQ_TokenLamMoi_TokenHash UNIQUE (TokenHash),
        CONSTRAINT FK_TokenLamMoi_Phien
            FOREIGN KEY (MaPhien) REFERENCES dbo.PhienDangNhap (MaPhien),
        CONSTRAINT CK_TokenLamMoi_HetHan CHECK (ThoiDiemHetHan > ThoiDiemTao)
    );
    PRINT '  [+] dbo.TokenLamMoi';
END
ELSE PRINT '  [=] dbo.TokenLamMoi';
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
                WHERE object_id = OBJECT_ID(N'dbo.TokenLamMoi')
                  AND name = N'IX_TokenLamMoi_MaPhien')
    CREATE NONCLUSTERED INDEX IX_TokenLamMoi_MaPhien
        ON dbo.TokenLamMoi (MaPhien);
GO

/* =====================================================================
   KIỂM CHỨNG — nhắc lại rằng hai bảng này KHÔNG được nhân bản
   ===================================================================== */
PRINT '';
PRINT '  ------------------------------------------------------------';
GO

IF EXISTS (SELECT 1 FROM sys.tables
            WHERE name IN (N'PhienDangNhap', N'TokenLamMoi') AND is_published = 1)
    RAISERROR(N'CANH BAO: PhienDangNhap hoac TokenLamMoi DANG duoc nhan ban. Go khoi publication — phien quan tri khong duoc nam tren cac site.', 16, 1);
ELSE
    PRINT '  [ok] PhienDangNhap va TokenLamMoi KHONG nam trong publication (dung nhu thiet ke)';
GO

PRINT '  Hoan tat lop Master. Buoc tiep theo: replication\30-distributor.sql';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
