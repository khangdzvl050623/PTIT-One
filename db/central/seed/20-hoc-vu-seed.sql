/* PTIT One — seed/20-hoc-vu-seed.sql (CENTRAL, dữ liệu DEMO)
   Yêu cầu: migration V2__hoc_vu_schema đã áp dụng VÀ đã chạy 10-auth-seed.sql.
   Chạy chủ động, KHÔNG tự chạy lúc API khởi động:
     sqlcmd -S "localhost\PTITONE" -d PTITONE_CENTRAL -E -C -b -f 65001 -i db\central\seed\20-hoc-vu-seed.sql

   Chạy lại không nhân đôi: chỉ INSERT dòng còn thiếu, không sửa/xóa dòng có sẵn.
   🔶 Danh mục lấy từ db/master/04-seed-danhmuc.sql — dữ liệu mẫu, KHÔNG phải quy chế PTIT.

   Ca nghiệp vụ có sẵn (học kỳ hiện tại 2026-1):
     Tiên quyết đạt        B25DCCN001 đạt INT1154 (8.0) → được học INT1155
     Tiên quyết chưa đạt   B25DCCN001 trượt BAS1150 (3.3) → bị chặn BAS1151
     Chưa có điểm TQ       B26DCCN001 với INT1155
     Lớp hết chỗ           INT1154-2026-1-HCM01 (sức chứa 1, B26DCCN003 đã giữ)
     Trùng lịch            INT1155-…-HCM01 (T2 tiết 1-3) và BAS1150-…-HCM01 (T2 tiết 2-4)
     Liên cơ sở            INT1358-2026-1-HCM01 trực tuyến; B26DCCN002 (HN) đăng ký được
     Đợt mở / đóng         HCM, HN đang mở · DN đã đóng
     Lớp chưa mở           INT1445-2026-1-HCM01 ở DU_KIEN
   Ngưỡng đạt chưa chốt: điểm mẫu chọn cách xa 4.0 và 5.0 để đúng với cả hai.

   Bộ đếm (SoLuongDaDangKy, SoTinChiDaDangKy, SoTinChiTichLuy) khớp đúng số dòng
   ghi danh bên dưới. Thêm/xóa ghi danh ở đây thì sửa bộ đếm cùng lúc. */

SET NOCOUNT ON;
SET XACT_ABORT ON;
-- sqlcmd mặc định QUOTED_IDENTIFIER OFF; ghi vào bảng có filtered index sẽ bị từ chối.
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

IF DB_NAME() NOT LIKE N'PTITONE[_]CENTRAL%'
    THROW 51000, N'Seed chi chay tren PTITONE_CENTRAL*.', 1;
IF OBJECT_ID(N'dbo.DangKyMonHoc', N'U') IS NULL
    THROW 51002, N'Chua co schema hoc vu. Chay migration V2__hoc_vu_schema truoc.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.CoSo WHERE MaCoSo = 'HCM')
    THROW 51003, N'Chua co co so. Chay 10-auth-seed.sql truoc.', 1;

BEGIN TRANSACTION;

/* ---------- Danh mục ---------- */
INSERT INTO dbo.KhungGioTiet (SoTiet, GioBatDau, GioKetThuc, Buoi)
SELECT v.SoTiet, v.GioBatDau, v.GioKetThuc, v.Buoi
  FROM (VALUES ( 1, '07:00', '07:50', 'SANG'),  ( 2, '08:00', '08:50', 'SANG'),
               ( 3, '09:00', '09:50', 'SANG'),  ( 4, '10:00', '10:50', 'SANG'),
               ( 5, '11:00', '11:50', 'SANG'),  ( 6, '13:00', '13:50', 'CHIEU'),
               ( 7, '14:00', '14:50', 'CHIEU'), ( 8, '15:00', '15:50', 'CHIEU'),
               ( 9, '16:00', '16:50', 'CHIEU'), (10, '17:00', '17:50', 'CHIEU'),
               (11, '18:00', '18:50', 'TOI'),   (12, '19:00', '19:50', 'TOI'))
       v (SoTiet, GioBatDau, GioKetThuc, Buoi)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.KhungGioTiet k WHERE k.SoTiet = v.SoTiet);

