package vn.ptit.one.shared.routing;

import java.util.function.Supplier;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Cơ sở mà request hiện tại đang thao tác. {@link RoutingDataSource} đọc giá
 * trị ở đây để chọn database.
 *
 * <p>Giá trị luôn đến từ phiên đã xác thực — {@code AuthenticatedUser.homeCampus()},
 * vốn lấy từ DB lúc kiểm phiên. <b>Tuyệt đối không</b> nhận từ tham số client
 * gửi lên: tin {@code ?campus=HN} là mở sẵn một lỗ hổng leo thang đặc quyền.
 *
 * <p>Đây là class cụ thể, không phải interface. Chỉ có một cách giữ "cơ sở của
 * request này", nên thêm một tầng trừu tượng chỉ tốn thêm một file để đọc.
 */
public final class SiteContext {

    /** Khoá của database danh mục. Dùng khi chưa biết cơ sở, hoặc với Admin Master. */
    public static final String MASTER = "MASTER";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private SiteContext() {
    }

    /** Cơ sở đang chọn, hoặc {@link #MASTER} nếu chưa ai đặt. */
    public static String current() {
        String site = CURRENT.get();
        return site != null ? site : MASTER;
    }

    public static void set(String maCoSo) {
        CURRENT.set(maCoSo);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Chạy một đoạn trên database của cơ sở khác, rồi trả lại nguyên trạng.
     * Dùng cho bước Host của saga và cho worker Outbox.
     *
     * <p><b>Ném lỗi nếu đang trong một giao dịch.</b> {@code AbstractRoutingDataSource}
     * phân giải khoá <em>một lần</em> lúc mượn connection; đổi cơ sở giữa chừng
     * thì connection đã mượn không đổi theo, nên lệnh tiếp theo vẫn ghi vào cơ sở
     * cũ — <em>mà không ném exception nào</em>. Kiểm ở đây biến một lỗi âm thầm
     * làm hỏng dữ liệu thành một lỗi nhìn thấy ngay lúc chạy.
     *
     * <p>Ghép nhiều cơ sở thì dùng saga: mỗi bước một giao dịch cục bộ.
     */
    public static <T> T runAt(String maCoSo, Supplier<T> body) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "Khong duoc doi co so khi dang trong giao dich. Mot giao dich = mot site; "
                            + "ghep nhieu site bang saga. Co so dang chon: " + current()
                            + ", yeu cau doi sang: " + maCoSo);
        }
        String truoc = CURRENT.get();
        CURRENT.set(maCoSo);
        try {
            return body.get();
        } finally {
            if (truoc != null) {
                CURRENT.set(truoc);
            } else {
                CURRENT.remove();
            }
        }
    }

    public static void runAt(String maCoSo, Runnable body) {
        runAt(maCoSo, () -> {
            body.run();
            return null;
        });
    }
}
