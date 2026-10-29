package vn.ptit.one.auth.security;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import vn.ptit.one.auth.model.RefreshTokenRecord;
import vn.ptit.one.auth.repository.AccountRepository;
import vn.ptit.one.auth.repository.RefreshTokenRepository;
import vn.ptit.one.shared.routing.SiteContext;
import vn.ptit.one.shared.routing.SiteProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Refresh chạy đúng lúc access token đã hết hạn, nên chỉ còn cách hỏi từng nút.
 * Lỗi cũ: luôn hỏi Master → mọi SV/GV bị đăng xuất sau 15 phút ở Phần 2.
 * Mock trả theo nút đang đứng — đúng cách RoutingDataSource chọn database.
 */
class LoginSiteResolverTest {

    private static final Optional<RefreshTokenRecord> CO =
            Optional.of(new RefreshTokenRecord(UUID.randomUUID(), UUID.randomUUID(), Instant.MAX, null, null));

    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final LoginSiteResolver resolver =
            new LoginSiteResolver(mock(AccountRepository.class), tokens, sites());

    private static SiteProperties sites() {
        Map<String, SiteProperties.Node> nodes = new LinkedHashMap<>();
        for (String site : List.of("HCM", "HN", "DN")) {
            nodes.put(site, new SiteProperties.Node("jdbc:x", "u", "p"));
        }
        return new SiteProperties(null, nodes, 5);
    }

    @Test
    void tokenNamOCoSoNaoThiTraDungCoSoDo() {
        when(tokens.findByHash(any())).thenAnswer(inv -> "HN".equals(SiteContext.current()) ? CO : Optional.empty());

        assertThat(resolver.siteOfRefreshToken("rt")).isEqualTo("HN");
        assertThat(SiteContext.explicit()).as("tra lai nguyen trang sau khi hoi").isNull();
    }

    @Test
    void khongNutNaoCoThiVeMaster() {
        when(tokens.findByHash(any())).thenReturn(Optional.empty());

        assertThat(resolver.siteOfRefreshToken("rt")).isEqualTo(SiteContext.MASTER);
    }

    @Test
    void motCoSoTatKhongChanNguoiDungCoSoKhac() {
        when(tokens.findByHash(any())).thenAnswer(inv -> {
            if ("HN".equals(SiteContext.current())) {
                throw new CannotGetJdbcConnectionException("HN tat");
            }
            return "DN".equals(SiteContext.current()) ? CO : Optional.empty();
        });

        assertThat(resolver.siteOfRefreshToken("rt")).isEqualTo("DN");
    }
}
