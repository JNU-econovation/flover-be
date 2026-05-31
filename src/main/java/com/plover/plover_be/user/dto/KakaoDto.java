package com.plover.plover_be.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KakaoDto {

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn
    ) {}

    public record UserInfoResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
        public record KakaoAccount(
                String email,
                Profile profile
        ) {
            public record Profile(
                    String nickname,
                    @JsonProperty("profile_image_url") String profileImageUrl
            ) {}
        }
    }
}
