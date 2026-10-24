/* =====================================================================
   PTIT One — migrate/50-tach-du-lieu.sql
   ---------------------------------------------------------------------
   ⭐ YÊU CẦU BẮT BUỘC SỐ 1 — PHÂN MẢNH NGANG.

   Tách `PTITONE_CENTRAL` (một database của Phần 1) thành danh bạ ở Master
   và các mảnh vận hành ở từng cơ sở.

   VỊ TỪ PHÂN MẢNH — ĐÂY LÀ PHẦN ĐƯỢC CHẤM, ĐỌC KỸ

   | Bảng            | Đi theo                           | Loại           |
   |-----------------|-----------------------------------|----------------|
   | SinhVien        | MaCoSoNha                         | ngang, sơ cấp  |
   | GiangVien       | MaCoSo                            | ngang, sơ cấp  |
   | TaiKhoan        | MaCoSo                            | ngang, sơ cấp  |
   | DotDangKy       | MaCoSo                            | ngang, sơ cấp  |
   | LopHocPhan      | MaCoSoHost                        | ngang, sơ cấp  |
   | SinhVienHocKy   | ⋉ SinhVien                        | dẫn xuất       |
   | DangKyMonHoc    | ⋉ SinhVien  (nằm ở cơ sở NHÀ)     | dẫn xuất       |
   | LichHoc         | ⋉ LopHocPhan                      | dẫn xuất       |
   | DangKyHocPhan   | ⋉ LopHocPhan ⚠️ KHÔNG ⋉ SinhVien  | dẫn xuất       |
   | Diem            | ⋉ DangKyHocPhan                   | dẫn xuất bậc 2 |

   ⚠️ DÒNG `DangKyHocPhan` LÀ DÒNG DỄ SAI NHẤT, và `AGENTS.md` ghi rõ đây
      là lỗi đã từng mắc. Lượt đăng ký đi theo **lớp**, không theo sinh
      viên — vì chỗ ngồi là tài sản của lớp, mà lớp thuộc về cơ sở mở nó.
      Sinh viên HCM học lớp ở HN thì dòng đăng ký đó nằm ở **HN**, và
      `Diem` đi theo nên điểm cũng nằm ở HN. Chính vì vậy mới cần Outbox
      để trả bảng điểm về cơ sở nhà.

   KHÔNG CHUYỂN — và vì sao
     PhienDangNhap, TokenLamMoi, MaKichHoat, MaXacThuc : dữ liệu phiên,
       ngắn hạn. Người dùng đăng nhập lại là có. Chuyển sang chỉ mang theo
       rủi ro mà không đem lại gì.
     ThongBao, ThongBaoNguoiNhan : phạm vi TOAN_TRUONG phải nhân ra mọi
       site nên không đối soát được bằng phép cộng. Để một phiên bản sau.

   ⚠️ XOÁ RỒI CHÈN. Script dọn sạch mảnh của site trước khi nạp, để chạy
      lại nhiều lần vẫn ra đúng một kết quả. Ai đã nhập tay dữ liệu lên
      site thì nó MẤT. Đây là script di trú một chiều từ CENTRAL.

   CHẠY Ở ĐÂU : CHỈ trên PTITONE_MASTER
                  .\run.ps1 -Script migrate\50-tach-du-lieu.sql -On MASTER
   SAU ĐÓ      : .\run.ps1 -Script migrate\51-doi-soat.sql -On MASTER
   ===================================================================== */

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

IF DB_NAME() <> N'$(DbMaster)'
BEGIN
    DECLARE @DbHienTai SYSNAME = DB_NAME();
    RAISERROR(N'Script nay CHI chay tren $(DbMaster). Database hien tai: %s', 16, 1, @DbHienTai);
    SET NOEXEC ON;
END
GO

IF DB_ID(N'$(DbCentral)') IS NULL
BEGIN
    RAISERROR(N'Khong thay $(DbCentral) tren instance nay. Script migrate doc CENTRAL bang ten hai phan.', 16, 1);
    SET NOEXEC ON;
END
GO

