package com.plover.plover_be.facility.service;

import com.plover.plover_be.facility.dto.FacilityDto;
import com.plover.plover_be.facility.repository.ToiletRepository;
import com.plover.plover_be.facility.repository.TrashBinRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock private TrashBinRepository trashBinRepository;
    @Mock private ToiletRepository toiletRepository;
    @InjectMocks private FacilityService facilityService;

    private static final double LAT = 35.1496;
    private static final double LNG = 126.9219;
    private static final int RADIUS = 1000;

    @DisplayName("주변 쓰레기통을 정상적으로 조회한다")
    @Test
    void find_nearby_trash_bins_성공() {
        // given
        TrashBinRepository.TrashBinView view = mock(TrashBinRepository.TrashBinView.class);
        given(view.getId()).willReturn(1L);
        given(view.getName()).willReturn("광주 쓰레기통");
        given(view.getRoadAddress()).willReturn("광주 동구 금남로 1");
        given(view.getLatitude()).willReturn(35.1496);
        given(view.getLongitude()).willReturn(126.9219);
        given(view.getTrashType()).willReturn("일반");
        given(view.getDistanceMeters()).willReturn(150.7);

        given(trashBinRepository.findNearby(LAT, LNG, RADIUS)).willReturn(List.of(view));

        // when
        List<FacilityDto.TrashBinResponse> result = facilityService.findNearbyTrashBins(LAT, LNG);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("광주 쓰레기통");
        assertThat(result.get(0).trashType()).isEqualTo("일반");
        assertThat(result.get(0).distanceMeters()).isEqualTo(151L);
    }

    @DisplayName("주변 쓰레기통이 없으면 빈 리스트를 반환한다")
    @Test
    void find_nearby_trash_bins_빈결과() {
        // given
        given(trashBinRepository.findNearby(LAT, LNG, RADIUS)).willReturn(List.of());

        // when
        List<FacilityDto.TrashBinResponse> result = facilityService.findNearbyTrashBins(LAT, LNG);

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("distanceMeters는 반올림하여 long으로 반환한다")
    @Test
    void find_nearby_trash_bins_거리_반올림() {
        // given
        TrashBinRepository.TrashBinView view1 = mock(TrashBinRepository.TrashBinView.class);
        given(view1.getId()).willReturn(1L);
        given(view1.getName()).willReturn("A");
        given(view1.getRoadAddress()).willReturn(null);
        given(view1.getLatitude()).willReturn(35.1);
        given(view1.getLongitude()).willReturn(126.9);
        given(view1.getTrashType()).willReturn(null);
        given(view1.getDistanceMeters()).willReturn(100.4);

        TrashBinRepository.TrashBinView view2 = mock(TrashBinRepository.TrashBinView.class);
        given(view2.getId()).willReturn(2L);
        given(view2.getName()).willReturn("B");
        given(view2.getRoadAddress()).willReturn(null);
        given(view2.getLatitude()).willReturn(35.1);
        given(view2.getLongitude()).willReturn(126.9);
        given(view2.getTrashType()).willReturn(null);
        given(view2.getDistanceMeters()).willReturn(200.5);

        given(trashBinRepository.findNearby(LAT, LNG, RADIUS)).willReturn(List.of(view1, view2));

        // when
        List<FacilityDto.TrashBinResponse> result = facilityService.findNearbyTrashBins(LAT, LNG);

        // then
        assertThat(result.get(0).distanceMeters()).isEqualTo(100L);
        assertThat(result.get(1).distanceMeters()).isEqualTo(201L);
    }

    @DisplayName("주변 화장실을 정상적으로 조회한다")
    @Test
    void find_nearby_toilets_성공() {
        // given
        ToiletRepository.ToiletView view = mock(ToiletRepository.ToiletView.class);
        given(view.getId()).willReturn(1L);
        given(view.getName()).willReturn("광주역 화장실");
        given(view.getRoadAddress()).willReturn("광주 북구 경열로 1");
        given(view.getLatitude()).willReturn(35.1496);
        given(view.getLongitude()).willReturn(126.9219);
        given(view.getToiletType()).willReturn("공중화장실");
        given(view.getOpenTimeType()).willReturn("24시간");
        given(view.getDistanceMeters()).willReturn(300.2);

        given(toiletRepository.findNearby(LAT, LNG, RADIUS)).willReturn(List.of(view));

        // when
        List<FacilityDto.ToiletResponse> result = facilityService.findNearbyToilets(LAT, LNG);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("광주역 화장실");
        assertThat(result.get(0).toiletType()).isEqualTo("공중화장실");
        assertThat(result.get(0).openTimeType()).isEqualTo("24시간");
        assertThat(result.get(0).distanceMeters()).isEqualTo(300L);
    }

    @DisplayName("주변 화장실이 없으면 빈 리스트를 반환한다")
    @Test
    void find_nearby_toilets_빈결과() {
        // given
        given(toiletRepository.findNearby(LAT, LNG, RADIUS)).willReturn(List.of());

        // when
        List<FacilityDto.ToiletResponse> result = facilityService.findNearbyToilets(LAT, LNG);

        // then
        assertThat(result).isEmpty();
    }
}
