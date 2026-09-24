/* PTIT One — CHECK va UNIQUE theo C1/D4/I6.
   -On HCM/HN/DN, sau 10. Chay lai kiem tra va bat lai CHECK voi WITH CHECK.
   Khong tu dat enum cho trang thai lop/dot/SV/SyncStatus chua chot trong specs.
   PK DangKyHocPhan da bao dam duy nhat cap lop/SV.
   PK DangKyMonHoc da duy nhat ca dong ket thuc; giu them filtered index theo I6. */
:ON ERROR EXIT
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
IF N'$(SiteTarget)' NOT IN (N'HCM', N'HN', N'DN')
   OR DB_NAME() <> N'$(DbTarget)' OR DB_NAME() = N'$(DbMaster)'
    THROW 51100, N'Chi chay tren dung Subscriber (-On HCM/HN/DN).', 1;
IF EXISTS (SELECT 1 FROM (VALUES (N'SinhVien'), (N'GiangVien'), (N'TaiKhoan'), (N'DotDangKy'), (N'LopHocPhan'), (N'LichHoc'), (N'DangKyHocPhan'), (N'Diem'), (N'SinhVienHocKy'), (N'DangKyMonHoc'), (N'LichHocMirror'), (N'YeuCauHocLienCoSo'), (N'KetQuaXuLyYeuCau'), (N'BangDiemMirror'), (N'OutboxSuKien')
    ) r(TableName) WHERE OBJECT_ID(N'dbo.' + r.TableName, N'U') IS NULL)
    THROW 51102, N'Chay 10-schema-vanhanh.sql truoc.', 1;
GO

BEGIN TRANSACTION;
GO
IF OBJECT_ID(N'dbo.CK_SinhVien_BoDem', N'C') IS NULL
    ALTER TABLE dbo.SinhVien WITH CHECK ADD CONSTRAINT CK_SinhVien_BoDem
        CHECK (SoTinChiTichLuy >= 0 AND SoMonLienCoSo >= 0);
ALTER TABLE dbo.SinhVien WITH CHECK CHECK CONSTRAINT CK_SinhVien_BoDem;
GO

IF OBJECT_ID(N'dbo.CK_TaiKhoan_VaiTro', N'C') IS NULL
    ALTER TABLE dbo.TaiKhoan WITH CHECK ADD CONSTRAINT CK_TaiKhoan_VaiTro
        CHECK (VaiTro IN ('SINH_VIEN', 'GIANG_VIEN', 'ADMIN_CO_SO'));
ALTER TABLE dbo.TaiKhoan WITH CHECK CHECK CONSTRAINT CK_TaiKhoan_VaiTro;
GO

IF OBJECT_ID(N'dbo.CK_TaiKhoan_ThucThe', N'C') IS NULL
    ALTER TABLE dbo.TaiKhoan WITH CHECK ADD CONSTRAINT CK_TaiKhoan_ThucThe
        CHECK ((VaiTro = 'ADMIN_CO_SO' AND MaThucThe IS NULL) OR (VaiTro IN ('SINH_VIEN', 'GIANG_VIEN') AND MaThucThe IS NOT NULL));
ALTER TABLE dbo.TaiKhoan WITH CHECK CHECK CONSTRAINT CK_TaiKhoan_ThucThe;
GO

IF OBJECT_ID(N'dbo.CK_DotDangKy_KhoangThoiGian', N'C') IS NULL
    ALTER TABLE dbo.DotDangKy WITH CHECK ADD CONSTRAINT CK_DotDangKy_KhoangThoiGian
        CHECK (ThoiGianDong > ThoiGianMo);
ALTER TABLE dbo.DotDangKy WITH CHECK CHECK CONSTRAINT CK_DotDangKy_KhoangThoiGian;
GO

IF OBJECT_ID(N'dbo.CK_LopHocPhan_SucChua', N'C') IS NULL
    ALTER TABLE dbo.LopHocPhan WITH CHECK ADD CONSTRAINT CK_LopHocPhan_SucChua
        CHECK (SoLuongToiDa > 0 AND SoLuongDaDangKy >= 0 AND SoLuongDaDangKy <= SoLuongToiDa);