/* =====================================================================
   PHẦN A0 — ĐỒNG BỘ DANH MỤC TỪ CENTRAL LÊN MASTER

   `master/04-seed-danhmuc.sql` được viết ĐỘC LẬP với Phần 1, nên danh mục
   ở Master đã trôi khỏi danh mục mà dữ liệu thật đang tham chiếu. Cụ thể:
   Master thiếu học kỳ `2026-1`, trong khi gần như toàn bộ `SinhVienHocKy`
   và `DotDangKy` của Phần 1 nằm ở kỳ đó. Không có bước này thì Phần B
   chết ngay ở khoá ngoại — và đó chính là cách lỗi này lộ ra lần đầu.

   Nguồn sự thật cho dữ liệu Phần 1 là CENTRAL, nên danh mục phải đi từ
   CENTRAL LÊN Master, rồi Master nhân bản xuống các site. Seed Master
   riêng rẽ là cách bảo đảm hai bên lệch nhau.

   CHỈ THÊM DÒNG THIẾU, không sửa và không xoá dòng sẵn có: Master có thể
   đã có dữ liệu đúng mà CENTRAL không có (ví dụ khoa mới). Mục tiêu ở đây
   là làm Master thành TẬP CHA đủ để mọi khoá ngoại của CENTRAL thoả.

   ⚠️ KHÔNG đồng bộ `CoSo`. Bảng đó ở Master mang thêm `TenLinkedServer`
      và `TenDatabase` — cấu hình định tuyến của Phần 2 mà CENTRAL không
      hề có. Ghi đè lên nó là tự xoá topology của chính mình.
   ===================================================================== */
PRINT '';
PRINT '=== PHAN A0: dong bo danh muc CENTRAL -> Master ===================';

DECLARE @dm TABLE (ThuTu INT, Ten SYSNAME);
INSERT INTO @dm VALUES
 (1,'Khoa'), (2,'ChuongTrinhDaoTao'), (3,'MonHoc'),
 (4,'CTDT_MonHoc'), (5,'MonHocTienQuyet'), (6,'HocKy'), (7,'KhungGioTiet');

DECLARE @tenDM SYSNAME, @sqlDM NVARCHAR(MAX), @colsDM NVARCHAR(MAX), @khop NVARCHAR(MAX);

DECLARE dm CURSOR LOCAL FAST_FORWARD FOR SELECT Ten FROM @dm ORDER BY ThuTu;
OPEN dm;
FETCH NEXT FROM dm INTO @tenDM;

WHILE @@FETCH_STATUS = 0
BEGIN
    /* Cột chung giữa hai bên. */
    SELECT @colsDM = STRING_AGG(QUOTENAME(c.COLUMN_NAME), ', ')
                      WITHIN GROUP (ORDER BY c.ORDINAL_POSITION)
      FROM [$(DbCentral)].INFORMATION_SCHEMA.COLUMNS c
     WHERE c.TABLE_SCHEMA = 'dbo' AND c.TABLE_NAME = @tenDM
       AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS m
                    WHERE m.TABLE_SCHEMA = 'dbo' AND m.TABLE_NAME = @tenDM
                      AND m.COLUMN_NAME = c.COLUMN_NAME);

    /* Điều kiện so khớp = khoá chính của bảng ở Master. Lấy động thay vì
       gõ tay: bảy bảng, mỗi bảng một kiểu khoá, gõ tay là sai. */
    SELECT @khop = STRING_AGG(N't.' + QUOTENAME(col.name) + N' = s.' + QUOTENAME(col.name), N' AND ')
      FROM sys.key_constraints kc
      JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
      JOIN sys.columns col ON col.object_id = ic.object_id AND col.column_id = ic.column_id
     WHERE kc.type = 'PK' AND kc.parent_object_id = OBJECT_ID(N'dbo.' + QUOTENAME(@tenDM));

    IF @colsDM IS NULL OR @khop IS NULL
        THROW 53000, N'Khong dung duoc cau dong bo danh muc.', 1;

    SET @sqlDM = N'INSERT INTO dbo.' + QUOTENAME(@tenDM) + N' (' + @colsDM + N')
                   SELECT ' + @colsDM + N'
                     FROM [$(DbCentral)].dbo.' + QUOTENAME(@tenDM) + N' s
                    WHERE NOT EXISTS (SELECT 1 FROM dbo.' + QUOTENAME(@tenDM) + N' t
                                       WHERE ' + @khop + N');';
    EXEC sp_executesql @sqlDM;

    PRINT '  [+] ' + @tenDM + ': them ' + CONVERT(VARCHAR(10), @@ROWCOUNT) + ' dong thieu';
    FETCH NEXT FROM dm INTO @tenDM;