INSERT INTO dbo.Khoa (MaKhoa, TenKhoa)
SELECT v.MaKhoa, v.TenKhoa
  FROM (VALUES ('CNTT', N'Công nghệ thông tin'), ('VT', N'Viễn thông'),
               ('ATTT', N'An toàn thông tin'), ('DPT', N'Công nghệ đa phương tiện'),
               ('QTKD', N'Quản trị kinh doanh'))
       v (MaKhoa, TenKhoa)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.Khoa k WHERE k.MaKhoa = v.MaKhoa);

INSERT INTO dbo.ChuongTrinhDaoTao (MaCTDT, TenCTDT, MaKhoa, TongTinChi)
SELECT v.MaCTDT, v.TenCTDT, v.MaKhoa, v.TongTinChi
  FROM (VALUES ('CN-CNTT-2022', N'Cử nhân Công nghệ thông tin', 'CNTT', 130),
               ('KS-ATTT-2022', N'Kỹ sư An toàn thông tin', 'ATTT', 145),
               ('CN-DPT-2022',  N'Cử nhân Công nghệ đa phương tiện', 'DPT', 130))
       v (MaCTDT, TenCTDT, MaKhoa, TongTinChi)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.ChuongTrinhDaoTao c WHERE c.MaCTDT = v.MaCTDT);

INSERT INTO dbo.MonHoc (MaMonHoc, TenMonHoc, SoTinChi, MaKhoa)
SELECT v.MaMonHoc, v.TenMonHoc, v.SoTinChi, v.MaKhoa
  FROM (VALUES ('BAS1150', N'Giải tích 1', 3, 'CNTT'),
               ('BAS1151', N'Giải tích 2', 3, 'CNTT'),
               ('BAS1203', N'Đại số tuyến tính', 3, 'CNTT'),
               ('INT1154', N'Tin học cơ sở 1', 3, 'CNTT'),
               ('INT1155', N'Tin học cơ sở 2', 3, 'CNTT'),
               ('INT1306', N'Cấu trúc dữ liệu và giải thuật', 3, 'CNTT'),
               ('INT1313', N'Lập trình hướng đối tượng', 3, 'CNTT'),
               ('INT1332', N'Cơ sở dữ liệu', 3, 'CNTT'),
               ('INT1339', N'Cơ sở dữ liệu phân tán', 3, 'CNTT'),
               ('INT1358', N'Mạng máy tính', 3, 'CNTT'),
               ('INT1434', N'Công nghệ phần mềm', 3, 'CNTT'),
               ('INT1445', N'Nhập môn trí tuệ nhân tạo', 3, 'CNTT'),
               ('ATT1234', N'Cơ sở an toàn thông tin', 3, 'ATTT'),
               ('DPT1201', N'Nhập môn đa phương tiện', 3, 'DPT'))
       v (MaMonHoc, TenMonHoc, SoTinChi, MaKhoa)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.MonHoc m WHERE m.MaMonHoc = v.MaMonHoc);

INSERT INTO dbo.MonHocTienQuyet (MaMonHoc, MaMonTienQuyet)
SELECT v.MaMonHoc, v.MaMonTienQuyet
  FROM (VALUES ('BAS1151', 'BAS1150'), ('INT1155', 'INT1154'), ('INT1306', 'INT1155'),
               ('INT1313', 'INT1155'), ('INT1332', 'INT1306'), ('INT1339', 'INT1332'),
               ('INT1434', 'INT1313'))
       v (MaMonHoc, MaMonTienQuyet)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.MonHocTienQuyet t
                    WHERE t.MaMonHoc = v.MaMonHoc AND t.MaMonTienQuyet = v.MaMonTienQuyet);

