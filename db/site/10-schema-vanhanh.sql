/* PTIT One — schema 21 bang tai moi site (C1, C9, I6).
   Chay SAU snapshot 32 da ap dung: -Script site\10-schema-vanhanh.sql -On HCM/HN/DN.
   Chi tao bang con thieu; khong sua/xoa du lieu khi chay lai.
   PK/FK o day; CHECK/UNIQUE o 11; chi muc truy van o 12.
   Khong IDENTITY, FK xuyen site, cascade delete hay trigger cap nhat bo dem. */
:ON ERROR EXIT
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF N'$(SiteTarget)' NOT IN (N'HCM', N'HN', N'DN')
   OR DB_NAME() <> N'$(DbTarget)' OR DB_NAME() = N'$(DbMaster)'
    THROW 51100, N'Chi chay schema van hanh tren dung Subscriber (-On HCM/HN/DN).', 1;
IF EXISTS (
    SELECT 1 FROM (VALUES (N'CoSo'), (N'Khoa'), (N'ChuongTrinhDaoTao'),
        (N'MonHoc'), (N'CTDT_MonHoc'), (N'MonHocTienQuyet'),
        (N'HocKy'), (N'KhungGioTiet'), (N'DanhBaNguoiDung')) r(TableName)
    WHERE OBJECT_ID(N'dbo.' + r.TableName, N'U') IS NULL)
    THROW 51101, N'Thieu bang tham chieu. Doi snapshot ap dung thanh cong truoc.', 1;
GO

BEGIN TRANSACTION;
GO
IF OBJECT_ID(N'dbo.SinhVien', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SinhVien (
        MaSinhVien varchar(20) NOT NULL,
        HoTen nvarchar(150) NOT NULL,
        NgaySinh date NULL,
        MaCoSoNha varchar(10) NOT NULL,
        MaCTDT varchar(20) NOT NULL,
        TrangThai varchar(20) NOT NULL,
        SoTinChiTichLuy smallint NOT NULL CONSTRAINT DF_SinhVien_TinChi DEFAULT (0),
        SoMonLienCoSo smallint NOT NULL CONSTRAINT DF_SinhVien_LienCoSo DEFAULT (0),
        GioiTinh varchar(3) NULL,
        DienThoai varchar(20) NULL,
        SoCCCD varchar(20) NULL,
        EmailCaNhan nvarchar(254) NULL,
        NoiSinh nvarchar(150) NULL,
        DanToc nvarchar(50) NULL,
        TonGiao nvarchar(50) NULL,
        HoKhau nvarchar(255) NULL,
        -- URL anh tren dich vu ngoai (Cloudinary). Khong luu nhi phan: snapshot
        -- replication se phinh vo ich.
        AnhDaiDien varchar(500) NULL,
        CONSTRAINT PK_SinhVien PRIMARY KEY CLUSTERED (MaSinhVien),
        CONSTRAINT FK_SinhVien_CoSo FOREIGN KEY (MaCoSoNha) REFERENCES dbo.CoSo (MaCoSo),
        CONSTRAINT FK_SinhVien_CTDT FOREIGN KEY (MaCTDT) REFERENCES dbo.ChuongTrinhDaoTao (MaCTDT)
    );
    PRINT '  [+] dbo.SinhVien';
END;
GO

IF OBJECT_ID(N'dbo.GiangVien', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.GiangVien (
        MaGiangVien varchar(20) NOT NULL,
        HoTen nvarchar(150) NOT NULL,
        MaCoSo varchar(10) NOT NULL,
        MaKhoa varchar(10) NOT NULL,
        HocVi nvarchar(50) NULL,
        GioiTinh varchar(3) NULL,
        DienThoai varchar(20) NULL,
        SoCCCD varchar(20) NULL,
        EmailCaNhan nvarchar(254) NULL,
        NoiSinh nvarchar(150) NULL,
        DanToc nvarchar(50) NULL,
        TonGiao nvarchar(50) NULL,
        HoKhau nvarchar(255) NULL,
        -- URL anh tren dich vu ngoai (Cloudinary). Khong luu nhi phan: snapshot
        -- replication se phinh vo ich.
        AnhDaiDien varchar(500) NULL,
        CONSTRAINT PK_GiangVien PRIMARY KEY CLUSTERED (MaGiangVien),
        CONSTRAINT FK_GiangVien_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo),
        CONSTRAINT FK_GiangVien_Khoa FOREIGN KEY (MaKhoa) REFERENCES dbo.Khoa (MaKhoa)
    );
    PRINT '  [+] dbo.GiangVien';
END;
GO

-- MaThucThe da hinh SV/GV; khong FK toi DanhBa replica vi tao tai khoan truoc Outbox.
IF OBJECT_ID(N'dbo.TaiKhoan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TaiKhoan (
        TenDangNhap varchar(50) NOT NULL,
        -- NULL khi tai khoan cap xong nhung chua kich hoat (F02): chua co
        -- mat khau nao ca, khong phai mat khau rong.
        MatKhauHash varchar(255) NULL,
        VaiTro varchar(20) NOT NULL,
        MaThucThe varchar(20) NULL,
        MaCoSo varchar(10) NOT NULL,
        Email varchar(254) NULL,
        EmailDaXacMinh bit NOT NULL
            CONSTRAINT DF_TaiKhoan_EmailDaXacMinh DEFAULT (0),
        ThoiDiemXacMinhEmail datetime2(3) NULL,
        CONSTRAINT PK_TaiKhoan PRIMARY KEY CLUSTERED (TenDangNhap),
        CONSTRAINT FK_TaiKhoan_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo)
    );
    PRINT '  [+] dbo.TaiKhoan';
