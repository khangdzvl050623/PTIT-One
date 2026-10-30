package vn.ptit.one.notification.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Nội dung sự kiện {@code ThongBaoDaGui} trong Outbox của Master: thông báo
 * Admin Master vừa gửi, kèm danh sách người nhận ĐÃ CHỐT lúc gửi. Mỗi cơ sở
 * chỉ ghi phần người nhận có hồ sơ tại đó.
 */
public record NotificationSent(
        UUID maThongBao,
        String mucDo,
        String tieuDe,
        String noiDung,
        String lienKet,
        String phamVi,
        String maCoSo,
        String maLopHP,
        String doiTuong,
        String nguoiTao,
        Instant ngayGui,
        List<Recipient> nguoiNhan) {

    public static final String LOAI = "ThongBaoDaGui";
}
