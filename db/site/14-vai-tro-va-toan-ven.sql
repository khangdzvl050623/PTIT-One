/* =====================================================================
   PTIT One — site/14-vai-tro-va-toan-ven.sql   (-On HCM | HN | DN, sau 13)
   ---------------------------------------------------------------------
   D13 — ba lớp phân quyền. File này dựng hai lớp ở tầng CSDL:

   DATABASE ROLE  quyền theo VAI TRÒ, đúng ma trận B3:
                  r_SinhVien · r_GiangVien · r_AdminCoSo · r_AdminMaster
                  (quyền theo DÒNG — "chỉ điểm của chính mình" — là việc
                  của tầng ứng dụng, không làm ở đây)
   TRIGGER        toàn vẹn theo SITE, không cần biết người dùng:
                  • dòng phải mang mã CHÍNH cơ sở này
                  • không sửa Diem khi lớp đã khoá điểm

   ⚠️ Không đụng login ứng dụng `ptitone_api`: API vẫn chạy bằng quyền cũ.
      Role là để chứng minh B3 ở tầng CSDL — demo bằng tests/97.

   ⚠️ Trigger chỉ chặn UPDATE điểm, không chặn INSERT/DELETE: migrate/50
      nạp lại điểm của lớp đã khoá, và thiết kế chỉ ghi "chặn SỬA Diem".
   ===================================================================== */
:ON ERROR EXIT
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF N'$(SiteTarget)' NOT IN (N'HCM', N'HN', N'DN')
   OR DB_NAME() <> N'$(DbTarget)' OR DB_NAME() = N'$(DbMaster)'
    THROW 51100, N'Chi chay tren dung Subscriber (-On HCM/HN/DN).', 1;
GO

/* =====================================================================
   1. VAI TRÒ + user demo (không có login — demo bằng EXECUTE AS USER)
   ===================================================================== */
DECLARE @vt TABLE (VaiTro SYSNAME, UserDemo SYSNAME);
INSERT INTO @vt VALUES (N'r_SinhVien', N'u_demo_sv'), (N'r_GiangVien', N'u_demo_gv'),
                       (N'r_AdminCoSo', N'u_demo_adcs'), (N'r_AdminMaster', N'u_demo_master');
DECLARE @r SYSNAME, @u SYSNAME, @sql NVARCHAR(MAX);
DECLARE c CURSOR LOCAL FAST_FORWARD FOR SELECT VaiTro, UserDemo FROM @vt;
OPEN c; FETCH NEXT FROM c INTO @r, @u;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF DATABASE_PRINCIPAL_ID(@r) IS NULL EXEC (N'CREATE ROLE ' + @r + N';');
    IF DATABASE_PRINCIPAL_ID(@u) IS NULL EXEC (N'CREATE USER ' + @u + N' WITHOUT LOGIN;');
    EXEC sp_addrolemember @r, @u;
    FETCH NEXT FROM c INTO @r, @u;
END
CLOSE c; DEALLOCATE c;
PRINT '  [+] 4 vai tro + 4 user demo';
GO

/* =====================================================================
   2. QUYỀN — mỗi dòng một ô của B3. S = SELECT, W = INSERT/UPDATE/DELETE.
      GRANT ghi thì cũng GRANT đọc. DENY thắng GRANT, nên các ô "—" và
      "chỉ đọc" được DENY tường minh để demo thấy thông báo từ chối.
   ===================================================================== */
DECLARE @danhMuc TABLE (Ten SYSNAME);
INSERT INTO @danhMuc VALUES (N'CoSo'), (N'Khoa'), (N'MonHoc'), (N'MonHocTienQuyet'),
    (N'ChuongTrinhDaoTao'), (N'CTDT_MonHoc'), (N'HocKy'), (N'KhungGioTiet');

DECLARE @q TABLE (VaiTro SYSNAME, Lenh VARCHAR(5), Quyen VARCHAR(40), Bang SYSNAME);
/* Danh mục nhân bản: mọi vai trò chỉ đọc ở cơ sở (Master mới ghi). */
INSERT INTO @q
SELECT v.r, x.l, x.p, d.Ten
  FROM @danhMuc d
 CROSS JOIN (VALUES (N'r_SinhVien'), (N'r_GiangVien'), (N'r_AdminCoSo'), (N'r_AdminMaster')) v(r)
 CROSS JOIN (VALUES ('GRANT', 'SELECT'), ('DENY', 'INSERT, UPDATE, DELETE')) x(l, p);

