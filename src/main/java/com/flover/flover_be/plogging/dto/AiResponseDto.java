package com.flover.flover_be.plogging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record AiResponseDto(
        String status,
        @JsonProperty("total_count") int totalCount,
        Map<String, Integer> counts,
        List<Detection> detections,
        @JsonProperty("inference_speed_ms") double inferenceSpeedMs,
        double latitude,
        double longitude
) {
    public record Detection(String type, double confidence) {}
}