INSERT INTO dbo.CTDT_MonHoc (MaCTDT, MaMonHoc, HocKyGoiY, BatBuoc)
SELECT v.MaCTDT, v.MaMonHoc, v.HocKyGoiY, v.BatBuoc
  FROM (VALUES ('CN-CNTT-2022', 'BAS1150', 1, 1), ('CN-CNTT-2022', 'BAS1203', 1, 1),
               ('CN-CNTT-2022', 'INT1154', 1, 1), ('CN-CNTT-2022', 'BAS1151', 2, 1),
               ('CN-CNTT-2022', 'INT1155', 2, 1), ('CN-CNTT-2022', 'INT1306', 3, 1),
               ('CN-CNTT-2022', 'INT1313', 3, 1), ('CN-CNTT-2022', 'INT1332', 4, 1),
               ('CN-CNTT-2022', 'INT1358', 4, 1), ('CN-CNTT-2022', 'INT1339', 5, 1),
               ('CN-CNTT-2022', 'INT1434', 5, 1), ('CN-CNTT-2022', 'INT1445', 6, 0),
               ('KS-ATTT-2022', 'BAS1150', 1, 1), ('KS-ATTT-2022', 'INT1154', 1, 1),
               ('KS-ATTT-2022', 'INT1155', 2, 1), ('KS-ATTT-2022', 'INT1332', 4, 1),
               ('KS-ATTT-2022', 'ATT1234', 4, 1), ('CN-DPT-2022',  'INT1154', 1, 1),
               ('CN-DPT-2022',  'DPT1201', 1, 1))
       v (MaCTDT, MaMonHoc, HocKyGoiY, BatBuoc)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.CTDT_MonHoc c
                    WHERE c.MaCTDT = v.MaCTDT AND c.MaMonHoc = v.MaMonHoc);

INSERT INTO dbo.HocKy (MaHocKy, TenHocKy, NamHoc, NgayBatDau, NgayKetThuc)
SELECT v.MaHocKy, v.TenHocKy, v.NamHoc, v.NgayBatDau, v.NgayKetThuc
  FROM (VALUES ('2025-1',  N'Học kỳ 1 năm học 2025-2026', '2025-2026', '2025-09-08', '2026-01-11'),
               ('2025-2',  N'Học kỳ 2 năm học 2025-2026', '2025-2026', '2026-01-19', '2026-05-24'),
               ('2025-HE', N'Học kỳ hè 2025-2026',        '2025-2026', '2026-06-01', '2026-07-26'),
               ('2026-1',  N'Học kỳ 1 năm học 2026-2027', '2026-2027', '2026-09-07', '2027-01-10'))
       v (MaHocKy, TenHocKy, NamHoc, NgayBatDau, NgayKetThuc)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.HocKy h WHERE h.MaHocKy = v.MaHocKy);

/* ---------- Hồ sơ — mã khớp MaThucThe trong 10-auth-seed.sql ---------- */
INSERT INTO dbo.SinhVien (MaSinhVien, HoTen, NgaySinh, MaCoSoNha, MaCTDT, TrangThai, SoTinChiTichLuy)
SELECT v.MaSinhVien, v.HoTen, v.NgaySinh, v.MaCoSoNha, v.MaCTDT, v.TrangThai, v.SoTinChiTichLuy
  FROM (VALUES ('B26DCCN001', N'Nguyễn Minh An',  '2008-03-12', 'HCM', 'CN-CNTT-2022', 'DANG_HOC', 0),
               ('B26DCCN002', N'Trần Bảo Bình',   '2008-07-01', 'HN',  'CN-CNTT-2022', 'DANG_HOC', 0),
               ('B26DCCN003', N'Lê Thu Chi',      '2008-11-23', 'HCM', 'CN-CNTT-2022', 'DANG_HOC', 0),
               ('B26DCCN004', N'Phạm Quốc Dũng',  '2008-01-30', 'HCM', 'CN-CNTT-2022', 'THOI_HOC', 0),
               -- 3 = tín chỉ INT1154 đã đạt ở 2025-1 (BAS1150 trượt nên không tính).
               ('B25DCCN001', N'Hoàng Gia Huy',   '2007-05-18', 'HCM', 'CN-CNTT-2022', 'DANG_HOC', 3))
       v (MaSinhVien, HoTen, NgaySinh, MaCoSoNha, MaCTDT, TrangThai, SoTinChiTichLuy)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.SinhVien s WHERE s.MaSinhVien = v.MaSinhVien);

