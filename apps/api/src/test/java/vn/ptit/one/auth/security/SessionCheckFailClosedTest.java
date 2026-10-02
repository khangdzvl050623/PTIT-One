package vn.ptit.one.auth.security;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import tools.jackson.databind.json.JsonMapper;
import vn.ptit.one.auth.service.SessionService;
import vn.ptit.one.shared.exception.ApiErrorWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DB lỗi lúc kiểm phiên → từ chối bằng 503, KHÔNG chấp nhận JWT chỉ vì chữ ký
 * hợp lệ. Dùng mock vì cần giả lập DB hỏng; đây không phải bằng chứng tích hợp
 * SQL Server — việc đó thuộc AuthFlowIntegrationTest.
 */
class SessionCheckFailClosedTest {

    @Test
    @SuppressWarnings("unchecked")
    void databaseFailureDuringSessionCheckIsRejectedWith503() throws Exception {
        SessionService sessions = mock(SessionService.class);
        when(sessions.verify(any(), anyString(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("CENTRAL không phản hồi"));
        ObjectProvider<SessionService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sessions);

        Jwt jwt = Jwt.withTokenValue("chu-ky-hop-le").header("alg", "HS256")
                .subject("B26DCCN001")
                .claim(AccessTokenIssuer.CLAIM_SESSION, UUID.randomUUID().toString())
                .claim(AccessTokenIssuer.CLAIM_VERSION, 1)
                .build();

        Throwable thrown = catchThrowable(() -> new SessionJwtAuthenticationConverter(provider).convert(jwt));
        assertThat(thrown).isInstanceOf(SessionCheckUnavailableException.class);

        MockHttpServletResponse response = new MockHttpServletResponse();
        new JsonAuthErrorHandler(new ApiErrorWriter(JsonMapper.builder().build()))
                .commence(new MockHttpServletRequest(), response, (SessionCheckUnavailableException) thrown);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("\"code\":\"SERVICE_UNAVAILABLE\"")
                .doesNotContain("CENTRAL không phản hồi");
    }
}
