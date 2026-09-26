package vn.ptit.one.course.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ptit.one.course.model.CourseSummary;
import vn.ptit.one.course.model.Faculty;
import vn.ptit.one.course.model.Term;

/** Danh mục môn học và đồ thị tiên quyết. SQL nằm hết ở đây. */
@Repository
@Profile("central")
public class CourseRepository {

    /**
     * Khoá tuần tự hoá mọi thay đổi tiên quyết.
     *
     * <p>Một khoá duy nhất cho cả đồ thị, không khoá theo từng môn: "không có
     * chu trình" là tính chất TOÀN CỤC. Hai admin sửa hai môn khác nhau, mỗi
     * request xét riêng đều hợp lệ, ghép lại vẫn tạo được chu trình — khoá theo
     * dòng không chặn được tình huống đó. Danh mục sửa rất thưa nên khoá rộng
     * không tốn gì.
     */
    public static final String PREREQUISITE_LOCK = "PTITONE:MonHocTienQuyet";

    private static final String COLUMNS = """
            SELECT m.MaMonHoc, m.TenMonHoc, m.SoTinChi, m.MaKhoa, k.TenKhoa
              FROM dbo.MonHoc m
              JOIN dbo.Khoa k ON k.MaKhoa = m.MaKhoa
            """;

    private static final String FIND_ONE = COLUMNS + " WHERE m.MaMonHoc = ?";

    /** Môn mà {@code ?} phải đạt trước. */
    private static final String FIND_PREREQUISITES = COLUMNS + """
              JOIN dbo.MonHocTienQuyet tq ON tq.MaMonTienQuyet = m.MaMonHoc
             WHERE tq.MaMonHoc = ?
             ORDER BY m.MaMonHoc
            """;

    /** Chiều ngược: môn nào đang lấy {@code ?} làm tiên quyết. */
    private static final String FIND_DEPENDENTS = COLUMNS + """
              JOIN dbo.MonHocTienQuyet tq ON tq.MaMonHoc = m.MaMonHoc
             WHERE tq.MaMonTienQuyet = ?
             ORDER BY m.MaMonHoc
            """;

    /**
     * Từ {@code @start} đi theo cạnh "môn → tiên quyết của nó" có tới được
     * {@code @target} không.
     *
     * <p>Thêm cạnh {@code target → start} tạo chu trình khi và chỉ khi câu này
     * trả về > 0. Chặn được chu trình dài, không chỉ môn tự làm tiên quyết.
     */
    private static final String REACHES = """
            WITH DuongDi AS (
                SELECT MaMonTienQuyet
                  FROM dbo.MonHocTienQuyet
                 WHERE MaMonHoc = ?
                UNION ALL
                SELECT tq.MaMonTienQuyet
                  FROM dbo.MonHocTienQuyet tq
                  JOIN DuongDi d ON tq.MaMonHoc = d.MaMonTienQuyet
            )
            SELECT COUNT(*) FROM DuongDi WHERE MaMonTienQuyet = ?
            OPTION (MAXRECURSION 100)
            """;

    /**
     * Môn này có lớp trong học kỳ nào đang mở đợt đăng ký không.
     *
     * <p>Đổi tiên quyết giữa lúc sinh viên đang đăng ký sẽ khiến hai người nộp
     * cùng một phút bị xét theo hai bộ quy tắc khác nhau.
     */
    private static final String HAS_OPEN_REGISTRATION = """
            SELECT COUNT(*)
              FROM dbo.LopHocPhan l
              JOIN dbo.DotDangKy d ON d.MaHocKy = l.MaHocKy
             WHERE l.MaMonHoc = ? AND d.TrangThai = 'DANG_MO'
            """;

    private final JdbcTemplate jdbc;

    public CourseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- Đọc danh mục ----------------------------------------------------

