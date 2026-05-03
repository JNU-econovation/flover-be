package com.flover.flover_be.plogging.dto;

import com.flover.flover_be.plogging.domain.PloggingMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.List;

public class PloggingDto {

    public record CompleteRequest(
            @NotNull PloggingMode mode,
            @NotNull LocalDateTime startedAt,
            @NotNull LocalDateTime finishedAt,
            @PositiveOrZero int distanceMeters,
            @PositiveOrZero int stepCount,
            @PositiveOrZero int caloriesBurned,
            @Positive int ploggingSeconds,
            @PositiveOrZero int restSeconds,
            String placeName,
            @NotNull Double startLatitude,
            @NotNull Double startLongitude,
            @NotNull Double endLatitude,
            @NotNull Double endLongitude,
            @NotNull @Valid List<RoutePointRequest> routePoints,
            String mapImageUrl,
            @NotNull List<String> photoUrls
    ) {}

    public record RoutePointRequest(
            @NotNull Double latitude,
            @NotNull Double longitude
    ) {}

    public record CompleteResponse(Long ploggingSessionId) {}
}
