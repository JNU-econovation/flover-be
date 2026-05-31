package com.plover.plover_be.user.service;

import com.plover.plover_be.global.config.KakaoProperties;
import com.plover.plover_be.global.jwt.JwtProvider;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.dto.AuthDto;
import com.plover.plover_be.user.dto.KakaoDto;
import com.plover.plover_be.user.exception.KakaoAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KakaoAuthServiceTest {

    @Mock private RestClient restClient;
    @Mock private UserService userService;
    @Mock private JwtProvider jwtProvider;
    @Mock private KakaoProperties kakaoProperties;

    @InjectMocks private KakaoAuthService kakaoAuthService;

    private RestClient.RequestBodyUriSpec postUriSpec;
    private RestClient.RequestBodySpec postBodySpec;
    private RestClient.ResponseSpec postResponseSpec;

    private RestClient.RequestHeadersUriSpec getUriSpec;
    private RestClient.RequestHeadersSpec getHeadersSpec;
    private RestClient.ResponseSpec getResponseSpec;

    @BeforeEach
    void setUp() {
        postUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        postBodySpec = mock(RestClient.RequestBodySpec.class);
        postResponseSpec = mock(RestClient.ResponseSpec.class);

        getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        getHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        getResponseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(postUriSpec);
        when(postUriSpec.uri(anyString())).thenReturn(postBodySpec);
        when(postBodySpec.contentType(any())).thenReturn(postBodySpec);
        doReturn(postBodySpec).when(postBodySpec).body(any(Object.class));
        when(postBodySpec.retrieve()).thenReturn(postResponseSpec);

        when(restClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.header(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
    }

    private KakaoDto.UserInfoResponse buildUserInfo(Long id, String email) {
        var profile = new KakaoDto.UserInfoResponse.KakaoAccount.Profile("홍길동", "http://img.url");
        var account = new KakaoDto.UserInfoResponse.KakaoAccount(email, profile);
        return new KakaoDto.UserInfoResponse(id, account);
    }

    private void stubKakaoApi(KakaoDto.UserInfoResponse userInfo) {
        when(postResponseSpec.body(KakaoDto.TokenResponse.class))
                .thenReturn(new KakaoDto.TokenResponse("kakao-token", "Bearer", "refresh", 3600L));
        when(getResponseSpec.body(KakaoDto.UserInfoResponse.class)).thenReturn(userInfo);
    }

    @DisplayName("카카오 로그인 성공 시 JWT를 반환한다")
    @Test
    void 카카오_로그인_성공() {
        // given
        KakaoDto.UserInfoResponse userInfo = buildUserInfo(999L, "user@test.com");
        User user = User.create(OAuthProvider.KAKAO, "999", "user@test.com", "플러버123456", null);

        stubKakaoApi(userInfo);
        when(userService.upsertOAuthUser(OAuthProvider.KAKAO, "999", "user@test.com")).thenReturn(user);
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        // when
        AuthDto.LoginResponse result = kakaoAuthService.kakaoLogin("auth-code");

        // then
        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @DisplayName("카카오 로그인 시 provider=KAKAO, providerId=카카오ID(String)로 upsert를 호출한다")
    @Test
    void 카카오_upsert_인자_검증() {
        // given
        KakaoDto.UserInfoResponse userInfo = buildUserInfo(999L, "user@test.com");
        User user = User.create(OAuthProvider.KAKAO, "999", "user@test.com", "플러버123456", null);

        stubKakaoApi(userInfo);
        when(userService.upsertOAuthUser(any(), any(), any())).thenReturn(user);
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        // when
        kakaoAuthService.kakaoLogin("auth-code");

        // then
        verify(userService).upsertOAuthUser(OAuthProvider.KAKAO, "999", "user@test.com");
    }

    @DisplayName("카카오 계정에 이메일이 없으면 null로 upsert를 호출한다")
    @Test
    void 이메일_없는_카카오_계정_upsert() {
        // given
        KakaoDto.UserInfoResponse userInfo = buildUserInfo(999L, null);
        User user = User.create(OAuthProvider.KAKAO, "999", null, "플러버123456", null);

        stubKakaoApi(userInfo);
        when(userService.upsertOAuthUser(any(), any(), any())).thenReturn(user);
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        // when
        kakaoAuthService.kakaoLogin("auth-code");

        // then
        verify(userService).upsertOAuthUser(OAuthProvider.KAKAO, "999", null);
    }

    @DisplayName("카카오 토큰 발급에 실패하면 KakaoAuthException이 발생한다")
    @Test
    void 카카오_토큰_실패시_KakaoAuthException_발생() {
        when(postResponseSpec.body(KakaoDto.TokenResponse.class))
                .thenThrow(mock(RestClientResponseException.class));

        assertThatThrownBy(() -> kakaoAuthService.kakaoLogin("bad-code"))
                .isInstanceOf(KakaoAuthException.class);
    }
}