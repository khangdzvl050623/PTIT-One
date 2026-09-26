package vn.ptit.one.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.auth.model.RefreshTokenRecord;

/** {@code TokenLamMoi}: chỉ lưu SHA-256, không bao giờ lưu token gốc. */
@Repository
@Profile("central")
public class RefreshTokenRepository {

    private static final String INSERT = """
            INSERT INTO dbo.TokenLamMoi
                   (MaToken, MaPhien, TokenHash, ThoiDiemTao, ThoiDiemHetHan)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String FIND_BY_HASH = """
            SELECT MaToken, MaPhien, ThoiDiemHetHan, ThoiDiemDaDung, ThoiDiemThuHoi
              FROM dbo.TokenLamMoi
             WHERE TokenHash = ?
            """;

    /* Điều kiện "chưa dùng" nằm TRONG câu UPDATE, rồi đọc số dòng: bằng 0 nghĩa
       là đã có người rotate trước — không bao giờ có hai successor. */
    private static final String MARK_USED = """
            UPDATE dbo.TokenLamMoi
               SET ThoiDiemDaDung = ?
             WHERE MaToken = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
            """;

    private static final String REVOKE_ALL_FOR_SESSION = """
            UPDATE dbo.TokenLamMoi
               SET ThoiDiemThuHoi = ?
             WHERE MaPhien = ? AND ThoiDiemThuHoi IS NULL
            """;

    private static final String REVOKE_ALL_FOR_USER = """
            UPDATE t
               SET t.ThoiDiemThuHoi = ?
              FROM dbo.TokenLamMoi t
              JOIN dbo.PhienDangNhap p ON p.MaPhien = t.MaPhien
             WHERE p.TenDangNhap = ? AND t.ThoiDiemThuHoi IS NULL
            """;

    private final JdbcTemplate jdbc;

    public RefreshTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID tokenId, UUID sessionId, byte[] tokenHash, Instant createdAt, Instant expiresAt) {
        jdbc.update(INSERT, tokenId.toString(), sessionId.toString(), tokenHash,
                SqlTime.toDb(createdAt), SqlTime.toDb(expiresAt));
    }

    public Optional<RefreshTokenRecord> findByHash(byte[] tokenHash) {
        return jdbc.query(FIND_BY_HASH, (rs, rowNum) -> new RefreshTokenRecord(
                        UUID.fromString(rs.getString("MaToken")),
                        UUID.fromString(rs.getString("MaPhien")),
                        SqlTime.fromDb(rs, "ThoiDiemHetHan"),
                        SqlTime.fromDb(rs, "ThoiDiemDaDung"),
                        SqlTime.fromDb(rs, "ThoiDiemThuHoi")),
                (Object) tokenHash).stream().findFirst();
    }

    /** @return 1 nếu rotate được, 0 nếu token đã dùng/thu hồi */
    public int markUsed(UUID tokenId, Instant now) {
        return jdbc.update(MARK_USED, SqlTime.toDb(now), tokenId.toString());
    }

    public int revokeAllForSession(UUID sessionId, Instant now) {
        return jdbc.update(REVOKE_ALL_FOR_SESSION, SqlTime.toDb(now), sessionId.toString());
    }

    public int revokeAllForUser(String username, Instant now) {
        return jdbc.update(REVOKE_ALL_FOR_USER, SqlTime.toDb(now), username);
    }
}
