/* PTIT One — V8: các cột lý lịch sinh viên và ảnh đại diện.

   Màn "Thông tin sinh viên" của frontend đã dựng theo cổng sinh viên hiện tại
   và cần nhóm trường lý lịch mà `V2` chưa có. Tám cột dưới đây lấy đúng theo
   những ô mà UI hiển thị (`apps/web/src/features/ho-so/types.ts`), không thêm
   cột nào chưa có chỗ dùng.

   TẤT CẢ đều NULL được: hồ sơ hiện có trong DB không có dữ liệu này, và cổng
   gốc cũng cho sinh viên bỏ trống. UI đã khai `| null` cho từng ô nên không
   phải sửa gì phía giao diện.

   Những gì KHÔNG nằm ở đây, vì đã có sẵn:
   - `NgaySinh`, `SoTinChiTichLuy`, `TrangThai`, `MaCTDT` — `SinhVien` (`V2`)
   - `Email` trường cấp — `TaiKhoan` (`V6`)
   - `TenCTDT`, `TongTinChi` — `ChuongTrinhDaoTao`; `TenKhoa` — `Khoa`

   Điểm thang 4 và điểm trung bình CỐ Ý không có cột. Trọng số và ngưỡng đạt đã
   nằm ở cấu hình (`GradePolicy`, `ptitone.grade.*`) vì là giả định thiết kế
   chưa đối chiếu quy chế; bảng quy đổi thang 4 cùng loại nên để cùng chỗ. Lưu
   thành cột thì nó lệch khỏi `DiemTongKet` ngay lần sửa điểm đầu tiên.

   Phần 2: `SinhVien` phân mảnh ngang theo `MaCoSoNha`, nên các cột này đi theo
   mảnh của sinh viên — không phát sinh nhu cầu nhân bản mới. */

SET XACT_ABORT ON;
GO

ALTER TABLE dbo.SinhVien ADD
    GioiTinh    varchar(3)    NULL,
    DienThoai   varchar(20)   NULL,
    SoCCCD      varchar(20)   NULL,
    EmailCaNhan nvarchar(254) NULL,
    NoiSinh     nvarchar(150) NULL,
    DanToc      nvarchar(50)  NULL,
    TonGiao     nvarchar(50)  NULL,
    HoKhau      nvarchar(255) NULL,
    /* URL ảnh trên dịch vụ lưu ảnh ngoài (Cloudinary — xem apps/api/.env.example).
       Lưu URL chứ không lưu nhị phân: ảnh đại diện không phải dữ liệu nghiệp vụ,
       và Phần 2 nhân bản bảng này thì ảnh nhị phân sẽ làm snapshot phình vô ích. */
    AnhDaiDien  varchar(500)  NULL;
GO

/* Chỉ nhận đúng hai giá trị UI dùng; NULL vẫn qua được vì CHECK cho qua khi
   biểu thức là NULL. */
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_SinhVien_GioiTinh')
    ALTER TABLE dbo.SinhVien WITH CHECK ADD CONSTRAINT CK_SinhVien_GioiTinh
        CHECK (GioiTinh IS NULL OR GioiTinh IN ('NAM', 'NU'));
GO

/* Hai sinh viên không thể cùng một số CCCD. Filtered index để các hồ sơ chưa
   nhập (NULL) không đụng nhau — unique thường sẽ chặn dòng NULL thứ hai. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_SinhVien_SoCCCD')
    CREATE UNIQUE NONCLUSTERED INDEX UQ_SinhVien_SoCCCD
        ON dbo.SinhVien (SoCCCD)
        WHERE SoCCCD IS NOT NULL;
GO
