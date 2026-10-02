/* PTIT One — V3: mỗi cơ sở chỉ có MỘT đợt đăng ký mở trong một học kỳ.

   Quyết định nhóm 02/10/2026: "đợt đang mở" = TrangThai = 'DANG_MO' VÀ thời
   điểm hiện tại nằm trong [ThoiGianMo, ThoiGianDong]. Hai điều kiện cùng phải
   đúng; ứng dụng kiểm vế thời gian vì filtered index không nhận hàm không tất
   định như SYSUTCDATETIME().

   Index dưới đây chặn vế còn lại: không thể có hai dòng DANG_MO cho cùng một
   (cơ sở, học kỳ). Muốn mở đợt bổ sung thì phải đóng đợt cũ trước.

   Phần 2: DotDangKy là bảng CỤC BỘ tại site, KHÔNG nhân bản — mỗi cơ sở tự
   quyết lịch đăng ký của mình. Index này vì vậy chỉ có hiệu lực trong site,
   đúng như ngữ nghĩa mong muốn. */

CREATE UNIQUE NONCLUSTERED INDEX UQ_DotDangKy_MotDotMo
    ON dbo.DotDangKy (MaCoSo, MaHocKy)
    WHERE TrangThai = 'DANG_MO';
GO
