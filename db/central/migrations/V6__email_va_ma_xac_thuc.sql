/* PTIT One — V6__email_va_ma_xac_thuc.sql (CENTRAL, Phần 1) — A1

   1. Email đặt cùng chỗ với danh tính tài khoản: TaiKhoan (SV/GV/Admin cơ sở)
      và TaiKhoanMaster (Admin Master). Mã khôi phục CHỈ gửi tới email đã lưu
      VÀ đã xác minh — email người dùng gõ vào form chỉ để đối chiếu.

   2. MaKichHoat.EmailNhan: địa chỉ đã nhận mã kích hoạt qua thư. Có giá trị
      nghĩa là mã chỉ đi qua thư (Admin không thấy mã), nên kích hoạt thành công
      cũng là bằng chứng sở hữu email đó → đánh dấu đã xác minh.

   3. MaXacThuc: mã 6 chữ số một lần cho xác minh email và khôi phục mật khẩu.
      Entropy thấp (10^6) nên hash KHÔNG phải lớp phòng thủ — hạn 10 phút và
      tối đa 5 lần sai mới là. Hash là HMAC-SHA256 với khoá PTITONE_OTP_SECRET
      để lộ bảng này cũng không dò ngược được mã.
      ⚠️ Phần 2: CỤC BỘ TẠI SITE NHÀ (Master với Admin Master), KHÔNG NHÂN BẢN.

   Không IDENTITY: MaXacThuc là UUID do ứng dụng sinh. Không MERGE. */

IF DB_NAME() NOT LIKE N'PTITONE[_]CENTRAL%'
    THROW 51000, N'V6__email_va_ma_xac_thuc chi chay tren PTITONE_CENTRAL*.', 1;
GO

ALTER TABLE dbo.TaiKhoan ADD
    Email                varchar(254) NULL,
    EmailDaXacMinh       bit          NOT NULL CONSTRAINT DF_TaiKhoan_EmailDaXacMinh DEFAULT (0),
    ThoiDiemXacMinhEmail datetime2(3) NULL;
GO
ALTER TABLE dbo.TaiKhoan ADD CONSTRAINT CK_TaiKhoan_EmailXacMinh
    CHECK (EmailDaXacMinh = 0 OR (Email IS NOT NULL AND ThoiDiemXacMinhEmail IS NOT NULL));
GO

ALTER TABLE dbo.TaiKhoanMaster ADD
    Email                varchar(254) NULL,
    EmailDaXacMinh       bit          NOT NULL CONSTRAINT DF_TaiKhoanMaster_EmailDaXacMinh DEFAULT (0),
    ThoiDiemXacMinhEmail datetime2(3) NULL;
GO
ALTER TABLE dbo.TaiKhoanMaster ADD CONSTRAINT CK_TaiKhoanMaster_EmailXacMinh
    CHECK (EmailDaXacMinh = 0 OR (Email IS NOT NULL AND ThoiDiemXacMinhEmail IS NOT NULL));
GO

ALTER TABLE dbo.MaKichHoat ADD EmailNhan varchar(254) NULL;
GO

CREATE TABLE dbo.MaXacThuc (
    MaXacThuc      uniqueidentifier NOT NULL,
    TenDangNhap    varchar(50)      NOT NULL,
    MucDich        varchar(20)      NOT NULL,
    -- Địa chỉ đã gửi mã tới. Xác minh email: chỉ đánh dấu nếu tài khoản vẫn mang đúng địa chỉ này.
    Email          varchar(254)     NOT NULL,
    MaHash         binary(32)       NOT NULL,
    ThoiDiemTao    datetime2(3)     NOT NULL,
    ThoiDiemHetHan datetime2(3)     NOT NULL,
    ThoiDiemDaDung datetime2(3)     NULL,
    ThoiDiemThuHoi datetime2(3)     NULL,
    SoLanSai       tinyint          NOT NULL CONSTRAINT DF_MaXacThuc_SoLanSai DEFAULT (0),
    CONSTRAINT PK_MaXacThuc PRIMARY KEY CLUSTERED (MaXacThuc),
    CONSTRAINT FK_MaXacThuc_DanhBa FOREIGN KEY (TenDangNhap) REFERENCES dbo.DanhBaNguoiDung (TenDangNhap),
    CONSTRAINT CK_MaXacThuc_MucDich CHECK (MucDich IN ('XAC_MINH_EMAIL', 'KHOI_PHUC_MAT_KHAU')),
    CONSTRAINT CK_MaXacThuc_HetHan CHECK (ThoiDiemHetHan > ThoiDiemTao),
    CONSTRAINT CK_MaXacThuc_KetThuc CHECK (ThoiDiemDaDung IS NULL OR ThoiDiemThuHoi IS NULL)
);
GO

-- Xin mã mới thì mã cũ cùng mục đích phải bị thu hồi trước: tối đa MỘT mã còn sống.
CREATE UNIQUE NONCLUSTERED INDEX UQ_MaXacThuc_ConSong
    ON dbo.MaXacThuc (TenDangNhap, MucDich)
    WHERE ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL;
GO
