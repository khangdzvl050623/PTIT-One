package vn.ptit.one.auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Tài khoản đã giữ chỗ trong danh bạ Master, chờ worker dựng ở cơ sở nhà
 * (Phần 2). Đi trong nội dung sự kiện Outbox, nên chỉ mang HASH của mã kích
 * hoạt — mã gốc chỉ xuất hiện một lần, trong response cấp hồ sơ.
 */
public record ReservedAccount(
        String username,
        Role role,
        String maCoSo,
        String email,
        UUID codeId,
        byte[] codeHash,
        Instant codeCreatedAt,
        Instant codeExpiresAt,
        String codeRecipient,
        String nguoiCap) {
}
