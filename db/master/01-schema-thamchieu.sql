/* =====================================================================
   PTIT One — master/01-schema-thamchieu.sql
   ---------------------------------------------------------------------
   TÁM bảng dữ liệu tham chiếu. Đây là toàn bộ nội dung được NHÂN BẢN
   ra mọi cơ sở (mục D1).

   CHẠY Ở ĐÂU : CHỈ trên PTITONE_MASTER
                  .\run.ps1 -Script master\01-schema-thamchieu.sql -On MASTER

   CHẠY LẠI ĐƯỢC : có — mọi CREATE đều bọc IF OBJECT_ID(...) IS NULL

   ⚠️ MỌI bảng ở đây BẮT BUỘC có PRIMARY KEY — Transactional Replication
      không nhân bản được bảng thiếu khoá chính.
   ⚠️ KHÔNG dùng IDENTITY ở bất kỳ đâu (quyết định D8).
   ===================================================================== */

SET NOCOUNT ON;
GO

/* ---------------------------------------------------------------------
   0. CHẶN CHẠY NHẦM DATABASE
      Chạy nhầm lên CSDL vận hành sẽ tạo bảng tham chiếu ở đó, phá vỡ
      ranh giới Master/vận hành ở mục C0.
   --------------------------------------------------------------------- */
IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    RAISERROR(N'Script nay CHI chay tren $(DbMaster). Database hien tai: %s',
              16, 1, DB_NAME());
    SET NOEXEC ON;
END
GO

/* =====================================================================
   1. CoSo — danh mục cơ sở đào tạo

   ⚠️ Bảng này CHỈ chứa CƠ SỞ ĐÀO TẠO CÓ THẬT.
      "MASTER" KHÔNG phải một mã cơ sở — nó là VAI TRÒ TRIỂN KHAI, và
      thuộc về db/config.ps1, không thuộc về dữ liệu.

   TenLinkedServer + TenDatabase là thông tin triển khai được đưa vào
   bảng vì sp_ChuyenCoSoSinhVien (mục D8) cần dựng tên bốn phần từ đây.
   Chúng đóng vai trò DANH SÁCH TRẮNG, nhờ đó dynamic SQL trong thủ tục
   đó không có đường injection.
   ===================================================================== */
IF OBJECT_ID(N'dbo.CoSo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CoSo (
        MaCoSo          VARCHAR(10)   NOT NULL,
        TenCoSo         NVARCHAR(100) NOT NULL,
        ThanhPho        NVARCHAR(50)  NOT NULL,
        DiaChi          NVARCHAR(200) NULL,

        -- Dùng cho sp_ChuyenCoSoSinhVien dựng tên bốn phần
        TenLinkedServer SYSNAME       NULL,   -- NULL = cơ sở cục bộ, không qua Linked Server
        TenDatabase     SYSNAME       NOT NULL,

        DangHoatDong    BIT           NOT NULL
            CONSTRAINT DF_CoSo_DangHoatDong DEFAULT (1),

        CONSTRAINT PK_CoSo PRIMARY KEY CLUSTERED (MaCoSo)
    );
    PRINT '  [+] dbo.CoSo';
END
ELSE PRINT '  [=] dbo.CoSo';
GO

/* =====================================================================
   2. Khoa
   ===================================================================== */
