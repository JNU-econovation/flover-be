package com.plover.plover_be.plogging.service;

import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import com.plover.plover_be.plogging.repository.PloggingRoutePointRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.service.UserPloggingPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PloggingUserDataServiceTest {

    @Mock private PloggingSessionRepository ploggingSessionRepository;
    @Mock private PloggingPhotoRepository ploggingPhotoRepository;
    @Mock private PloggingRoutePointRepository ploggingRoutePointRepository;
    @InjectMocks private PloggingUserDataService ploggingUserDataService;

    @DisplayName("사용자의 플로깅 누적 통계를 반환한다")
    @Test
    void find_stats_returns_user_plogging_stats() {
        // given
        Long userId = 1L;
        PloggingSessionRepository.PloggingStatsView stats =
                mock(PloggingSessionRepository.PloggingStatsView.class);
        given(stats.getCount()).willReturn(3L);
        given(stats.getTotalStepCount()).willReturn(12_000L);
        given(stats.getTotalDistanceMeters()).willReturn(8_500L);
        given(ploggingSessionRepository.findStatsByUserId(userId)).willReturn(stats);

        // when
        UserPloggingPort.PloggingStats result = ploggingUserDataService.findStats(userId);

        // then
        assertThat(result.count()).isEqualTo(3L);
        assertThat(result.totalStepCount()).isEqualTo(12_000L);
        assertThat(result.totalDistanceMeters()).isEqualTo(8_500L);
    }

    @DisplayName("사용자의 플로깅 데이터를 외래 키 순서에 맞춰 삭제한다")
    @Test
    void delete_by_user_id_deletes_plogging_data_in_fk_order() {
        // given
        Long userId = 1L;

        // when
        ploggingUserDataService.deleteByUserId(userId);

        // then
        InOrder inOrder = inOrder(
                ploggingPhotoRepository,
                ploggingRoutePointRepository,
                ploggingSessionRepository
        );
        inOrder.verify(ploggingPhotoRepository).deleteByPloggingSessionUserId(userId);
        inOrder.verify(ploggingRoutePointRepository).deleteByPloggingSessionUserId(userId);
        inOrder.verify(ploggingSessionRepository).deleteByUserId(userId);
    }
}
