package com.flover.flover_be.plogging.dto;

import com.flover.flover_be.plogging.domain.PloggingMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double startLatitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double startLongitude,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double endLatitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double endLongitude,
            @NotNull @Valid List<RoutePointRequest> routePoints,
            String mapImageUrl,
            @NotNull List<@NotBlank String> photoUrls
    ) {}

    public record RoutePointRequest(
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
    ) {}

    public record CompleteResponse(Long ploggingSessionId) {}
}
