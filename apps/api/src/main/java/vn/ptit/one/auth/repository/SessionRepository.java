package vn.ptit.one.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.model.SessionSnapshot;

/** {@code PhienDangNhap}: mỗi lần đăng nhập một dòng, cũng là một token family. */
@Repository
@Profile("central")
public class SessionRepository {

    private static final String INSERT = """
            INSERT INTO dbo.PhienDangNhap
                   (MaPhien, TenDangNhap, PhienBanLucTao, ThoiDiemTao, ThoiDiemHetHan)
            VALUES (?, ?, ?, ?, ?)
            """;

    /* Hai lượt tìm theo khóa chính — rẻ đủ để chạy ở mỗi request. */
    private static final String FIND_SNAPSHOT = """
            SELECT p.MaPhien, p.TenDangNhap, p.ThoiDiemHetHan, p.ThoiDiemThuHoi,
                   d.LoaiNguoiDung, d.MaCoSo, d.MaThucThe, d.TrangThai, d.PhienBanTaiKhoan
              FROM dbo.PhienDangNhap p
              JOIN dbo.DanhBaNguoiDung d ON d.TenDangNhap = p.TenDangNhap
             WHERE p.MaPhien = ?
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
        return jdbc.query(FIND_SNAPSHOT, (rs, rowNum) -> new SessionSnapshot(
                        UUID.fromString(rs.getString("MaPhien")),
                        rs.getString("TenDangNhap"),
                        SqlTime.fromDb(rs, "ThoiDiemHetHan"),
                        SqlTime.fromDb(rs, "ThoiDiemThuHoi"),
                        Role.valueOf(rs.getString("LoaiNguoiDung")),
                        rs.getString("MaCoSo"),
                        rs.getString("MaThucThe"),
                        rs.getString("TrangThai"),
                        rs.getInt("PhienBanTaiKhoan")),
                sessionId.toString()).stream().findFirst();
    }
}
