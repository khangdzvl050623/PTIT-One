/* PTIT One — V2__hoc_vu_schema.sql (CENTRAL, Phần 1)
   Schema học vụ: 7 bảng danh mục + 9 bảng vận hành, kèm CHECK/UNIQUE/chỉ mục.
   Hợp nhất db/master/01 + db/site/10..12 vào MỘT database. CoSo, DanhBaNguoiDung,
   TaiKhoan đã có ở V1 — không tạo lại.

   KHÔNG mang sang từ bản Phần 2: LichHocMirror, BangDiemMirror, OutboxSuKien,
   YeuCauHocLienCoSo, KetQuaXuLyYeuCau. Ở một DB, đăng ký liên cơ sở là một
   giao dịch cục bộ; mirror/outbox/saga chỉ xuất hiện khi tách site.

   Phần 2 KHÔNG chạy file này: site dựng bằng các script trong db/master và db/site.
   Vị trí từng bảng khi tách site:
     Master, nhân bản:   Khoa, ChuongTrinhDaoTao, MonHoc, CTDT_MonHoc,
                         MonHocTienQuyet, HocKy, KhungGioTiet
     Cục bộ tại site:    SinhVien, GiangVien, DotDangKy, LopHocPhan, LichHoc,
                         DangKyHocPhan, Diem (Host) · SinhVienHocKy, DangKyMonHoc (Home)

   ⚠️ FK CHỈ CÓ Ở PHẦN 1 (đánh dấu [P1] bên dưới): ở Phần 2 Host giữ cả sinh
   viên khách, còn DangKyMonHoc ở Home trỏ tới lớp ở site khác. Code nghiệp vụ
   KHÔNG được dựa vào lỗi FK để ra quyết định — tự kiểm tường minh, nếu không
   sang Phần 2 sẽ im lặng mất kiểm tra.

   Bộ đếm (SoLuongDaDangKy, SoTinChiDaDangKy, SoTinChiDangGiuCho) do ứng dụng
   sở hữu: UPDATE ... WHERE <điều kiện còn chỗ> rồi kiểm @@ROWCOUNT. Không
   trigger nào cập nhật chúng. Không IDENTITY, không MERGE, không cascade. */
IF DB_NAME() NOT LIKE N'PTITONE[_]CENTRAL%'
    THROW 51000, N'V2__hoc_vu_schema chi chay tren PTITONE_CENTRAL*.', 1;
GO

/* ===================== DANH MỤC (Master ở Phần 2) ===================== */

CREATE TABLE dbo.Khoa (
    MaKhoa  varchar(10)   NOT NULL,
    TenKhoa nvarchar(150) NOT NULL,
    CONSTRAINT PK_Khoa PRIMARY KEY CLUSTERED (MaKhoa)
);
GO

CREATE TABLE dbo.ChuongTrinhDaoTao (
    MaCTDT     varchar(20)   NOT NULL,
    TenCTDT    nvarchar(200) NOT NULL,
    MaKhoa     varchar(10)   NOT NULL,
    TongTinChi smallint      NOT NULL,
    CONSTRAINT PK_ChuongTrinhDaoTao PRIMARY KEY CLUSTERED (MaCTDT),
    CONSTRAINT FK_ChuongTrinhDaoTao_Khoa FOREIGN KEY (MaKhoa) REFERENCES dbo.Khoa (MaKhoa),
    CONSTRAINT CK_ChuongTrinhDaoTao_TongTinChi CHECK (TongTinChi > 0)
);
GO

CREATE TABLE dbo.MonHoc (
    MaMonHoc  varchar(20)   NOT NULL,
    TenMonHoc nvarchar(200) NOT NULL,
    SoTinChi  tinyint       NOT NULL,
    MaKhoa    varchar(10)   NOT NULL,
    CONSTRAINT PK_MonHoc PRIMARY KEY CLUSTERED (MaMonHoc),
    CONSTRAINT FK_MonHoc_Khoa FOREIGN KEY (MaKhoa) REFERENCES dbo.Khoa (MaKhoa),
    CONSTRAINT CK_MonHoc_SoTinChi CHECK (SoTinChi BETWEEN 1 AND 15)
);
GO

