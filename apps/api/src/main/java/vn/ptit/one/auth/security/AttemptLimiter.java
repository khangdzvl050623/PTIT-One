package vn.ptit.one.auth.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Giới hạn tần suất theo cửa sổ cố định, trong bộ nhớ (A1).
 *
 * <p>Phần 1 chạy MỘT tiến trình API nên bộ đếm trong bộ nhớ là đủ; không thêm
 * Redis (cấm theo thiết kế). Khởi động lại thì bộ đếm về 0 — chấp nhận được, vì
 * mỗi mã một lần còn có bộ đếm sai bền vững trong DB.
 */
@Component
public class AttemptLimiter {

    /** Dọn khoá hết hạn khi bảng vượt ngưỡng này, để không phình vô hạn. */
    private static final int PRUNE_THRESHOLD = 10_000;

    private record Window(Instant start, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public AttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Đã chạm trần trong cửa sổ hiện tại chưa — không tính thêm lần nào. */
    public boolean isBlocked(String key, int max, Duration window) {
        Window current = windows.get(key);
        return current != null && !expired(current, window, clock.instant()) && current.count() >= max;
    }

    /** Tính thêm một lần (ví dụ một lần đăng nhập sai). */
    public void record(String key, Duration window) {
        Instant now = clock.instant();
        windows.compute(key, (k, current) -> current == null || expired(current, window, now)
                ? new Window(now, 1)
                : new Window(current.start(), current.count() + 1));
        if (windows.size() > PRUNE_THRESHOLD) {
            windows.entrySet().removeIf(entry -> expired(entry.getValue(), window, now));
        }
    }

    /** Tính một lần rồi báo còn trong trần không — cho thao tác bị giới hạn theo số lần gọi. */
    public boolean tryAcquire(String key, int max, Duration window) {
        if (isBlocked(key, max, window)) {
            return false;
        }
        record(key, window);
        return true;
    }

    public void reset(String key) {
        windows.remove(key);
    }

    private static boolean expired(Window current, Duration window, Instant now) {
        return !now.isBefore(current.start().plus(window));
    }
}