END
CLOSE dm; DEALLOCATE dm;
GO

/* =====================================================================
   PHẦN A — DANH BẠ ĐỊNH VỊ TẠI MASTER

   Phải nạp TRƯỚC các site: đây là bảng duy nhất biết người nào thuộc cơ
   sở nào, và `SiteContext` ở tầng ứng dụng dựa vào nó để định tuyến.
   ===================================================================== */
PRINT '';
PRINT '=== PHAN A: danh ba dinh vi tai Master ============================';

BEGIN TRANSACTION;

/* Chỉ xoá dòng do di trú sinh ra. Admin Master seed riêng ở master/04,
   KHÔNG được đụng tới. */
DELETE FROM dbo.DanhBaNguoiDung WHERE LoaiNguoiDung <> 'ADMIN_MASTER';

INSERT INTO dbo.DanhBaNguoiDung
       (TenDangNhap, MaCoSo, LoaiNguoiDung, MaThucThe, TrangThai, PhienBanTaiKhoan)
SELECT tk.TenDangNhap,
       tk.MaCoSo,
       tk.VaiTro,
       tk.MaThucThe,
       /* Tài khoản chưa có mật khẩu là tài khoản đã cấp nhưng chưa kích
          hoạt (F02). Bất biến của danh bạ: chỉ HOAT_DONG mới đăng nhập
          được, nên trạng thái phải phản ánh đúng điều đó. */
       CASE WHEN tk.MatKhauHash IS NULL THEN 'CHO_KICH_HOAT' ELSE 'HOAT_DONG' END,
       1
  FROM [$(DbCentral)].dbo.TaiKhoan tk;

PRINT '  [+] DanhBaNguoiDung: ' + CONVERT(VARCHAR(10), @@ROWCOUNT) + ' dong';

/* Tài khoản Admin Master cũng lấy từ CENTRAL, cùng lý do với danh bạ: CENTRAL
   là nguồn sự thật cho dữ liệu Phần 1, còn master/04 chỉ seed được một tài
   khoản CHƯA có mật khẩu. Không có bước này thì Admin Master đăng nhập vào
   Master là hỏng, và lỗi trả về chỉ là "sai mật khẩu". */
UPDATE m
   SET m.MatKhauHash          = c.MatKhauHash,
       m.HoTen                = c.HoTen,
       /* Email đi CÙNG hash, không phải chi tiết trang trí: Admin Master
          khôi phục mật khẩu qua thư, nên bỏ nó lại là lấy mất đường về của
          đúng tài khoản không ai cấp lại hộ được. */
       m.Email                = c.Email,
       m.EmailDaXacMinh       = c.EmailDaXacMinh,
       m.ThoiDiemXacMinhEmail = c.ThoiDiemXacMinhEmail
  FROM dbo.TaiKhoanMaster m
  JOIN [$(DbCentral)].dbo.TaiKhoanMaster c ON c.TenDangNhap = m.TenDangNhap
 WHERE m.MatKhauHash IS NULL
    OR m.MatKhauHash <> c.MatKhauHash
    OR ISNULL(m.Email, '') <> ISNULL(c.Email, '')
    OR m.EmailDaXacMinh <> c.EmailDaXacMinh;

PRINT '  [+] TaiKhoanMaster: dong bo ' + CONVERT(VARCHAR(10), @@ROWCOUNT) + ' dong tu CENTRAL';

INSERT INTO dbo.TaiKhoanMaster (TenDangNhap, MatKhauHash, VaiTro, HoTen,
                                Email, EmailDaXacMinh, ThoiDiemXacMinhEmail)
SELECT c.TenDangNhap, c.MatKhauHash, 'ADMIN_MASTER', c.HoTen,
       c.Email, c.EmailDaXacMinh, c.ThoiDiemXacMinhEmail
  FROM [$(DbCentral)].dbo.TaiKhoanMaster c
 WHERE NOT EXISTS (SELECT 1 FROM dbo.TaiKhoanMaster m WHERE m.TenDangNhap = c.TenDangNhap)
   AND EXISTS (SELECT 1 FROM dbo.DanhBaNguoiDung d WHERE d.TenDangNhap = c.TenDangNhap);