CREATE TABLE dbo.CTDT_MonHoc (
    MaCTDT    varchar(20) NOT NULL,
    MaMonHoc  varchar(20) NOT NULL,
    -- Học kỳ thứ mấy trong lộ trình.
    HocKyGoiY tinyint     NULL,
    BatBuoc   bit         NOT NULL CONSTRAINT DF_CTDT_MonHoc_BatBuoc DEFAULT (1),
    CONSTRAINT PK_CTDT_MonHoc PRIMARY KEY CLUSTERED (MaCTDT, MaMonHoc),
    CONSTRAINT FK_CTDT_MonHoc_CTDT FOREIGN KEY (MaCTDT) REFERENCES dbo.ChuongTrinhDaoTao (MaCTDT),
    CONSTRAINT FK_CTDT_MonHoc_MonHoc FOREIGN KEY (MaMonHoc) REFERENCES dbo.MonHoc (MaMonHoc)
);
GO

/* Tự quan hệ N:M. Chỉ chặn tự trỏ chính mình; chu trình dài hơn (A→B→A)
   do API danh mục kiểm lúc ghi — CHECK không nhìn được sang dòng khác. */
CREATE TABLE dbo.MonHocTienQuyet (
    MaMonHoc       varchar(20) NOT NULL,
    MaMonTienQuyet varchar(20) NOT NULL,
    CONSTRAINT PK_MonHocTienQuyet PRIMARY KEY CLUSTERED (MaMonHoc, MaMonTienQuyet),
    CONSTRAINT FK_MonHocTienQuyet_Mon FOREIGN KEY (MaMonHoc) REFERENCES dbo.MonHoc (MaMonHoc),
    CONSTRAINT FK_MonHocTienQuyet_MonTQ FOREIGN KEY (MaMonTienQuyet) REFERENCES dbo.MonHoc (MaMonHoc),
    CONSTRAINT CK_MonHocTienQuyet_KhongTuTro CHECK (MaMonHoc <> MaMonTienQuyet)
);
GO

/* Lịch chung toàn trường. NgayBatDau là mốc suy ra số tuần trong LichHoc.
   Đợt đăng ký của từng cơ sở KHÔNG ở đây mà ở DotDangKy. */
CREATE TABLE dbo.HocKy (
    MaHocKy     varchar(20)   NOT NULL,
    TenHocKy    nvarchar(100) NOT NULL,
    -- Ví dụ '2026-2027'.
    NamHoc      varchar(9)    NOT NULL,
    NgayBatDau  date          NOT NULL,
    NgayKetThuc date          NOT NULL,
    CONSTRAINT PK_HocKy PRIMARY KEY CLUSTERED (MaHocKy),
    CONSTRAINT CK_HocKy_KhoangNgay CHECK (NgayKetThuc > NgayBatDau)
);
GO

/* Dùng chung mọi cơ sở — nếu không, so trùng lịch giữa hai cơ sở vô nghĩa. */
CREATE TABLE dbo.KhungGioTiet (
    SoTiet     tinyint     NOT NULL,
    GioBatDau  time(0)     NOT NULL,
    GioKetThuc time(0)     NOT NULL,
    Buoi       varchar(10) NOT NULL,
    CONSTRAINT PK_KhungGioTiet PRIMARY KEY CLUSTERED (SoTiet),
    CONSTRAINT CK_KhungGioTiet_Gio CHECK (GioKetThuc > GioBatDau),
    CONSTRAINT CK_KhungGioTiet_Buoi CHECK (Buoi IN ('SANG', 'CHIEU', 'TOI'))
);
GO

/* ===================== HỒ SƠ (cục bộ tại site ở Phần 2) ===================== */

/* MaSinhVien không mang mã cơ sở (C9): sinh viên chuyển cơ sở giữ nguyên mã. */
CREATE TABLE dbo.SinhVien (
    MaSinhVien      varchar(20)   NOT NULL,
    HoTen           nvarchar(150) NOT NULL,
    NgaySinh        date          NULL,
    MaCoSoNha       varchar(10)   NOT NULL,
    MaCTDT          varchar(20)   NOT NULL,
    TrangThai       varchar(20)   NOT NULL CONSTRAINT DF_SinhVien_TrangThai DEFAULT ('DANG_HOC'),
    SoTinChiTichLuy smallint      NOT NULL CONSTRAINT DF_SinhVien_TinChi DEFAULT (0),
    SoMonLienCoSo   smallint      NOT NULL CONSTRAINT DF_SinhVien_LienCoSo DEFAULT (0),
    CONSTRAINT PK_SinhVien PRIMARY KEY CLUSTERED (MaSinhVien),
    CONSTRAINT FK_SinhVien_CoSo FOREIGN KEY (MaCoSoNha) REFERENCES dbo.CoSo (MaCoSo),
    CONSTRAINT FK_SinhVien_CTDT FOREIGN KEY (MaCTDT) REFERENCES dbo.ChuongTrinhDaoTao (MaCTDT),
    CONSTRAINT CK_SinhVien_TrangThai CHECK (TrangThai IN ('DANG_HOC', 'BAO_LUU', 'THOI_HOC', 'TOT_NGHIEP')),
    CONSTRAINT CK_SinhVien_BoDem CHECK (SoTinChiTichLuy >= 0 AND SoMonLienCoSo >= 0)
);
GO