END;
GO

IF OBJECT_ID(N'dbo.DotDangKy', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DotDangKy (
        MaDot varchar(50) NOT NULL,
        MaHocKy varchar(20) NOT NULL,
        MaCoSo varchar(10) NOT NULL,
        ThoiGianMo datetime2(0) NOT NULL,
        ThoiGianDong datetime2(0) NOT NULL,
        TrangThai varchar(20) NOT NULL,
        CONSTRAINT PK_DotDangKy PRIMARY KEY CLUSTERED (MaDot),
        CONSTRAINT FK_DotDangKy_HocKy FOREIGN KEY (MaHocKy) REFERENCES dbo.HocKy (MaHocKy),
        CONSTRAINT FK_DotDangKy_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo)
    );
    PRINT '  [+] dbo.DotDangKy';
END;
GO

-- PhongHoc o tung buoi (C1 nhom 3b). PhienBanLich o Host de doi chieu snapshot (D3).
IF OBJECT_ID(N'dbo.LopHocPhan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.LopHocPhan (
        MaLopHP varchar(80) NOT NULL,
        MaMonHoc varchar(20) NOT NULL,
        MaHocKy varchar(20) NOT NULL,
        MaCoSoHost varchar(10) NOT NULL,
        MaGiangVien varchar(20) NULL,
        SoLuongToiDa smallint NOT NULL,
        SoLuongDaDangKy smallint NOT NULL CONSTRAINT DF_LopHocPhan_SiSo DEFAULT (0),
        TrangThai varchar(20) NOT NULL,
        ChoPhepLienCoSo bit NOT NULL CONSTRAINT DF_LopHocPhan_LienCoSo DEFAULT (0),
        HinhThucHoc varchar(20) NOT NULL,
        PhienBanLich int NOT NULL CONSTRAINT DF_LopHocPhan_PhienBanLich DEFAULT (1),
        CONSTRAINT PK_LopHocPhan PRIMARY KEY CLUSTERED (MaLopHP),
        CONSTRAINT FK_LopHocPhan_MonHoc FOREIGN KEY (MaMonHoc) REFERENCES dbo.MonHoc (MaMonHoc),
        CONSTRAINT FK_LopHocPhan_HocKy FOREIGN KEY (MaHocKy) REFERENCES dbo.HocKy (MaHocKy),
        CONSTRAINT FK_LopHocPhan_CoSo FOREIGN KEY (MaCoSoHost) REFERENCES dbo.CoSo (MaCoSo),
        CONSTRAINT FK_LopHocPhan_GiangVien FOREIGN KEY (MaGiangVien) REFERENCES dbo.GiangVien (MaGiangVien)
    );
    PRINT '  [+] dbo.LopHocPhan';
