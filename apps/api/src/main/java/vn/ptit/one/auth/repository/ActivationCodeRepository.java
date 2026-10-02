package vn.ptit.one.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.auth.model.ActivationCodeRecord;

/**
 * {@code MaKichHoat}: chỉ lưu SHA-256, không lưu mã gốc.
 *
 * <p>Phần 2: cục bộ tại site nhà, cùng chỗ với {@code TaiKhoan}.
 */
@Repository
@Profile("central")
public class ActivationCodeRepository {

    private static final String INSERT = """
            INSERT INTO dbo.MaKichHoat (MaKichHoat, TenDangNhap, MaHash, ThoiDiemTao, ThoiDiemHetHan)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String REVOKE_LIVE = """
            UPDATE dbo.MaKichHoat
               SET ThoiDiemThuHoi = ?
             WHERE TenDangNhap = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
            """;

    /* UPDLOCK: hai lần nhập cùng lúc xếp hàng ở đây, nên bộ đếm sai và việc
       "dùng một lần" không bị hai request cùng vượt qua. */
    private static final String LOCK_LIVE = """
            SELECT MaKichHoat, MaHash, ThoiDiemHetHan, SoLanSai
              FROM dbo.MaKichHoat WITH (UPDLOCK, ROWLOCK)
             WHERE TenDangNhap = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
            """;

    /* Lần sai thứ maxAttempts thu hồi luôn mã — phải cấp mã mới. */
    private static final String RECORD_FAILURE = """
            UPDATE dbo.MaKichHoat
               SET SoLanSai = SoLanSai + 1,
                   ThoiDiemThuHoi = CASE WHEN SoLanSai + 1 >= ? THEN ? ELSE NULL END
             WHERE MaKichHoat = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
            """;

    private static final String MARK_USED = """
            UPDATE dbo.MaKichHoat
               SET ThoiDiemDaDung = ?
             WHERE MaKichHoat = ? AND ThoiDiemDaDung IS NULL AND ThoiDiemThuHoi IS NULL
            """;

    private final JdbcTemplate jdbc;

    public ActivationCodeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID codeId, String username, byte[] hash, Instant createdAt, Instant expiresAt) {
        jdbc.update(INSERT, codeId.toString(), username, hash, SqlTime.toDb(createdAt), SqlTime.toDb(expiresAt));
    }

    public int revokeLive(String username, Instant now) {
        return jdbc.update(REVOKE_LIVE, SqlTime.toDb(now), username);
    }

    public Optional<ActivationCodeRecord> lockLive(String username) {
        return jdbc.query(LOCK_LIVE, (rs, rowNum) -> new ActivationCodeRecord(
                        UUID.fromString(rs.getString("MaKichHoat")),
                        rs.getBytes("MaHash"),
                        SqlTime.fromDb(rs, "ThoiDiemHetHan"),
                        rs.getInt("SoLanSai")),
                username).stream().findFirst();
    }

    public int recordFailure(UUID codeId, int maxAttempts, Instant now) {
        return jdbc.update(RECORD_FAILURE, maxAttempts, SqlTime.toDb(now), codeId.toString());
    }

    public int markUsed(UUID codeId, Instant now) {
        return jdbc.update(MARK_USED, SqlTime.toDb(now), codeId.toString());
    }
}