CREATE TABLE dbo.GiangVien (
    MaGiangVien varchar(20)   NOT NULL,
    HoTen       nvarchar(150) NOT NULL,
    MaCoSo      varchar(10)   NOT NULL,
    MaKhoa      varchar(10)   NOT NULL,
    HocVi       nvarchar(50)  NULL,
    CONSTRAINT PK_GiangVien PRIMARY KEY CLUSTERED (MaGiangVien),
    CONSTRAINT FK_GiangVien_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo),
    CONSTRAINT FK_GiangVien_Khoa FOREIGN KEY (MaKhoa) REFERENCES dbo.Khoa (MaKhoa)
);
GO

/* Đồng bộ TaiKhoan (V1) với site/11: SV/GV phải gắn thực thể, admin cơ sở thì
   không; một thực thể tối đa một tài khoản. MaThucThe đa hình SV/GV nên không FK. */
ALTER TABLE dbo.TaiKhoan WITH CHECK ADD CONSTRAINT CK_TaiKhoan_ThucThe
    CHECK ((VaiTro = 'ADMIN_CO_SO' AND MaThucThe IS NULL)
        OR (VaiTro IN ('SINH_VIEN', 'GIANG_VIEN') AND MaThucThe IS NOT NULL));
GO
CREATE UNIQUE NONCLUSTERED INDEX UQ_TaiKhoan_MaThucThe
    ON dbo.TaiKhoan (MaThucThe) WHERE MaThucThe IS NOT NULL;
GO

/* ===================== VẬN HÀNH (cục bộ tại site ở Phần 2) ===================== */

/* MaDot = <MaCoSo>-<MaHocKy>-<STT> (C9). */
CREATE TABLE dbo.DotDangKy (
    MaDot        varchar(50)  NOT NULL,
    MaHocKy      varchar(20)  NOT NULL,
    MaCoSo       varchar(10)  NOT NULL,
    ThoiGianMo   datetime2(0) NOT NULL,
    ThoiGianDong datetime2(0) NOT NULL,
    TrangThai    varchar(20)  NOT NULL CONSTRAINT DF_DotDangKy_TrangThai DEFAULT ('CHUA_MO'),
    CONSTRAINT PK_DotDangKy PRIMARY KEY CLUSTERED (MaDot),
    CONSTRAINT FK_DotDangKy_HocKy FOREIGN KEY (MaHocKy) REFERENCES dbo.HocKy (MaHocKy),
    CONSTRAINT FK_DotDangKy_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo),
    CONSTRAINT CK_DotDangKy_KhoangThoiGian CHECK (ThoiGianDong > ThoiGianMo),
    CONSTRAINT CK_DotDangKy_TrangThai CHECK (TrangThai IN ('CHUA_MO', 'DANG_MO', 'DA_DONG'))
);
GO

/* MaLopHP = <MaMonHoc>-<MaHocKy>-<MaCoSo><STT> (C9): lớp thuộc về cơ sở Host.
   DA_KHOA = đã khóa điểm (trigger T3 ở Phần 2 chặn sửa Diem theo trạng thái này).
   Liên cơ sở chỉ cho lớp TRUC_TUYEN (v1). */
