/* PTIT One — V7: mã cơ sở phải lưu đúng chữ hoa.

   LỖI ĐÃ XẢY RA THẬT (03/10/2026). Hai hồ sơ sinh viên được cấp trước commit
   fdd68a0 lưu MaCoSoNha = 'hcm' chữ thường. Hệ quả: tài khoản đó KHÔNG đăng ký
   được học phần nào — EnrollmentService so sánh cơ sở bằng String.equals của
   Java (phân biệt hoa thường), nên 'hcm' ≠ 'HCM' và mọi lớp đều bị coi là
   "thuộc cơ sở khác" với mã lỗi ENROLLMENT_CROSS_CAMPUS.

   Vì sao database không chặn: collation Vietnamese_CI_AS KHÔNG phân biệt hoa
   thường, nên khoá ngoại tới dbo.CoSo nhận 'hcm' y như 'HCM'. Tầng SQL thấy
   hợp lệ, tầng Java thấy khác nhau — sai lệch im lặng.

   ⚠️ Mọi phép so sánh hoa thường trong file này PHẢI có COLLATE nhị phân.
   Viết CHECK (MaCoSo = UPPER(MaCoSo)) dưới collation CI là VÔ DỤNG: hai vế
   luôn bằng nhau nên ràng buộc không bao giờ chặn gì.

   Đường ghi trong ứng dụng đã vá ở fdd68a0 (AccountRepository.findCampus lấy
   giá trị chuẩn từ dbo.CoSo). V7 làm hai việc còn lại: sửa dữ liệu cũ trên mọi
   máy, và để database tự ép buộc từ nay thay vì chỉ dựa vào code.

   Phần 2: chín cột dưới đây nằm rải trên cả bảng nhân bản (DanhBaNguoiDung) và
   bảng cục bộ tại site. Chạy V7 TRƯỚC khi dựng publication, vì sửa dữ liệu trên
   bảng đã nhân bản thì phải đẩy lại snapshot. */

SET XACT_ABORT ON;
GO

/* ---------- 1. Chuẩn hoá dbo.CoSo trước ----------

   Các bảng khác lấy giá trị chuẩn từ đây, nên bảng này phải đúng trước. Khoá
   ngoại là CI nên đổi hoa thường ở đây không làm vỡ bảng con. */

UPDATE dbo.CoSo
   SET MaCoSo = UPPER(MaCoSo)
 WHERE MaCoSo COLLATE Latin1_General_BIN2 <> UPPER(MaCoSo) COLLATE Latin1_General_BIN2;
GO

/* ---------- 2. Sửa chín cột tham chiếu ----------

   Lấy giá trị chuẩn bằng JOIN với dbo.CoSo: phép so khớp là CI nên 'hcm' tìm
   được dòng 'HCM', và ta ghi lại đúng giá trị đã lưu trong CoSo. Điều kiện
   BIN2 ở cuối để chỉ ghi những dòng thật sự lệch. */

UPDATE d SET d.MaCoSo = c.MaCoSo
  FROM dbo.DanhBaNguoiDung d
  JOIN dbo.CoSo c ON c.MaCoSo = d.MaCoSo
 WHERE d.MaCoSo COLLATE Latin1_General_BIN2 <> c.MaCoSo COLLATE Latin1_General_BIN2;

UPDATE t SET t.MaCoSo = c.MaCoSo
  FROM dbo.TaiKhoan t
  JOIN dbo.CoSo c ON c.MaCoSo = t.MaCoSo
 WHERE t.MaCoSo COLLATE Latin1_General_BIN2 <> c.MaCoSo COLLATE Latin1_General_BIN2;

UPDATE s SET s.MaCoSoNha = c.MaCoSo
  FROM dbo.SinhVien s
  JOIN dbo.CoSo c ON c.MaCoSo = s.MaCoSoNha
 WHERE s.MaCoSoNha COLLATE Latin1_General_BIN2 <> c.MaCoSo COLLATE Latin1_General_BIN2;

UPDATE g SET g.MaCoSo = c.MaCoSo
  FROM dbo.GiangVien g
  JOIN dbo.CoSo c ON c.MaCoSo = g.MaCoSo
 WHERE g.MaCoSo COLLATE Latin1_General_BIN2 <> c.MaCoSo COLLATE Latin1_General_BIN2;

UPDATE dk SET dk.MaCoSo = c.MaCoSo
  FROM dbo.DotDangKy dk
  JOIN dbo.CoSo c ON c.MaCoSo = dk.MaCoSo
 WHERE dk.MaCoSo COLLATE Latin1_General_BIN2 <> c.MaCoSo COLLATE Latin1_General_BIN2;

UPDATE l SET l.MaCoSoHost = c.MaCoSo
  FROM dbo.LopHocPhan l
  JOIN dbo.CoSo c ON c.MaCoSo = l.MaCoSoHost
 WHERE l.MaCoSoHost COLLATE Latin1_General_BIN2 <> c.MaCoSo COLLATE Latin1_General_BIN2;

UPDATE dh SET dh.MaCoSoNhaSV = c.MaCoSo
  FROM dbo.DangKyHocPhan dh
  JOIN dbo.CoSo c ON c.MaCoSo = dh.MaCoSoNhaSV
 WHERE dh.MaCoSoNhaSV COLLATE Latin1_General_BIN2 <> c.MaCoSo COLLATE Latin1_General_BIN2;