INSERT INTO @q VALUES
 /* --- Sinh viên --------------------------------------------------- */
 (N'r_SinhVien', 'GRANT', 'SELECT', N'SinhVien'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'GiangVien'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'LopHocPhan'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'LichHoc'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'DotDangKy'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'DangKyHocPhan'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'Diem'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'SinhVienHocKy'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'DangKyMonHoc'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'YeuCauHocLienCoSo'),
 (N'r_SinhVien', 'GRANT', 'SELECT', N'BangDiemMirror'),
 (N'r_SinhVien', 'GRANT', 'UPDATE(MatKhauHash)', N'TaiKhoan'),          -- đổi mật khẩu
 (N'r_SinhVien', 'DENY', 'INSERT, UPDATE, DELETE', N'Diem'),
 (N'r_SinhVien', 'DENY', 'INSERT, UPDATE, DELETE', N'LopHocPhan'),
 (N'r_SinhVien', 'DENY', 'INSERT, UPDATE, DELETE', N'DangKyHocPhan'),  -- chỉ "qua chức năng đăng ký"
 (N'r_SinhVien', 'DENY', 'SELECT', N'DanhBaNguoiDung'),
 (N'r_SinhVien', 'DENY', 'SELECT', N'OutboxSuKien'),
 /* --- Giảng viên -------------------------------------------------- */
 (N'r_GiangVien', 'GRANT', 'SELECT', N'SinhVien'),
 (N'r_GiangVien', 'GRANT', 'SELECT', N'GiangVien'),
 (N'r_GiangVien', 'GRANT', 'SELECT', N'LopHocPhan'),
 (N'r_GiangVien', 'GRANT', 'SELECT', N'LichHoc'),
 (N'r_GiangVien', 'GRANT', 'SELECT', N'DotDangKy'),
 (N'r_GiangVien', 'GRANT', 'SELECT', N'DangKyHocPhan'),
 (N'r_GiangVien', 'GRANT', 'SELECT, UPDATE', N'Diem'),                -- nhập điểm
 (N'r_GiangVien', 'GRANT', 'UPDATE(MatKhauHash)', N'TaiKhoan'),
 (N'r_GiangVien', 'DENY', 'INSERT, UPDATE, DELETE', N'LopHocPhan'),
 (N'r_GiangVien', 'DENY', 'INSERT, UPDATE, DELETE', N'DangKyHocPhan'),
 (N'r_GiangVien', 'DENY', 'SELECT', N'SinhVienHocKy'),
 (N'r_GiangVien', 'DENY', 'SELECT', N'DangKyMonHoc'),
 (N'r_GiangVien', 'DENY', 'SELECT', N'YeuCauHocLienCoSo'),
 (N'r_GiangVien', 'DENY', 'SELECT', N'BangDiemMirror'),
 (N'r_GiangVien', 'DENY', 'SELECT', N'DanhBaNguoiDung'),
 (N'r_GiangVien', 'DENY', 'SELECT', N'OutboxSuKien'),
 /* --- Admin cơ sở ------------------------------------------------- */
 (N'r_AdminCoSo', 'GRANT', 'SELECT, INSERT, UPDATE, DELETE', N'SinhVien'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT, INSERT, UPDATE, DELETE', N'GiangVien'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT, INSERT, UPDATE, DELETE', N'TaiKhoan'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT, INSERT, UPDATE, DELETE', N'LopHocPhan'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT, INSERT, UPDATE, DELETE', N'LichHoc'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT, INSERT, UPDATE, DELETE', N'DangKyHocPhan'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT, INSERT, UPDATE, DELETE', N'DotDangKy'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT', N'Diem'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT', N'SinhVienHocKy'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT', N'DangKyMonHoc'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT', N'YeuCauHocLienCoSo'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT', N'BangDiemMirror'),
 (N'r_AdminCoSo', 'GRANT', 'SELECT', N'DanhBaNguoiDung'),
 (N'r_AdminCoSo', 'DENY', 'INSERT, UPDATE, DELETE', N'Diem'),           -- điểm là của GV
 (N'r_AdminCoSo', 'DENY', 'INSERT, UPDATE, DELETE', N'DanhBaNguoiDung'),
 (N'r_AdminCoSo', 'DENY', 'SELECT', N'OutboxSuKien');

/* --- Admin Master: ĐỌC mọi bảng của cơ sở, không ghi gì ở đây ------- */
INSERT INTO @q
SELECT N'r_AdminMaster', x.l, x.p, t.Ten
  FROM (VALUES (N'SinhVien'), (N'GiangVien'), (N'TaiKhoan'), (N'LopHocPhan'), (N'LichHoc'),
               (N'DangKyHocPhan'), (N'Diem'), (N'DotDangKy'), (N'SinhVienHocKy'), (N'DangKyMonHoc'),
               (N'YeuCauHocLienCoSo'), (N'BangDiemMirror'), (N'OutboxSuKien'), (N'DanhBaNguoiDung')) t(Ten)
 CROSS JOIN (VALUES ('GRANT', 'SELECT'), ('DENY', 'INSERT, UPDATE, DELETE')) x(l, p);

DECLARE @vaiTro SYSNAME, @lenh VARCHAR(5), @quyen VARCHAR(40), @bang SYSNAME, @sql NVARCHAR(400), @n INT = 0;
DECLARE g CURSOR LOCAL FAST_FORWARD FOR SELECT VaiTro, Lenh, Quyen, Bang FROM @q;
OPEN g; FETCH NEXT FROM g INTO @vaiTro, @lenh, @quyen, @bang;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF OBJECT_ID(N'dbo.' + QUOTENAME(@bang), N'U') IS NULL
        THROW 51401, N'Thieu bang van hanh — chay site/10 truoc.', 1;
    /* UPDATE(cot) phải viết sau ON: GRANT UPDATE ON dbo.T (cot) TO ... */
    SET @sql = CASE WHEN @quyen LIKE 'UPDATE(%'
                    THEN @lenh + N' UPDATE ON dbo.' + QUOTENAME(@bang)
                         + N' (' + SUBSTRING(@quyen, 8, LEN(@quyen) - 8) + N') TO ' + @vaiTro
                    ELSE @lenh + N' ' + @quyen + N' ON dbo.' + QUOTENAME(@bang) + N' TO ' + @vaiTro END;
    EXEC (@sql);
    SET @n += 1;
    FETCH NEXT FROM g INTO @vaiTro, @lenh, @quyen, @bang;
END
CLOSE g; DEALLOCATE g;
PRINT '  [+] ' + CONVERT(VARCHAR(10), @n) + ' lenh GRANT/DENY theo B3';
GO

/* =====================================================================
   3. TRIGGER TOÀN VẸN THEO SITE — NOT FOR REPLICATION (bắt buộc ở
      Subscriber; site/13 tự kiểm). Các bảng này không nhân bản, nhưng
      giữ quy ước chung để không ai phải nhớ ngoại lệ.
   ===================================================================== */
DECLARE @cs TABLE (Bang SYSNAME, Cot SYSNAME);
INSERT INTO @cs VALUES (N'SinhVien', N'MaCoSoNha'), (N'GiangVien', N'MaCoSo'), (N'TaiKhoan', N'MaCoSo'),
                       (N'DotDangKy', N'MaCoSo'), (N'LopHocPhan', N'MaCoSoHost');
DECLARE @b SYSNAME, @cot SYSNAME, @sql NVARCHAR(MAX);
DECLARE t CURSOR LOCAL FAST_FORWARD FOR SELECT Bang, Cot FROM @cs;
OPEN t; FETCH NEXT FROM t INTO @b, @cot;
WHILE @@FETCH_STATUS = 0
BEGIN
    /* Chuyển cơ sở (2PC) ghi vào cơ sở MỚI với mã cơ sở mới, migrate/50 lọc
       theo cơ sở — cả hai đều qua được. Chỉ đường ghi nhầm cơ sở bị chặn. */
    SET @sql = N'CREATE OR ALTER TRIGGER dbo.' + QUOTENAME(N'TR_' + @b + N'_DungCoSo') + N'
ON dbo.' + QUOTENAME(@b) + N'
AFTER INSERT, UPDATE
NOT FOR REPLICATION
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted WHERE ' + QUOTENAME(@cot) + N' <> N''$(SiteTarget)'')
        THROW 53301, N''' + @b + N'.' + @cot + N' phai la $(SiteTarget) - dong cua co so khac khong thuoc database nay.'', 1;
END;';
    EXEC (@sql);
    FETCH NEXT FROM t INTO @b, @cot;
END
CLOSE t; DEALLOCATE t;
GO

CREATE OR ALTER TRIGGER dbo.TR_Diem_KhoaDiem
ON dbo.Diem
AFTER UPDATE
NOT FOR REPLICATION
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i JOIN dbo.LopHocPhan l ON l.MaLopHP = i.MaLopHP
                WHERE l.TrangThai = 'DA_KHOA')
        THROW 53302, N'Diem: lop da khoa diem, khong sua duoc.', 1;
END;
GO
PRINT '  [+] 5 trigger dung co so + TR_Diem_KhoaDiem (NOT FOR REPLICATION)';
GO