CREATE TABLE dbo.LopHocPhan (
    MaLopHP         varchar(80) NOT NULL,
    MaMonHoc        varchar(20) NOT NULL,
    MaHocKy         varchar(20) NOT NULL,
    MaCoSoHost      varchar(10) NOT NULL,
    MaGiangVien     varchar(20) NULL,
    SoLuongToiDa    smallint    NOT NULL,
    SoLuongDaDangKy smallint    NOT NULL CONSTRAINT DF_LopHocPhan_SiSo DEFAULT (0),
    TrangThai       varchar(20) NOT NULL CONSTRAINT DF_LopHocPhan_TrangThai DEFAULT ('DU_KIEN'),
    ChoPhepLienCoSo bit         NOT NULL CONSTRAINT DF_LopHocPhan_LienCoSo DEFAULT (0),
    HinhThucHoc     varchar(20) NOT NULL,
    -- Tăng mỗi lần đổi lịch; DangKyMonHoc giữ bản đã thấy lúc đăng ký.
    PhienBanLich    int         NOT NULL CONSTRAINT DF_LopHocPhan_PhienBanLich DEFAULT (1),
    CONSTRAINT PK_LopHocPhan PRIMARY KEY CLUSTERED (MaLopHP),
    CONSTRAINT FK_LopHocPhan_MonHoc FOREIGN KEY (MaMonHoc) REFERENCES dbo.MonHoc (MaMonHoc),
    CONSTRAINT FK_LopHocPhan_HocKy FOREIGN KEY (MaHocKy) REFERENCES dbo.HocKy (MaHocKy),
    CONSTRAINT FK_LopHocPhan_CoSo FOREIGN KEY (MaCoSoHost) REFERENCES dbo.CoSo (MaCoSo),
    CONSTRAINT FK_LopHocPhan_GiangVien FOREIGN KEY (MaGiangVien) REFERENCES dbo.GiangVien (MaGiangVien),
    CONSTRAINT CK_LopHocPhan_SucChua
        CHECK (SoLuongToiDa > 0 AND SoLuongDaDangKy >= 0 AND SoLuongDaDangKy <= SoLuongToiDa),
    CONSTRAINT CK_LopHocPhan_TrangThai CHECK (TrangThai IN ('DU_KIEN', 'MO', 'DA_KHOA', 'DA_HUY')),
    CONSTRAINT CK_LopHocPhan_HinhThucHoc CHECK (HinhThucHoc IN ('TRUC_TIEP', 'TRUC_TUYEN', 'KET_HOP')),
    CONSTRAINT CK_LopHocPhan_LienCoSo CHECK (ChoPhepLienCoSo = 0 OR HinhThucHoc = 'TRUC_TUYEN'),
    CONSTRAINT CK_LopHocPhan_PhienBanLich CHECK (PhienBanLich > 0)
);
GO

/* Thu: 2 = thứ Hai … 8 = Chủ nhật. Tuần tính từ HocKy.NgayBatDau. */
CREATE TABLE dbo.LichHoc (
    MaLopHP     varchar(80)   NOT NULL,
    Thu         tinyint       NOT NULL,
    TietBatDau  tinyint       NOT NULL,
    SoTiet      tinyint       NOT NULL,
    PhongHoc    nvarchar(100) NULL,
    TuanBatDau  smallint      NOT NULL,
    TuanKetThuc smallint      NOT NULL,
    CONSTRAINT PK_LichHoc PRIMARY KEY CLUSTERED (MaLopHP, Thu, TietBatDau),
    CONSTRAINT FK_LichHoc_LopHocPhan FOREIGN KEY (MaLopHP) REFERENCES dbo.LopHocPhan (MaLopHP),
    CONSTRAINT FK_LichHoc_KhungGioTiet FOREIGN KEY (TietBatDau) REFERENCES dbo.KhungGioTiet (SoTiet),
    CONSTRAINT CK_LichHoc_KhoangLich
        CHECK (Thu BETWEEN 2 AND 8 AND TietBatDau >= 1 AND SoTiet > 0
               AND TuanBatDau >= 1 AND TuanKetThuc >= TuanBatDau)
);
GO

/* Ghi danh phía LỚP (Host). Khóa kép, không khóa thay thế (C9).
   HoTenSinhVien/MaCoSoNhaSV: Phần 2 Host không có hồ sơ SV khách nên giữ bản sao. */