PRINT '  [+] TaiKhoanMaster: them ' + CONVERT(VARCHAR(10), @@ROWCOUNT) + ' tai khoan thieu';

COMMIT TRANSACTION;
GO

/* =====================================================================
   PHẦN B — MẢNH VẬN HÀNH VỀ TỪNG CƠ SỞ

   Danh sách cột lấy bằng cách GIAO tập cột của CENTRAL với tập cột của
   site đích, đọc từ INFORMATION_SCHEMA của chính site đó. Liệt kê tay thì
   script mục ngay lần schema thêm cột — đã vấp đúng lỗi đó khi viết
   sp_ChuyenCoSoSinhVien (thiếu TranTinChi và MaCoSoHost).
   ===================================================================== */
PRINT '';
PRINT '=== PHAN B: tach manh van hanh ve tung co so ======================';

DECLARE @bang TABLE (ThuTu INT, Ten SYSNAME, DieuKien NVARCHAR(1000));

/* Thứ tự chèn = thứ tự khoá ngoại. Xoá thì đi ngược lại. */
INSERT INTO @bang (ThuTu, Ten, DieuKien) VALUES
 (1, 'SinhVien',      N'MaCoSoNha = @site'),
 (2, 'GiangVien',     N'MaCoSo = @site'),
 (3, 'TaiKhoan',      N'MaCoSo = @site'),
 (4, 'SinhVienHocKy', N'MaSinhVien IN (SELECT MaSinhVien FROM [$(DbCentral)].dbo.SinhVien WHERE MaCoSoNha = @site)'),
 (5, 'DotDangKy',     N'MaCoSo = @site'),
 (6, 'LopHocPhan',    N'MaCoSoHost = @site'),
 (7, 'LichHoc',       N'MaLopHP IN (SELECT MaLopHP FROM [$(DbCentral)].dbo.LopHocPhan WHERE MaCoSoHost = @site)'),
 /* ⚠️ Theo LỚP, không theo sinh viên. */
 (8, 'DangKyHocPhan', N'MaLopHP IN (SELECT MaLopHP FROM [$(DbCentral)].dbo.LopHocPhan WHERE MaCoSoHost = @site)'),
 (9, 'DangKyMonHoc',  N'MaSinhVien IN (SELECT MaSinhVien FROM [$(DbCentral)].dbo.SinhVien WHERE MaCoSoNha = @site)'),
 /* Dẫn xuất bậc 2: đi theo DangKyHocPhan, tức cũng theo lớp. */
 (10,'Diem',          N'MaLopHP IN (SELECT MaLopHP FROM [$(DbCentral)].dbo.LopHocPhan WHERE MaCoSoHost = @site)');

DECLARE @site VARCHAR(10), @lnk SYSNAME, @db SYSNAME, @pre NVARCHAR(300);

DECLARE cs CURSOR LOCAL FAST_FORWARD FOR
    SELECT MaCoSo, TenLinkedServer, TenDatabase
      FROM dbo.CoSo WHERE DangHoatDong = 1 ORDER BY MaCoSo;
