/* PTIT One — tests/10-verify-hoc-vu.sql (CENTRAL)
   Nghiệm thu ràng buộc V2: mỗi ca ghi sai phải bị chặn bởi ĐÚNG constraint
   mong đợi (so tên trong thông báo lỗi, không chỉ "có lỗi"). Toàn bộ chạy trong
   một transaction và ROLLBACK — không để lại dữ liệu.
   Cần: V2 + seed 10-auth-seed.sql + 20-hoc-vu-seed.sql.
     sqlcmd -S "localhost\PTITONE" -d PTITONE_CENTRAL -E -C -b -f 65001 -i db\central\tests\10-verify-hoc-vu.sql */

SET NOCOUNT ON;
-- OFF: vi phạm constraint chỉ hủy câu lệnh, transaction vẫn dùng tiếp được cho ca sau.
SET XACT_ABORT OFF;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

IF DB_NAME() NOT LIKE N'PTITONE[_]CENTRAL%'
    THROW 51000, N'Test chi chay tren PTITONE_CENTRAL*.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.LopHocPhan WHERE MaLopHP = 'INT1154-2026-1-HCM01')
    THROW 51004, N'Chua co fixture. Chay 20-hoc-vu-seed.sql truoc.', 1;

DECLARE @KetQua TABLE (Ca nvarchar(120) NOT NULL, KetQua nvarchar(400) NOT NULL);
DECLARE @Mong nvarchar(128);

BEGIN TRANSACTION;

