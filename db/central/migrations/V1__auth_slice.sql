/* PTIT One — V1__auth_slice.sql (CENTRAL, Phần 1)
   Lát cắt auth khép kín: CoSo, DanhBaNguoiDung, TaiKhoan, TaiKhoanMaster,
   PhienDangNhap, TokenLamMoi. TV5 chủ trì theo AUTH-02, TV2 review.
   Schema học vụ của TV2 nối tiếp từ V2; CoSo/DanhBa/TaiKhoan đã có ở đây
   thì V2 không tạo lại.

   Cấu trúc bám db/master/01..03 và db/site/10 để Phần 2 tách ra site mà
   không đổi tên cột. Khác biệt duy nhất: một DB nên TaiKhoan có FK tới
   DanhBaNguoiDung (ở site không FK được vì tài khoản tạo trước Outbox).

   ⚠️ BA GHI CHÚ PHẦN 2 — đọc trước khi tách site:
   1. PhienDangNhap: CỤC BỘ TẠI SITE, KHÔNG NHÂN BẢN. Phiên ghi lúc đăng
      nhập; xếp nhầm vào nhóm bảng tham chiếu thì DENY ở Subscriber chặn
      chính việc đăng nhập.
   2. TokenLamMoi: như trên.
   3. DanhBaNguoiDung.PhienBanTaiKhoan nằm trong 1 trong 9 bảng NHÂN BẢN,
      MASTER sở hữu, site CHỈ ĐỌC. Sang Phần 2, "logout-all có hiệu lực
      ngay" thành "sau độ trễ nhân bản" (~3 giây đo trên máy HCM). Cách xử
      lý chốt ở Phần 2.

   Không IDENTITY: khóa phiên/token là UUID do ứng dụng sinh. Không MERGE. */

IF DB_NAME() NOT LIKE N'PTITONE[_]CENTRAL%'
    THROW 51000, N'V1__auth_slice chi chay tren PTITONE_CENTRAL*.', 1;
GO

CREATE TABLE dbo.CoSo (
    MaCoSo          varchar(10)   NOT NULL,
    TenCoSo         nvarchar(100) NOT NULL,
    ThanhPho        nvarchar(50)  NOT NULL,
    DiaChi          nvarchar(200) NULL,
    -- Phần 2: sp_ChuyenCoSoSinhVien dựng tên bốn phần từ hai cột này.
    TenLinkedServer sysname       NULL,
    TenDatabase     sysname       NOT NULL,
    DangHoatDong    bit           NOT NULL CONSTRAINT DF_CoSo_DangHoatDong DEFAULT (1),
    CONSTRAINT PK_CoSo PRIMARY KEY CLUSTERED (MaCoSo)
);
GO

/* Danh bạ: nguồn quyền authoritative cho trạng thái, cơ sở, vai trò và
   phiên bản tài khoản. KHÔNG chứa mật khẩu. */
