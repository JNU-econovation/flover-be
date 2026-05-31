package com.plover.plover_be.facility.controller;

import com.plover.plover_be.facility.dto.FacilityDto;
import com.plover.plover_be.facility.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Facility", description = "주변 시설 API")
@Validated
@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @Operation(summary = "주변 쓰레기통 조회", description = "현재 위치 기준 반경 1000m 이내 쓰레기통을 가까운 순으로 반환합니다.")
    @GetMapping("/trash-bins")
    public ResponseEntity<List<FacilityDto.TrashBinResponse>> getNearbyTrashBins(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lng
    ) {
        return ResponseEntity.ok(facilityService.findNearbyTrashBins(lat, lng));
    }

    @Operation(summary = "주변 화장실 조회", description = "현재 위치 기준 반경 1000m 이내 화장실을 가까운 순으로 반환합니다.")
    @GetMapping("/toilets")
    public ResponseEntity<List<FacilityDto.ToiletResponse>> getNearbyToilets(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lng
    ) {
        return ResponseEntity.ok(facilityService.findNearbyToilets(lat, lng));
    }
}
