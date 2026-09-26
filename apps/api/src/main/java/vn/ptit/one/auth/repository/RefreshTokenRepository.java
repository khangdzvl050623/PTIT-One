package vn.ptit.one.auth.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** {@code TokenLamMoi}: chỉ lưu SHA-256, không bao giờ lưu token gốc. */
@Repository
@Profile("central")
public class RefreshTokenRepository {

    private static final String INSERT = """
            INSERT INTO dbo.TokenLamMoi
                   (MaToken, MaPhien, TokenHash, ThoiDiemTao, ThoiDiemHetHan)
            VALUES (?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;

    public RefreshTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID tokenId, UUID sessionId, byte[] tokenHash, Instant createdAt, Instant expiresAt) {
        jdbc.update(INSERT, tokenId.toString(), sessionId.toString(), tokenHash,
                SqlTime.toDb(createdAt), SqlTime.toDb(expiresAt));
    }
}
