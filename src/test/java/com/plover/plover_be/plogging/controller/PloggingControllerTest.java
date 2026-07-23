package com.plover.plover_be.plogging.controller;

import com.plover.plover_be.crew.service.CrewPloggingCompletionService;
import com.plover.plover_be.global.auth.LoginUserId;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.service.PloggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PloggingControllerTest {

    @Mock private PloggingService ploggingService;
    @Mock private CrewPloggingCompletionService crewPloggingCompletionService;
    @InjectMocks private PloggingController ploggingController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver loginUserIdResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(LoginUserId.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return 1L;
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(ploggingController)
                .setCustomArgumentResolvers(loginUserIdResolver)
                .build();
    }

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

    @DisplayName("UTC 오프셋이 포함된 같이 플로깅 완료 요청을 수신한다")
    @Test
    void complete_accepts_utc_offset_timestamps() throws Exception {
        // given
        String requestBody = """
                {
                  "mode": "FREE",
                  "startedAt": "2026-07-23T14:30:00.000Z",
                  "finishedAt": "2026-07-23T14:30:30.000Z",
                  "distanceMeters": 100,
                  "stepCount": 30,
                  "caloriesBurned": 10,
                  "ploggingSeconds": 30,
                  "restSeconds": 0,
                  "placeName": "",
                  "startLatitude": 37.5,
                  "startLongitude": 127.0,
                  "endLatitude": 37.5,
                  "endLongitude": 127.0,
                  "routePoints": [],
                  "mapImageUrl": null,
                  "photoUrls": [],
                  "crewPloggingSessionId": 10
                }
                """;

        // when & then
        mockMvc.perform(post("/api/plogging-sessions/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
        verify(crewPloggingCompletionService).complete(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(PloggingDto.CompleteRequest.class)
        );
    }

    @DisplayName("개인 플로깅 상세 조회 응답의 기록 시각은 UTC 오프셋을 포함한다")
    @Test
    void get_session_returns_record_times_with_utc_offset() throws Exception {
        // given
        PloggingDto.SessionDetailResponse response = new PloggingDto.SessionDetailResponse(
                10L,
                PloggingMode.FREE,
                Instant.parse("2026-07-24T15:00:00Z"),
                Instant.parse("2026-07-24T15:00:30Z"),
                "공원",
                100,
                30,
                10,
                30,
                0,
                null,
                List.of()
        );
        given(ploggingService.findSession(1L, 10L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/plogging-sessions/{ploggingSessionId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startedAt").value("2026-07-24T15:00:00Z"))
                .andExpect(jsonPath("$.finishedAt").value("2026-07-24T15:00:30Z"));
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
