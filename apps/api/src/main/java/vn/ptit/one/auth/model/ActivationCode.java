package vn.ptit.one.auth.model;

import java.time.Instant;

/**
 * Mã kích hoạt vừa cấp. DB không giữ mã gốc nên không xem lại được — mất thì
 * cấp mã mới.
 *
 * @param maKichHoat mã gốc, trả cho Admin Master ĐÚNG MỘT LẦN; {@code null} khi
 *                   mã chỉ đi qua thư tới {@code guiToiEmail} — Admin không thấy
 *                   mã, nên kích hoạt được cũng chứng minh người dùng sở hữu email
 * @param guiToiEmail địa chỉ nhận thư; {@code null} khi Admin tự trao mã
 */
public record ActivationCode(String tenDangNhap, String maKichHoat, Instant hetHan, String guiToiEmail) {
}