ALTER TABLE dbo.LopHocPhan WITH CHECK CHECK CONSTRAINT CK_LopHocPhan_SucChua;
GO

IF OBJECT_ID(N'dbo.CK_LopHocPhan_HinhThucHoc', N'C') IS NULL
    ALTER TABLE dbo.LopHocPhan WITH CHECK ADD CONSTRAINT CK_LopHocPhan_HinhThucHoc
        CHECK (HinhThucHoc IN ('TRUC_TIEP', 'TRUC_TUYEN', 'KET_HOP'));
ALTER TABLE dbo.LopHocPhan WITH CHECK CHECK CONSTRAINT CK_LopHocPhan_HinhThucHoc;
GO

IF OBJECT_ID(N'dbo.CK_LopHocPhan_LienCoSo', N'C') IS NULL
    ALTER TABLE dbo.LopHocPhan WITH CHECK ADD CONSTRAINT CK_LopHocPhan_LienCoSo
        CHECK (ChoPhepLienCoSo = 0 OR HinhThucHoc = 'TRUC_TUYEN');
ALTER TABLE dbo.LopHocPhan WITH CHECK CHECK CONSTRAINT CK_LopHocPhan_LienCoSo;
GO

IF OBJECT_ID(N'dbo.CK_LopHocPhan_PhienBanLich', N'C') IS NULL
    ALTER TABLE dbo.LopHocPhan WITH CHECK ADD CONSTRAINT CK_LopHocPhan_PhienBanLich
        CHECK (PhienBanLich > 0);
ALTER TABLE dbo.LopHocPhan WITH CHECK CHECK CONSTRAINT CK_LopHocPhan_PhienBanLich;
GO

IF OBJECT_ID(N'dbo.CK_LichHoc_KhoangLich', N'C') IS NULL
    ALTER TABLE dbo.LichHoc WITH CHECK ADD CONSTRAINT CK_LichHoc_KhoangLich
        CHECK (Thu BETWEEN 2 AND 8 AND TietBatDau >= 1 AND SoTiet > 0 AND TuanBatDau >= 1 AND TuanKetThuc >= TuanBatDau);
ALTER TABLE dbo.LichHoc WITH CHECK CHECK CONSTRAINT CK_LichHoc_KhoangLich;
GO

IF OBJECT_ID(N'dbo.CK_LichHocMirror_KhoangLich', N'C') IS NULL
    ALTER TABLE dbo.LichHocMirror WITH CHECK ADD CONSTRAINT CK_LichHocMirror_KhoangLich
        CHECK (Thu BETWEEN 2 AND 8 AND TietBatDau >= 1 AND SoTiet > 0 AND TuanBatDau >= 1 AND TuanKetThuc >= TuanBatDau);
ALTER TABLE dbo.LichHocMirror WITH CHECK CHECK CONSTRAINT CK_LichHocMirror_KhoangLich;
GO

IF OBJECT_ID(N'dbo.CK_DangKyHocPhan_TrangThai', N'C') IS NULL
    ALTER TABLE dbo.DangKyHocPhan WITH CHECK ADD CONSTRAINT CK_DangKyHocPhan_TrangThai
        CHECK (TrangThai IN ('DANG_XU_LY', 'DA_DANG_KY', 'TU_CHOI', 'DANG_HUY', 'DA_HUY'));
ALTER TABLE dbo.DangKyHocPhan WITH CHECK CHECK CONSTRAINT CK_DangKyHocPhan_TrangThai;
GO

IF OBJECT_ID(N'dbo.CK_DangKyMonHoc_TrangThai', N'C') IS NULL
    ALTER TABLE dbo.DangKyMonHoc WITH CHECK ADD CONSTRAINT CK_DangKyMonHoc_TrangThai
        CHECK (TrangThai IN ('DANG_XU_LY', 'DA_DANG_KY', 'TU_CHOI', 'DANG_HUY', 'DA_HUY'));
