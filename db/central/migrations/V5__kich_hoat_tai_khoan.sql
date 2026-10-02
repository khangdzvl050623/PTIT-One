/* PTIT One — V5__kich_hoat_tai_khoan.sql (CENTRAL, Phần 1) — F02

   Cấp tài khoản chỉ ở Master (chốt 02/10/2026): Admin Master tạo hồ sơ +
   danh bạ + tài khoản CHƯA có mật khẩu, kèm một mã kích hoạt dùng một lần.
   Người dùng đặt mật khẩu bằng mã đó; tới lúc ấy mới đăng nhập được.

   1. TaiKhoan.MatKhauHash cho phép NULL: NULL nghĩa là CHƯA KÍCH HOẠT.
      Trạng thái kích hoạt nằm ở TaiKhoan (cục bộ tại site), KHÔNG ở
      DanhBaNguoiDung — ở Phần 2 danh bạ là bảng nhân bản, site bị DENY ghi,
      nên việc kích hoạt (xảy ra tại site) không được phải ghi danh bạ.
      DanhBaNguoiDung.TrangThai giữ nghĩa thiết kế: CHO_KICH_HOAT = site chưa
      dựng xong tài khoản (Outbox chưa chạy). Phần 1 một DB nên dựng ngay
      trong cùng giao dịch, danh bạ vào thẳng HOAT_DONG.

   2. MaKichHoat: chỉ lưu SHA-256 của mã. Mã 80 bit ngẫu nhiên nên SHA-256
      là đủ (giống TokenLamMoi), không cần Argon2. Sai quá số lần cho phép
      thì mã bị thu hồi.
      ⚠️ Phần 2: CỤC BỘ TẠI SITE NHÀ, KHÔNG NHÂN BẢN — cùng chỗ với TaiKhoan.

   Không IDENTITY: MaKichHoat là UUID do ứng dụng sinh. Không MERGE. */

IF DB_NAME() NOT LIKE N'PTITONE[_]CENTRAL%'
    THROW 51000, N'V5__kich_hoat_tai_khoan chi chay tren PTITONE_CENTRAL*.', 1;
GO

ALTER TABLE dbo.TaiKhoan ALTER COLUMN MatKhauHash varchar(255) NULL;
GO

CREATE TABLE dbo.MaKichHoat (
    MaKichHoat     uniqueidentifier NOT NULL,
    TenDangNhap    varchar(50)      NOT NULL,
    MaHash         binary(32)       NOT NULL,
    ThoiDiemTao    datetime2(3)     NOT NULL,
    ThoiDiemHetHan datetime2(3)     NOT NULL,
    ThoiDiemDaDung datetime2(3)     NULL,
    -- Cấp mã mới, hoặc nhập sai quá số lần cho phép.
    ThoiDiemThuHoi datetime2(3)     NULL,
    SoLanSai       tinyint          NOT NULL CONSTRAINT DF_MaKichHoat_SoLanSai DEFAULT (0),
    CONSTRAINT PK_MaKichHoat PRIMARY KEY CLUSTERED (MaKichHoat),
    CONSTRAINT FK_MaKichHoat_TaiKhoan FOREIGN KEY (TenDangNhap) REFERENCES dbo.TaiKhoan (TenDangNhap),
    CONSTRAINT CK_MaKichHoat_HetHan CHECK (ThoiDiemHetHan > ThoiDiemTao),
    CONSTRAINT CK_MaKichHoat_KetThuc CHECK (ThoiDiemDaDung IS NULL OR ThoiDiemThuHoi IS NULL)
);
GO

-- Mỗi tài khoản tối đa MỘT mã còn sống: cấp mã mới phải thu hồi mã cũ trước.
CREATE UNIQUE NONCLUSTERED INDEX UQ_MaKichHoat_ConSong
    ON dbo.MaKichHoat (TenDangNhap)
    WHERE ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL;
GO
