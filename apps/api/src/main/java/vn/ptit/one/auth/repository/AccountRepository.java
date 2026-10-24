package vn.ptit.one.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.auth.model.Campus;
import vn.ptit.one.auth.model.AccountContact;
import vn.ptit.one.auth.model.AccountRecord;
import vn.ptit.one.auth.model.AccountSummary;
import vn.ptit.one.auth.model.AccountRecord.Credential;
import vn.ptit.one.auth.model.AccountRecord.Source;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.shared.routing.SiteContext;

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
    private static final String FIND_BY_USERNAME_MAU = """
            SELECT d.TenDangNhap, d.MaCoSo, d.LoaiNguoiDung, d.MaThucThe,
                   d.TrangThai, d.PhienBanTaiKhoan,
                   t.MatKhauHash AS HashCoSo, t.VaiTro AS VaiTroCoSo,
                   t.MaCoSo AS CoSoTaiKhoan, t.MaThucThe AS ThucTheTaiKhoan,
                   m.MatKhauHash AS HashMaster, m.VaiTro AS VaiTroMaster,
                   m.DangHoatDong AS MasterHoatDong
              FROM dbo.DanhBaNguoiDung d
%s
%s
             WHERE d.TenDangNhap = ?
            """;

    /* Phần 2: DanhBaNguoiDung là bảng nhân bản, MASTER sở hữu — câu này sẽ
       phải chạy ở MASTER và có hiệu lực tại site sau độ trễ nhân bản. */
    private static final String BUMP_VERSION = """
            UPDATE dbo.DanhBaNguoiDung
               SET PhienBanTaiKhoan = PhienBanTaiKhoan + 1, NgayCapNhat = SYSUTCDATETIME()
             WHERE TenDangNhap = ?
            """;

    /* CHI doc danh ba. Dung o Phan 2 de biet nguoi dung thuoc co so nao
       TRUOC khi dinh tuyen toi database cua co so do. */
    private static final String FIND_CAMPUS = """
            SELECT MaCoSo FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = ?
            """;

    private static final String INSERT_DIRECTORY = """
            INSERT INTO dbo.DanhBaNguoiDung (TenDangNhap, MaCoSo, LoaiNguoiDung, MaThucThe, TrangThai)
            VALUES (?, ?, ?, ?, ?)
            """;

    /* MatKhauHash NULL = chưa kích hoạt: không đăng nhập được cho tới khi
       người dùng đặt mật khẩu bằng mã kích hoạt. */
    private static final String INSERT_SITE_ACCOUNT = """
            INSERT INTO dbo.TaiKhoan (TenDangNhap, MatKhauHash, VaiTro, MaThucThe, MaCoSo, Email)
            VALUES (?, NULL, ?, ?, ?, ?)
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

    private static final String SUMMARY_MAU = """
            SELECT d.TenDangNhap, d.LoaiNguoiDung, d.MaCoSo, d.MaThucThe, d.TrangThai,
                   CASE WHEN t.MatKhauHash IS NOT NULL OR m.MatKhauHash IS NOT NULL
                        THEN 1 ELSE 0 END AS DaKichHoat
              FROM dbo.DanhBaNguoiDung d
%s
%s
            """;

    private static final String FIND_CONTACT_MAU = """
            SELECT d.TenDangNhap, d.TrangThai,
                   CASE WHEN t.TenDangNhap IS NOT NULL THEN 'SITE' ELSE 'MASTER' END AS Nguon,
                   COALESCE(t.MatKhauHash, m.MatKhauHash) AS MatKhauHash,
                   COALESCE(t.Email, m.Email) AS Email,
                   COALESCE(t.EmailDaXacMinh, m.EmailDaXacMinh) AS EmailDaXacMinh
              FROM dbo.DanhBaNguoiDung d