ALTER TABLE dbo.DangKyMonHoc WITH CHECK CHECK CONSTRAINT CK_DangKyMonHoc_TrangThai;
GO

IF OBJECT_ID(N'dbo.CK_YeuCauHocLienCoSo_TrangThai', N'C') IS NULL
    ALTER TABLE dbo.YeuCauHocLienCoSo WITH CHECK ADD CONSTRAINT CK_YeuCauHocLienCoSo_TrangThai
        CHECK (TrangThai IN ('DANG_XU_LY', 'DA_DANG_KY', 'TU_CHOI', 'DANG_HUY', 'DA_HUY'));
ALTER TABLE dbo.YeuCauHocLienCoSo WITH CHECK CHECK CONSTRAINT CK_YeuCauHocLienCoSo_TrangThai;
GO

IF OBJECT_ID(N'dbo.CK_Diem_ThangDiem', N'C') IS NULL
    ALTER TABLE dbo.Diem WITH CHECK ADD CONSTRAINT CK_Diem_ThangDiem
        CHECK ((DiemChuyenCan IS NULL OR DiemChuyenCan BETWEEN 0 AND 10) AND (DiemGiuaKy IS NULL OR DiemGiuaKy BETWEEN 0 AND 10) AND (DiemCuoiKy IS NULL OR DiemCuoiKy BETWEEN 0 AND 10) AND (DiemTongKet IS NULL OR DiemTongKet BETWEEN 0 AND 10));
ALTER TABLE dbo.Diem WITH CHECK CHECK CONSTRAINT CK_Diem_ThangDiem;
GO

IF OBJECT_ID(N'dbo.CK_Diem_Version', N'C') IS NULL
    ALTER TABLE dbo.Diem WITH CHECK ADD CONSTRAINT CK_Diem_Version
        CHECK (Version > 0);
ALTER TABLE dbo.Diem WITH CHECK CHECK CONSTRAINT CK_Diem_Version;
GO

IF OBJECT_ID(N'dbo.CK_SinhVienHocKy_TranTinChi', N'C') IS NULL
    ALTER TABLE dbo.SinhVienHocKy WITH CHECK ADD CONSTRAINT CK_SinhVienHocKy_TranTinChi
        CHECK (TranTinChi > 0 AND SoTinChiDaDangKy >= 0 AND SoTinChiDangGiuCho >= 0 AND SoTinChiDaDangKy + SoTinChiDangGiuCho <= TranTinChi);
ALTER TABLE dbo.SinhVienHocKy WITH CHECK CHECK CONSTRAINT CK_SinhVienHocKy_TranTinChi;
GO

IF OBJECT_ID(N'dbo.CK_DangKyMonHoc_TinChi_PhienBan', N'C') IS NULL
    ALTER TABLE dbo.DangKyMonHoc WITH CHECK ADD CONSTRAINT CK_DangKyMonHoc_TinChi_PhienBan
        CHECK (SoTinChi BETWEEN 1 AND 15 AND PhienBanLich > 0);
ALTER TABLE dbo.DangKyMonHoc WITH CHECK CHECK CONSTRAINT CK_DangKyMonHoc_TinChi_PhienBan;
GO

IF OBJECT_ID(N'dbo.CK_LichHocMirror_PhienBanLich', N'C') IS NULL
    ALTER TABLE dbo.LichHocMirror WITH CHECK ADD CONSTRAINT CK_LichHocMirror_PhienBanLich
        CHECK (PhienBanLich > 0);
ALTER TABLE dbo.LichHocMirror WITH CHECK CHECK CONSTRAINT CK_LichHocMirror_PhienBanLich;
GO

IF OBJECT_ID(N'dbo.CK_YeuCauHocLienCoSo_Snapshot', N'C') IS NULL
    ALTER TABLE dbo.YeuCauHocLienCoSo WITH CHECK ADD CONSTRAINT CK_YeuCauHocLienCoSo_Snapshot
        CHECK (SoTinChi BETWEEN 1 AND 15 AND HinhThucHoc = 'TRUC_TUYEN' AND ISJSON(LichHocJson) = 1);
