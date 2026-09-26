package vn.ptit.one.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AccountRecord.Credential;
import vn.ptit.one.auth.model.AccountRecord.Source;
import vn.ptit.one.auth.model.Role;

/** Đọc danh bạ + mật khẩu để đăng nhập. Chỉ module auth dùng. */
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
