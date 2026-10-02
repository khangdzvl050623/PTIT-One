package vn.ptit.one.auth.model;

import java.time.Instant;

/**
 * Mã kích hoạt vừa cấp, trả cho Admin Master ĐÚNG MỘT LẦN. DB không giữ mã
 * gốc nên không xem lại được — mất thì cấp mã mới.
 */
public record ActivationCode(String tenDangNhap, String maKichHoat, Instant hetHan) {
}