INSERT INTO dbo.GiangVien (MaGiangVien, HoTen, MaCoSo, MaKhoa, HocVi)
SELECT v.MaGiangVien, v.HoTen, v.MaCoSo, v.MaKhoa, v.HocVi
  FROM (VALUES ('GVHCM001', N'Nguyễn Hữu Phước', 'HCM', 'CNTT', N'Tiến sĩ'),
               ('GVHN001',  N'Đặng Thị Hạnh',    'HN',  'CNTT', N'Thạc sĩ'))
       v (MaGiangVien, HoTen, MaCoSo, MaKhoa, HocVi)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.GiangVien g WHERE g.MaGiangVien = v.MaGiangVien);

/* ---------- Đợt đăng ký ---------- */
INSERT INTO dbo.DotDangKy (MaDot, MaHocKy, MaCoSo, ThoiGianMo, ThoiGianDong, TrangThai)
SELECT v.MaDot, v.MaHocKy, v.MaCoSo, v.ThoiGianMo, v.ThoiGianDong, v.TrangThai
  FROM (VALUES ('HCM-2025-1-01', '2025-1', 'HCM', '2025-08-15 00:00', '2025-09-20 23:59', 'DA_DONG'),
               ('HCM-2026-1-01', '2026-1', 'HCM', '2026-09-01 00:00', '2026-12-31 23:59', 'DANG_MO'),
               ('HN-2026-1-01',  '2026-1', 'HN',  '2026-09-01 00:00', '2026-12-31 23:59', 'DANG_MO'),
               ('DN-2026-1-01',  '2026-1', 'DN',  '2026-08-01 00:00', '2026-08-31 23:59', 'DA_DONG'))
       v (MaDot, MaHocKy, MaCoSo, ThoiGianMo, ThoiGianDong, TrangThai)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.DotDangKy d WHERE d.MaDot = v.MaDot);

/* ---------- Lớp học phần + lịch ---------- */
INSERT INTO dbo.LopHocPhan (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, MaGiangVien,
                            SoLuongToiDa, SoLuongDaDangKy, TrangThai, ChoPhepLienCoSo, HinhThucHoc)
SELECT v.MaLopHP, v.MaMonHoc, v.MaHocKy, v.MaCoSoHost, v.MaGiangVien,
       v.SoLuongToiDa, v.SoLuongDaDangKy, v.TrangThai, v.ChoPhepLienCoSo, v.HinhThucHoc
  FROM (VALUES
        -- 2025-1 đã khóa điểm: nguồn điểm tiên quyết của B25DCCN001.
        ('INT1154-2025-1-HCM01', 'INT1154', '2025-1', 'HCM', 'GVHCM001', 40, 1, 'DA_KHOA',  0, 'TRUC_TIEP'),
        ('BAS1150-2025-1-HCM01', 'BAS1150', '2025-1', 'HCM', 'GVHCM001', 40, 1, 'DA_KHOA',  0, 'TRUC_TIEP'),
        -- 2026-1 đang mở.
        ('INT1155-2026-1-HCM01', 'INT1155', '2026-1', 'HCM', 'GVHCM001', 40, 0, 'MO',       0, 'TRUC_TIEP'),
        ('BAS1150-2026-1-HCM01', 'BAS1150', '2026-1', 'HCM', 'GVHCM001', 40, 0, 'MO',       0, 'TRUC_TIEP'),
        ('BAS1151-2026-1-HCM01', 'BAS1151', '2026-1', 'HCM', 'GVHCM001', 40, 0, 'MO',       0, 'TRUC_TIEP'),
        ('INT1154-2026-1-HCM01', 'INT1154', '2026-1', 'HCM', 'GVHCM001',  1, 1, 'MO',       0, 'TRUC_TIEP'),
        ('INT1358-2026-1-HCM01', 'INT1358', '2026-1', 'HCM', 'GVHCM001', 60, 0, 'MO',       1, 'TRUC_TUYEN'),
        ('INT1445-2026-1-HCM01', 'INT1445', '2026-1', 'HCM', NULL,       40, 0, 'DU_KIEN',  0, 'TRUC_TIEP'),
        ('INT1154-2026-1-HN01',  'INT1154', '2026-1', 'HN',  'GVHN001',  40, 0, 'MO',       0, 'TRUC_TIEP'),
        ('BAS1150-2026-1-HN01',  'BAS1150', '2026-1', 'HN',  'GVHN001',  40, 0, 'MO',       0, 'TRUC_TIEP'))
       v (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, MaGiangVien,
          SoLuongToiDa, SoLuongDaDangKy, TrangThai, ChoPhepLienCoSo, HinhThucHoc)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.LopHocPhan l WHERE l.MaLopHP = v.MaLopHP);

