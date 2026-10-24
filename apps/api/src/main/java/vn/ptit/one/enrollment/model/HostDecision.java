package vn.ptit.one.enrollment.model;

/**
 * Quyết định của Host cho một {@code MaYeuCau}, đọc từ {@code KetQuaXuLyYeuCau}.
 *
 * <p>⭐ Bảng đó lưu <b>mọi</b> kết quả, <b>kể cả từ chối</b>. Đây là điểm mấu
 * chốt, không phải chi tiết vặt: nếu chỉ lưu các yêu cầu thành công thì khi Host
 * từ chối (lớp đầy) mà phản hồi mất trên đường về, Home retry → Host đánh giá
 * lại từ đầu → lúc này có người vừa huỷ lớp → <b>kết quả khác lần trước</b>.
 * Retry trở thành không tất định: 5 lần retry là 5 lần đánh giá độc lập, và sinh
 * viên có thể "được duyệt" ở lần thứ 3 cho một yêu cầu đã bị từ chối ở lần thứ
 * nhất.
 *
 * <p>Lưu cả từ chối biến nó thành <b>Idempotent Receiver có lưu phản hồi</b>:
 * retry luôn trả về đúng quyết định đã ghi.
 *
 * @param ketQua một trong {@code DA_DANG_KY} · {@code TU_CHOI} · {@code DA_HUY}
 * @param maLoi  mã máy đọc được, {@code null} khi duyệt. Để Home map sang thông
 *               báo cho người dùng mà không phải so khớp chuỗi tiếng Việt
 */
public record HostDecision(String ketQua, String maLoi, String lyDo) {

    public static HostDecision duyet() {
        return new HostDecision(EnrollmentStatus.DA_DANG_KY, null, null);
    }

    public static HostDecision tuChoi(String maLoi, String lyDo) {
        return new HostDecision(EnrollmentStatus.TU_CHOI, maLoi, lyDo);
    }

    public static HostDecision daHuy() {
        return new HostDecision(EnrollmentStatus.DA_HUY, null, null);
    }

    public boolean duocDuyet() {
        return EnrollmentStatus.DA_DANG_KY.equals(ketQua);
    }
}
