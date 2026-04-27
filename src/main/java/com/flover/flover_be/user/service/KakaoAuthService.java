package com.flover.flover_be.user.service;

import com.flover.flover_be.global.config.KakaoProperties;
import com.flover.flover_be.global.jwt.JwtProvider;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.dto.AuthDto;
import com.flover.flover_be.user.dto.KakaoDto;
import com.flover.flover_be.user.exception.AuthErrorCode;
import com.flover.flover_be.user.exception.KakaoAuthException;
import com.flover.flover_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com";
    private static final String KAKAO_API_URL = "https://kapi.kakao.com";

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;

    @Transactional
    public AuthDto.LoginResponse kakaoLogin(String code) {
        String kakaoAccessToken = getKakaoAccessToken(code);
        KakaoDto.UserInfoResponse userInfo = getKakaoUserInfo(kakaoAccessToken);
        User user = upsertUser(userInfo);
        String jwtToken = jwtProvider.generateToken(user.getId());

        return new AuthDto.LoginResponse(jwtToken, "Bearer", user.getId(), user.getNickname(), user.getEmail());
    }

    private String getKakaoAccessToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoProperties.clientId());
        params.add("client_secret", kakaoProperties.clientSecret());
        params.add("redirect_uri", kakaoProperties.redirectUri());
        params.add("code", code);

        try {
            KakaoDto.TokenResponse response = restClient.post()
                    .uri(KAKAO_AUTH_URL + "/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(KakaoDto.TokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new KakaoAuthException(AuthErrorCode.KAKAO_TOKEN_FAILED);
            }
            return response.accessToken();
        } catch (RestClientResponseException e) {
            log.error("카카오 토큰 요청 실패 - status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new KakaoAuthException(AuthErrorCode.KAKAO_TOKEN_FAILED);
        } catch (RestClientException e) {
            log.error("카카오 토큰 요청 실패 - 네트워크 오류: {}", e.getMessage());
            throw new KakaoAuthException(AuthErrorCode.KAKAO_TOKEN_FAILED);
        }
    }

    private KakaoDto.UserInfoResponse getKakaoUserInfo(String accessToken) {
        try {
            KakaoDto.UserInfoResponse response = restClient.get()
                    .uri(KAKAO_API_URL + "/v2/user/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoDto.UserInfoResponse.class);

            if (response == null) {
                throw new KakaoAuthException(AuthErrorCode.KAKAO_USER_INFO_FAILED);
            }
            return response;
        } catch (RestClientException e) {
            throw new KakaoAuthException(AuthErrorCode.KAKAO_USER_INFO_FAILED);
        }
    }

    private User upsertUser(KakaoDto.UserInfoResponse userInfo) {
        KakaoDto.UserInfoResponse.KakaoAccount account = userInfo.kakaoAccount();
        String email = account != null ? account.email() : null;
        String nickname = (account != null && account.profile() != null) ? account.profile().nickname() : null;
        String profileImageUrl = (account != null && account.profile() != null) ? account.profile().profileImageUrl() : null;

        return userRepository.findByKakaoId(userInfo.id())
                .map(user -> {
                    user.updateProfile(nickname, profileImageUrl);
                    return user;
                })
                .orElseGet(() -> userRepository.save(User.create(userInfo.id(), email, nickname, profileImageUrl)));
    }
}
