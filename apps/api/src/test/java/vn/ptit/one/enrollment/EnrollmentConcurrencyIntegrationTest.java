package vn.ptit.one.enrollment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.enrollment.service.EnrollmentService;
import vn.ptit.one.shared.exception.ApiException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tương tranh F08 trên SQL Server thật — ca bắt buộc của kế hoạch Phần 1:
 * lớp trống 30 chỗ, 100 sinh viên khác nhau cùng hợp lệ, đăng ký đồng thời →
 * đúng 30 thành công và đối soát không lệch.
 *
 * <p>Gọi thẳng {@link EnrollmentService} từ nhiều luồng, mỗi luồng một giao dịch
 * thật trên một kết nối riêng. Bỏ qua HTTP vì đăng nhập 100 tài khoản không thêm
 * gì cho phép kiểm này — thứ đang được chứng minh là câu UPDATE có điều kiện và
 * {@code sp_getapplock} ở tầng DB, không phải tầng web.
 *
 * <p>Fixture riêng, tạo và xoá bằng SQL: 100 sinh viên {@code T08R001..100} và
 * lớp {@code TEST-F08-RACE} (môn BAS1203, không lịch, không tiên quyết).
 */
@SpringBootTest(properties = "ptitone.auth.jwt-secret=cHRpdG9uZS1pbnRlZ3JhdGlvbi10ZXN0LWtleS0zMi1ieXRlcw==")
@ActiveProfiles("central")
@EnabledIfEnvironmentVariable(named = "PTITONE_DB_URL", matches = ".+")
class EnrollmentConcurrencyIntegrationTest {

    private static final String LOP = "TEST-F08-RACE";
    private static final String PREFIX = "T08R";
    private static final int SUC_CHUA = 30;
    private static final int SO_SINH_VIEN = 100;

    @Autowired
    private EnrollmentService enrollments;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void createFixture() {
        dropFixture();
        jdbc.update("""
                INSERT INTO dbo.LopHocPhan (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, SoLuongToiDa,
                                            TrangThai, ChoPhepLienCoSo, HinhThucHoc)
                VALUES (?, 'BAS1203', '2026-1', 'HCM', ?, 'MO', 0, 'TRUC_TIEP')
                """, LOP, SUC_CHUA);
        for (int i = 1; i <= SO_SINH_VIEN; i++) {
            jdbc.update("""
                    INSERT INTO dbo.SinhVien (MaSinhVien, HoTen, MaCoSoNha, MaCTDT, TrangThai)
                    VALUES (?, ?, 'HCM', 'CN-CNTT-2022', 'DANG_HOC')
                    """, maSinhVien(i), "Sinh viên thử " + i);
        }
    }

    @AfterEach
    void dropFixture() {
        String like = PREFIX + "%";
        jdbc.update("""
                DELETE n FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao
                 WHERE t.MaLopHP = ?
                """, LOP);
        jdbc.update("DELETE FROM dbo.ThongBao WHERE MaLopHP = ?", LOP);
        jdbc.update("DELETE FROM dbo.Diem WHERE MaSinhVien LIKE ?", like);
        jdbc.update("DELETE FROM dbo.DangKyMonHoc WHERE MaSinhVien LIKE ?", like);
        jdbc.update("DELETE FROM dbo.DangKyHocPhan WHERE MaSinhVien LIKE ?", like);
        jdbc.update("DELETE FROM dbo.SinhVienHocKy WHERE MaSinhVien LIKE ?", like);
        jdbc.update("DELETE FROM dbo.SinhVien WHERE MaSinhVien LIKE ?", like);
        jdbc.update("DELETE FROM dbo.LopHocPhan WHERE MaLopHP = ?", LOP);
    }

    @Test
    void lop30Cho100SinhVienThiDungBaMuoiNguoiVao() throws Exception {
        AtomicInteger thanhCong = new AtomicInteger();
        ConcurrentHashMap<String, AtomicInteger> loi = new ConcurrentHashMap<>();

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 1; i <= SO_SINH_VIEN; i++) {
            AuthenticatedUser sv = student(maSinhVien(i));
            tasks.add(() -> {
                try {
                    if (enrollments.register(sv, LOP).created()) {
                        thanhCong.incrementAndGet();
                    }
                } catch (ApiException ex) {
                    loi.computeIfAbsent(ex.code(), k -> new AtomicInteger()).incrementAndGet();
                }
            });
        }
        runTogether(tasks);