UPDATE dm SET dm.MaCoSoHost = c.MaCoSo
  FROM dbo.DangKyMonHoc dm
  JOIN dbo.CoSo c ON c.MaCoSo = dm.MaCoSoHost
 WHERE dm.MaCoSoHost COLLATE Latin1_General_BIN2 <> c.MaCoSo COLLATE Latin1_General_BIN2;

UPDATE tb SET tb.MaCoSo = c.MaCoSo
  FROM dbo.ThongBao tb
  JOIN dbo.CoSo c ON c.MaCoSo = tb.MaCoSo
 WHERE tb.MaCoSo COLLATE Latin1_General_BIN2 <> c.MaCoSo COLLATE Latin1_General_BIN2;
GO

/* ---------- 3. Ràng buộc: từ nay database tự chặn ----------

   Ép chữ hoa trên CẢ dbo.CoSo và chín cột tham chiếu. Hai điều đó cộng khoá
   ngoại (so khớp CI) cho ra bất biến thật sự cần: giá trị ở bảng con trùng
   dbo.CoSo tới từng byte, nên String.equals của Java luôn đúng.

   CHECK không tham chiếu được bảng khác, nên không thể viết trực tiếp
   "phải trùng CoSo tới từng byte" — ép chữ hoa hai phía là cách đạt điều đó.

   Cột NULL được (DanhBaNguoiDung của Admin Master, ThongBao phạm vi toàn
   trường) không cần xử lý riêng: CHECK cho qua khi biểu thức là NULL. */

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_CoSo_MaCoSo_ChuHoa')
    ALTER TABLE dbo.CoSo WITH CHECK ADD CONSTRAINT CK_CoSo_MaCoSo_ChuHoa
        CHECK (MaCoSo COLLATE Latin1_General_BIN2 = UPPER(MaCoSo) COLLATE Latin1_General_BIN2);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_DanhBaNguoiDung_MaCoSo_ChuHoa')
    ALTER TABLE dbo.DanhBaNguoiDung WITH CHECK ADD CONSTRAINT CK_DanhBaNguoiDung_MaCoSo_ChuHoa
        CHECK (MaCoSo COLLATE Latin1_General_BIN2 = UPPER(MaCoSo) COLLATE Latin1_General_BIN2);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_TaiKhoan_MaCoSo_ChuHoa')
    ALTER TABLE dbo.TaiKhoan WITH CHECK ADD CONSTRAINT CK_TaiKhoan_MaCoSo_ChuHoa
        CHECK (MaCoSo COLLATE Latin1_General_BIN2 = UPPER(MaCoSo) COLLATE Latin1_General_BIN2);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_SinhVien_MaCoSoNha_ChuHoa')
    ALTER TABLE dbo.SinhVien WITH CHECK ADD CONSTRAINT CK_SinhVien_MaCoSoNha_ChuHoa
        CHECK (MaCoSoNha COLLATE Latin1_General_BIN2 = UPPER(MaCoSoNha) COLLATE Latin1_General_BIN2);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_GiangVien_MaCoSo_ChuHoa')
    ALTER TABLE dbo.GiangVien WITH CHECK ADD CONSTRAINT CK_GiangVien_MaCoSo_ChuHoa
        CHECK (MaCoSo COLLATE Latin1_General_BIN2 = UPPER(MaCoSo) COLLATE Latin1_General_BIN2);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_DotDangKy_MaCoSo_ChuHoa')
    ALTER TABLE dbo.DotDangKy WITH CHECK ADD CONSTRAINT CK_DotDangKy_MaCoSo_ChuHoa
        CHECK (MaCoSo COLLATE Latin1_General_BIN2 = UPPER(MaCoSo) COLLATE Latin1_General_BIN2);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_LopHocPhan_MaCoSoHost_ChuHoa')
    ALTER TABLE dbo.LopHocPhan WITH CHECK ADD CONSTRAINT CK_LopHocPhan_MaCoSoHost_ChuHoa
        CHECK (MaCoSoHost COLLATE Latin1_General_BIN2 = UPPER(MaCoSoHost) COLLATE Latin1_General_BIN2);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_DangKyHocPhan_MaCoSoNhaSV_ChuHoa')
    ALTER TABLE dbo.DangKyHocPhan WITH CHECK ADD CONSTRAINT CK_DangKyHocPhan_MaCoSoNhaSV_ChuHoa
        CHECK (MaCoSoNhaSV COLLATE Latin1_General_BIN2 = UPPER(MaCoSoNhaSV) COLLATE Latin1_General_BIN2);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_DangKyMonHoc_MaCoSoHost_ChuHoa')
    ALTER TABLE dbo.DangKyMonHoc WITH CHECK ADD CONSTRAINT CK_DangKyMonHoc_MaCoSoHost_ChuHoa
        CHECK (MaCoSoHost COLLATE Latin1_General_BIN2 = UPPER(MaCoSoHost) COLLATE Latin1_General_BIN2);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_ThongBao_MaCoSo_ChuHoa')
    ALTER TABLE dbo.ThongBao WITH CHECK ADD CONSTRAINT CK_ThongBao_MaCoSo_ChuHoa
        CHECK (MaCoSo COLLATE Latin1_General_BIN2 = UPPER(MaCoSo) COLLATE Latin1_General_BIN2);
GO
