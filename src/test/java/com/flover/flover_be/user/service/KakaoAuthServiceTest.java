package com.flover.flover_be.user.service;

import com.flover.flover_be.global.config.KakaoProperties;
import com.flover.flover_be.global.jwt.JwtProvider;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.dto.AuthDto;
import com.flover.flover_be.user.dto.KakaoDto;
import com.flover.flover_be.user.exception.KakaoAuthException;
import com.flover.flover_be.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KakaoAuthServiceTest {

    @Mock private RestClient restClient;
    @Mock private UserRepository userRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private KakaoProperties kakaoProperties;

    @InjectMocks
    private KakaoAuthService kakaoAuthService;

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

    private KakaoDto.UserInfoResponse buildUserInfo(Long id, String email, String nickname) {
        var profile = new KakaoDto.UserInfoResponse.KakaoAccount.Profile(nickname, "http://img.url");
        var account = new KakaoDto.UserInfoResponse.KakaoAccount(email, profile);
        return new KakaoDto.UserInfoResponse(id, account);
    }

    @Test
    void 신규_유저면_save_호출() {
        KakaoDto.UserInfoResponse userInfo = buildUserInfo(999L, "user@test.com", "홍길동");
        User saved = User.create(999L, "user@test.com", "플러버123456", null);

        when(postResponseSpec.body(KakaoDto.TokenResponse.class))
                .thenReturn(new KakaoDto.TokenResponse("kakao-token", "Bearer", "refresh", 3600L));
        when(getResponseSpec.body(KakaoDto.UserInfoResponse.class)).thenReturn(userInfo);
        when(userRepository.findByKakaoId(999L)).thenReturn(Optional.empty());
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        AuthDto.LoginResponse result = kakaoAuthService.kakaoLogin("auth-code");

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 신규_유저_기본_닉네임이_플러버_형식으로_생성됨() {
        KakaoDto.UserInfoResponse userInfo = buildUserInfo(999L, "user@test.com", "홍길동");

        when(postResponseSpec.body(KakaoDto.TokenResponse.class))
                .thenReturn(new KakaoDto.TokenResponse("kakao-token", "Bearer", "refresh", 3600L));
        when(getResponseSpec.body(KakaoDto.UserInfoResponse.class)).thenReturn(userInfo);
        when(userRepository.findByKakaoId(999L)).thenReturn(Optional.empty());
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        kakaoAuthService.kakaoLogin("auth-code");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).matches("플러버\\d{6}");
    }

    @Test
    void 닉네임_중복시_재생성() {
        KakaoDto.UserInfoResponse userInfo = buildUserInfo(999L, "user@test.com", "홍길동");

        when(postResponseSpec.body(KakaoDto.TokenResponse.class))
                .thenReturn(new KakaoDto.TokenResponse("kakao-token", "Bearer", "refresh", 3600L));
        when(getResponseSpec.body(KakaoDto.UserInfoResponse.class)).thenReturn(userInfo);
        when(userRepository.findByKakaoId(999L)).thenReturn(Optional.empty());
        when(userRepository.existsByNickname(anyString())).thenReturn(true).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        kakaoAuthService.kakaoLogin("auth-code");

        verify(userRepository, times(2)).existsByNickname(anyString());
    }

    @Test
    void 기존_유저면_save_호출_안하고_닉네임_유지() {
        KakaoDto.UserInfoResponse userInfo = buildUserInfo(999L, "user@test.com", "새닉네임");
        User existing = User.create(999L, "user@test.com", "기존닉네임", "old.jpg");

        when(postResponseSpec.body(KakaoDto.TokenResponse.class))
                .thenReturn(new KakaoDto.TokenResponse("kakao-token", "Bearer", "refresh", 3600L));
        when(getResponseSpec.body(KakaoDto.UserInfoResponse.class)).thenReturn(userInfo);
        when(userRepository.findByKakaoId(999L)).thenReturn(Optional.of(existing));
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        kakaoAuthService.kakaoLogin("auth-code");

        assertThat(existing.getNickname()).isEqualTo("기존닉네임");
        verify(userRepository, never()).save(any());
    }

    @Test
    void 카카오_토큰_실패시_KakaoAuthException_발생() {
        when(postResponseSpec.body(KakaoDto.TokenResponse.class))
                .thenThrow(mock(RestClientResponseException.class));

        assertThatThrownBy(() -> kakaoAuthService.kakaoLogin("bad-code"))
                .isInstanceOf(KakaoAuthException.class);
    }
}
