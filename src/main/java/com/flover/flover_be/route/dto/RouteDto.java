package com.flover.flover_be.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class RouteDto {
    public record Request(
            double lat,
            double lon,
            @Positive int distance,
            @NotBlank String mode
    ) {}

    public record Response(
            double distanceMeter,
            long timeMillis,
            String encodedPath
    ) {}
}
