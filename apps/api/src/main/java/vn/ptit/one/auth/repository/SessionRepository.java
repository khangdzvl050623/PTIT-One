package vn.ptit.one.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.model.SessionSnapshot;

/**
 * {@code PhienDangNhap}: mỗi lần đăng nhập một dòng, cũng là một token family.
 *
 * <p>Thứ tự khóa trong module auth: {@code PhienDangNhap} → {@code TokenLamMoi}
 * → {@code DanhBaNguoiDung}/{@code TaiKhoan}, ở MỌI luồng (refresh, logout,
 * logout-all, sau này khóa tài khoản/đổi mật khẩu). Refresh giữ khóa dòng phiên
 * trong lúc đọc danh bạ; luồng nào khóa danh bạ trước rồi mới thu hồi phiên sẽ
 * deadlock với nó.
 */
@Repository
@Profile("central")
public class SessionRepository {

    private static final String INSERT = """
            INSERT INTO dbo.PhienDangNhap
                   (MaPhien, TenDangNhap, PhienBanLucTao, ThoiDiemTao, ThoiDiemHetHan)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SNAPSHOT_COLUMNS = """
            SELECT p.MaPhien, p.TenDangNhap, p.ThoiDiemHetHan, p.ThoiDiemThuHoi, p.PhienBanLucTao,
                   d.LoaiNguoiDung, d.MaCoSo, d.MaThucThe, d.TrangThai, d.PhienBanTaiKhoan
            """;

    /* Hai lượt tìm theo khóa chính — rẻ đủ để chạy ở mỗi request. */
    private static final String FIND_SNAPSHOT = SNAPSHOT_COLUMNS + """
              FROM dbo.PhienDangNhap p
              JOIN dbo.DanhBaNguoiDung d ON d.TenDangNhap = p.TenDangNhap
             WHERE p.MaPhien = ?
            """;

    /* UPDLOCK trên dòng phiên: hai refresh cùng phiên xếp hàng tại đây, người
       sau thấy token đã dùng. Không khóa DanhBa để không đảo thứ tự với logout-all. */
    private static final String LOCK_SNAPSHOT = SNAPSHOT_COLUMNS + """
              FROM dbo.PhienDangNhap p WITH (UPDLOCK, ROWLOCK)
              JOIN dbo.DanhBaNguoiDung d ON d.TenDangNhap = p.TenDangNhap
             WHERE p.MaPhien = ?
            """;

    private static final String REVOKE = """
            UPDATE dbo.PhienDangNhap
               SET ThoiDiemThuHoi = ?, LyDoThuHoi = ?
             WHERE MaPhien = ? AND ThoiDiemThuHoi IS NULL
            """;

    private static final String REVOKE_ALL_FOR_USER = """
            UPDATE dbo.PhienDangNhap
               SET ThoiDiemThuHoi = ?, LyDoThuHoi = ?
             WHERE TenDangNhap = ? AND ThoiDiemThuHoi IS NULL
            """;

    private final JdbcTemplate jdbc;

    public SessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID sessionId, String username, int accountVersion, Instant createdAt, Instant expiresAt) {
        jdbc.update(INSERT, sessionId.toString(), username, accountVersion,
                SqlTime.toDb(createdAt), SqlTime.toDb(expiresAt));
    }

    public Optional<SessionSnapshot> findSnapshot(UUID sessionId) {
        return jdbc.query(FIND_SNAPSHOT, (rs, rowNum) -> map(rs), sessionId.toString()).stream().findFirst();
    }

    /** Chỉ gọi trong transaction; khóa giữ tới commit. */
    public Optional<SessionSnapshot> lockSnapshot(UUID sessionId) {
        return jdbc.query(LOCK_SNAPSHOT, (rs, rowNum) -> map(rs), sessionId.toString()).stream().findFirst();
    }

    /** @return số phiên thực sự bị thu hồi (0 nếu đã thu hồi từ trước) */
    public int revoke(UUID sessionId, Instant now, String reason) {
        return jdbc.update(REVOKE, SqlTime.toDb(now), reason, sessionId.toString());
    }

    public int revokeAllForUser(String username, Instant now, String reason) {
        return jdbc.update(REVOKE_ALL_FOR_USER, SqlTime.toDb(now), reason, username);
    }

    private static SessionSnapshot map(ResultSet rs) throws SQLException {
        return new SessionSnapshot(
                UUID.fromString(rs.getString("MaPhien")),
                rs.getString("TenDangNhap"),
                SqlTime.fromDb(rs, "ThoiDiemHetHan"),
                SqlTime.fromDb(rs, "ThoiDiemThuHoi"),
                Role.valueOf(rs.getString("LoaiNguoiDung")),
                rs.getString("MaCoSo"),
                rs.getString("MaThucThe"),
                rs.getString("TrangThai"),
                rs.getInt("PhienBanTaiKhoan"),
                rs.getInt("PhienBanLucTao"));
    }
}
