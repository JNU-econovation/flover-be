package com.flover.flover_be.user.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDto {

    public record CallbackRequest(@NotBlank String code) {}

    public record LoginResponse(
            String accessToken,
            String tokenType,
            Long userId,
            String nickname,
            String email
    ) {}

}
