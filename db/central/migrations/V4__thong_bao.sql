/* PTIT One — V4: thông báo học vụ và trạng thái đọc (Phần 1).

   Nhóm cho phép bổ sung ngày 02/10/2026: module `notification` sở hữu đúng hai
   bảng này. Hai loại thông báo dùng chung một bảng:
     SOAN     — Admin/GV soạn, có nháp, gửi theo phạm vi
     TU_DONG  — sinh ra cùng giao dịch nghiệp vụ (đăng ký, huỷ, công bố/sửa điểm)

   Người nhận ghi theo THỰC THỂ (MaSinhVien/MaGiangVien), không theo tài khoản:
   sinh viên kích hoạt tài khoản muộn (F02) vẫn thấy thông báo đã gửi trước đó.
   Danh sách người nhận CHỐT lúc gửi — sinh viên chuyển lớp sau đó không làm
   lịch sử thông báo đổi theo.

   Không IDENTITY (C9): MaThongBao là GUID do ứng dụng sinh. Thông báo không mang
   ngữ nghĩa cơ sở nên không nhúng mã cơ sở vào khoá. Không trigger, không cascade.

   Phần 2: thông báo là dữ liệu cục bộ tại site của người nhận; kế hoạch phân
   tán sẽ quyết định khi tách site, không mặc định nhân bản. */
IF DB_NAME() NOT LIKE N'PTITONE[_]CENTRAL%'
    THROW 51000, N'V4__thong_bao chi chay tren PTITONE_CENTRAL*.', 1;
GO

CREATE TABLE dbo.ThongBao (
    MaThongBao     uniqueidentifier NOT NULL,
    Loai           varchar(20)      NOT NULL,
    TrangThai      varchar(20)      NOT NULL,
    MucDo          varchar(20)      NOT NULL CONSTRAINT DF_ThongBao_MucDo DEFAULT ('THONG_THUONG'),
    TieuDe         nvarchar(200)    NOT NULL,
    NoiDung        nvarchar(4000)   NOT NULL,
    -- Đường dẫn NỘI BỘ của web (bắt đầu bằng '/'), ví dụ /sinh-vien/bang-diem?maHocKy=2026-1.
    LienKet        varchar(300)     NULL,
    PhamVi         varchar(20)      NOT NULL,
    MaCoSo         varchar(10)      NULL,
    MaLopHP        varchar(80)      NULL,
    DoiTuong       varchar(20)      NOT NULL,
    -- NULL với thông báo tự sinh.
    NguoiTao       varchar(50)      NULL,
    VaiTroNguoiTao varchar(20)      NULL,
    SuKien         varchar(30)      NULL,
    -- Khoá chống trùng của thông báo tự sinh: gọi lại cùng sự kiện không tạo bản thứ hai.
    KhoaSuKien     varchar(200)     NULL,
    NgayTao        datetime2(0)     NOT NULL,
    NgayGui        datetime2(0)     NULL,
    CONSTRAINT PK_ThongBao PRIMARY KEY CLUSTERED (MaThongBao),
    CONSTRAINT FK_ThongBao_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo (MaCoSo),
    CONSTRAINT FK_ThongBao_LopHocPhan FOREIGN KEY (MaLopHP) REFERENCES dbo.LopHocPhan (MaLopHP),
    CONSTRAINT CK_ThongBao_Loai CHECK (Loai IN ('SOAN', 'TU_DONG')),
    CONSTRAINT CK_ThongBao_TrangThai CHECK (TrangThai IN ('NHAP', 'DA_GUI')),
    CONSTRAINT CK_ThongBao_MucDo CHECK (MucDo IN ('THONG_THUONG', 'QUAN_TRONG')),
    CONSTRAINT CK_ThongBao_DoiTuong CHECK (DoiTuong IN ('SINH_VIEN', 'GIANG_VIEN', 'TAT_CA')),
    CONSTRAINT CK_ThongBao_PhamVi CHECK (
           (PhamVi = 'TOAN_TRUONG'  AND MaCoSo IS NULL     AND MaLopHP IS NULL)
        OR (PhamVi = 'CO_SO'        AND MaCoSo IS NOT NULL AND MaLopHP IS NULL)
        OR (PhamVi = 'LOP_HOC_PHAN' AND MaLopHP IS NOT NULL)
        OR (PhamVi = 'CA_NHAN')),
    -- Thông báo tự sinh không có nháp và luôn có sự kiện nguồn.
    CONSTRAINT CK_ThongBao_TuDong CHECK (
           Loai = 'SOAN'
        OR (TrangThai = 'DA_GUI' AND SuKien IS NOT NULL AND KhoaSuKien IS NOT NULL)),
    CONSTRAINT CK_ThongBao_DaGui CHECK (TrangThai = 'NHAP' OR NgayGui IS NOT NULL),
    CONSTRAINT CK_ThongBao_LienKet CHECK (LienKet IS NULL OR LienKet LIKE '/%')
);
GO
CREATE UNIQUE NONCLUSTERED INDEX UQ_ThongBao_KhoaSuKien
    ON dbo.ThongBao (KhoaSuKien) WHERE KhoaSuKien IS NOT NULL;
GO
-- "Thông báo tôi đã soạn" của người gửi.
CREATE NONCLUSTERED INDEX IX_ThongBao_NguoiTao
    ON dbo.ThongBao (NguoiTao, NgayTao) WHERE NguoiTao IS NOT NULL;
GO

/* Hộp thư theo người nhận: khoá chính bắt đầu bằng người nhận nên đọc hộp thư
   và đếm chưa đọc chỉ quét đúng phần của một người. */
CREATE TABLE dbo.ThongBaoNguoiNhan (
    LoaiNguoiNhan varchar(20)      NOT NULL,
    MaNguoiNhan   varchar(20)      NOT NULL,
    MaThongBao    uniqueidentifier NOT NULL,
    NgayDoc       datetime2(0)     NULL,
    CONSTRAINT PK_ThongBaoNguoiNhan PRIMARY KEY CLUSTERED (LoaiNguoiNhan, MaNguoiNhan, MaThongBao),
    CONSTRAINT FK_ThongBaoNguoiNhan_ThongBao FOREIGN KEY (MaThongBao) REFERENCES dbo.ThongBao (MaThongBao),
    CONSTRAINT CK_ThongBaoNguoiNhan_Loai CHECK (LoaiNguoiNhan IN ('SINH_VIEN', 'GIANG_VIEN'))
);
GO
-- Thống kê "đã đọc bao nhiêu" cho người gửi.
CREATE NONCLUSTERED INDEX IX_ThongBaoNguoiNhan_ThongBao
    ON dbo.ThongBaoNguoiNhan (MaThongBao) INCLUDE (NgayDoc);
GO
