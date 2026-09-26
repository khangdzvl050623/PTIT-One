package vn.ptit.one.auth.model;

/** Bốn vai trò, trùng tên với {@code LoaiNguoiDung}/{@code VaiTro} trong DB. */
public enum Role {
    SINH_VIEN,
    GIANG_VIEN,
    ADMIN_CO_SO,
    ADMIN_MASTER;

    /** Tên authority Spring Security, dùng với {@code hasRole("...")}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
