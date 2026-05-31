package com.plover.plover_be.user.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDto {

    public record CallbackRequest(@NotBlank String code) {}

    public record KakaoTokenRequest(@NotBlank String accessToken) {}

    public record LoginResponse(
            String accessToken,
            String tokenType,
            Long userId,
            String nickname,
            String email
    ) {}

}