CREATE TABLE dbo.DangKyHocPhan (
    MaLopHP       varchar(80)      NOT NULL,
    MaSinhVien    varchar(20)      NOT NULL,
    MaCoSoNhaSV   varchar(10)      NOT NULL,
    HoTenSinhVien nvarchar(150)    NOT NULL,
    NgayDangKy    datetime2(0)     NOT NULL CONSTRAINT DF_DangKyHocPhan_Ngay DEFAULT (SYSUTCDATETIME()),
    TrangThai     varchar(20)      NOT NULL CONSTRAINT DF_DangKyHocPhan_TrangThai DEFAULT ('DA_DANG_KY'),
    MaYeuCau      uniqueidentifier NULL,
    CONSTRAINT PK_DangKyHocPhan PRIMARY KEY CLUSTERED (MaLopHP, MaSinhVien),
    CONSTRAINT FK_DangKyHocPhan_LopHocPhan FOREIGN KEY (MaLopHP) REFERENCES dbo.LopHocPhan (MaLopHP),
    CONSTRAINT FK_DangKyHocPhan_CoSo FOREIGN KEY (MaCoSoNhaSV) REFERENCES dbo.CoSo (MaCoSo),
    -- [P1] Phần 2 KHÔNG có: Host giữ cả sinh viên khách.
    CONSTRAINT FK_DangKyHocPhan_SinhVien FOREIGN KEY (MaSinhVien) REFERENCES dbo.SinhVien (MaSinhVien),
    CONSTRAINT CK_DangKyHocPhan_TrangThai
        CHECK (TrangThai IN ('DANG_XU_LY', 'DA_DANG_KY', 'TU_CHOI', 'DANG_HUY', 'DA_HUY'))
);
GO
CREATE UNIQUE NONCLUSTERED INDEX UQ_DangKyHocPhan_MaYeuCau
    ON dbo.DangKyHocPhan (MaYeuCau) WHERE MaYeuCau IS NOT NULL;
GO
-- Bảng điểm/đăng ký theo sinh viên: PK bắt đầu bằng MaLopHP nên cần chiều ngược.
CREATE NONCLUSTERED INDEX IX_DangKyHocPhan_SinhVien
    ON dbo.DangKyHocPhan (MaSinhVien, MaLopHP) INCLUDE (TrangThai);
GO

/* Dẫn xuất bậc 2 qua DangKyHocPhan. Version = số thứ tự sự kiện, không phải
   rowversion. Công thức DiemTongKet và ngưỡng đạt chưa chốt — không mã hóa ở đây. */
CREATE TABLE dbo.Diem (
    MaLopHP       varchar(80)  NOT NULL,
    MaSinhVien    varchar(20)  NOT NULL,
    DiemChuyenCan decimal(3,1) NULL,
    DiemGiuaKy    decimal(3,1) NULL,
    DiemCuoiKy    decimal(3,1) NULL,
    DiemTongKet   decimal(3,1) NULL,
    Version       bigint       NOT NULL CONSTRAINT DF_Diem_Version DEFAULT (1),
    NgayCongBo    datetime2(0) NULL,
    CONSTRAINT PK_Diem PRIMARY KEY CLUSTERED (MaLopHP, MaSinhVien),
    CONSTRAINT FK_Diem_DangKyHocPhan FOREIGN KEY (MaLopHP, MaSinhVien)
        REFERENCES dbo.DangKyHocPhan (MaLopHP, MaSinhVien),
    CONSTRAINT CK_Diem_ThangDiem
        CHECK ((DiemChuyenCan IS NULL OR DiemChuyenCan BETWEEN 0 AND 10)
           AND (DiemGiuaKy    IS NULL OR DiemGiuaKy    BETWEEN 0 AND 10)
           AND (DiemCuoiKy    IS NULL OR DiemCuoiKy    BETWEEN 0 AND 10)
           AND (DiemTongKet   IS NULL OR DiemTongKet   BETWEEN 0 AND 10)),
    CONSTRAINT CK_Diem_Version CHECK (Version > 0)
);
GO

/* Trần tín chỉ theo (SV, học kỳ) ở Home. Cộng SoTinChiDangGiuCho ngay khi tạo
   yêu cầu DANG_XU_LY; chỉ trả khi có kết quả dứt khoát. TranTinChi truyền rõ
   khi tạo dòng — 24 chỉ là giả định, không đóng cứng thành DEFAULT. */
