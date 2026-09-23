/* =====================================================================
   PTIT One — master/02-danhba-nguoidung.sql
   ---------------------------------------------------------------------
   DANH BẠ ĐỊNH VỊ — nền tảng của Location Transparency.

   Đây là bảng giải bài toán con gà và quả trứng: muốn biết định tuyến
   vào CSDL nào thì phải biết người dùng thuộc cơ sở nào; muốn biết điều
   đó thì phải đọc CSDL. Danh bạ được nhân bản về MỌI site nên bước đầu
   tiên của đăng nhập đọc được từ replica của site nào cũng xong.

   CHẠY Ở ĐÂU : CHỈ trên PTITONE_MASTER
                  .\run.ps1 -Script master\02-danhba-nguoidung.sql -On MASTER

   ⚠️ PHỦ MỌI VAI TRÒ, không riêng sinh viên. Nếu chỉ có danh bạ sinh
      viên thì giảng viên và Admin buộc phải TỰ CHỌN cơ sở lúc đăng nhập
      — tức là bắt người dùng cung cấp thông tin định vị, làm hỏng chính
      tuyên bố Location Transparency ở mục D7.
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

IF OBJECT_ID(N'dbo.CoSo', N'U') IS NULL
BEGIN
    RAISERROR(N'Chua co bang CoSo. Chay master\01-schema-thamchieu.sql truoc.', 16, 1);
    SET NOEXEC ON;
END
GO

/* =====================================================================
   DanhBaNguoiDung
   ---------------------------------------------------------------------
   KHÔNG BAO GIỜ chứa mật khẩu. Nó chỉ trả lời đúng một câu hỏi:
   "người dùng này thuộc cơ sở nào?" — việc xác thực diễn ra tại site đó.
   ===================================================================== */
IF OBJECT_ID(N'dbo.DanhBaNguoiDung', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DanhBaNguoiDung (
        TenDangNhap      VARCHAR(50)  NOT NULL,

        /* ⚠️ NULL khi LoaiNguoiDung = 'ADMIN_MASTER'.
           'MASTER' KHÔNG phải một mã cơ sở — nó là vai trò triển khai.
           Cột này chỉ nhận mã cơ sở CÓ THẬT trong bảng CoSo. */
        MaCoSo           VARCHAR(10)  NULL,

        LoaiNguoiDung    VARCHAR(20)  NOT NULL,

        /* MaSinhVien hoặc MaGiangVien. NULL với tài khoản quản trị. */
        MaThucThe        VARCHAR(20)  NULL,

        /* CHO_KICH_HOAT : dòng danh bạ đã có nhưng hồ sơ ở cơ sở nhà
                           CHƯA tạo xong. Bất biến: chỉ đăng nhập được
                           khi HOAT_DONG — nhờ vậy Outbox tạo sinh viên
                           không để lọt trạng thái mồ côi nào ra ngoài.
           DANG_CHUYEN   : đang chuyển cơ sở, chặn phát sinh thao tác mới. */
        TrangThai        VARCHAR(20)  NOT NULL
            CONSTRAINT DF_DanhBaNguoiDung_TrangThai DEFAULT ('CHO_KICH_HOAT'),

        /* Vô hiệu hoá JWT cũ sau khi chuyển cơ sở. JWT mang theo giá trị
           này; thủ tục chuyển cơ sở tăng số phiên bản → mọi token cũ hết
           hiệu lực ngay, không cần danh sách thu hồi.
           ⚠️ Tầng ứng dụng đối chiếu với bản danh bạ NẠP SẴN TRONG BỘ NHỚ,
              không truy vấn CSDL mỗi request. */
        PhienBanTaiKhoan INT          NOT NULL
            CONSTRAINT DF_DanhBaNguoiDung_PhienBan DEFAULT (1),

        NgayCapNhat      DATETIME2(0) NOT NULL
            CONSTRAINT DF_DanhBaNguoiDung_NgayCapNhat DEFAULT (SYSUTCDATETIME()),

        CONSTRAINT PK_DanhBaNguoiDung PRIMARY KEY CLUSTERED (TenDangNhap),

        CONSTRAINT FK_DanhBaNguoiDung_CoSo
            FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo),

        CONSTRAINT CK_DanhBaNguoiDung_LoaiNguoiDung
            CHECK (LoaiNguoiDung IN
                   ('SINH_VIEN', 'GIANG_VIEN', 'ADMIN_CO_SO', 'ADMIN_MASTER')),

        CONSTRAINT CK_DanhBaNguoiDung_TrangThai
            CHECK (TrangThai IN
                   ('CHO_KICH_HOAT', 'HOAT_DONG', 'DANG_CHUYEN', 'NGUNG')),

        /* ⚠️ Ràng buộc then chốt: ADMIN_MASTER thì MaCoSo phải NULL,
           mọi vai trò khác thì MaCoSo phải có giá trị.
           Không có nó, 'MASTER' sẽ lại lẻn vào như một mã cơ sở. */
        CONSTRAINT CK_DanhBaNguoiDung_CoSoTheoVaiTro
            CHECK ((LoaiNguoiDung =  'ADMIN_MASTER' AND MaCoSo IS NULL)
                OR (LoaiNguoiDung <> 'ADMIN_MASTER' AND MaCoSo IS NOT NULL)),

        CONSTRAINT CK_DanhBaNguoiDung_PhienBan
            CHECK (PhienBanTaiKhoan > 0)
    );
    PRINT '  [+] dbo.DanhBaNguoiDung';
