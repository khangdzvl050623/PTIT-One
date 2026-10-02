package vn.ptit.one.auth.model;

import java.util.Locale;

/**
 * Email của một tài khoản và nơi lưu nó.
 *
 * @param source      {@code TaiKhoan} (SITE) hay {@code TaiKhoanMaster} (MASTER)
 * @param hasPassword {@code false} khi chưa kích hoạt
 */
public record AccountContact(
        String username,
        AccountRecord.Source source,
        String status,
        boolean hasPassword,
        String email,
        boolean emailVerified) {

    /** Email người dùng gõ chỉ để ĐỐI CHIẾU; mã luôn gửi tới email đã lưu. */
    public boolean verifiedEmailMatches(String presented) {
        return emailVerified && email != null && presented != null
                && email.equalsIgnoreCase(presented.trim());
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