CREATE TABLE dbo.SinhVienHocKy (
    MaSinhVien         varchar(20) NOT NULL,
    MaHocKy            varchar(20) NOT NULL,
    SoTinChiDaDangKy   smallint    NOT NULL CONSTRAINT DF_SinhVienHocKy_DaDangKy DEFAULT (0),
    SoTinChiDangGiuCho smallint    NOT NULL CONSTRAINT DF_SinhVienHocKy_GiuCho DEFAULT (0),
    TranTinChi         smallint    NOT NULL,
    CONSTRAINT PK_SinhVienHocKy PRIMARY KEY CLUSTERED (MaSinhVien, MaHocKy),
    CONSTRAINT FK_SinhVienHocKy_SinhVien FOREIGN KEY (MaSinhVien) REFERENCES dbo.SinhVien (MaSinhVien),
    CONSTRAINT FK_SinhVienHocKy_HocKy FOREIGN KEY (MaHocKy) REFERENCES dbo.HocKy (MaHocKy),
    CONSTRAINT CK_SinhVienHocKy_TranTinChi
        CHECK (TranTinChi > 0 AND SoTinChiDaDangKy >= 0 AND SoTinChiDangGiuCho >= 0
               AND SoTinChiDaDangKy + SoTinChiDangGiuCho <= TranTinChi)
);
GO

/* Ghi danh phía SINH VIÊN (Home), một dòng cho mỗi (SV, kỳ, môn).
   SoTinChi/PhienBanLich là bản đã thấy lúc đăng ký. */
CREATE TABLE dbo.DangKyMonHoc (
    MaSinhVien   varchar(20)      NOT NULL,
    MaHocKy      varchar(20)      NOT NULL,
    MaMonHoc     varchar(20)      NOT NULL,
    MaLopHP      varchar(80)      NOT NULL,
    MaCoSoHost   varchar(10)      NOT NULL,
    TrangThai    varchar(20)      NOT NULL,
    MaYeuCau     uniqueidentifier NULL,
    SoTinChi     tinyint          NOT NULL,
    PhienBanLich int              NOT NULL,
    CONSTRAINT PK_DangKyMonHoc PRIMARY KEY CLUSTERED (MaSinhVien, MaHocKy, MaMonHoc),
    CONSTRAINT FK_DangKyMonHoc_SinhVienHocKy FOREIGN KEY (MaSinhVien, MaHocKy)
        REFERENCES dbo.SinhVienHocKy (MaSinhVien, MaHocKy),
    CONSTRAINT FK_DangKyMonHoc_MonHoc FOREIGN KEY (MaMonHoc) REFERENCES dbo.MonHoc (MaMonHoc),
    CONSTRAINT FK_DangKyMonHoc_CoSo FOREIGN KEY (MaCoSoHost) REFERENCES dbo.CoSo (MaCoSo),
    -- [P1] Phần 2 KHÔNG có: lớp có thể nằm ở site khác.
    CONSTRAINT FK_DangKyMonHoc_LopHocPhan FOREIGN KEY (MaLopHP) REFERENCES dbo.LopHocPhan (MaLopHP),
    CONSTRAINT CK_DangKyMonHoc_TrangThai
        CHECK (TrangThai IN ('DANG_XU_LY', 'DA_DANG_KY', 'TU_CHOI', 'DANG_HUY', 'DA_HUY')),
    CONSTRAINT CK_DangKyMonHoc_TinChi_PhienBan CHECK (SoTinChi BETWEEN 1 AND 15 AND PhienBanLich > 0)
);
GO
/* Chống trùng môn (I6). Tên trạng thái phải khớp CK ở trên từng ký tự — ghi
   sai tên là ràng buộc im lặng không áp dụng. PK đã duy nhất cả dòng kết thúc;
   index này giữ đúng bản Phần 2 để code chống trùng mang sang không đổi. */
CREATE UNIQUE NONCLUSTERED INDEX UQ_DangKyMonHoc_SV_Ky_Mon
    ON dbo.DangKyMonHoc (MaSinhVien, MaHocKy, MaMonHoc)
    WHERE TrangThai IN ('DANG_XU_LY', 'DA_DANG_KY', 'DANG_HUY');
GO

/* Tra danh mục lớp theo học kỳ + môn (J3). Chưa benchmark, không tuyên bố tối ưu. */
CREATE NONCLUSTERED INDEX IX_LopHocPhan_HocKy_MonHoc
    ON dbo.LopHocPhan (MaHocKy, MaMonHoc)
    INCLUDE (TrangThai, SoLuongDaDangKy, SoLuongToiDa, HinhThucHoc, ChoPhepLienCoSo);
GO
