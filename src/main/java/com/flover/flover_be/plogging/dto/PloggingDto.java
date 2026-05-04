package com.flover.flover_be.plogging.dto;

import com.flover.flover_be.plogging.domain.PloggingMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

    public record SessionSummaryResponse(
            Long ploggingSessionId,
            PloggingMode mode,
            String placeName,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            int distanceMeters
    ) {}

    public record SessionListResponse(
            List<SessionSummaryResponse> content,
            boolean hasNext
    ) {}

    public record SessionDetailResponse(
            Long ploggingSessionId,
            PloggingMode mode,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String placeName,
            int distanceMeters,
            int stepCount,
            int caloriesBurned,
            int ploggingSeconds,
            int restSeconds,
            String mapImageUrl,
            List<String> photoUrls
    ) {}

    public record MonthlyStatsResponse(
            int year,
            int month,
            long totalStepCount,
            long totalDistanceMeters,
            long totalCaloriesBurned,
            long totalPloggingCount,
            long totalPloggingSeconds
    ) {}

    public record WeeklyStatsResponse(
            LocalDate startDate,
            LocalDate endDate,
            List<DailyStatsResponse> dailyStats
    ) {}

    public record DailyStatsResponse(
            LocalDate date,
            DayOfWeek dayOfWeek,
            long stepCount,
            long distanceMeters,
            long caloriesBurned,
            long ploggingCount,
            long ploggingSeconds
    ) {}
}
