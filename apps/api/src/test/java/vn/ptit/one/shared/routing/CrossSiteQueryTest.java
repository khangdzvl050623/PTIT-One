package vn.ptit.one.shared.routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hai chế độ của port: màn XEM bỏ qua cơ sở tắt; kết quả bị CHỐT (người nhận
 * lúc gửi) hoặc phép kiểm an toàn (trước khi xoá danh mục) thì phải ném — bỏ
 * qua ở đó là mất người nhận / gãy nhân bản vĩnh viễn.
 */
class CrossSiteQueryTest {

    private final CrossSiteQuery query = new CrossSiteQuery(sites());

    private static SiteProperties sites() {
        Map<String, SiteProperties.Node> nodes = new LinkedHashMap<>();
        for (String site : List.of("HCM", "HN", "DN")) {
            nodes.put(site, new SiteProperties.Node("jdbc:x", "u", "p"));
        }
        return new SiteProperties(null, nodes, 5);
    }

    /** Mỗi cơ sở trả mã của chính nó; HN đang tắt. */
    private static List<String> hnTat() {
        if ("HN".equals(SiteContext.current())) {
            throw new CannotGetJdbcConnectionException("HN tat");
        }
        return List.of(SiteContext.current());
    }

    @AfterEach
    void don() {
        SiteContext.clear();
    }

    @Test
    void manXemBoQuaCoSoTat() {
        assertThat(query.collect(CrossSiteQueryTest::hnTat)).containsExactly("HCM", "DN");
    }

    @Test
    void ketQuaBiChotThiCoSoTatPhaiNem() {
        assertThatThrownBy(() -> query.collectAll(CrossSiteQueryTest::hnTat))
                .isInstanceOf(CannotGetJdbcConnectionException.class);
    }

    @Test
    void phepKiemAnToanCoSoTatPhaiNem() {
        assertThatThrownBy(() -> query.anySite(() -> hnTat().isEmpty()))
                .isInstanceOf(CannotGetJdbcConnectionException.class);
    }

    @Test
    void chiTinhLaMasterKhiDaDatTuongMinh() {
        assertThat(query.atMaster()).as("Phan 1: chua ai dat co so").isFalse();
        SiteContext.set(SiteContext.MASTER);
        assertThat(query.atMaster()).isTrue();
        SiteContext.set("HCM");
        assertThat(query.atMaster()).isFalse();
    }
}