        assertThat(thanhCong.get()).isEqualTo(SUC_CHUA);
        // Người trượt phải trượt VÌ LỚP ĐẦY, không vì deadlock hay timeout.
        assertThat(loi.keySet()).containsOnly("CLASS_FULL");
        assertThat(loi.get("CLASS_FULL").get()).isEqualTo(SO_SINH_VIEN - SUC_CHUA);
        assertReconciled(SUC_CHUA);
    }

    /** Bấm đăng ký 10 lần cùng lúc: một lần tạo, chín lần trả lại kết quả cũ, sĩ số chỉ +1. */
    @Test
    void bamNhieuLanCungLucChiTinhMotLan() throws Exception {
        AuthenticatedUser sv = student(maSinhVien(1));
        AtomicInteger moi = new AtomicInteger();
        AtomicInteger daCo = new AtomicInteger();

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> {
                if (enrollments.register(sv, LOP).created()) {
                    moi.incrementAndGet();
                } else {
                    daCo.incrementAndGet();
                }
            });
        }
        runTogether(tasks);

        assertThat(moi.get()).isEqualTo(1);
        assertThat(daCo.get()).isEqualTo(9);
        assertReconciled(1);
    }

    /** Huỷ rồi đăng ký lại dùng lại đúng dòng cũ, bộ đếm về đúng chỗ. */
    @Test
    void huyRoiDangKyLaiKhongLechBoDem() {
        AuthenticatedUser sv = student(maSinhVien(1));

        enrollments.register(sv, LOP);
        enrollments.cancel(sv, LOP);
        assertReconciled(0);
        assertThat(jdbc.queryForObject(
                "SELECT SoTinChiDaDangKy FROM dbo.SinhVienHocKy WHERE MaSinhVien = ? AND MaHocKy = '2026-1'",
                Integer.class, maSinhVien(1))).isZero();

        assertThat(enrollments.register(sv, LOP).created()).isTrue();
        assertReconciled(1);
    }

    // --- Tiện ích --------------------------------------------------------

    /**
     * Đối soát: bộ đếm của lớp, số dòng ghi danh phía lớp, phía sinh viên, dòng
     * điểm rỗng và tổng tín chỉ phải cùng một con số.
     */
    private void assertReconciled(int expected) {
        assertThat(count("SELECT SoLuongDaDangKy FROM dbo.LopHocPhan WHERE MaLopHP = ?"))
                .as("bộ đếm sĩ số").isEqualTo(expected);
        assertThat(count("SELECT COUNT(*) FROM dbo.DangKyHocPhan WHERE MaLopHP = ? AND TrangThai = 'DA_DANG_KY'"))
                .as("ghi danh phía lớp").isEqualTo(expected);
        assertThat(count("SELECT COUNT(*) FROM dbo.DangKyMonHoc WHERE MaLopHP = ? AND TrangThai = 'DA_DANG_KY'"))
                .as("ghi danh phía sinh viên").isEqualTo(expected);
        assertThat(count("SELECT COUNT(*) FROM dbo.Diem WHERE MaLopHP = ?"))
                .as("dòng điểm").isEqualTo(expected);
        int soTinChi = jdbc.queryForObject("SELECT SoTinChi FROM dbo.MonHoc WHERE MaMonHoc = 'BAS1203'", Integer.class);
        assertThat(jdbc.queryForObject("""
                SELECT ISNULL(SUM(SoTinChiDaDangKy), 0) FROM dbo.SinhVienHocKy
                 WHERE MaSinhVien LIKE ? AND MaHocKy = '2026-1'
                """, Integer.class, PREFIX + "%")).as("tổng tín chỉ").isEqualTo(expected * soTinChi);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class, LOP);
    }

    /** Thả mọi luồng cùng lúc bằng một chốt, để các request thật sự chồng lên nhau. */
    private static void runTogether(List<Runnable> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Runnable task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    task.run();
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static AuthenticatedUser student(String maSinhVien) {
        return new AuthenticatedUser(maSinhVien, Role.SINH_VIEN, maSinhVien, "HCM",
                UUID.randomUUID(), 1, Instant.now().plusSeconds(3600));
    }

    private static String maSinhVien(int i) {
        return "%s%03d".formatted(PREFIX, i);
    }
}