IF OBJECT_ID(N'dbo.Khoa', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Khoa (
        MaKhoa  VARCHAR(10)   NOT NULL,
        TenKhoa NVARCHAR(150) NOT NULL,
        CONSTRAINT PK_Khoa PRIMARY KEY CLUSTERED (MaKhoa)
    );
    PRINT '  [+] dbo.Khoa';
END
ELSE PRINT '  [=] dbo.Khoa';
GO

/* =====================================================================
   3. ChuongTrinhDaoTao
   ===================================================================== */
IF OBJECT_ID(N'dbo.ChuongTrinhDaoTao', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ChuongTrinhDaoTao (
        MaCTDT     VARCHAR(20)   NOT NULL,
        TenCTDT    NVARCHAR(200) NOT NULL,
        MaKhoa     VARCHAR(10)   NOT NULL,
        TongTinChi SMALLINT      NOT NULL,

        CONSTRAINT PK_ChuongTrinhDaoTao PRIMARY KEY CLUSTERED (MaCTDT),
        CONSTRAINT FK_ChuongTrinhDaoTao_Khoa
            FOREIGN KEY (MaKhoa) REFERENCES dbo.Khoa (MaKhoa),
        CONSTRAINT CK_ChuongTrinhDaoTao_TongTinChi
            CHECK (TongTinChi > 0)
    );
    PRINT '  [+] dbo.ChuongTrinhDaoTao';
END
ELSE PRINT '  [=] dbo.ChuongTrinhDaoTao';
GO

/* =====================================================================
   4. MonHoc — bảng bị đọc nhiều nhất hệ thống (~80.000 lượt/ngày, mục B2).
      Chính hồ sơ đọc/ghi 5.300:1 của nó là căn cứ định lượng cho quyết
      định nhân bản.
   ===================================================================== */
IF OBJECT_ID(N'dbo.MonHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MonHoc (
        MaMonHoc  VARCHAR(20)   NOT NULL,
        TenMonHoc NVARCHAR(200) NOT NULL,
        SoTinChi  TINYINT       NOT NULL,
        MaKhoa    VARCHAR(10)   NOT NULL,

        CONSTRAINT PK_MonHoc PRIMARY KEY CLUSTERED (MaMonHoc),
        CONSTRAINT FK_MonHoc_Khoa
            FOREIGN KEY (MaKhoa) REFERENCES dbo.Khoa (MaKhoa),
        CONSTRAINT CK_MonHoc_SoTinChi
            CHECK (SoTinChi BETWEEN 1 AND 15)
    );
    PRINT '  [+] dbo.MonHoc';
END
ELSE PRINT '  [=] dbo.MonHoc';
GO

/* =====================================================================
   5. CTDT_MonHoc — chương trình đào tạo gồm những môn nào

   Thiếu bảng này thì không xét được tiến độ học tập, không gợi ý được
   môn nên đăng ký, và không tính được điều kiện tốt nghiệp.
   ===================================================================== */
IF OBJECT_ID(N'dbo.CTDT_MonHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CTDT_MonHoc (
        MaCTDT    VARCHAR(20) NOT NULL,
        MaMonHoc  VARCHAR(20) NOT NULL,
        HocKyGoiY TINYINT     NULL,      -- học kỳ thứ mấy trong lộ trình
        BatBuoc   BIT         NOT NULL
            CONSTRAINT DF_CTDT_MonHoc_BatBuoc DEFAULT (1),

        CONSTRAINT PK_CTDT_MonHoc PRIMARY KEY CLUSTERED (MaCTDT, MaMonHoc),
        CONSTRAINT FK_CTDT_MonHoc_CTDT
            FOREIGN KEY (MaCTDT)   REFERENCES dbo.ChuongTrinhDaoTao (MaCTDT),
        CONSTRAINT FK_CTDT_MonHoc_MonHoc
            FOREIGN KEY (MaMonHoc) REFERENCES dbo.MonHoc (MaMonHoc)
    );
    PRINT '  [+] dbo.CTDT_MonHoc';
END
ELSE PRINT '  [=] dbo.CTDT_MonHoc';
GO

/* =====================================================================
   6. MonHocTienQuyet — tự quan hệ N:M trên MonHoc

   Điều kiện tiên quyết được kiểm TẠI HOME, vì bảng điểm nằm ở Home.
   Đây là ví dụ sách giáo khoa của "push computation to data" (mục D3).
   ===================================================================== */
IF OBJECT_ID(N'dbo.MonHocTienQuyet', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MonHocTienQuyet (
        MaMonHoc       VARCHAR(20) NOT NULL,
        MaMonTienQuyet VARCHAR(20) NOT NULL,

        CONSTRAINT PK_MonHocTienQuyet
            PRIMARY KEY CLUSTERED (MaMonHoc, MaMonTienQuyet),
        CONSTRAINT FK_MonHocTienQuyet_Mon
            FOREIGN KEY (MaMonHoc)       REFERENCES dbo.MonHoc (MaMonHoc),
        CONSTRAINT FK_MonHocTienQuyet_MonTQ
            FOREIGN KEY (MaMonTienQuyet) REFERENCES dbo.MonHoc (MaMonHoc),
        -- Chặn môn tự làm tiên quyết của chính nó
        CONSTRAINT CK_MonHocTienQuyet_KhongTuTro
            CHECK (MaMonHoc <> MaMonTienQuyet)
    );
    PRINT '  [+] dbo.MonHocTienQuyet';
END
ELSE PRINT '  [=] dbo.MonHocTienQuyet';
GO

/* =====================================================================
   7. HocKy — lịch chung TOÀN TRƯỜNG

   ⚠️ Đợt đăng ký của từng cơ sở KHÔNG nằm ở đây. DotDangKy là bảng CỤC BỘ
      tại mỗi site và KHÔNG được nhân bản — vì Subscriber chỉ đọc, nếu gộp
      vào đây thì mỗi cơ sở sẽ không tự mở được lịch đăng ký của mình.

   NgayBatDau là mốc để suy ra SỐ TUẦN dùng trong LichHoc.
   ===================================================================== */
IF OBJECT_ID(N'dbo.HocKy', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.HocKy (
        MaHocKy     VARCHAR(20)   NOT NULL,
        TenHocKy    NVARCHAR(100) NOT NULL,
        NamHoc      VARCHAR(9)    NOT NULL,   -- ví dụ '2025-2026'
        NgayBatDau  DATE          NOT NULL,   -- mốc tính số tuần
        NgayKetThuc DATE          NOT NULL,

        CONSTRAINT PK_HocKy PRIMARY KEY CLUSTERED (MaHocKy),
        CONSTRAINT CK_HocKy_KhoangNgay
            CHECK (NgayKetThuc > NgayBatDau)
    );
    PRINT '  [+] dbo.HocKy';
END
ELSE PRINT '  [=] dbo.HocKy';
GO

/* =====================================================================
   8. KhungGioTiet — khung giờ của từng tiết học

   ⚠️ PHẢI DÙNG CHUNG Ở MỌI CƠ SỞ. Nếu mỗi nơi tự quy ước khung giờ tiết
      hoặc cách đánh số tuần thì phép so Thu/TietBatDau/Tuan giữa hai site
      là VÔ NGHĨA — và toàn bộ việc kiểm trùng lịch liên cơ sở sụp đổ.

      Đó là lý do bảng này nằm ở Master và được nhân bản, thay vì để mỗi
      cơ sở tự quy ước bằng lời.
   ===================================================================== */
IF OBJECT_ID(N'dbo.KhungGioTiet', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.KhungGioTiet (
        SoTiet     TINYINT     NOT NULL,
        GioBatDau  TIME(0)     NOT NULL,
        GioKetThuc TIME(0)     NOT NULL,
        Buoi       VARCHAR(10) NOT NULL,   -- SANG / CHIEU / TOI

        CONSTRAINT PK_KhungGioTiet PRIMARY KEY CLUSTERED (SoTiet),
        CONSTRAINT CK_KhungGioTiet_Gio
            CHECK (GioKetThuc > GioBatDau),
        CONSTRAINT CK_KhungGioTiet_Buoi
            CHECK (Buoi IN ('SANG', 'CHIEU', 'TOI'))
    );
    PRINT '  [+] dbo.KhungGioTiet';
END
ELSE PRINT '  [=] dbo.KhungGioTiet';
GO

/* =====================================================================
   KIỂM CHỨNG
   ===================================================================== */
PRINT '';
PRINT '  ------------------------------------------------------------';
PRINT '  Bang tham chieu trong $(DbMaster):';
GO

SELECT  t.name                                    AS [Bang],
        (SELECT COUNT(*) FROM sys.key_constraints k
          WHERE k.parent_object_id = t.object_id
            AND k.type = 'PK')                    AS [CoPK],
        (SELECT COUNT(*) FROM sys.foreign_keys f
          WHERE f.parent_object_id = t.object_id)  AS [SoFK],
        (SELECT COUNT(*) FROM sys.check_constraints c
          WHERE c.parent_object_id = t.object_id)  AS [SoCHECK]
  FROM  sys.tables t
 WHERE  t.name IN (N'CoSo', N'Khoa', N'ChuongTrinhDaoTao', N'MonHoc',
                   N'CTDT_MonHoc', N'MonHocTienQuyet', N'HocKy', N'KhungGioTiet')
 ORDER BY t.name;
GO

/* Mọi bảng phải có PK, nếu không Transactional Replication sẽ từ chối */
IF EXISTS (
    SELECT 1 FROM sys.tables t
     WHERE t.name IN (N'CoSo', N'Khoa', N'ChuongTrinhDaoTao', N'MonHoc',
                      N'CTDT_MonHoc', N'MonHocTienQuyet', N'HocKy', N'KhungGioTiet')
       AND NOT EXISTS (SELECT 1 FROM sys.key_constraints k
                        WHERE k.parent_object_id = t.object_id AND k.type = 'PK'))
    RAISERROR(N'CO BANG THIEU PRIMARY KEY — Transactional Replication se tu choi!', 16, 1);
ELSE
    PRINT '  [ok] Moi bang deu co PRIMARY KEY';
GO

PRINT '  Buoc tiep theo: master\02-danhba-nguoidung.sql';
PRINT '  ------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
