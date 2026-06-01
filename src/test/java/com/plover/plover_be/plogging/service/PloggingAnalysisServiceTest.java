package com.plover.plover_be.plogging.service;

import com.plover.plover_be.plogging.domain.TrashDetection;
import com.plover.plover_be.plogging.dto.AiResponseDto;
import com.plover.plover_be.plogging.repository.TrashDetectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
class PloggingAnalysisServiceTest {

    @Mock private RestClient restClient;
    @Mock private TrashDetectionRepository trashDetectionRepository;
    @Mock private ObjectProvider<JdbcTemplate> postgresJdbcTemplateProvider;

    private PloggingAnalysisService service;

    private RestClient.RequestBodyUriSpec postUriSpec;
    private RestClient.RequestBodySpec postBodySpec;
    private RestClient.ResponseSpec responseSpec;

    private static final byte[] IMAGE_BYTES = "fake-image-data".getBytes();
    private static final String FILENAME = "trash.jpg";
    private static final Double LATITUDE = 37.5;
    private static final Double LONGITUDE = 127.0;
    private static final LocalDateTime REPORTED_AT = LocalDateTime.of(2026, 5, 28, 12, 0);

    @BeforeEach
    void setUp() {
        service = new PloggingAnalysisService(
                restClient, trashDetectionRepository, postgresJdbcTemplateProvider,
                "http://ai-test/predict", "test-api-key"
        );

        postUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        postBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(postUriSpec);
        when(postUriSpec.uri(anyString())).thenReturn(postBodySpec);
        when(postBodySpec.header(anyString(), anyString())).thenReturn(postBodySpec);
        when(postBodySpec.contentType(any(MediaType.class))).thenReturn(postBodySpec);
        when(postBodySpec.body(any(MultiValueMap.class))).thenReturn(postBodySpec);
        when(postBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @DisplayName("AI 서버 성공 응답 수신 시 감지된 쓰레기 정보를 저장한다")
    @Test
    void analyze_async_단일_감지항목_저장() {
        // given
        AiResponseDto response = new AiResponseDto(
                "success", 1,
                Map.of("비닐류", 1),
                List.of(new AiResponseDto.Detection("비닐류", 0.955)),
                81.88, LATITUDE, LONGITUDE
        );
        given(responseSpec.body(AiResponseDto.class)).willReturn(response);

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        ArgumentCaptor<List<TrashDetection>> captor = ArgumentCaptor.forClass(List.class);
        verify(trashDetectionRepository).saveAll(captor.capture());

        List<TrashDetection> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTrashType()).isEqualTo("비닐류");
        assertThat(saved.get(0).getCount()).isEqualTo(1);
        assertThat(saved.get(0).getConfidence()).isEqualTo(0.955);
        assertThat(saved.get(0).getLatitude()).isEqualTo(LATITUDE);
        assertThat(saved.get(0).getLongitude()).isEqualTo(LONGITUDE);
    }

    @DisplayName("감지 항목이 여러 개일 때 모두 저장한다")
    @Test
    void analyze_async_복수_감지항목_모두_저장() {
        // given
        AiResponseDto response = new AiResponseDto(
                "success", 3,
                Map.of("비닐류", 2, "종이류", 1),
                List.of(
                        new AiResponseDto.Detection("비닐류", 0.95),
                        new AiResponseDto.Detection("비닐류", 0.88),
                        new AiResponseDto.Detection("종이류", 0.72)
                ),
                95.0, LATITUDE, LONGITUDE
        );
        given(responseSpec.body(AiResponseDto.class)).willReturn(response);

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        ArgumentCaptor<List<TrashDetection>> captor = ArgumentCaptor.forClass(List.class);
        verify(trashDetectionRepository).saveAll(captor.capture());

        List<TrashDetection> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved.get(0).getTrashType()).isEqualTo("비닐류");
        assertThat(saved.get(0).getCount()).isEqualTo(1);
        assertThat(saved.get(2).getTrashType()).isEqualTo("종이류");
        assertThat(saved.get(2).getCount()).isEqualTo(1);
    }