END;
GO

IF OBJECT_ID(N'dbo.LichHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.LichHoc (
        MaLopHP varchar(80) NOT NULL,
        Thu tinyint NOT NULL,
        TietBatDau tinyint NOT NULL,
        SoTiet tinyint NOT NULL,
        PhongHoc nvarchar(100) NULL,
        TuanBatDau smallint NOT NULL,
        TuanKetThuc smallint NOT NULL,
        CONSTRAINT PK_LichHoc PRIMARY KEY CLUSTERED (MaLopHP, Thu, TietBatDau),
        CONSTRAINT FK_LichHoc_LopHocPhan FOREIGN KEY (MaLopHP) REFERENCES dbo.LopHocPhan (MaLopHP),
        CONSTRAINT FK_LichHoc_KhungGioTiet FOREIGN KEY (TietBatDau) REFERENCES dbo.KhungGioTiet (SoTiet)
    );
    PRINT '  [+] dbo.LichHoc';
END;
GO

-- Host giu ca sinh vien khach: TUYET DOI KHONG FK toi dbo.SinhVien.
IF OBJECT_ID(N'dbo.DangKyHocPhan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DangKyHocPhan (
        MaLopHP varchar(80) NOT NULL,
        MaSinhVien varchar(20) NOT NULL,
        MaCoSoNhaSV varchar(10) NOT NULL,
        HoTenSinhVien nvarchar(150) NOT NULL,
        NgayDangKy datetime2(0) NOT NULL CONSTRAINT DF_DangKyHocPhan_Ngay DEFAULT (SYSUTCDATETIME()),
        TrangThai varchar(20) NOT NULL CONSTRAINT DF_DangKyHocPhan_TrangThai DEFAULT ('DA_DANG_KY'),
        MaYeuCau uniqueidentifier NULL,
        CONSTRAINT PK_DangKyHocPhan PRIMARY KEY CLUSTERED (MaLopHP, MaSinhVien),
        CONSTRAINT FK_DangKyHocPhan_LopHocPhan FOREIGN KEY (MaLopHP) REFERENCES dbo.LopHocPhan (MaLopHP),
        CONSTRAINT FK_DangKyHocPhan_CoSo FOREIGN KEY (MaCoSoNhaSV) REFERENCES dbo.CoSo (MaCoSo)
    );
    PRINT '  [+] dbo.DangKyHocPhan';
END;
GO

