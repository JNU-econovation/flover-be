package com.flover.flover_be.route.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RouteDto {
    public record Request(
            @NotNull Double lat,
            @NotNull Double lon,
            @NotNull @Positive Integer time,
            String mode
    ) {
        public Request {
            if (mode == null || mode.isBlank()) {
                mode = "PLOGGING";
            }
        }
    }

    public record Response(
            double distanceMeter,
            long timeMillis,
            String encodedPath
    ) {}
}