    @DisplayName("counts 맵에서 해당 타입의 count를 정확히 매핑한다")
    @Test
    void analyze_async_counts_매핑_정확성() {
        // given
        AiResponseDto response = new AiResponseDto(
                "success", 1,
                Map.of("캔류", 3),
                List.of(new AiResponseDto.Detection("캔류", 0.80)),
                50.0, LATITUDE, LONGITUDE
        );
        given(responseSpec.body(AiResponseDto.class)).willReturn(response);

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        ArgumentCaptor<List<TrashDetection>> captor = ArgumentCaptor.forClass(List.class);
        verify(trashDetectionRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getCount()).isEqualTo(1);
    }

    @DisplayName("counts 맵에 타입이 없으면 count를 1로 저장한다")
    @Test
    void analyze_async_counts_맵_없는_타입은_count_1() {
        // given
        AiResponseDto response = new AiResponseDto(
                "success", 1,
                Map.of(),
                List.of(new AiResponseDto.Detection("유리류", 0.65)),
                60.0, LATITUDE, LONGITUDE
        );
        given(responseSpec.body(AiResponseDto.class)).willReturn(response);

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        ArgumentCaptor<List<TrashDetection>> captor = ArgumentCaptor.forClass(List.class);
        verify(trashDetectionRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getCount()).isEqualTo(1);
    }

    @DisplayName("AI 서버 연결 실패 시 DB에 저장하지 않는다")
    @Test
    void analyze_async_restclient_예외_발생시_저장안함() {
        // given
        given(responseSpec.body(AiResponseDto.class))
                .willThrow(new RestClientException("AI 서버 연결 거부"));

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        verify(trashDetectionRepository, never()).saveAll(any());
    }

    @DisplayName("AI 응답 status가 success가 아니면 저장하지 않는다")
    @Test
    void analyze_async_비성공_status_저장안함() {
        // given
        AiResponseDto failResponse = new AiResponseDto(
                "error", 0,
                Map.of(), List.of(),
                0.0, LATITUDE, LONGITUDE
        );
        given(responseSpec.body(AiResponseDto.class)).willReturn(failResponse);

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        verify(trashDetectionRepository, never()).saveAll(any());
    }

    @DisplayName("AI 서버가 null을 반환하면 저장하지 않는다")
    @Test
    void analyze_async_null_응답_저장안함() {
        // given
        given(responseSpec.body(AiResponseDto.class)).willReturn(null);

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        verify(trashDetectionRepository, never()).saveAll(any());
    }

    @DisplayName("AI 서버 요청 시 X-API-Key 헤더를 포함한다")
    @Test
    void analyze_async_api_key_헤더_포함() {
        // given
        AiResponseDto response = new AiResponseDto(
                "success", 0,
                Map.of(), List.of(),
                10.0, LATITUDE, LONGITUDE
        );
        given(responseSpec.body(AiResponseDto.class)).willReturn(response);

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        verify(postBodySpec).header("X-API-Key", "test-api-key");
    }

    @DisplayName("감지 항목이 없으면 저장하지 않는다")
    @Test
    void analyze_async_감지항목_없으면_저장안함() {
        // given
        AiResponseDto response = new AiResponseDto(
                "success", 0,
                Map.of(), List.of(),
                30.0, LATITUDE, LONGITUDE
        );
        given(responseSpec.body(AiResponseDto.class)).willReturn(response);

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        verify(trashDetectionRepository, never()).saveAll(any());
    }

    @DisplayName("예상치 못한 예외 발생 시 DB에 저장하지 않는다")
    @Test
    void analyze_async_런타임_예외_발생시_저장안함() {
        // given
        given(responseSpec.body(AiResponseDto.class))
                .willThrow(new RuntimeException("예상치 못한 오류"));

        // when
        service.analyzeAsync(IMAGE_BYTES, FILENAME, LATITUDE, LONGITUDE, REPORTED_AT);

        // then
        verify(trashDetectionRepository, never()).saveAll(any());
    }
}