ALTER TABLE dbo.YeuCauHocLienCoSo WITH CHECK CHECK CONSTRAINT CK_YeuCauHocLienCoSo_Snapshot;
GO

IF OBJECT_ID(N'dbo.CK_YeuCauHocLienCoSo_SoLanThu', N'C') IS NULL
    ALTER TABLE dbo.YeuCauHocLienCoSo WITH CHECK ADD CONSTRAINT CK_YeuCauHocLienCoSo_SoLanThu
        CHECK (SoLanThu >= 0);
ALTER TABLE dbo.YeuCauHocLienCoSo WITH CHECK CHECK CONSTRAINT CK_YeuCauHocLienCoSo_SoLanThu;
GO

IF OBJECT_ID(N'dbo.CK_KetQuaXuLyYeuCau_KetQua', N'C') IS NULL
    ALTER TABLE dbo.KetQuaXuLyYeuCau WITH CHECK ADD CONSTRAINT CK_KetQuaXuLyYeuCau_KetQua
        CHECK (KetQua IN ('DA_DANG_KY', 'TU_CHOI', 'DA_HUY'));
ALTER TABLE dbo.KetQuaXuLyYeuCau WITH CHECK CHECK CONSTRAINT CK_KetQuaXuLyYeuCau_KetQua;
GO

IF OBJECT_ID(N'dbo.CK_BangDiemMirror_Diem_Version', N'C') IS NULL
    ALTER TABLE dbo.BangDiemMirror WITH CHECK ADD CONSTRAINT CK_BangDiemMirror_Diem_Version
        CHECK (DiemTongKet BETWEEN 0 AND 10 AND SoTinChi BETWEEN 1 AND 15 AND Version > 0);
ALTER TABLE dbo.BangDiemMirror WITH CHECK CHECK CONSTRAINT CK_BangDiemMirror_Diem_Version;
GO

IF OBJECT_ID(N'dbo.CK_OutboxSuKien_Payload', N'C') IS NULL
    ALTER TABLE dbo.OutboxSuKien WITH CHECK ADD CONSTRAINT CK_OutboxSuKien_Payload
        CHECK (ISJSON(NoiDung) = 1 AND Version > 0 AND SoLanThu >= 0);
ALTER TABLE dbo.OutboxSuKien WITH CHECK CHECK CONSTRAINT CK_OutboxSuKien_Payload;
GO

IF OBJECT_ID(N'dbo.CK_OutboxSuKien_TrangThai', N'C') IS NULL
    ALTER TABLE dbo.OutboxSuKien WITH CHECK ADD CONSTRAINT CK_OutboxSuKien_TrangThai
        CHECK (TrangThai IN ('PENDING', 'SENT'));
ALTER TABLE dbo.OutboxSuKien WITH CHECK CHECK CONSTRAINT CK_OutboxSuKien_TrangThai;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.DangKyHocPhan') AND name = N'UQ_DangKyHocPhan_MaYeuCau')
    CREATE UNIQUE NONCLUSTERED INDEX UQ_DangKyHocPhan_MaYeuCau
        ON dbo.DangKyHocPhan (MaYeuCau) WHERE MaYeuCau IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.DangKyMonHoc') AND name = N'UQ_DangKyMonHoc_SV_Ky_Mon')
    CREATE UNIQUE NONCLUSTERED INDEX UQ_DangKyMonHoc_SV_Ky_Mon
        ON dbo.DangKyMonHoc (MaSinhVien, MaHocKy, MaMonHoc) WHERE TrangThai IN ('DANG_XU_LY', 'DA_DANG_KY', 'DANG_HUY');
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.TaiKhoan') AND name = N'UQ_TaiKhoan_MaThucThe')
    CREATE UNIQUE NONCLUSTERED INDEX UQ_TaiKhoan_MaThucThe
        ON dbo.TaiKhoan (MaThucThe) WHERE MaThucThe IS NOT NULL;
GO
COMMIT TRANSACTION;
GO
PRINT '  [ok] Da ap dung CHECK va UNIQUE. Bo dem van phai cap nhat nguyen tu trong nghiep vu.';
GO