INSERT INTO dbo.LichHoc (MaLopHP, Thu, TietBatDau, SoTiet, PhongHoc, TuanBatDau, TuanKetThuc)
SELECT v.MaLopHP, v.Thu, v.TietBatDau, v.SoTiet, v.PhongHoc, v.TuanBatDau, v.TuanKetThuc
  FROM (VALUES ('INT1154-2025-1-HCM01', 2, 1, 3, N'A2-201',     1, 15),
               ('BAS1150-2025-1-HCM01', 4, 1, 3, N'A2-305',     1, 15),
               ('INT1155-2026-1-HCM01', 2, 1, 3, N'A2-201',     1, 15),
               -- Tiết 2-4 chồng lên tiết 1-3 của INT1155 cùng thứ Hai: ca trùng lịch.
               ('BAS1150-2026-1-HCM01', 2, 2, 3, N'A2-305',     1, 15),
               ('BAS1151-2026-1-HCM01', 3, 1, 3, N'A2-305',     1, 15),
               ('INT1154-2026-1-HCM01', 4, 7, 3, N'A3-102',     1, 15),
               ('INT1358-2026-1-HCM01', 6, 7, 3, N'Trực tuyến', 1, 15),
               ('INT1445-2026-1-HCM01', 5, 1, 3, NULL,          1, 15),
               ('INT1154-2026-1-HN01',  3, 1, 3, N'P.301',      1, 15),
               ('BAS1150-2026-1-HN01',  5, 1, 3, N'P.305',      1, 15))
       v (MaLopHP, Thu, TietBatDau, SoTiet, PhongHoc, TuanBatDau, TuanKetThuc)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.LichHoc h
                    WHERE h.MaLopHP = v.MaLopHP AND h.Thu = v.Thu AND h.TietBatDau = v.TietBatDau);

/* ---------- Ghi danh: Home (SinhVienHocKy → DangKyMonHoc) và Host (DangKyHocPhan → Diem) ---------- */
INSERT INTO dbo.SinhVienHocKy (MaSinhVien, MaHocKy, SoTinChiDaDangKy, SoTinChiDangGiuCho, TranTinChi)
SELECT v.MaSinhVien, v.MaHocKy, v.SoTinChiDaDangKy, 0, 24
  FROM (VALUES ('B25DCCN001', '2025-1', 6),
               ('B26DCCN003', '2026-1', 3))
       v (MaSinhVien, MaHocKy, SoTinChiDaDangKy)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.SinhVienHocKy k
                    WHERE k.MaSinhVien = v.MaSinhVien AND k.MaHocKy = v.MaHocKy);

INSERT INTO dbo.DangKyHocPhan (MaLopHP, MaSinhVien, MaCoSoNhaSV, HoTenSinhVien, NgayDangKy, TrangThai)
SELECT v.MaLopHP, s.MaSinhVien, s.MaCoSoNha, s.HoTen, v.NgayDangKy, 'DA_DANG_KY'
  FROM (VALUES ('INT1154-2025-1-HCM01', 'B25DCCN001', '2025-08-20 09:00'),
               ('BAS1150-2025-1-HCM01', 'B25DCCN001', '2025-08-20 09:05'),
               ('INT1154-2026-1-HCM01', 'B26DCCN003', '2026-09-02 08:30'))
       v (MaLopHP, MaSinhVien, NgayDangKy)
  JOIN dbo.SinhVien s ON s.MaSinhVien = v.MaSinhVien
 WHERE NOT EXISTS (SELECT 1 FROM dbo.DangKyHocPhan d
                    WHERE d.MaLopHP = v.MaLopHP AND d.MaSinhVien = v.MaSinhVien);

