package com.flover.flover_be.user.dto;

import jakarta.validation.constraints.NotBlank;

public class UserDto {

    public record NicknameRequest(@NotBlank String nickname) {}

    public record NicknameResponse(Long userId, String nickname) {}
}
