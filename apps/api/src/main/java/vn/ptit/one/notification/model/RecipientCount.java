package vn.ptit.one.notification.model;

/** Số người nhận: dự kiến với bản nháp, đã chốt với bản đã gửi. */
public record RecipientCount(int soSinhVien, int soGiangVien) {

    public int tong() {
        return soSinhVien + soGiangVien;
    }
}