SET @Mong = N'CK_LopHocPhan_SucChua';
BEGIN TRY
    INSERT dbo.LopHocPhan (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, SoLuongToiDa, SoLuongDaDangKy, TrangThai, HinhThucHoc)
    VALUES ('T-SUCCHUA', 'INT1154', '2026-1', 'HCM', 10, 11, 'MO', 'TRUC_TIEP');
    INSERT @KetQua VALUES (N'Sĩ số vượt sức chứa', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Sĩ số vượt sức chứa',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'CK_LopHocPhan_TrangThai';
BEGIN TRY
    INSERT dbo.LopHocPhan (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, SoLuongToiDa, TrangThai, HinhThucHoc)
    VALUES ('T-TRANGTHAI', 'INT1154', '2026-1', 'HCM', 10, 'DA_DUYET', 'TRUC_TIEP');
    INSERT @KetQua VALUES (N'Trạng thái lớp ngoài bộ từ vựng', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Trạng thái lớp ngoài bộ từ vựng',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'CK_LopHocPhan_LienCoSo';
BEGIN TRY
    INSERT dbo.LopHocPhan (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, SoLuongToiDa, TrangThai, ChoPhepLienCoSo, HinhThucHoc)
    VALUES ('T-LIENCOSO', 'INT1154', '2026-1', 'HCM', 10, 'MO', 1, 'TRUC_TIEP');
    INSERT @KetQua VALUES (N'Liên cơ sở cho lớp trực tiếp', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Liên cơ sở cho lớp trực tiếp',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'CK_DangKyMonHoc_TrangThai';
BEGIN TRY
    INSERT dbo.DangKyMonHoc (MaSinhVien, MaHocKy, MaMonHoc, MaLopHP, MaCoSoHost, TrangThai, SoTinChi, PhienBanLich)
    VALUES ('B26DCCN003', '2026-1', 'INT1155', 'INT1155-2026-1-HCM01', 'HCM', 'CHO_DUYET', 3, 1);
    INSERT @KetQua VALUES (N'Trạng thái đăng ký CHO_DUYET (tên cấm)', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Trạng thái đăng ký CHO_DUYET (tên cấm)',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

-- B26DCCN003 đã giữ INT1154 ở lớp HCM01; lớp HN01 khác lớp nhưng CÙNG môn.
SET @Mong = N'PK_DangKyMonHoc';
BEGIN TRY
    INSERT dbo.DangKyMonHoc (MaSinhVien, MaHocKy, MaMonHoc, MaLopHP, MaCoSoHost, TrangThai, SoTinChi, PhienBanLich)
    VALUES ('B26DCCN003', '2026-1', 'INT1154', 'INT1154-2026-1-HN01', 'HN', 'DANG_XU_LY', 3, 1);
    INSERT @KetQua VALUES (N'Trùng môn ở lớp khác', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Trùng môn ở lớp khác',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'CK_SinhVienHocKy_TranTinChi';
BEGIN TRY
    UPDATE dbo.SinhVienHocKy SET SoTinChiDangGiuCho = SoTinChiDangGiuCho + 22
     WHERE MaSinhVien = 'B26DCCN003' AND MaHocKy = '2026-1';
    INSERT @KetQua VALUES (N'Vượt trần tín chỉ (3 + 22 > 24)', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Vượt trần tín chỉ (3 + 22 > 24)',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'FK_DangKyHocPhan_SinhVien';
BEGIN TRY
    INSERT dbo.DangKyHocPhan (MaLopHP, MaSinhVien, MaCoSoNhaSV, HoTenSinhVien)
    VALUES ('INT1155-2026-1-HCM01', 'KHONG_TON_TAI', 'HCM', N'Không có');
    INSERT @KetQua VALUES (N'Ghi danh SV không tồn tại [P1]', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Ghi danh SV không tồn tại [P1]',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'FK_Diem_DangKyHocPhan';
BEGIN TRY
    INSERT dbo.Diem (MaLopHP, MaSinhVien, DiemTongKet) VALUES ('INT1155-2026-1-HCM01', 'B26DCCN001', 7.0);
    INSERT @KetQua VALUES (N'Điểm khi chưa ghi danh', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Điểm khi chưa ghi danh',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'CK_Diem_ThangDiem';
BEGIN TRY
    UPDATE dbo.Diem SET DiemCuoiKy = 11 WHERE MaLopHP = 'INT1154-2025-1-HCM01' AND MaSinhVien = 'B25DCCN001';
    INSERT @KetQua VALUES (N'Điểm ngoài thang 0-10', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Điểm ngoài thang 0-10',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'CK_MonHocTienQuyet_KhongTuTro';
BEGIN TRY
    INSERT dbo.MonHocTienQuyet (MaMonHoc, MaMonTienQuyet) VALUES ('INT1154', 'INT1154');
    INSERT @KetQua VALUES (N'Môn tự làm tiên quyết', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Môn tự làm tiên quyết',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'CK_LichHoc_KhoangLich';
BEGIN TRY
    INSERT dbo.LichHoc (MaLopHP, Thu, TietBatDau, SoTiet, TuanBatDau, TuanKetThuc)
    VALUES ('INT1155-2026-1-HCM01', 9, 1, 3, 1, 15);
    INSERT @KetQua VALUES (N'Lịch học thứ 9', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Lịch học thứ 9',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

SET @Mong = N'CK_DotDangKy_TrangThai';
BEGIN TRY
    INSERT dbo.DotDangKy (MaDot, MaHocKy, MaCoSo, ThoiGianMo, ThoiGianDong, TrangThai)
    VALUES ('T-DOT', '2026-1', 'HCM', '2026-09-01', '2026-10-01', 'MO');
    INSERT @KetQua VALUES (N'Trạng thái đợt ngoài bộ từ vựng', N'FAIL: không bị chặn');
END TRY BEGIN CATCH
    INSERT @KetQua VALUES (N'Trạng thái đợt ngoài bộ từ vựng',
        CASE WHEN ERROR_MESSAGE() LIKE N'%' + @Mong + N'%' THEN N'PASS' ELSE N'FAIL: ' + ERROR_MESSAGE() END);
END CATCH;

/* Kỹ thuật chống vượt sức chứa của ứng dụng: lớp đầy thì UPDATE có điều kiện
   không đụng dòng nào (@@ROWCOUNT = 0), KHÔNG phải lỗi. */
UPDATE dbo.LopHocPhan SET SoLuongDaDangKy = SoLuongDaDangKy + 1
 WHERE MaLopHP = 'INT1154-2026-1-HCM01' AND SoLuongDaDangKy < SoLuongToiDa;
DECLARE @SoDong int = @@ROWCOUNT;
INSERT @KetQua VALUES (N'Giữ chỗ lớp đầy trả @@ROWCOUNT = 0',
    CASE WHEN @SoDong = 0 THEN N'PASS' ELSE N'FAIL: vẫn cập nhật được lớp đầy' END);

ROLLBACK TRANSACTION;

SELECT Ca, KetQua FROM @KetQua;
DECLARE @SoLoi int = (SELECT COUNT(*) FROM @KetQua WHERE KetQua <> N'PASS');
DECLARE @SoCa int = (SELECT COUNT(*) FROM @KetQua);
IF @SoLoi > 0
    THROW 51005, N'Co ca rang buoc KHONG dat. Xem bang ket qua o tren.', 1;
PRINT CONCAT(N'PASS: ', @SoCa, N' ca rang buoc, da ROLLBACK.');
