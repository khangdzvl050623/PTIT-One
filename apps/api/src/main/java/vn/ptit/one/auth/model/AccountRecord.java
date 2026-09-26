package vn.ptit.one.auth.model;

import java.util.Objects;

/**
 * Tài khoản đọc cho việc đăng nhập: dòng danh bạ + thông tin đăng nhập.
 *
 * <p>Danh bạ là nguồn authoritative cho vai trò, cơ sở, thực thể, trạng thái
 * và phiên bản. {@code TaiKhoan}/{@code TaiKhoanMaster} phải khớp danh bạ;
 * lệch thì TỪ CHỐI, không tự chọn bên có quyền cao hơn.
 *
 * @param credential {@code null} khi danh bạ có dòng nhưng không có tài khoản
 */
public record AccountRecord(
        String username,
        Role role,
        String campus,
        String entityId,
        String status,
        int version,
        Credential credential) {

    public static final String ACTIVE = "HOAT_DONG";

    /**
     * Dòng mật khẩu đọc từ {@code TaiKhoan} (SV/GV/Admin cơ sở) hoặc
     * {@code TaiKhoanMaster}.
     *
     * @param source bảng nguồn; có dòng ở cả hai bảng thì {@code BOTH}
     */
    public record Credential(Source source, String passwordHash, Role role, String campus,
            String entityId, boolean active) {
    }

    public enum Source { SITE, MASTER, BOTH }

    public boolean canSignIn() {
        return ACTIVE.equals(status) && credentialMatchesDirectory();
    }

    public boolean credentialMatchesDirectory() {
        if (credential == null || credential.passwordHash() == null || !credential.active()) {
            return false;
        }
        if (role == Role.ADMIN_MASTER) {
            return credential.source() == Source.MASTER && credential.role() == Role.ADMIN_MASTER;
        }
        return credential.source() == Source.SITE
                && credential.role() == role
                && Objects.equals(credential.campus(), campus)
                && Objects.equals(credential.entityId(), entityId);
    }
}
