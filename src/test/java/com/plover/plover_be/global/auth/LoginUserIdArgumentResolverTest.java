package com.plover.plover_be.global.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class LoginUserIdArgumentResolverTest {

    private final LoginUserIdArgumentResolver resolver = new LoginUserIdArgumentResolver();

    @DisplayName("@LoginUserId 어노테이션과 Long 타입 파라미터를 지원한다")
    @Test
    void supports_loginUserId_annotation_with_long_type() {
        // given
        MethodParameter parameter = mock(MethodParameter.class);
        given(parameter.hasParameterAnnotation(LoginUserId.class)).willReturn(true);
        given(parameter.getParameterType()).willReturn((Class) Long.class);

        // when & then
        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @DisplayName("@LoginUserId 어노테이션이 없으면 지원하지 않는다")
    @Test
    void not_supports_without_annotation() {
        // given
        MethodParameter parameter = mock(MethodParameter.class);
        given(parameter.hasParameterAnnotation(LoginUserId.class)).willReturn(false);

        // when & then
        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @DisplayName("request attribute에 userId가 있으면 반환한다")
    @Test
    void resolves_userId_from_attribute() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, 1L);
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        given(webRequest.getNativeRequest()).willReturn(request);

        // when
        Object result = resolver.resolveArgument(mock(MethodParameter.class), null, webRequest, null);

        // then
        assertThat(result).isEqualTo(1L);
    }

    @DisplayName("request attribute에 userId가 없으면 AuthException이 발생한다")
    @Test
    void throws_auth_exception_when_no_userId() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        given(webRequest.getNativeRequest()).willReturn(request);

        // when & then
        assertThatThrownBy(() -> resolver.resolveArgument(mock(MethodParameter.class), null, webRequest, null))
                .isInstanceOf(AuthException.class);
    }
}
