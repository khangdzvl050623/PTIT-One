package vn.ptit.one.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.auth.model.VerificationCodeRecord;
import vn.ptit.one.auth.model.VerificationPurpose;

/**
 * {@code MaXacThuc}: mã 6 số xác minh email / khôi phục mật khẩu. Chỉ lưu HMAC.
 *
 * <p>Phần 2: cục bộ tại site nhà (Master với Admin Master), không nhân bản.
 */
@Repository
@Profile("central")
public class VerificationCodeRepository {

    private static final String INSERT = """
            INSERT INTO dbo.MaXacThuc (MaXacThuc, TenDangNhap, MucDich, Email, MaHash, ThoiDiemTao, ThoiDiemHetHan)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String REVOKE_LIVE = """
            UPDATE dbo.MaXacThuc SET ThoiDiemThuHoi = ?
             WHERE TenDangNhap = ? AND MucDich = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
            """;

    /* UPDLOCK: hai lần nhập cùng lúc xếp hàng, bộ đếm sai và "dùng một lần" không bị vượt. */
    private static final String LOCK_LIVE = """
            SELECT MaXacThuc, Email, MaHash, ThoiDiemHetHan, SoLanSai
              FROM dbo.MaXacThuc WITH (UPDLOCK, ROWLOCK)
             WHERE TenDangNhap = ? AND MucDich = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
            """;

    private static final String RECORD_FAILURE = """
            UPDATE dbo.MaXacThuc
               SET SoLanSai = SoLanSai + 1,
                   ThoiDiemThuHoi = CASE WHEN SoLanSai + 1 >= ? THEN ? ELSE NULL END
             WHERE MaXacThuc = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
            """;

    private static final String MARK_USED = """
            UPDATE dbo.MaXacThuc SET ThoiDiemDaDung = ?
             WHERE MaXacThuc = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
            """;

    private final JdbcTemplate jdbc;

    public VerificationCodeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID codeId, String username, VerificationPurpose purpose, String email, byte[] hash,
            Instant createdAt, Instant expiresAt) {
        jdbc.update(INSERT, codeId.toString(), username, purpose.name(), email, hash,
                SqlTime.toDb(createdAt), SqlTime.toDb(expiresAt));
    }

    public int revokeLive(String username, VerificationPurpose purpose, Instant now) {
        return jdbc.update(REVOKE_LIVE, SqlTime.toDb(now), username, purpose.name());
    }

    public Optional<VerificationCodeRecord> lockLive(String username, VerificationPurpose purpose) {
        return jdbc.query(LOCK_LIVE, (rs, rowNum) -> new VerificationCodeRecord(
                        UUID.fromString(rs.getString("MaXacThuc")),
                        rs.getString("Email"),
                        rs.getBytes("MaHash"),
                        SqlTime.fromDb(rs, "ThoiDiemHetHan"),
                        rs.getInt("SoLanSai")),
                username, purpose.name()).stream().findFirst();
    }

    public int recordFailure(UUID codeId, int maxAttempts, Instant now) {
        return jdbc.update(RECORD_FAILURE, maxAttempts, SqlTime.toDb(now), codeId.toString());
    }

    public int markUsed(UUID codeId, Instant now) {
        return jdbc.update(MARK_USED, SqlTime.toDb(now), codeId.toString());
    }
}