%s
%s
             WHERE d.TenDangNhap = ? AND (t.TenDangNhap IS NOT NULL OR m.TenDangNhap IS NOT NULL)
            """;

    /* ---------------------------------------------------------------------
       HAI BẢNG TÀI KHOẢN, VÀ KHÔNG NÚT NÀO CÓ ĐỦ CẢ HAI

         PTITONE_MASTER : DanhBaNguoiDung + TaiKhoanMaster, KHÔNG có TaiKhoan
         PTITONE_<site> : DanhBaNguoiDung + TaiKhoan, KHÔNG có TaiKhoanMaster
         PTITONE_CENTRAL: có cả ba (Phần 1)

       Ba câu ở trên join cả hai bảng và COALESCE giữa chúng, nên ở Phần 2
       chúng hỏng tại CẢ HAI nút với lỗi "Invalid object name" — trả về 503
       chung chung, không chỉ ra được thiếu bảng nào.

       Thay vì tách tay mỗi câu thành hai bản (và bẫy lại người viết câu kế
       tiếp), bảng vắng mặt được thay bằng một bảng dẫn xuất RỖNG có cùng
       danh sách cột. Hình dạng câu truy vấn không đổi, mọi tham chiếu cột
       vẫn phân giải được, và `WHERE 1 = 0` làm nó rỗng một cách KIỂM CHỨNG
       ĐƯỢC — khác hẳn việc tạo một bảng thật rỗng ở nút đó, vốn sẽ biến lỗi
       "thiếu bảng" thành "có bảng nhưng không có dòng", tức đổi một lỗi ồn
       ào lấy một kết quả sai lặng lẽ.
       --------------------------------------------------------------------- */

    private static final String JOIN_TAIKHOAN =
            "              LEFT JOIN dbo.TaiKhoan       t ON t.TenDangNhap = d.TenDangNhap";

    private static final String JOIN_TAIKHOAN_MASTER =
            "              LEFT JOIN dbo.TaiKhoanMaster m ON m.TenDangNhap = d.TenDangNhap";

    private static final String JOIN_TAIKHOAN_RONG = """
                  LEFT JOIN (SELECT CAST(NULL AS varchar(50))  AS TenDangNhap,
                                    CAST(NULL AS varchar(255)) AS MatKhauHash,
                                    CAST(NULL AS varchar(20))  AS VaiTro,
                                    CAST(NULL AS varchar(10))  AS MaCoSo,
                                    CAST(NULL AS varchar(20))  AS MaThucThe,
                                    CAST(NULL AS varchar(254)) AS Email,
                                    CAST(NULL AS bit)          AS EmailDaXacMinh
                              WHERE 1 = 0) t ON 1 = 0""";

    private static final String JOIN_TAIKHOAN_MASTER_RONG = """
                  LEFT JOIN (SELECT CAST(NULL AS varchar(50))  AS TenDangNhap,
                                    CAST(NULL AS varchar(255)) AS MatKhauHash,
                                    CAST(NULL AS varchar(20))  AS VaiTro,
                                    CAST(NULL AS bit)          AS DangHoatDong,
                                    CAST(NULL AS varchar(254)) AS Email,
                                    CAST(NULL AS bit)          AS EmailDaXacMinh
                              WHERE 1 = 0) m ON 1 = 0""";

    /**
     * Ghép câu truy vấn hợp với nút đang đứng.
     *
     * <p>{@code SiteContext.explicit()} trả {@code null} ở Phần 1 — nơi không
     * ai đặt cơ sở bao giờ vì chỉ có một database, và database đó có đủ cả ba
     * bảng. Vì vậy phân biệt "null" với "MASTER" là cần thiết, không phải vẽ vời.
     */
    private static String sql(String mau) {
        String site = SiteContext.explicit();
        if (site == null) {
            return mau.formatted(JOIN_TAIKHOAN, JOIN_TAIKHOAN_MASTER);
        }
        return SiteContext.MASTER.equals(site)
                ? mau.formatted(JOIN_TAIKHOAN_RONG, JOIN_TAIKHOAN_MASTER)
                : mau.formatted(JOIN_TAIKHOAN, JOIN_TAIKHOAN_MASTER_RONG);
    }

    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AccountRecord> findByUsername(String username) {
        return jdbc.query(sql(FIND_BY_USERNAME_MAU), (rs, rowNum) -> map(rs), username).stream().findFirst();
    }

    /**
     * Cơ sở của một tài khoản, đọc từ danh bạ và CHỈ từ danh bạ.
     *
     * <p>Phần 2 cần câu này trước khi đăng nhập được: {@code TaiKhoan} nằm ở
     * từng cơ sở, mà lúc nhận tên đăng nhập thì chưa biết cơ sở nào. Danh bạ ở
     * Master là nơi duy nhất trả lời được.
     *
     * <p>Không dùng {@link #findByUsername} cho việc này: câu đó join sang
     * {@code TaiKhoan}, mà Master không có bảng đó — chạy ở Master là lỗi
     * "Invalid object name".
     *
     * <p>Trả về rỗng khi không tìm thấy, và cũng rỗng với Admin Master (cột
     * {@code MaCoSo} là NULL vì họ không thuộc cơ sở nào). Cả hai trường hợp
     * đều phải xử lý tại Master, nên gộp lại là đúng chứ không phải mất thông tin.
     */


    public Optional<String> findCampusOfUser(String username) {
        return jdbc.queryForList(FIND_CAMPUS, String.class, username).stream()
                .filter(campus -> campus != null)
                .findFirst();
    }

    /** Làm mọi JWT đang lưu hành của tài khoản mất hiệu lực ở request kế tiếp. */
    public int bumpVersion(String username) {
        return jdbc.update(BUMP_VERSION, username);
    }

    /**
     * Mã cơ sở đúng như trong bảng {@code CoSo}. Collation không phân biệt hoa
     * thường nên {@code hcm} vẫn khớp {@code HCM} — phải ghi giá trị CHUẨN, vì
     * code Java so sánh phân biệt hoa thường.
     */
    /**
     * Cơ sở ĐANG HOẠT ĐỘNG, để giao diện đổ ô chọn.
     *
     * <p>Bỏ cơ sở đã tắt: chọn một cơ sở không hoạt động rồi thống kê hay cấp
     * hồ sơ vào đó là tạo dữ liệu không ai dùng.
     */
    public List<Campus> listCampuses() {
        return jdbc.query("""
                SELECT MaCoSo, TenCoSo, ThanhPho FROM dbo.CoSo
                 WHERE DangHoatDong = 1 ORDER BY MaCoSo
                """, (rs, rowNum) -> new Campus(rs.getString("MaCoSo"), rs.getString("TenCoSo"),
                        rs.getString("ThanhPho")));
    }

    public Optional<String> findCampus(String maCoSo) {
        return jdbc.queryForList("SELECT MaCoSo FROM dbo.CoSo WHERE MaCoSo = ?", String.class, maCoSo.trim())
                .stream().findFirst();
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

    /** @param email chưa xác minh; {@code null} nếu Admin không nhập */
    public void insertSiteAccount(String username, Role role, String entityId, String campus, String email) {
        jdbc.update(INSERT_SITE_ACCOUNT, username, role.name(), entityId, campus, email);
    }

    public Optional<AccountContact> findContact(String username) {
        return jdbc.query(sql(FIND_CONTACT_MAU), (rs, rowNum) -> new AccountContact(
                        rs.getString("TenDangNhap"),
                        Source.valueOf(rs.getString("Nguon")),
                        rs.getString("TrangThai"),
                        rs.getString("MatKhauHash") != null,
                        rs.getString("Email"),
                        rs.getBoolean("EmailDaXacMinh")),
                username).stream().findFirst();
    }

    /** Đổi email thì mất trạng thái đã xác minh — phải xác minh lại địa chỉ mới. */
    public int setEmail(String username, Source source, String email) {
        return jdbc.update("UPDATE dbo.%s SET Email = ?, EmailDaXacMinh = 0, ThoiDiemXacMinhEmail = NULL WHERE TenDangNhap = ?"
                .formatted(table(source)), email, username);
    }

    /** Chỉ đánh dấu khi tài khoản VẪN mang đúng địa chỉ đã nhận mã. @return 0 nếu email đã đổi */
    public int markEmailVerified(String username, Source source, String email, Instant now) {
        return jdbc.update("""
                UPDATE dbo.%s SET EmailDaXacMinh = 1, ThoiDiemXacMinhEmail = ?
                 WHERE TenDangNhap = ? AND Email = ?
                """.formatted(table(source)), SqlTime.toDb(now), username, email);
    }

    /** Đổi mật khẩu / khôi phục: tài khoản phải đã kích hoạt. */
    public int updatePassword(String username, Source source, String passwordHash) {
        return jdbc.update("UPDATE dbo.%s SET MatKhauHash = ? WHERE TenDangNhap = ? AND MatKhauHash IS NOT NULL"
                .formatted(table(source)), passwordHash, username);
    }

    /* Tên bảng lấy từ enum, không bao giờ từ dữ liệu người dùng. */
    private static String table(Source source) {
        return switch (source) {
            case SITE -> "TaiKhoan";
            case MASTER -> "TaiKhoanMaster";
            case BOTH -> throw new IllegalArgumentException("Tài khoản có dòng ở cả hai bảng");
        };
    }

    /** @return 1 nếu đặt được, 0 nếu tài khoản đã có mật khẩu */
    public int setInitialPassword(String username, String passwordHash) {
        return jdbc.update(SET_INITIAL_PASSWORD, passwordHash, username);
    }

    /**
     * Xoá mật khẩu, đưa tài khoản về trạng thái "chưa kích hoạt".
     *
     * <p>Chỉ dùng cho luồng Admin cấp lại mật khẩu. Sau lệnh này tài khoản
     * KHÔNG đăng nhập được cho tới khi chủ tài khoản tự đặt mật khẩu mới bằng
     * mã — nhờ vậy {@link #setInitialPassword} giữ nguyên ràng buộc
     * "chỉ ghi khi chưa có mật khẩu" và không cần một đường ghi thứ hai.
     *
     * <p>Chỉ áp cho tài khoản site: Admin Master ở bảng riêng và không cấp lại
     * mật khẩu qua đường này.
     *
     * @return 0 nếu không có tài khoản site tên đó
     */
    public int clearPassword(String username) {
        return jdbc.update("UPDATE dbo.TaiKhoan SET MatKhauHash = NULL WHERE TenDangNhap = ?",
                username);
    }

    /** @return 0 nếu là Admin Master hoặc trạng thái hiện tại không chuyển được */
    public int updateStatus(String username, String status) {
        return jdbc.update(UPDATE_STATUS, status, username);
    }

    public Optional<AccountSummary> findSummary(String username) {
        return jdbc.query(sql(SUMMARY_MAU) + " WHERE d.TenDangNhap = ?", (rs, rowNum) -> mapSummary(rs), username)
                .stream().findFirst();
    }

    /** {@code maCoSo}/{@code role} null nghĩa là không lọc theo tiêu chí đó. */
    public List<AccountSummary> search(String maCoSo, Role role) {
        StringBuilder sql = new StringBuilder(sql(SUMMARY_MAU)).append(" WHERE 1 = 1");
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