CREATE TABLE dbo.DanhBaNguoiDung (
    TenDangNhap      varchar(50)  NOT NULL,
    -- NULL khi ADMIN_MASTER. 'MASTER' KHÔNG phải mã cơ sở.
    MaCoSo           varchar(10)  NULL,
    LoaiNguoiDung    varchar(20)  NOT NULL,
    -- MaSinhVien hoặc MaGiangVien; NULL với tài khoản quản trị.
    MaThucThe        varchar(20)  NULL,
    TrangThai        varchar(20)  NOT NULL CONSTRAINT DF_DanhBaNguoiDung_TrangThai DEFAULT ('CHO_KICH_HOAT'),
    -- JWT mang giá trị này; tăng lên là mọi phiên cũ bị từ chối ở request kế tiếp.
    PhienBanTaiKhoan int          NOT NULL CONSTRAINT DF_DanhBaNguoiDung_PhienBan DEFAULT (1),
    NgayCapNhat      datetime2(0) NOT NULL CONSTRAINT DF_DanhBaNguoiDung_NgayCapNhat DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_DanhBaNguoiDung PRIMARY KEY CLUSTERED (TenDangNhap),
    CONSTRAINT FK_DanhBaNguoiDung_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo),
    CONSTRAINT CK_DanhBaNguoiDung_LoaiNguoiDung
        CHECK (LoaiNguoiDung IN ('SINH_VIEN', 'GIANG_VIEN', 'ADMIN_CO_SO', 'ADMIN_MASTER')),
    CONSTRAINT CK_DanhBaNguoiDung_TrangThai
        CHECK (TrangThai IN ('CHO_KICH_HOAT', 'HOAT_DONG', 'DANG_CHUYEN', 'NGUNG')),
    CONSTRAINT CK_DanhBaNguoiDung_CoSoTheoVaiTro
        CHECK ((LoaiNguoiDung =  'ADMIN_MASTER' AND MaCoSo IS NULL)
            OR (LoaiNguoiDung <> 'ADMIN_MASTER' AND MaCoSo IS NOT NULL)),
    -- SV/GV phải gắn thực thể; quản trị thì không.
    CONSTRAINT CK_DanhBaNguoiDung_ThucTheTheoVaiTro
        CHECK ((LoaiNguoiDung IN ('SINH_VIEN', 'GIANG_VIEN') AND MaThucThe IS NOT NULL)
            OR (LoaiNguoiDung IN ('ADMIN_CO_SO', 'ADMIN_MASTER') AND MaThucThe IS NULL)),
    CONSTRAINT CK_DanhBaNguoiDung_PhienBan CHECK (PhienBanTaiKhoan > 0)
);
GO

-- Filtered: nhiều tài khoản quản trị cùng có MaThucThe NULL.
CREATE UNIQUE NONCLUSTERED INDEX UQ_DanhBaNguoiDung_MaThucThe
    ON dbo.DanhBaNguoiDung (MaThucThe) WHERE MaThucThe IS NOT NULL;
CREATE NONCLUSTERED INDEX IX_DanhBaNguoiDung_CoSo_Loai
    ON dbo.DanhBaNguoiDung (MaCoSo, LoaiNguoiDung) INCLUDE (MaThucThe, TrangThai);
GO

/* Tài khoản SV/GV/Admin cơ sở. Phần 2: phân mảnh ngang theo MaCoSo. */
CREATE TABLE dbo.TaiKhoan (
    TenDangNhap varchar(50)  NOT NULL,
    -- Chuỗi DelegatingPasswordEncoder, ví dụ {argon2id-v1}$argon2id$... Không lưu mật khẩu gốc.
    MatKhauHash varchar(255) NOT NULL,
    VaiTro      varchar(20)  NOT NULL,
    MaThucThe   varchar(20)  NULL,
    MaCoSo      varchar(10)  NOT NULL,
    CONSTRAINT PK_TaiKhoan PRIMARY KEY CLUSTERED (TenDangNhap),
    CONSTRAINT FK_TaiKhoan_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo),
    CONSTRAINT FK_TaiKhoan_DanhBa FOREIGN KEY (TenDangNhap) REFERENCES dbo.DanhBaNguoiDung (TenDangNhap),
    CONSTRAINT CK_TaiKhoan_VaiTro CHECK (VaiTro IN ('SINH_VIEN', 'GIANG_VIEN', 'ADMIN_CO_SO'))
);
GO

/* Admin Master không thuộc cơ sở nào nên không ở TaiKhoan. Phần 2: KHÔNG nhân bản. */
CREATE TABLE dbo.TaiKhoanMaster (
    TenDangNhap  varchar(50)   NOT NULL,
    MatKhauHash  varchar(255)  NOT NULL,
    VaiTro       varchar(20)   NOT NULL CONSTRAINT DF_TaiKhoanMaster_VaiTro DEFAULT ('ADMIN_MASTER'),
    HoTen        nvarchar(100) NOT NULL,
    DangHoatDong bit           NOT NULL CONSTRAINT DF_TaiKhoanMaster_DangHoatDong DEFAULT (1),
    NgayTao      datetime2(0)  NOT NULL CONSTRAINT DF_TaiKhoanMaster_NgayTao DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_TaiKhoanMaster PRIMARY KEY CLUSTERED (TenDangNhap),
    CONSTRAINT CK_TaiKhoanMaster_VaiTro CHECK (VaiTro = 'ADMIN_MASTER'),
    CONSTRAINT FK_TaiKhoanMaster_DanhBa FOREIGN KEY (TenDangNhap) REFERENCES dbo.DanhBaNguoiDung (TenDangNhap)
);
GO

