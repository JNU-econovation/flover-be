package com.plover.plover_be.crew.controller;

import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.service.CrewPloggingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrewPloggingControllerTest {

    @Mock private CrewPloggingService crewPloggingService;
    @InjectMocks private CrewPloggingController crewPloggingController;

    @DisplayName("세션 전체 취소는 요청 본문 없이 로그인 사용자 ID를 서비스에 전달한다")
    @Test
    void cancel_session_uses_authenticated_user_id() {
        // given
        CrewPloggingDto.SessionResponse response = new CrewPloggingDto.SessionResponse(
                10L, CrewPloggingStatus.CANCELED, null, null, null,
                false, null, false, 0, false
        );
        given(crewPloggingService.cancelSession(1L, 10L)).willReturn(response);

        // when
        CrewPloggingDto.SessionResponse body = crewPloggingController.cancelSession(1L, 10L).getBody();

        // then
        assertThat(body).isEqualTo(response);
        verify(crewPloggingService).cancelSession(1L, 10L);
    }
}
