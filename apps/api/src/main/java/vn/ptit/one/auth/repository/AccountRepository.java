package vn.ptit.one.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AccountSummary;
import vn.ptit.one.auth.model.AccountRecord.Credential;
import vn.ptit.one.auth.model.AccountRecord.Source;
import vn.ptit.one.auth.model.Role;

/**
 * Danh bạ + tài khoản: đọc để đăng nhập, ghi khi cấp/kích hoạt/khoá (F02).
 * Chỉ module auth dùng.
 *
 * <p>Phần 2: các câu ghi {@code DanhBaNguoiDung} chạy ở MASTER (site bị DENY);
 * {@code TaiKhoan} ghi ở site nhà. Phần 1 một DB nên chung giao dịch.
 */
@Repository
@Profile("central")
public class AccountRepository {

    /* LEFT JOIN cả hai bảng tài khoản: có dòng ở cả hai (dữ liệu hỏng) thì
       model thấy Source.BOTH và từ chối, thay vì SQL tự chọn một bên. */
    private static final String FIND_BY_USERNAME = """
            SELECT d.TenDangNhap, d.MaCoSo, d.LoaiNguoiDung, d.MaThucThe,
                   d.TrangThai, d.PhienBanTaiKhoan,
                   t.MatKhauHash AS HashCoSo, t.VaiTro AS VaiTroCoSo,
                   t.MaCoSo AS CoSoTaiKhoan, t.MaThucThe AS ThucTheTaiKhoan,
                   m.MatKhauHash AS HashMaster, m.VaiTro AS VaiTroMaster,
                   m.DangHoatDong AS MasterHoatDong
              FROM dbo.DanhBaNguoiDung d
              LEFT JOIN dbo.TaiKhoan       t ON t.TenDangNhap = d.TenDangNhap
              LEFT JOIN dbo.TaiKhoanMaster m ON m.TenDangNhap = d.TenDangNhap
             WHERE d.TenDangNhap = ?
            """;

    /* Phần 2: DanhBaNguoiDung là bảng nhân bản, MASTER sở hữu — câu này sẽ
       phải chạy ở MASTER và có hiệu lực tại site sau độ trễ nhân bản. */
    private static final String BUMP_VERSION = """
            UPDATE dbo.DanhBaNguoiDung
               SET PhienBanTaiKhoan = PhienBanTaiKhoan + 1, NgayCapNhat = SYSUTCDATETIME()
             WHERE TenDangNhap = ?
            """;

    private static final String INSERT_DIRECTORY = """
            INSERT INTO dbo.DanhBaNguoiDung (TenDangNhap, MaCoSo, LoaiNguoiDung, MaThucThe, TrangThai)
            VALUES (?, ?, ?, ?, ?)
            """;

    /* MatKhauHash NULL = chưa kích hoạt: không đăng nhập được cho tới khi
       người dùng đặt mật khẩu bằng mã kích hoạt. */
    private static final String INSERT_SITE_ACCOUNT = """
            INSERT INTO dbo.TaiKhoan (TenDangNhap, MatKhauHash, VaiTro, MaThucThe, MaCoSo)
            VALUES (?, NULL, ?, ?, ?)
            """;

    /* Điều kiện "chưa có mật khẩu" nằm TRONG câu UPDATE: kích hoạt đúng một lần. */
    private static final String SET_INITIAL_PASSWORD = """
            UPDATE dbo.TaiKhoan SET MatKhauHash = ?
             WHERE TenDangNhap = ? AND MatKhauHash IS NULL
            """;

    /* Chỉ chuyển giữa HOAT_DONG và NGUNG; CHO_KICH_HOAT/DANG_CHUYEN do luồng
       khác sở hữu. Admin Master không khoá qua đây (TaiKhoanMaster riêng). */
    private static final String UPDATE_STATUS = """
            UPDATE dbo.DanhBaNguoiDung SET TrangThai = ?, NgayCapNhat = SYSUTCDATETIME()
             WHERE TenDangNhap = ? AND LoaiNguoiDung <> 'ADMIN_MASTER'
               AND TrangThai IN ('HOAT_DONG', 'NGUNG')
            """;