    /** Lọc theo khoa và/hoặc từ khoá; bỏ trống cả hai thì trả toàn bộ. */
    public List<CourseSummary> search(String maKhoa, String tuKhoa) {
        StringBuilder sql = new StringBuilder(COLUMNS).append(" WHERE 1 = 1");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (maKhoa != null && !maKhoa.isBlank()) {
            sql.append(" AND m.MaKhoa = ?");
            args.add(maKhoa.trim());
        }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            // Tham số hoá cả ký tự đại diện; không nối chuỗi vào câu lệnh.
            sql.append(" AND (m.MaMonHoc LIKE ? OR m.TenMonHoc LIKE ?)");
            String pattern = '%' + tuKhoa.trim() + '%';
            args.add(pattern);
            args.add(pattern);
        }
        sql.append(" ORDER BY m.MaMonHoc");
        return jdbc.query(sql.toString(), (rs, rowNum) -> mapCourse(rs), args.toArray());
    }

    public Optional<CourseSummary> findOne(String maMonHoc) {
        return jdbc.query(FIND_ONE, (rs, rowNum) -> mapCourse(rs), maMonHoc).stream().findFirst();
    }

    public List<CourseSummary> findPrerequisites(String maMonHoc) {
        return jdbc.query(FIND_PREREQUISITES, (rs, rowNum) -> mapCourse(rs), maMonHoc);
    }

    public List<CourseSummary> findDependents(String maMonHoc) {
        return jdbc.query(FIND_DEPENDENTS, (rs, rowNum) -> mapCourse(rs), maMonHoc);
    }

    public boolean exists(String maMonHoc) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.MonHoc WHERE MaMonHoc = ?", Integer.class, maMonHoc);
        return count != null && count > 0;
    }

    public boolean facultyExists(String maKhoa) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.Khoa WHERE MaKhoa = ?", Integer.class, maKhoa);
        return count != null && count > 0;
    }

    public List<Faculty> findFaculties() {
        return jdbc.query("SELECT MaKhoa, TenKhoa FROM dbo.Khoa ORDER BY MaKhoa",
                (rs, rowNum) -> new Faculty(rs.getString("MaKhoa"), rs.getString("TenKhoa")));
    }

    public List<Term> findTerms() {
        return jdbc.query("""
                SELECT MaHocKy, TenHocKy, NamHoc, NgayBatDau, NgayKetThuc
                  FROM dbo.HocKy ORDER BY NgayBatDau DESC
                """, (rs, rowNum) -> new Term(
                        rs.getString("MaHocKy"),
                        rs.getString("TenHocKy"),
                        rs.getString("NamHoc"),
                        rs.getObject("NgayBatDau", java.time.LocalDate.class),
                        rs.getObject("NgayKetThuc", java.time.LocalDate.class)));
    }

    // --- Ghi danh mục ----------------------------------------------------

    public void insert(String maMonHoc, String tenMonHoc, int soTinChi, String maKhoa) {
        jdbc.update("""
                INSERT INTO dbo.MonHoc (MaMonHoc, TenMonHoc, SoTinChi, MaKhoa)
                VALUES (?, ?, ?, ?)
                """, maMonHoc, tenMonHoc, soTinChi, maKhoa);
    }

    /** @return số dòng đổi; 0 nghĩa là không có môn đó */
    public int update(String maMonHoc, String tenMonHoc, int soTinChi, String maKhoa) {
        return jdbc.update("""
                UPDATE dbo.MonHoc SET TenMonHoc = ?, SoTinChi = ?, MaKhoa = ?
                 WHERE MaMonHoc = ?
                """, tenMonHoc, soTinChi, maKhoa, maMonHoc);
    }

    // --- Đồ thị tiên quyết -----------------------------------------------

    /**
     * Giữ khoá tới hết transaction. Phải gọi TRONG transaction, nếu không
     * {@code LockOwner='Transaction'} sẽ lỗi.
     *
     * @return true nếu lấy được khoá
     */
    public boolean acquirePrerequisiteLock(int timeoutMillis) {
        Integer result = jdbc.queryForObject("""
                DECLARE @ketQua int;
                EXEC @ketQua = sp_getapplock @Resource = ?, @LockMode = 'Exclusive',
                     @LockOwner = 'Transaction', @LockTimeout = ?;
                SELECT @ketQua;
                """, Integer.class, PREREQUISITE_LOCK, timeoutMillis);
        // >= 0: lấy được (0 ngay lập tức, 1 sau khi chờ). Âm là timeout/deadlock.
        return result != null && result >= 0;
    }

    public boolean reaches(String start, String target) {
        Integer count = jdbc.queryForObject(REACHES, Integer.class, start, target);
        return count != null && count > 0;
    }

    public boolean hasOpenRegistration(String maMonHoc) {
        Integer count = jdbc.queryForObject(HAS_OPEN_REGISTRATION, Integer.class, maMonHoc);
        return count != null && count > 0;
    }

    public void deletePrerequisites(String maMonHoc) {
        jdbc.update("DELETE FROM dbo.MonHocTienQuyet WHERE MaMonHoc = ?", maMonHoc);
    }

    public void insertPrerequisite(String maMonHoc, String maMonTienQuyet) {
        jdbc.update("""
                INSERT INTO dbo.MonHocTienQuyet (MaMonHoc, MaMonTienQuyet) VALUES (?, ?)
                """, maMonHoc, maMonTienQuyet);
    }

    private static CourseSummary mapCourse(ResultSet rs) throws SQLException {
        return new CourseSummary(
                rs.getString("MaMonHoc"),
                rs.getString("TenMonHoc"),
                rs.getInt("SoTinChi"),
                rs.getString("MaKhoa"),
                rs.getString("TenKhoa"));
    }
}
