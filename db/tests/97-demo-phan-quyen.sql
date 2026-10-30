/* =====================================================================
   PTIT One — tests/97-demo-phan-quyen.sql   (-On HCM, sau site/14)
   ---------------------------------------------------------------------
   Demo D13: mỗi vai trò thử một việc ĐƯỢC phép và một việc BỊ CẤM, cộng
   hai trigger toàn vẹn. Đóng vai bằng EXECUTE AS USER (user demo không có
   login, nên không phải giữ mật khẩu nào). Mỗi ca chạy trong giao dịch
   rồi ROLLBACK — không đổi dữ liệu.

   In bảng kết quả + "PHAN QUYEN: PASS". Lệch một ca là THROW.
   ===================================================================== */
SET NOCOUNT ON;
SET XACT_ABORT OFF;   -- lỗi quyền phải bắt được trong CATCH, không giết cả lô
GO
IF N'$(SiteTarget)' NOT IN (N'HCM', N'HN', N'DN') OR DB_NAME() <> N'$(DbTarget)'
    THROW 51100, N'Chay tren mot co so (-On HCM).', 1;
IF DATABASE_PRINCIPAL_ID(N'r_SinhVien') IS NULL
    THROW 53400, N'Chua co vai tro — chay site/14 truoc.', 1;
GO

DECLARE @lop VARCHAR(80) = (SELECT TOP 1 d.MaLopHP FROM dbo.Diem d
                              JOIN dbo.LopHocPhan l ON l.MaLopHP = d.MaLopHP
                             WHERE l.TrangThai = 'MO' ORDER BY d.MaLopHP);
DECLARE @khac VARCHAR(10) = CASE N'$(SiteTarget)' WHEN N'HCM' THEN 'HN' ELSE 'HCM' END;
IF @lop IS NULL THROW 53401, N'Can mot lop MO co dong Diem o co so nay (chay migrate/50).', 1;

DECLARE @ca TABLE (So INT, NguoiDung SYSNAME NULL, ThaoTac NVARCHAR(400), MongDoi VARCHAR(5));
INSERT INTO @ca VALUES
 (1,  N'u_demo_sv',     N'SELECT TOP 1 1 FROM dbo.Diem',                                                   'DUOC'),
 (2,  N'u_demo_sv',     N'UPDATE dbo.Diem SET DiemCuoiKy = 10 WHERE MaLopHP = @lop',                       'CHAN'),
 (3,  N'u_demo_sv',     N'UPDATE dbo.LopHocPhan SET SoLuongToiDa = 999 WHERE MaLopHP = @lop',              'CHAN'),
 (4,  N'u_demo_sv',     N'SELECT TOP 1 1 FROM dbo.DanhBaNguoiDung',                                        'CHAN'),
 (5,  N'u_demo_gv',     N'UPDATE dbo.Diem SET DiemCuoiKy = DiemCuoiKy WHERE MaLopHP = @lop',               'DUOC'),
 (6,  N'u_demo_gv',     N'DELETE FROM dbo.DangKyHocPhan WHERE MaLopHP = @lop',                             'CHAN'),
 (7,  N'u_demo_adcs',   N'UPDATE dbo.LopHocPhan SET SoLuongToiDa = SoLuongToiDa WHERE MaLopHP = @lop',     'DUOC'),
 (8,  N'u_demo_adcs',   N'UPDATE dbo.Diem SET DiemCuoiKy = 10 WHERE MaLopHP = @lop',                       'CHAN'),
 (9,  N'u_demo_adcs',   N'UPDATE dbo.MonHoc SET TenMonHoc = TenMonHoc',                                    'CHAN'),
 (10, N'u_demo_master', N'SELECT TOP 1 1 FROM dbo.DangKyHocPhan',                                          'DUOC'),
 (11, N'u_demo_master', N'UPDATE dbo.SinhVien SET HoTen = HoTen',                                          'CHAN'),
 -- Trigger: chạy bằng dbo — quyền đủ, chỉ trigger được phép chặn.
 (12, NULL,             N'UPDATE dbo.LopHocPhan SET MaCoSoHost = @khac WHERE MaLopHP = @lop',              'CHAN'),
 (13, NULL,             N'UPDATE dbo.LopHocPhan SET TrangThai = ''DA_KHOA'' WHERE MaLopHP = @lop;
                          UPDATE dbo.Diem SET DiemCuoiKy = DiemCuoiKy WHERE MaLopHP = @lop',               'CHAN');

DECLARE @kq TABLE (So INT, VaiTro SYSNAME NULL, ThaoTac NVARCHAR(400), MongDoi VARCHAR(5),
                   ThucTe VARCHAR(5), Loi NVARCHAR(160));
DECLARE @so INT, @nd SYSNAME, @tt NVARCHAR(400), @md VARCHAR(5), @thuc VARCHAR(5), @loi NVARCHAR(160);
DECLARE c CURSOR LOCAL FAST_FORWARD FOR SELECT So, NguoiDung, ThaoTac, MongDoi FROM @ca ORDER BY So;
OPEN c; FETCH NEXT FROM c INTO @so, @nd, @tt, @md;
WHILE @@FETCH_STATUS = 0
BEGIN
    SELECT @thuc = 'DUOC', @loi = NULL;
    BEGIN TRANSACTION;
    BEGIN TRY
        IF @nd IS NOT NULL EXECUTE AS USER = @nd;
        EXEC sp_executesql @tt, N'@lop VARCHAR(80), @khac VARCHAR(10)', @lop, @khac;
        IF @nd IS NOT NULL REVERT;
    END TRY
    BEGIN CATCH
        SELECT @thuc = 'CHAN', @loi = LEFT(ERROR_MESSAGE(), 160);
        IF @nd IS NOT NULL AND USER_NAME() = @nd REVERT;
    END CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    INSERT INTO @kq VALUES (@so, (SELECT r.name FROM sys.database_role_members m
                                    JOIN sys.database_principals r ON r.principal_id = m.role_principal_id
                                   WHERE m.member_principal_id = DATABASE_PRINCIPAL_ID(@nd)),
                            LEFT(@tt, 70), @md, @thuc, @loi);
    FETCH NEXT FROM c INTO @so, @nd, @tt, @md;
END
CLOSE c; DEALLOCATE c;

SELECT So, ISNULL(VaiTro, N'(trigger)') AS VaiTro, ThaoTac, MongDoi, ThucTe,
       CASE WHEN MongDoi = ThucTe THEN 'KHOP' ELSE 'LECH' END AS KetLuan, Loi
  FROM @kq ORDER BY So;

IF EXISTS (SELECT 1 FROM @kq WHERE MongDoi <> ThucTe)
    THROW 53490, N'PHAN QUYEN: LECH — xem bang tren.', 1;
PRINT '  PHAN QUYEN: PASS - 4 vai tro theo B3 + 2 trigger toan ven (D13).';
GO
