package com.flover.flover_be.facility.dto;

public class FacilityDto {

    public record TrashBinResponse(
            Long id,
            String name,
            String roadAddress,
            double latitude,
            double longitude,
            String trashType,
            long distanceMeters
    ) {}

    public record ToiletResponse(
            Long id,
            String name,
            String roadAddress,
            double latitude,
            double longitude,
            String toiletType,
            String openTimeType,
            long distanceMeters
    ) {}
}
