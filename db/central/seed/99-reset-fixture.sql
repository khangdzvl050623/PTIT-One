/* PTIT One — đưa fixture về đúng seed.

   DÙNG KHI: `clean verify` đỏ ở những ca ĐẾM (sĩ số lớp, số lớp theo cơ sở,
   số tín chỉ, thống kê) sau khi bạn bấm thử giao diện hoặc Swagger. Những ca
   đó so với con số của seed, nên mỗi lượt đăng ký hay mỗi lớp tạo tay đều làm
   chúng lệch — không phải lỗi code.

   LÀM GÌ: xoá SẠCH phần dữ liệu giao dịch rồi để seed dựng lại. Không chữa
   từng dòng vì sửa vá không bao giờ đuổi kịp: một lượt huỷ lớp vừa đổi trạng
   thái ghi danh, vừa đổi bộ đếm, vừa sinh thông báo.

   KHÔNG đụng: danh mục (môn, khoa, học kỳ, CTĐT, cơ sở), hồ sơ sinh viên và
   giảng viên, tài khoản và mật khẩu. Nên tài khoản bạn tự cấp để thử vẫn còn,
   chỉ mất phần đã đăng ký của nó.

   CHẠY (từ gốc repo), rồi chạy lại seed học vụ để dựng lại:

     sqlcmd -S <server> -d PTITONE_CENTRAL -E -N -C -b -I -i db/central/seed/99-reset-fixture.sql
     sqlcmd -S <server> -d PTITONE_CENTRAL -E -N -C -b -I -i db/central/seed/20-hoc-vu-seed.sql

   Cờ -I là bắt buộc: SinhVien có filtered index (UQ_SinhVien_SoCCCD) nên
   SQL Server đòi QUOTED_IDENTIFIER ON, mà sqlcmd mặc định tắt. */

SET XACT_ABORT ON;
SET NOCOUNT ON;
GO

BEGIN TRAN;

/* Xoá theo chiều khoá ngoại: con trước, cha sau.
   Diem → DangKyHocPhan → LopHocPhan; DangKyMonHoc → SinhVienHocKy. */
DELETE FROM dbo.ThongBaoNguoiNhan;
DELETE FROM dbo.ThongBao;
DELETE FROM dbo.Diem;
DELETE FROM dbo.DangKyHocPhan;
DELETE FROM dbo.DangKyMonHoc;
DELETE FROM dbo.SinhVienHocKy;
DELETE FROM dbo.LichHoc;
DELETE FROM dbo.LopHocPhan;
DELETE FROM dbo.DotDangKy;

/* Trả trạng thái 10 tài khoản seed về đúng bảng ở đầu 10-auth-seed.sql.
   B26DCCN003 và B26DCCN004 CỐ Ý không đăng nhập được — có ca test dựa vào
   đúng hai trạng thái đó, nên bấm "mở khoá" trong màn quản trị tài khoản là
   làm đỏ AuthFlowIntegrationTest. Tài khoản bạn tự cấp không nằm trong bảng
   này nên không bị đụng tới. */
UPDATE d
   SET TrangThai = s.TrangThai
  FROM dbo.DanhBaNguoiDung d
  JOIN (VALUES
        ('B26DCCN001', 'HOAT_DONG'), ('B26DCCN002', 'HOAT_DONG'),
        ('B26DCCN003', 'CHO_KICH_HOAT'), ('B26DCCN004', 'NGUNG'),
        ('B25DCCN001', 'HOAT_DONG'), ('GVHCM001', 'HOAT_DONG'),
        ('GVHN001', 'HOAT_DONG'), ('admin.hcm', 'HOAT_DONG'),
        ('admin.hn', 'HOAT_DONG'), ('admin.master', 'HOAT_DONG')
       ) AS s (TenDangNhap, TrangThai) ON s.TenDangNhap = d.TenDangNhap
 WHERE d.TrangThai <> s.TrangThai;

COMMIT;
GO

SELECT 'Da xoa du lieu giao dich. Chay tiep 20-hoc-vu-seed.sql de dung lai.' AS BuocTiepTheo,
       (SELECT COUNT(*) FROM dbo.LopHocPhan)    AS LopHocPhan,
       (SELECT COUNT(*) FROM dbo.DangKyHocPhan) AS DangKyHocPhan,
       (SELECT COUNT(*) FROM dbo.Diem)          AS Diem,
       (SELECT COUNT(*) FROM dbo.SinhVien)      AS SinhVien_GiuNguyen;
GO