/* Mỗi lần đăng nhập = một phiên = một token family.
   ⚠️ Phần 2: CỤC BỘ TẠI SITE, KHÔNG NHÂN BẢN (ghi chú 1 ở đầu file).
   Thời điểm lưu theo UTC. Hạn phiên cố định ThoiDiemTao + 7 ngày. */
CREATE TABLE dbo.PhienDangNhap (
    MaPhien          uniqueidentifier NOT NULL,
    TenDangNhap      varchar(50)      NOT NULL,
    PhienBanLucTao   int              NOT NULL,
    ThoiDiemTao      datetime2(3)     NOT NULL,
    ThoiDiemHetHan   datetime2(3)     NOT NULL,
    ThoiDiemThuHoi   datetime2(3)     NULL,
    LyDoThuHoi       varchar(30)      NULL,
    CONSTRAINT PK_PhienDangNhap PRIMARY KEY CLUSTERED (MaPhien),
    CONSTRAINT FK_PhienDangNhap_DanhBa FOREIGN KEY (TenDangNhap) REFERENCES dbo.DanhBaNguoiDung (TenDangNhap),
    CONSTRAINT CK_PhienDangNhap_HetHan CHECK (ThoiDiemHetHan > ThoiDiemTao),
    CONSTRAINT CK_PhienDangNhap_LyDo
        CHECK (LyDoThuHoi IN ('DANG_XUAT', 'DANG_XUAT_TAT_CA', 'PHAT_HIEN_DUNG_LAI',
                              'DOI_MAT_KHAU', 'KHOI_PHUC_MAT_KHAU', 'THAY_DOI_TAI_KHOAN')),
    CONSTRAINT CK_PhienDangNhap_ThuHoi
        CHECK ((ThoiDiemThuHoi IS NULL AND LyDoThuHoi IS NULL)
            OR (ThoiDiemThuHoi IS NOT NULL AND LyDoThuHoi IS NOT NULL))
);
GO

-- Logout-all / khóa tài khoản: tìm mọi phiên còn sống của một người.
CREATE NONCLUSTERED INDEX IX_PhienDangNhap_TenDangNhap_ConHieuLuc
    ON dbo.PhienDangNhap (TenDangNhap) INCLUDE (ThoiDiemHetHan)
    WHERE ThoiDiemThuHoi IS NULL;
GO

/* Refresh token opaque 32 byte. Chỉ lưu SHA-256, KHÔNG lưu token gốc.
   Token đã dùng giữ tới hết hạn phiên để còn phát hiện replay.
   ⚠️ Phần 2: CỤC BỘ TẠI SITE, KHÔNG NHÂN BẢN (ghi chú 2 ở đầu file). */
CREATE TABLE dbo.TokenLamMoi (
    MaToken        uniqueidentifier NOT NULL,
    MaPhien        uniqueidentifier NOT NULL,
    TokenHash      binary(32)       NOT NULL,
    ThoiDiemTao    datetime2(3)     NOT NULL,
    ThoiDiemHetHan datetime2(3)     NOT NULL,
    ThoiDiemDaDung datetime2(3)     NULL,
    ThoiDiemThuHoi datetime2(3)     NULL,
    CONSTRAINT PK_TokenLamMoi PRIMARY KEY CLUSTERED (MaToken),
    CONSTRAINT UQ_TokenLamMoi_TokenHash UNIQUE (TokenHash),
    CONSTRAINT FK_TokenLamMoi_Phien FOREIGN KEY (MaPhien) REFERENCES dbo.PhienDangNhap (MaPhien),
    CONSTRAINT CK_TokenLamMoi_HetHan CHECK (ThoiDiemHetHan > ThoiDiemTao)
);
GO

CREATE NONCLUSTERED INDEX IX_TokenLamMoi_MaPhien ON dbo.TokenLamMoi (MaPhien);
GO
