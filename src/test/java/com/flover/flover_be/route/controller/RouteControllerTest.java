package com.flover.flover_be.route.controller;

import com.flover.flover_be.global.exception.GlobalExceptionHandler;
import com.flover.flover_be.route.client.RouteEngineClient;
import com.flover.flover_be.route.dto.RouteDto;
import com.flover.flover_be.route.exception.RouteErrorCode;
import com.flover.flover_be.route.exception.RouteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RouteControllerTest {

    @Mock
    private RouteEngineClient routeEngineClient;

    @InjectMocks
    private RouteController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @DisplayName("정상 요청 시 200 OK와 경로 데이터를 반환한다")
    @Test
    void get_plogging_route_성공_200_반환() throws Exception {
        // given
        RouteDto.Response mockResponse = new RouteDto.Response(2500.0, 605000, "encodedPathData");
        given(routeEngineClient.getRoute(any(RouteDto.Request.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/routes")
                        .param("lat", "35.1769")
                        .param("lon", "126.9058")
                        .param("time", "30")
                        .param("mode", "PLOGGING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceMeter").value(2500.0))
                .andExpect(jsonPath("$.timeMillis").value(605000))
                .andExpect(jsonPath("$.encodedPath").value("encodedPathData"));
    }

    @DisplayName("time 파라미터가 30분일 때 distance 2500m로 변환되어 전달된다")
    @Test
    void get_plogging_route_시간_거리_변환_검증() throws Exception {
        // given
        RouteDto.Response mockResponse = new RouteDto.Response(2500.0, 605000, "encodedPathData");
        given(routeEngineClient.getRoute(any(RouteDto.Request.class))).willReturn(mockResponse);

        // when
        mockMvc.perform(get("/api/v1/routes")
                        .param("lat", "35.1769")
                        .param("lon", "126.9058")
                        .param("time", "30"))
                .andExpect(status().isOk());

        // then
        org.mockito.ArgumentCaptor<RouteDto.Request> captor =
                org.mockito.ArgumentCaptor.forClass(RouteDto.Request.class);
        verify(routeEngineClient).getRoute(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().distance()).isEqualTo(2500);
    }

    @DisplayName("mode 미지정 시 기본값 PLOGGING이 적용된다")
    @Test
    void get_plogging_route_기본_모드_PLOGGING() throws Exception {
        // given
        RouteDto.Response mockResponse = new RouteDto.Response(2500.0, 605000, "encodedPathData");
        given(routeEngineClient.getRoute(any(RouteDto.Request.class))).willReturn(mockResponse);

        // when
        mockMvc.perform(get("/api/v1/routes")
                        .param("lat", "35.1769")
                        .param("lon", "126.9058")
                        .param("time", "30"))
                .andExpect(status().isOk());

        // then
        org.mockito.ArgumentCaptor<RouteDto.Request> captor =
                org.mockito.ArgumentCaptor.forClass(RouteDto.Request.class);
        verify(routeEngineClient).getRoute(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().mode()).isEqualTo("PLOGGING");
    }

    @DisplayName("lat 파라미터 누락 시 400을 반환한다")
    @Test
    void get_plogging_route_lat_누락_400_반환() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/routes")
                        .param("lon", "126.9058")
                        .param("time", "30"))
                .andExpect(status().isBadRequest());

        verify(routeEngineClient, never()).getRoute(any());
    }

    @DisplayName("lon 파라미터 누락 시 400을 반환한다")
    @Test
    void get_plogging_route_lon_누락_400_반환() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/routes")
                        .param("lat", "35.1769")
                        .param("time", "30"))
                .andExpect(status().isBadRequest());

        verify(routeEngineClient, never()).getRoute(any());
    }

    @DisplayName("time 파라미터 누락 시 400을 반환한다")
    @Test
    void get_plogging_route_time_누락_400_반환() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/routes")
                        .param("lat", "35.1769")
                        .param("lon", "126.9058"))
                .andExpect(status().isBadRequest());

        verify(routeEngineClient, never()).getRoute(any());
    }

    @DisplayName("라우팅 엔진 통신 실패 시 503을 반환한다")
    @Test
    void get_plogging_route_엔진_실패_503_반환() throws Exception {
        // given
        given(routeEngineClient.getRoute(any(RouteDto.Request.class)))
                .willThrow(new RouteException(RouteErrorCode.ROUTE_ENGINE_CONNECTION_FAILED));

        // when & then
        mockMvc.perform(get("/api/v1/routes")
                        .param("lat", "35.1769")
                        .param("lon", "126.9058")
                        .param("time", "30")
                        .param("mode", "PLOGGING"))
                .andExpect(status().isServiceUnavailable());
    }
}
