package vn.ptit.one.notification.model;

/**
 * Người nhận theo thực thể, không theo tài khoản.
 *
 * @param loai {@code SINH_VIEN} hoặc {@code GIANG_VIEN}
 * @param ma   mã sinh viên / mã giảng viên
 */
public record Recipient(String loai, String ma) {

    public static Recipient student(String maSinhVien) {
        return new Recipient(NotificationTerms.SINH_VIEN, maSinhVien);
    }

    public static Recipient teacher(String maGiangVien) {
        return new Recipient(NotificationTerms.GIANG_VIEN, maGiangVien);
    }
}