    private static final String SUMMARY = """
            SELECT d.TenDangNhap, d.LoaiNguoiDung, d.MaCoSo, d.MaThucThe, d.TrangThai,
                   CASE WHEN t.MatKhauHash IS NOT NULL OR m.MatKhauHash IS NOT NULL
                        THEN 1 ELSE 0 END AS DaKichHoat
              FROM dbo.DanhBaNguoiDung d
              LEFT JOIN dbo.TaiKhoan       t ON t.TenDangNhap = d.TenDangNhap
              LEFT JOIN dbo.TaiKhoanMaster m ON m.TenDangNhap = d.TenDangNhap
            """;

    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AccountRecord> findByUsername(String username) {
        return jdbc.query(FIND_BY_USERNAME, (rs, rowNum) -> map(rs), username).stream().findFirst();
    }

    /** Làm mọi JWT đang lưu hành của tài khoản mất hiệu lực ở request kế tiếp. */
    public int bumpVersion(String username) {
        return jdbc.update(BUMP_VERSION, username);
    }

    public boolean campusExists(String maCoSo) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.CoSo WHERE MaCoSo = ?",
                Integer.class, maCoSo);
        return count != null && count > 0;
    }

    /** Tên đăng nhập hoặc mã thực thể đã có trong danh bạ. */
    public boolean directoryTaken(String username, String entityId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = ? OR MaThucThe = ?
                """, Integer.class, username, entityId);
        return count != null && count > 0;
    }

    public void insertDirectory(String username, String campus, Role role, String entityId, String status) {
        jdbc.update(INSERT_DIRECTORY, username, campus, role.name(), entityId, status);
    }

    public void insertSiteAccount(String username, Role role, String entityId, String campus) {
        jdbc.update(INSERT_SITE_ACCOUNT, username, role.name(), entityId, campus);
    }

    /** @return 1 nếu đặt được, 0 nếu tài khoản đã có mật khẩu */
    public int setInitialPassword(String username, String passwordHash) {
        return jdbc.update(SET_INITIAL_PASSWORD, passwordHash, username);
    }

    /** @return 0 nếu là Admin Master hoặc trạng thái hiện tại không chuyển được */
    public int updateStatus(String username, String status) {
        return jdbc.update(UPDATE_STATUS, status, username);
    }

    public Optional<AccountSummary> findSummary(String username) {
        return jdbc.query(SUMMARY + " WHERE d.TenDangNhap = ?", (rs, rowNum) -> mapSummary(rs), username)
                .stream().findFirst();
    }

    /** {@code maCoSo}/{@code role} null nghĩa là không lọc theo tiêu chí đó. */
    public List<AccountSummary> search(String maCoSo, Role role) {
        StringBuilder sql = new StringBuilder(SUMMARY).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (maCoSo != null) {
            sql.append(" AND d.MaCoSo = ?");
            args.add(maCoSo);
        }
        if (role != null) {
            sql.append(" AND d.LoaiNguoiDung = ?");
            args.add(role.name());
        }
        sql.append(" ORDER BY d.LoaiNguoiDung, d.TenDangNhap");
        return jdbc.query(sql.toString(), (rs, rowNum) -> mapSummary(rs), args.toArray());
    }

    private static AccountSummary mapSummary(ResultSet rs) throws SQLException {
        return new AccountSummary(
                rs.getString("TenDangNhap"),
                Role.valueOf(rs.getString("LoaiNguoiDung")),
                rs.getString("MaCoSo"),
                rs.getString("MaThucThe"),
                rs.getString("TrangThai"),
                rs.getBoolean("DaKichHoat"));
    }

    private static AccountRecord map(ResultSet rs) throws SQLException {
        return new AccountRecord(
                rs.getString("TenDangNhap"),
                Role.valueOf(rs.getString("LoaiNguoiDung")),
                rs.getString("MaCoSo"),
                rs.getString("MaThucThe"),
                rs.getString("TrangThai"),
                rs.getInt("PhienBanTaiKhoan"),
                credential(rs));
    }

    private static Credential credential(ResultSet rs) throws SQLException {
        String siteHash = rs.getString("HashCoSo");
        String masterHash = rs.getString("HashMaster");
        if (siteHash != null && masterHash != null) {
            return new Credential(Source.BOTH, null, null, null, null, false);
        }
        if (siteHash != null) {
            return new Credential(Source.SITE, siteHash, Role.valueOf(rs.getString("VaiTroCoSo")),
                    rs.getString("CoSoTaiKhoan"), rs.getString("ThucTheTaiKhoan"), true);
        }
        if (masterHash != null) {
            return new Credential(Source.MASTER, masterHash, Role.valueOf(rs.getString("VaiTroMaster")),
                    null, null, rs.getBoolean("MasterHoatDong"));
        }
        return null;
    }
}
