package com.plover.plover_be.user.dto;

import jakarta.validation.constraints.NotBlank;

public class UserDto {

    public record NicknameRequest(@NotBlank String nickname) {}

    public record NicknameResponse(Long userId, String nickname) {}

    public record ProfileImageResponse(Long userId, String profileImageUrl) {}

    public record ProfileImageUrlRequest(@NotBlank String imageUrl) {}

    public record UserInfoResponse(String nickname, int level, String title, String profileImageUrl, long experience) {}

    public record PloggingStatsResponse(
            long totalPloggingCount,
            long totalStepCount,
            long totalDistanceMeters
    ) {}
}
