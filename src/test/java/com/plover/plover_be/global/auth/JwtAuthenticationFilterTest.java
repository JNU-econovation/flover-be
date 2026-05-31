package com.plover.plover_be.global.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plover.plover_be.global.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtProvider jwtProvider;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private JwtAuthenticationFilter filter;

    @DisplayName("유효한 토큰이면 userId가 request attribute에 저장된다")
    @Test
    void valid_token_saves_userId_attribute() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        given(jwtProvider.getUserId("valid-token")).willReturn(1L);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE)).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    @DisplayName("토큰이 없으면 attribute 없이 다음 필터로 넘어간다")
    @Test
    void no_token_passes_through() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE)).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @DisplayName("유효하지 않은 토큰이면 401을 반환하고 다음 필터로 넘어가지 않는다")
    @Test
    void invalid_token_returns_401() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        given(jwtProvider.getUserId("invalid-token"))
                .willThrow(new AuthException(AuthErrorCode.INVALID_TOKEN));

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