-- Version la so thu tu su kien, KHONG phai rowversion. Cong thuc diem C1 con la gia dinh.
IF OBJECT_ID(N'dbo.Diem', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Diem (
        MaLopHP varchar(80) NOT NULL,
        MaSinhVien varchar(20) NOT NULL,
        DiemChuyenCan decimal(3,1) NULL,
        DiemGiuaKy decimal(3,1) NULL,
        DiemCuoiKy decimal(3,1) NULL,
        DiemTongKet decimal(3,1) NULL,
        Version bigint NOT NULL CONSTRAINT DF_Diem_Version DEFAULT (1),
        NgayCongBo datetime2(0) NULL,
        CONSTRAINT PK_Diem PRIMARY KEY CLUSTERED (MaLopHP, MaSinhVien),
        CONSTRAINT FK_Diem_DangKyHocPhan FOREIGN KEY (MaLopHP, MaSinhVien)
            REFERENCES dbo.DangKyHocPhan (MaLopHP, MaSinhVien)
    );
    PRINT '  [+] dbo.Diem';
END;
GO

-- TranTinChi phai truyen ro; 24 la gia dinh, khong dong cung thanh quy che.
IF OBJECT_ID(N'dbo.SinhVienHocKy', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SinhVienHocKy (
        MaSinhVien varchar(20) NOT NULL,
        MaHocKy varchar(20) NOT NULL,
        SoTinChiDaDangKy smallint NOT NULL CONSTRAINT DF_SinhVienHocKy_DaDangKy DEFAULT (0),
        SoTinChiDangGiuCho smallint NOT NULL CONSTRAINT DF_SinhVienHocKy_GiuCho DEFAULT (0),
        TranTinChi smallint NOT NULL,
        CONSTRAINT PK_SinhVienHocKy PRIMARY KEY CLUSTERED (MaSinhVien, MaHocKy),
        CONSTRAINT FK_SinhVienHocKy_SinhVien FOREIGN KEY (MaSinhVien) REFERENCES dbo.SinhVien (MaSinhVien),
        CONSTRAINT FK_SinhVienHocKy_HocKy FOREIGN KEY (MaHocKy) REFERENCES dbo.HocKy (MaHocKy)
    );
    PRINT '  [+] dbo.SinhVienHocKy';
END;
GO

-- MaLopHP co the o Host khac nen KHONG FK toi LopHocPhan cuc bo. PK dung nguyen I6.
IF OBJECT_ID(N'dbo.DangKyMonHoc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DangKyMonHoc (
        MaSinhVien varchar(20) NOT NULL,
        MaHocKy varchar(20) NOT NULL,
        MaMonHoc varchar(20) NOT NULL,
        MaLopHP varchar(80) NOT NULL,
        MaCoSoHost varchar(10) NOT NULL,
        TrangThai varchar(20) NOT NULL,
        MaYeuCau uniqueidentifier NULL,
        SoTinChi tinyint NOT NULL,
        PhienBanLich int NOT NULL,
        CONSTRAINT PK_DangKyMonHoc PRIMARY KEY CLUSTERED (MaSinhVien, MaHocKy, MaMonHoc),
        CONSTRAINT FK_DangKyMonHoc_SinhVienHocKy FOREIGN KEY (MaSinhVien, MaHocKy)
            REFERENCES dbo.SinhVienHocKy (MaSinhVien, MaHocKy),
        CONSTRAINT FK_DangKyMonHoc_MonHoc FOREIGN KEY (MaMonHoc) REFERENCES dbo.MonHoc (MaMonHoc),
        CONSTRAINT FK_DangKyMonHoc_CoSo FOREIGN KEY (MaCoSoHost) REFERENCES dbo.CoSo (MaCoSo)
    );
    PRINT '  [+] dbo.DangKyMonHoc';
END;
GO

-- Home ghi ngay luc tao yeu cau, chi cho lop lien co so. PhienBanLich la version cua mirror.
IF OBJECT_ID(N'dbo.LichHocMirror', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.LichHocMirror (
        MaSinhVien varchar(20) NOT NULL,
        MaHocKy varchar(20) NOT NULL,
        MaMonHoc varchar(20) NOT NULL,
        Thu tinyint NOT NULL,
        TietBatDau tinyint NOT NULL,
        SoTiet tinyint NOT NULL,
        PhongHoc nvarchar(100) NULL,
        TuanBatDau smallint NOT NULL,
        TuanKetThuc smallint NOT NULL,
        PhienBanLich int NOT NULL,
        LastSyncedAt datetime2(0) NOT NULL CONSTRAINT DF_LichHocMirror_Synced DEFAULT (SYSUTCDATETIME()),
        SyncStatus varchar(20) NOT NULL,
        CONSTRAINT PK_LichHocMirror PRIMARY KEY CLUSTERED (MaSinhVien, MaHocKy, MaMonHoc, Thu, TietBatDau),
        CONSTRAINT FK_LichHocMirror_DangKyMonHoc FOREIGN KEY (MaSinhVien, MaHocKy, MaMonHoc)
            REFERENCES dbo.DangKyMonHoc (MaSinhVien, MaHocKy, MaMonHoc),
        CONSTRAINT FK_LichHocMirror_KhungGioTiet FOREIGN KEY (TietBatDau) REFERENCES dbo.KhungGioTiet (SoTiet)
    );
    PRINT '  [+] dbo.LichHocMirror';
END;
GO

-- UUID do ung dung cap; snapshot lop tu xa khong co FK toi LopHocPhan cuc bo.
IF OBJECT_ID(N'dbo.YeuCauHocLienCoSo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.YeuCauHocLienCoSo (
        MaYeuCau uniqueidentifier NOT NULL,
        MaSinhVien varchar(20) NOT NULL,
        MaLopHP varchar(80) NOT NULL,
        MaCoSoHost varchar(10) NOT NULL,
        TrangThai varchar(20) NOT NULL,
        LyDoTuChoi nvarchar(500) NULL,
        SoLanThu int NOT NULL CONSTRAINT DF_YeuCauHocLienCoSo_SoLanThu DEFAULT (0),
        MaMonHoc varchar(20) NOT NULL,
        TenMonHoc nvarchar(200) NOT NULL,
        SoTinChi tinyint NOT NULL,
        HinhThucHoc varchar(20) NOT NULL,
        LichHocJson nvarchar(max) NOT NULL,
        PhongHoc nvarchar(100) NULL,
        ThoiDiemDongBo datetime2(0) NOT NULL,
        CONSTRAINT PK_YeuCauHocLienCoSo PRIMARY KEY NONCLUSTERED (MaYeuCau),
        CONSTRAINT FK_YeuCauHocLienCoSo_SinhVien FOREIGN KEY (MaSinhVien) REFERENCES dbo.SinhVien (MaSinhVien),
        CONSTRAINT FK_YeuCauHocLienCoSo_CoSo FOREIGN KEY (MaCoSoHost) REFERENCES dbo.CoSo (MaCoSo),
        CONSTRAINT FK_YeuCauHocLienCoSo_MonHoc FOREIGN KEY (MaMonHoc) REFERENCES dbo.MonHoc (MaMonHoc)
    );
    PRINT '  [+] dbo.YeuCauHocLienCoSo';
END;
GO

-- Khong FK toi ghi danh/lop/SV: phai luu duoc tu choi lop khong ton tai va huy den truoc dang ky.
IF OBJECT_ID(N'dbo.KetQuaXuLyYeuCau', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.KetQuaXuLyYeuCau (
        MaYeuCau uniqueidentifier NOT NULL,
        KetQua varchar(20) NOT NULL,
        MaLoi varchar(50) NULL,
        LyDo nvarchar(500) NULL,
        MaLopHP varchar(80) NOT NULL,
        MaSinhVien varchar(20) NOT NULL,
        MaThaoTacHuy uniqueidentifier NULL,
        ThoiDiemXuLy datetime2(0) NOT NULL CONSTRAINT DF_KetQuaXuLyYeuCau_ThoiDiem DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT PK_KetQuaXuLyYeuCau PRIMARY KEY NONCLUSTERED (MaYeuCau)
    );
    PRINT '  [+] dbo.KetQuaXuLyYeuCau';
END;
GO

IF OBJECT_ID(N'dbo.BangDiemMirror', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BangDiemMirror (
        MaSinhVien varchar(20) NOT NULL,
        MaLopHP varchar(80) NOT NULL,
        MaMonHoc varchar(20) NOT NULL,
        TenMonHoc nvarchar(200) NOT NULL,
        SoTinChi tinyint NOT NULL,
        DiemTongKet decimal(3,1) NOT NULL,
        Version bigint NOT NULL,
        LastSyncedAt datetime2(0) NOT NULL,
        SyncStatus varchar(20) NOT NULL,
        CONSTRAINT PK_BangDiemMirror PRIMARY KEY CLUSTERED (MaSinhVien, MaLopHP),
        CONSTRAINT FK_BangDiemMirror_SinhVien FOREIGN KEY (MaSinhVien) REFERENCES dbo.SinhVien (MaSinhVien),
        CONSTRAINT FK_BangDiemMirror_MonHoc FOREIGN KEY (MaMonHoc) REFERENCES dbo.MonHoc (MaMonHoc)
    );
    PRINT '  [+] dbo.BangDiemMirror';
END;
GO

-- Dong bo diem: chi phat su kien cho SV khach; worker upsert Mirror TRUOC, SENT SAU.
IF OBJECT_ID(N'dbo.OutboxSuKien', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OutboxSuKien (
        EventId uniqueidentifier NOT NULL,
        LoaiSuKien varchar(50) NOT NULL,
        KhoaThucThe varchar(200) NOT NULL,
        NoiDung nvarchar(max) NOT NULL,
        Version bigint NOT NULL,
        TrangThai varchar(20) NOT NULL CONSTRAINT DF_OutboxSuKien_TrangThai DEFAULT ('PENDING'),
        SoLanThu int NOT NULL CONSTRAINT DF_OutboxSuKien_SoLanThu DEFAULT (0),
        ThoiDiemTao datetime2(0) NOT NULL CONSTRAINT DF_OutboxSuKien_Tao DEFAULT (SYSUTCDATETIME()),
        ThoiDiemXuLy datetime2(0) NULL,
        CONSTRAINT PK_OutboxSuKien PRIMARY KEY NONCLUSTERED (EventId)
    );
    PRINT '  [+] dbo.OutboxSuKien';
END;
GO

-- Phien dang nhap dat TAI SITE, canh TaiKhoan cua site do: kiem phien la duong
-- nong cua moi request, de o Master thi moi request phai di qua VPN va Master
-- mat ho so "ghi hiem" von la ly do no ton tai.
-- Admin Master khong thuoc co so nao nen co ban song song o PTITONE_MASTER.
IF OBJECT_ID(N'dbo.PhienDangNhap', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PhienDangNhap (
        MaPhien uniqueidentifier NOT NULL,
        TenDangNhap varchar(50) NOT NULL,
        PhienBanLucTao int NOT NULL,
        ThoiDiemTao datetime2(3) NOT NULL,
        ThoiDiemHetHan datetime2(3) NOT NULL,
        ThoiDiemThuHoi datetime2(3) NULL,
        LyDoThuHoi varchar(30) NULL,
        CONSTRAINT PK_PhienDangNhap PRIMARY KEY NONCLUSTERED (MaPhien),
        CONSTRAINT FK_PhienDangNhap_TaiKhoan FOREIGN KEY (TenDangNhap)
            REFERENCES dbo.TaiKhoan (TenDangNhap)
    );
    PRINT '  [+] dbo.PhienDangNhap';
END;
GO

-- Chi luu SHA-256, khong bao gio luu token goc. Rotate = danh dau da dung roi
-- cap token moi; dung lai token da dung bi coi la replay va thu hoi ca phien.
IF OBJECT_ID(N'dbo.TokenLamMoi', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TokenLamMoi (
        MaToken uniqueidentifier NOT NULL,
        MaPhien uniqueidentifier NOT NULL,
        TokenHash binary(32) NOT NULL,
        ThoiDiemTao datetime2(3) NOT NULL,
        ThoiDiemHetHan datetime2(3) NOT NULL,
        ThoiDiemDaDung datetime2(3) NULL,
        ThoiDiemThuHoi datetime2(3) NULL,
        CONSTRAINT PK_TokenLamMoi PRIMARY KEY NONCLUSTERED (MaToken),
        CONSTRAINT FK_TokenLamMoi_Phien FOREIGN KEY (MaPhien)
            REFERENCES dbo.PhienDangNhap (MaPhien)
    );
    PRINT '  [+] dbo.TokenLamMoi';
END;
GO

-- Ma kich hoat tai khoan (F02). FK toi TaiKhoan cuc bo, khong toi DanhBa replica.
IF OBJECT_ID(N'dbo.MaKichHoat', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MaKichHoat (
        MaKichHoat uniqueidentifier NOT NULL,
        TenDangNhap varchar(50) NOT NULL,
        MaHash binary(32) NOT NULL,
        ThoiDiemTao datetime2(3) NOT NULL,
        ThoiDiemHetHan datetime2(3) NOT NULL,
        ThoiDiemDaDung datetime2(3) NULL,
        -- Cap ma moi, hoac nhap sai qua so lan cho phep.
        ThoiDiemThuHoi datetime2(3) NULL,
        SoLanSai tinyint NOT NULL CONSTRAINT DF_MaKichHoat_SoLanSai DEFAULT (0),
        EmailNhan varchar(254) NULL,
        -- Ai cap ma nay; de truy vet thao tac khoi phuc cua quan tri.
        NguoiCap varchar(50) NULL,
        CONSTRAINT PK_MaKichHoat PRIMARY KEY NONCLUSTERED (MaKichHoat),
        CONSTRAINT FK_MaKichHoat_TaiKhoan FOREIGN KEY (TenDangNhap)
            REFERENCES dbo.TaiKhoan (TenDangNhap)
    );
    PRINT '  [+] dbo.MaKichHoat';
END;
GO

-- Ma 6 so xac minh email / khoi phuc mat khau (A1).
IF OBJECT_ID(N'dbo.MaXacThuc', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MaXacThuc (
        MaXacThuc uniqueidentifier NOT NULL,
        TenDangNhap varchar(50) NOT NULL,
        MucDich varchar(20) NOT NULL,
        -- Dia chi da gui ma toi; chi danh dau neu tai khoan van mang dia chi nay.
        Email varchar(254) NOT NULL,
        MaHash binary(32) NOT NULL,
        ThoiDiemTao datetime2(3) NOT NULL,
        ThoiDiemHetHan datetime2(3) NOT NULL,
        ThoiDiemDaDung datetime2(3) NULL,
        ThoiDiemThuHoi datetime2(3) NULL,
        SoLanSai tinyint NOT NULL CONSTRAINT DF_MaXacThuc_SoLanSai DEFAULT (0),
        CONSTRAINT PK_MaXacThuc PRIMARY KEY NONCLUSTERED (MaXacThuc),
        CONSTRAINT FK_MaXacThuc_TaiKhoan FOREIGN KEY (TenDangNhap)
            REFERENCES dbo.TaiKhoan (TenDangNhap)
    );
    PRINT '  [+] dbo.MaXacThuc';
END;
GO

-- Thong bao hoc vu. Hai loai: SOAN (quan tri go tay, co nhap) va TU_DONG
-- (sinh tu su kien nghiep vu, luon DA_GUI). Phat lai cung KhoaSuKien khong
-- tao ban thu hai.
IF OBJECT_ID(N'dbo.ThongBao', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ThongBao (
        MaThongBao uniqueidentifier NOT NULL,
        Loai varchar(20) NOT NULL,
        TrangThai varchar(20) NOT NULL,
        MucDo varchar(20) NOT NULL CONSTRAINT DF_ThongBao_MucDo DEFAULT ('THONG_THUONG'),
        TieuDe nvarchar(200) NOT NULL,
        NoiDung nvarchar(4000) NOT NULL,
        -- Duong dan NOI BO cua web, bat dau bang '/'.
        LienKet varchar(300) NULL,
        PhamVi varchar(20) NOT NULL,
        MaCoSo varchar(10) NULL,
        MaLopHP varchar(80) NULL,
        DoiTuong varchar(20) NOT NULL,
        -- NULL voi thong bao tu sinh.
        NguoiTao varchar(50) NULL,
        VaiTroNguoiTao varchar(20) NULL,
        SuKien varchar(30) NULL,
        KhoaSuKien varchar(200) NULL,
        NgayTao datetime2(0) NOT NULL,
        NgayGui datetime2(0) NULL,
        CONSTRAINT PK_ThongBao PRIMARY KEY NONCLUSTERED (MaThongBao),
        CONSTRAINT FK_ThongBao_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo),
        CONSTRAINT FK_ThongBao_LopHocPhan FOREIGN KEY (MaLopHP)
            REFERENCES dbo.LopHocPhan (MaLopHP)
    );
    PRINT '  [+] dbo.ThongBao';
END;
GO

-- Hop thu theo nguoi nhan: PK bat dau bang nguoi nhan nen doc hop thu chi quet
-- dung phan cua minh.
IF OBJECT_ID(N'dbo.ThongBaoNguoiNhan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ThongBaoNguoiNhan (
        LoaiNguoiNhan varchar(20) NOT NULL,
        MaNguoiNhan varchar(20) NOT NULL,
        MaThongBao uniqueidentifier NOT NULL,
        NgayDoc datetime2(0) NULL,
        CONSTRAINT PK_ThongBaoNguoiNhan PRIMARY KEY CLUSTERED
            (LoaiNguoiNhan, MaNguoiNhan, MaThongBao),
        CONSTRAINT FK_ThongBaoNguoiNhan_ThongBao FOREIGN KEY (MaThongBao)
            REFERENCES dbo.ThongBao (MaThongBao)
    );
    PRINT '  [+] dbo.ThongBaoNguoiNhan';
END;
GO

-- ---------------------------------------------------------------------
-- Bo sung cot cho database DA TON TAI.
--
-- Cac khoi CREATE TABLE o tren chi chay khi bang chua co, nen database
-- dung tu truoc se khong nhan duoc cot moi. Khoi nay bat dung truong hop
-- do. Database moi thi moi dieu kien duoi day deu sai va khoi nay khong
-- lam gi ca.
-- ---------------------------------------------------------------------
IF COL_LENGTH(N'dbo.SinhVien', N'GioiTinh') IS NULL
    ALTER TABLE dbo.SinhVien ADD
    GioiTinh varchar(3) NULL,
        DienThoai varchar(20) NULL,
        SoCCCD varchar(20) NULL,
        EmailCaNhan nvarchar(254) NULL,
        NoiSinh nvarchar(150) NULL,
        DanToc nvarchar(50) NULL,
        TonGiao nvarchar(50) NULL,
        HoKhau nvarchar(255) NULL,
        AnhDaiDien varchar(500) NULL;
GO

IF COL_LENGTH(N'dbo.GiangVien', N'GioiTinh') IS NULL
    ALTER TABLE dbo.GiangVien ADD
    GioiTinh varchar(3) NULL,
        DienThoai varchar(20) NULL,
        SoCCCD varchar(20) NULL,
        EmailCaNhan nvarchar(254) NULL,
        NoiSinh nvarchar(150) NULL,
        DanToc nvarchar(50) NULL,
        TonGiao nvarchar(50) NULL,
        HoKhau nvarchar(255) NULL,
        AnhDaiDien varchar(500) NULL;
GO

IF COL_LENGTH(N'dbo.TaiKhoan', N'Email') IS NULL
    ALTER TABLE dbo.TaiKhoan ADD
        Email varchar(254) NULL,
        EmailDaXacMinh bit NOT NULL
            CONSTRAINT DF_TaiKhoan_EmailDaXacMinh DEFAULT (0),
        ThoiDiemXacMinhEmail datetime2(3) NULL;
GO

-- Tai khoan cap xong nhung chua kich hoat thi chua co mat khau nao ca.
IF EXISTS (SELECT 1 FROM sys.columns
            WHERE object_id = OBJECT_ID(N'dbo.TaiKhoan')
              AND name = N'MatKhauHash' AND is_nullable = 0)
    ALTER TABLE dbo.TaiKhoan ALTER COLUMN MatKhauHash varchar(255) NULL;
GO

COMMIT TRANSACTION;
GO
PRINT '  [ok] Schema 21 bang da co. Chay tiep 11-rangbuoc.sql va 12-chimuc.sql.';
GO
