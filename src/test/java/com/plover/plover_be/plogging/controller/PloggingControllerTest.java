package com.plover.plover_be.plogging.controller;

import com.plover.plover_be.crew.service.CrewPloggingCompletionService;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.service.PloggingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PloggingControllerTest {

    @Mock private PloggingService ploggingService;
    @Mock private CrewPloggingCompletionService crewPloggingCompletionService;
    @InjectMocks private PloggingController ploggingController;

    @DisplayName("크루 세션 ID가 null이면 기존 개인 완료 서비스를 그대로 호출한다")
    @Test
    void complete_without_crew_session_uses_personal_service() {
        // given
        PloggingDto.CompleteRequest request = request(null);
        PloggingDto.CompleteResponse response = new PloggingDto.CompleteResponse(1L, 0, 10, 1, 1);
        given(ploggingService.complete(1L, request)).willReturn(response);

        // when
        ploggingController.completePlogging(1L, request);

        // then
        verify(ploggingService).complete(1L, request);
        verify(crewPloggingCompletionService, never()).complete(1L, request);
    }

    @DisplayName("크루 세션 ID가 있으면 크루 완료 서비스를 호출한다")
    @Test
    void complete_with_crew_session_uses_crew_service() {
        // given
        PloggingDto.CompleteRequest request = request(10L);

        // when
        ploggingController.completePlogging(1L, request);

        // then
        verify(crewPloggingCompletionService).complete(1L, request);
        verify(ploggingService, never()).complete(1L, request);
    }

    private PloggingDto.CompleteRequest request(Long crewSessionId) {
        return new PloggingDto.CompleteRequest(
                PloggingMode.FREE,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                1000, 2000, 100, 3600, 0, "공원",
                37.5, 127.0, 37.6, 127.1,
                List.of(), null, List.of(), crewSessionId
        );
    }
}
