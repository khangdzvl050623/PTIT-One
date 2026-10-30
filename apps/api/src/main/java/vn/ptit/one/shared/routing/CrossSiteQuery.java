package vn.ptit.one.shared.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Port {@code CrossSiteQuery} (C8): hỏi từng cơ sở rồi gộp, cho người đứng ở
 * Master — nút không có bảng vận hành nào ({@code LopHocPhan}, {@code GiangVien}…).
 *
 * <p>Mỗi cơ sở một lượt đọc riêng, NGOÀI giao dịch. Không dùng Linked Server ở
 * đây: đọc qua Linked Server trong một giao dịch bị SQL Server nâng thành giao
 * dịch phân tán, mà 2PC chỉ dành cho chuyển cơ sở.
 *
 * <p>Class cụ thể, không interface — chỉ có một cách hỏi các cơ sở.
 */
@Component
@Profile("distributed")
public class CrossSiteQuery {

    private static final Logger log = LoggerFactory.getLogger(CrossSiteQuery.class);

    private final SiteProperties sites;

    public CrossSiteQuery(SiteProperties sites) {
        this.sites = sites;
    }

    /** Đang đứng ở Master của Phần 2 — nơi phải hỏi các cơ sở thay vì đọc tại chỗ. */
    public boolean atMaster() {
        return SiteContext.MASTER.equals(SiteContext.explicit());
    }

    /**
     * Gộp danh sách từ mọi cơ sở. Cơ sở tắt thì bỏ qua và ghi log: màn xem thiếu
     * một cơ sở còn hơn trắng trơn — đúng kịch bản demo tắt site.
     */
    public <T> List<T> collect(Supplier<List<T>> body) {
        return collect(body, false);
    }

    /**
     * Như {@link #collect} nhưng cơ sở tắt thì NÉM. Dùng khi kết quả được CHỐT
     * lại — danh sách người nhận lúc gửi: thiếu một cơ sở là người ở đó không
     * bao giờ nhận được, kể cả khi cơ sở bật lại.
     */
    public <T> List<T> collectAll(Supplier<List<T>> body) {
        return collect(body, true);
    }

    private <T> List<T> collect(Supplier<List<T>> body, boolean strict) {
        List<T> all = new ArrayList<>();
        for (String site : sites.nodes().keySet()) {
            try {
                all.addAll(SiteContext.runAt(site, body));
            } catch (RuntimeException ex) {
                if (strict) {
                    throw ex;
                }
                log.warn("CrossSiteQuery: bỏ qua cơ sở {} — {}", site, ex.toString());
            }
        }
        return all;
    }

    /**
     * Có cơ sở nào trả {@code true} không. Cơ sở tắt thì NÉM: đây là phép kiểm an
     * toàn (ví dụ trước khi xoá danh mục), không chắc thì phải từ chối.
     */
    public boolean anySite(Supplier<Boolean> body) {
        for (String site : sites.nodes().keySet()) {
            if (SiteContext.runAt(site, body)) {
                return true;
            }
        }
        return false;
    }
}
