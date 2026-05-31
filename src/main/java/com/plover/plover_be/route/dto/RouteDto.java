package com.plover.plover_be.route.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

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

    public record RouteInfo(
            double distanceMeter,
            long timeMillis,
            String encodedPath,
            int ploggingScore
    ) {}

    public record Response(
            List<RouteInfo> routes
    ) {}
}