END
ELSE PRINT '  [=] dbo.DanhBaNguoiDung';
GO

/* ---------------------------------------------------------------------
   UQ trên MaThucThe — FILTERED vì cột này NULL với tài khoản quản trị.
   Unique thường sẽ coi nhiều NULL là trùng nhau và chặn tài khoản
   quản trị thứ hai.
   --------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.indexes
                WHERE name = N'UQ_DanhBaNguoiDung_MaThucThe'
                  AND object_id = OBJECT_ID(N'dbo.DanhBaNguoiDung'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UQ_DanhBaNguoiDung_MaThucThe
        ON dbo.DanhBaNguoiDung (MaThucThe)
     WHERE MaThucThe IS NOT NULL;
    PRINT '  [+] UQ_DanhBaNguoiDung_MaThucThe (filtered)';
END
ELSE PRINT '  [=] UQ_DanhBaNguoiDung_MaThucThe';
GO

/* ---------------------------------------------------------------------
   Chỉ mục tra ngược: từ MaSinhVien / MaGiangVien ra cơ sở.
   Dùng khi Admin tra cứu người dùng theo mã thực thể thay vì tên đăng nhập.
   --------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.indexes
                WHERE name = N'IX_DanhBaNguoiDung_CoSo_Loai'
                  AND object_id = OBJECT_ID(N'dbo.DanhBaNguoiDung'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_DanhBaNguoiDung_CoSo_Loai
        ON dbo.DanhBaNguoiDung (MaCoSo, LoaiNguoiDung)
     INCLUDE (MaThucThe, TrangThai);
    PRINT '  [+] IX_DanhBaNguoiDung_CoSo_Loai';
END
ELSE PRINT '  [=] IX_DanhBaNguoiDung_CoSo_Loai';
GO

/* =====================================================================
   KIỂM CHỨNG
   ===================================================================== */
PRINT '';
PRINT '  ------------------------------------------------------------';
GO

SELECT  c.name                AS [Cot],
        t.name                AS [KieuDuLieu],
        c.max_length          AS [DoDai],
        c.is_nullable         AS [ChoNull]
  FROM  sys.columns c
  JOIN  sys.types   t ON t.user_type_id = c.user_type_id
 WHERE  c.object_id = OBJECT_ID(N'dbo.DanhBaNguoiDung')
 ORDER BY c.column_id;
GO

SELECT name AS [RangBuoc], type_desc AS [Loai]
  FROM sys.check_constraints
 WHERE parent_object_id = OBJECT_ID(N'dbo.DanhBaNguoiDung')
 ORDER BY name;
GO

PRINT '  Buoc tiep theo: master\03-taikhoan-master.sql';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
