package vn.ptit.one.auth.model;

import java.util.Locale;

/**
 * Luật mật khẩu dùng chung cho kích hoạt, đổi và khôi phục mật khẩu. Độ dài
 * 8–128 đã kiểm ở DTO; trần 128 để không ai bắt Argon2 băm chuỗi cực dài.
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static boolean containsUsername(String username, String password) {
        return password.toLowerCase(Locale.ROOT).contains(username.trim().toLowerCase(Locale.ROOT));
    }
}