INSERT INTO dbo.DangKyMonHoc (MaSinhVien, MaHocKy, MaMonHoc, MaLopHP, MaCoSoHost,
                              TrangThai, SoTinChi, PhienBanLich)
SELECT d.MaSinhVien, l.MaHocKy, l.MaMonHoc, l.MaLopHP, l.MaCoSoHost,
       'DA_DANG_KY', m.SoTinChi, l.PhienBanLich
  FROM (VALUES ('INT1154-2025-1-HCM01', 'B25DCCN001'),
               ('BAS1150-2025-1-HCM01', 'B25DCCN001'),
               ('INT1154-2026-1-HCM01', 'B26DCCN003'))
       d (MaLopHP, MaSinhVien)
  JOIN dbo.LopHocPhan l ON l.MaLopHP = d.MaLopHP
  JOIN dbo.MonHoc m ON m.MaMonHoc = l.MaMonHoc
 WHERE NOT EXISTS (SELECT 1 FROM dbo.DangKyMonHoc k
                    WHERE k.MaSinhVien = d.MaSinhVien AND k.MaHocKy = l.MaHocKy
                      AND k.MaMonHoc = l.MaMonHoc);

INSERT INTO dbo.Diem (MaLopHP, MaSinhVien, DiemChuyenCan, DiemGiuaKy, DiemCuoiKy, DiemTongKet, NgayCongBo)
SELECT v.MaLopHP, v.MaSinhVien, v.CC, v.GK, v.CK, v.TK, '2026-01-20 10:00'
  FROM (VALUES ('INT1154-2025-1-HCM01', 'B25DCCN001', 9.0, 7.5, 8.0, 8.0),
               ('BAS1150-2025-1-HCM01', 'B25DCCN001', 5.0, 3.0, 3.0, 3.3))
       v (MaLopHP, MaSinhVien, CC, GK, CK, TK)
 WHERE NOT EXISTS (SELECT 1 FROM dbo.Diem d
                    WHERE d.MaLopHP = v.MaLopHP AND d.MaSinhVien = v.MaSinhVien);

COMMIT TRANSACTION;

/* Đối soát bộ đếm: mọi cột Lech phải bằng 0. */
SELECT l.MaLopHP, l.SoLuongDaDangKy,
       (SELECT COUNT(*) FROM dbo.DangKyHocPhan d
         WHERE d.MaLopHP = l.MaLopHP AND d.TrangThai IN ('DANG_XU_LY', 'DA_DANG_KY', 'DANG_HUY'))
       - l.SoLuongDaDangKy AS Lech
  FROM dbo.LopHocPhan l
 WHERE l.SoLuongDaDangKy > 0 OR EXISTS (SELECT 1 FROM dbo.DangKyHocPhan d WHERE d.MaLopHP = l.MaLopHP);

-- Kỳ vọng khi DB chỉ có fixture này: 12 tiết, 5 khoa, 14 môn, 4 học kỳ, 5 SV, 2 GV, 10 lớp, 3 ghi danh, 2 điểm.
SELECT (SELECT COUNT(*) FROM dbo.KhungGioTiet)  AS KhungGioTiet,
       (SELECT COUNT(*) FROM dbo.Khoa)          AS Khoa,
       (SELECT COUNT(*) FROM dbo.MonHoc)        AS MonHoc,
       (SELECT COUNT(*) FROM dbo.HocKy)         AS HocKy,
       (SELECT COUNT(*) FROM dbo.SinhVien)      AS SinhVien,
       (SELECT COUNT(*) FROM dbo.GiangVien)     AS GiangVien,
       (SELECT COUNT(*) FROM dbo.LopHocPhan)    AS LopHocPhan,
       (SELECT COUNT(*) FROM dbo.DangKyHocPhan) AS DangKyHocPhan,
       (SELECT COUNT(*) FROM dbo.Diem)          AS Diem;