OPEN cs;
FETCH NEXT FROM cs INTO @site, @lnk, @db;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @pre = CASE WHEN @lnk IS NULL
                    THEN QUOTENAME(@db) + N'.'
                    ELSE QUOTENAME(@lnk) + N'.' + QUOTENAME(@db) + N'.' END;

    PRINT '';
    PRINT '--- co so ' + @site + '  (' + @pre + ') ---';

    BEGIN TRY
        DECLARE @ten SYSNAME, @dk NVARCHAR(1000), @sql NVARCHAR(MAX), @cols NVARCHAR(MAX);

        /* --- Dọn các bảng KHÔNG chuyển nhưng PHẢI xoá ---
           Phiên đăng nhập và mã xác thực không đi theo dữ liệu (xem đầu file),
           nhưng chúng trỏ khoá ngoại vào TaiKhoan. Không xoá trước thì
           DELETE TaiKhoan bị chặn bởi FK_PhienDangNhap_TaiKhoan — và vì mỗi
           lần ai đó đăng nhập thử là lại sinh một dòng phiên, lỗi này chỉ xuất
           hiện ở lần chạy lại THỨ HAI, lúc đã quên mất vì sao.
           Xoá phiên là đúng nghiệp vụ: dữ liệu vừa đổi chỗ thì mọi phiên cũ
           phải mất hiệu lực. */
        SET @sql = N'
            DELETE FROM ' + @pre + N'dbo.TokenLamMoi;
            DELETE FROM ' + @pre + N'dbo.PhienDangNhap;
            DELETE FROM ' + @pre + N'dbo.MaXacThuc;
            DELETE FROM ' + @pre + N'dbo.MaKichHoat;';
        EXEC sp_executesql @sql;

        /* --- Dọn theo chiều NGƯỢC khoá ngoại --- */
        DECLARE xoa CURSOR LOCAL FAST_FORWARD FOR
            SELECT Ten FROM @bang ORDER BY ThuTu DESC;
        OPEN xoa;
        FETCH NEXT FROM xoa INTO @ten;
        WHILE @@FETCH_STATUS = 0
        BEGIN
            SET @sql = N'DELETE FROM ' + @pre + N'dbo.' + QUOTENAME(@ten) + N';';
            EXEC sp_executesql @sql;
            FETCH NEXT FROM xoa INTO @ten;
        END
        CLOSE xoa; DEALLOCATE xoa;

        /* --- Nạp theo chiều XUÔI khoá ngoại --- */
        DECLARE nap CURSOR LOCAL FAST_FORWARD FOR
            SELECT Ten, DieuKien FROM @bang ORDER BY ThuTu;
        OPEN nap;
        FETCH NEXT FROM nap INTO @ten, @dk;
        WHILE @@FETCH_STATUS = 0
        BEGIN
            /* Giao tập cột giữa CENTRAL và site đích. */
            SET @cols = NULL;
            SET @sql = N'
                SELECT @c = STRING_AGG(QUOTENAME(x.COLUMN_NAME), '', '')
                             WITHIN GROUP (ORDER BY x.ORDINAL_POSITION)
                  FROM (SELECT c.COLUMN_NAME, c.ORDINAL_POSITION
                          FROM [$(DbCentral)].INFORMATION_SCHEMA.COLUMNS c
                         WHERE c.TABLE_SCHEMA = ''dbo'' AND c.TABLE_NAME = @t) x
                 WHERE EXISTS (SELECT 1 FROM ' + @pre + N'INFORMATION_SCHEMA.COLUMNS s
                                WHERE s.TABLE_SCHEMA = ''dbo'' AND s.TABLE_NAME = @t
                                  AND s.COLUMN_NAME = x.COLUMN_NAME);';
            EXEC sp_executesql @sql, N'@t SYSNAME, @c NVARCHAR(MAX) OUTPUT',
                 @t = @ten, @c = @cols OUTPUT;

            IF @cols IS NULL
                THROW 53001, N'Khong tim thay cot chung — site thieu bang?', 1;

            SET @sql = N'INSERT INTO ' + @pre + N'dbo.' + QUOTENAME(@ten) + N' (' + @cols + N')
                         SELECT ' + @cols + N' FROM [$(DbCentral)].dbo.' + QUOTENAME(@ten) + N'
                          WHERE ' + @dk + N';';
            EXEC sp_executesql @sql, N'@site VARCHAR(10)', @site = @site;

            PRINT '  [+] ' + @ten + ': ' + CONVERT(VARCHAR(10), @@ROWCOUNT) + ' dong';
            FETCH NEXT FROM nap INTO @ten, @dk;
        END
        CLOSE nap; DEALLOCATE nap;
    END TRY
    BEGIN CATCH
        /* Một cơ sở chưa lên không được chặn các cơ sở còn lại. */
        IF CURSOR_STATUS('local', 'xoa') >= 0 BEGIN CLOSE xoa; DEALLOCATE xoa; END
        IF CURSOR_STATUS('local', 'nap') >= 0 BEGIN CLOSE nap; DEALLOCATE nap; END
        PRINT '  [!] BO QUA co so ' + @site + ': ' + ERROR_MESSAGE();
    END CATCH

    FETCH NEXT FROM cs INTO @site, @lnk, @db;
END
CLOSE cs; DEALLOCATE cs;
GO

PRINT '';
PRINT '-------------------------------------------------------------------';
PRINT '  Da tach du lieu. BAT BUOC doi soat truoc khi tin ket qua:';
PRINT '    .\run.ps1 -Script migrate\51-doi-soat.sql -On MASTER';
PRINT '-------------------------------------------------------------------';
GO

SET NOEXEC OFF;
GO
