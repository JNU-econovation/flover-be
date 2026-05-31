package com.plover.plover_be.facility.service;

import com.plover.plover_be.facility.dto.FacilityDto;
import com.plover.plover_be.facility.repository.ToiletRepository;
import com.plover.plover_be.facility.repository.TrashBinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private static final int DEFAULT_SEARCH_RADIUS_METERS = 1000;

    private final TrashBinRepository trashBinRepository;
    private final ToiletRepository toiletRepository;

    public List<FacilityDto.TrashBinResponse> findNearbyTrashBins(double lat, double lng) {
        return trashBinRepository.findNearby(lat, lng, DEFAULT_SEARCH_RADIUS_METERS).stream()
                .map(v -> new FacilityDto.TrashBinResponse(
                        v.getId(), v.getName(), v.getRoadAddress(),
                        v.getLatitude(), v.getLongitude(), v.getTrashType(),
                        Math.round(v.getDistanceMeters())
                ))
                .toList();
    }

    public List<FacilityDto.ToiletResponse> findNearbyToilets(double lat, double lng) {
        return toiletRepository.findNearby(lat, lng, DEFAULT_SEARCH_RADIUS_METERS).stream()
                .map(v -> new FacilityDto.ToiletResponse(
                        v.getId(), v.getName(), v.getRoadAddress(),
                        v.getLatitude(), v.getLongitude(), v.getToiletType(), v.getOpenTimeType(),
                        Math.round(v.getDistanceMeters())
                ))
                .toList();
    }
}
