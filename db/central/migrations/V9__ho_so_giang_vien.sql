/* PTIT One — V9: các cột lý lịch giảng viên và ảnh đại diện.

   Đúng bộ cột `V8` đã thêm cho `SinhVien`, vì màn "Thông tin" của giảng viên
   dùng chung một biểu mẫu với màn của sinh viên. Lệch bộ cột giữa hai bảng thì
   phải tách làm hai biểu mẫu, mà hai bên đang cần y hệt nhau.

   TẤT CẢ đều NULL được: hồ sơ giảng viên hiện có trong DB không có dữ liệu này.

   Những gì KHÔNG nằm ở đây, vì đã có sẵn:
   - `HoTen`, `MaCoSo`, `MaKhoa`, `HocVi` — `GiangVien` (`V2`), do Phòng Đào tạo
     quản; giảng viên không tự sửa
   - `Email` trường cấp — `TaiKhoan` (`V6`)

   `UQ_GiangVien_SoCCCD` chỉ unique TRONG bảng này. Không chặn được một số CCCD
   vừa nằm ở `SinhVien` vừa nằm ở `GiangVien` — muốn vậy phải có bảng người
   dùng chung, mà đồ án không có khái niệm đó. Thực tế một người vừa là sinh
   viên vừa là giảng viên không xảy ra trong phạm vi Phần 1.

   Phần 2: `GiangVien` thuộc nhóm bảng nhân bản một chiều từ CENTRAL xuống các
   cơ sở, nên các cột này đi theo bản nhân bản. Vẫn chỉ lưu URL ảnh, không lưu
   nhị phân — xem lý do ở `V8`. */

SET XACT_ABORT ON;
GO

ALTER TABLE dbo.GiangVien ADD
    GioiTinh    varchar(3)    NULL,
    DienThoai   varchar(20)   NULL,
    SoCCCD      varchar(20)   NULL,
    EmailCaNhan nvarchar(254) NULL,
    NoiSinh     nvarchar(150) NULL,
    DanToc      nvarchar(50)  NULL,
    TonGiao     nvarchar(50)  NULL,
    HoKhau      nvarchar(255) NULL,
    AnhDaiDien  varchar(500)  NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_GiangVien_GioiTinh')
    ALTER TABLE dbo.GiangVien WITH CHECK ADD CONSTRAINT CK_GiangVien_GioiTinh
        CHECK (GioiTinh IS NULL OR GioiTinh IN ('NAM', 'NU'));
GO

/* Filtered index để các hồ sơ chưa nhập (NULL) không đụng nhau — unique
   thường sẽ chặn dòng NULL thứ hai. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_GiangVien_SoCCCD')
    CREATE UNIQUE NONCLUSTERED INDEX UQ_GiangVien_SoCCCD
        ON dbo.GiangVien (SoCCCD)
        WHERE SoCCCD IS NOT NULL;
GO
