/* =====================================================================
   PTIT One — master/09-thong-bao.sql
   ---------------------------------------------------------------------
   ThongBao + ThongBaoNguoiNhan TẠI MASTER: nơi Admin Master soạn, sửa và
   xem thông báo của mình, và nơi CHỐT danh sách người nhận lúc gửi.

   Hộp thư của SV/GV vẫn ở cơ sở nhà. Gửi xong, worker Outbox
   (`ThongBaoDaGui`) chép thông báo + phần người nhận của từng cơ sở sang
   cơ sở đó — cùng cơ chế với vòng đời sinh viên, không thêm cơ chế mới.

   KHÔNG nhân bản: bản ở Master là của người soạn, không phải danh mục.
   Khác site/10: không FK sang LopHocPhan (Master không có bảng đó).

   CHẠY: .\run.ps1 -Script master\09-thong-bao.sql -On MASTER
   ===================================================================== */
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    RAISERROR(N'Script nay CHI chay tren $(DbMaster).', 16, 1);
    SET NOEXEC ON;
END
GO

IF OBJECT_ID(N'dbo.ThongBao', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ThongBao (
        MaThongBao uniqueidentifier NOT NULL,
        Loai varchar(20) NOT NULL,
        TrangThai varchar(20) NOT NULL,
        MucDo varchar(20) NOT NULL CONSTRAINT DF_ThongBao_MucDo DEFAULT ('THONG_THUONG'),
        TieuDe nvarchar(200) NOT NULL,
        NoiDung nvarchar(4000) NOT NULL,
        LienKet varchar(300) NULL,
        PhamVi varchar(20) NOT NULL,
        MaCoSo varchar(10) NULL,
        MaLopHP varchar(80) NULL,
        DoiTuong varchar(20) NOT NULL,
        NguoiTao varchar(50) NULL,
        VaiTroNguoiTao varchar(20) NULL,
        SuKien varchar(30) NULL,
        KhoaSuKien varchar(200) NULL,
        NgayTao datetime2(0) NOT NULL,
        NgayGui datetime2(0) NULL,
        CONSTRAINT PK_ThongBao PRIMARY KEY NONCLUSTERED (MaThongBao),
        CONSTRAINT FK_ThongBao_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo),
        CONSTRAINT CK_ThongBao_Loai CHECK (Loai IN ('SOAN', 'TU_DONG')),
        CONSTRAINT CK_ThongBao_TrangThai CHECK (TrangThai IN ('NHAP', 'DA_GUI')),
        CONSTRAINT CK_ThongBao_MucDo CHECK (MucDo IN ('THONG_THUONG', 'QUAN_TRONG')),
        CONSTRAINT CK_ThongBao_DoiTuong CHECK (DoiTuong IN ('SINH_VIEN', 'GIANG_VIEN', 'TAT_CA')),
        CONSTRAINT CK_ThongBao_PhamVi
            CHECK ((PhamVi = 'TOAN_TRUONG'  AND MaCoSo IS NULL     AND MaLopHP IS NULL)
                OR (PhamVi = 'CO_SO'        AND MaCoSo IS NOT NULL AND MaLopHP IS NULL)
                OR (PhamVi = 'LOP_HOC_PHAN' AND MaLopHP IS NOT NULL)
                OR (PhamVi = 'CA_NHAN')),
        CONSTRAINT CK_ThongBao_DaGui CHECK (TrangThai = 'NHAP' OR NgayGui IS NOT NULL)
    );
    CREATE NONCLUSTERED INDEX IX_ThongBao_NguoiTao ON dbo.ThongBao (NguoiTao, NgayTao);
    PRINT '  [+] dbo.ThongBao';
END
ELSE PRINT '  [=] dbo.ThongBao';
GO

IF OBJECT_ID(N'dbo.ThongBaoNguoiNhan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ThongBaoNguoiNhan (
        LoaiNguoiNhan varchar(20) NOT NULL,
        MaNguoiNhan varchar(20) NOT NULL,
        MaThongBao uniqueidentifier NOT NULL,
        NgayDoc datetime2(0) NULL,
        CONSTRAINT PK_ThongBaoNguoiNhan PRIMARY KEY CLUSTERED (LoaiNguoiNhan, MaNguoiNhan, MaThongBao),
        CONSTRAINT FK_ThongBaoNguoiNhan_ThongBao FOREIGN KEY (MaThongBao)
            REFERENCES dbo.ThongBao (MaThongBao)
    );
    -- Đếm người nhận theo thông báo (màn "đã gửi" của Admin Master).
    CREATE NONCLUSTERED INDEX IX_ThongBaoNguoiNhan_ThongBao ON dbo.ThongBaoNguoiNhan (MaThongBao);
    PRINT '  [+] dbo.ThongBaoNguoiNhan';
END
ELSE PRINT '  [=] dbo.ThongBaoNguoiNhan';
GO

SET NOEXEC OFF;
GO
