package com.plover.plover_be.plogging.service;

import com.plover.plover_be.plogging.domain.TrashDetection;
import com.plover.plover_be.plogging.dto.AiResponseDto;
import com.plover.plover_be.plogging.repository.TrashDetectionRepository;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class PloggingAnalysisService {

    private static final String RAW_TRASH_REPORT_PROVIDER = "plover";

    private final RestClient restClient;
    private final TrashDetectionRepository trashDetectionRepository;
    private final ObjectProvider<JdbcTemplate> postgresJdbcTemplateProvider;
    private final String aiServerUrl;
    private final String aiApiKey;

    public PloggingAnalysisService(
            RestClient restClient,
            TrashDetectionRepository trashDetectionRepository,
            @Qualifier("postgresJdbcTemplate") ObjectProvider<JdbcTemplate> postgresJdbcTemplateProvider,
            @Value("${ai.server.url}") String aiServerUrl,
            @Value("${ai.api.key}") String aiApiKey
    ) {
        this.restClient = restClient;
        this.trashDetectionRepository = trashDetectionRepository;
        this.postgresJdbcTemplateProvider = postgresJdbcTemplateProvider;
        this.aiServerUrl = aiServerUrl;
        this.aiApiKey = aiApiKey;
    }

    @Async("aiAnalysisExecutor")
    public void analyzeAsync(byte[] imageBytes, String filename, Double latitude, Double longitude, LocalDateTime reportedAt) {
        log.info("AI 이미지 분석 요청 시작: 위도={}, 경도={}", latitude, longitude);
        try {
            AiResponseDto response = callAiServer(imageBytes, filename, latitude, longitude);

            if (response == null || !"success".equals(response.status())) {
                log.warn("AI 서버로부터 유효하지 않은 응답 수신: status={}", response != null ? response.status() : "null");
                return;
            }
            saveDetections(response, latitude, longitude, reportedAt);
            log.info("AI 이미지 분석 완료: 총 감지 항목 수={}", response.totalCount());
        } catch (RestClientException e) {
            log.error("AI 서버 연결 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("AI 이미지 분석 처리 중 오류 발생: {}", e.getMessage());
        }
    }

    private AiResponseDto callAiServer(byte[] imageBytes, String filename, Double latitude, Double longitude) {

        ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return (filename != null) ? filename : "trash_image.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", imageResource);
        body.add("latitude", String.valueOf(latitude));
        body.add("longitude", String.valueOf(longitude));

        return restClient.post()
                .uri(aiServerUrl)
                .header("X-API-Key", aiApiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(AiResponseDto.class);
    }

    private void saveDetections(AiResponseDto response, Double latitude, Double longitude, LocalDateTime reportedAt) {
        Map<String, Integer> counts = response.counts();

        List<TrashDetection> detections = response.detections().stream()
                .map(detection -> TrashDetection.create(
                        latitude,
                        longitude,
                        detection.type(),
                        1,
                        detection.confidence()
                ))
                .toList();

        List<TrashDetection> savedDetections = trashDetectionRepository.saveAll(detections);
        saveRawTrashReports(savedDetections, reportedAt);
        log.info("쓰레기 감지 결과 저장 완료: {}건", detections.size());
    }

    private void saveRawTrashReports(List<TrashDetection> detections, LocalDateTime reportedAt) {
        JdbcTemplate postgresJdbcTemplate = postgresJdbcTemplateProvider.getIfAvailable();

        if (postgresJdbcTemplate == null || detections.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO raw_trash_reports (source_id, provider, reported_at, geometry)
                VALUES (?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326))
                """;

        for (TrashDetection detection : detections) {
            postgresJdbcTemplate.update(
                    sql,
                    "trash_detection:" + detection.getId(),
                    RAW_TRASH_REPORT_PROVIDER,
                    reportedAt,
                    detection.getLongitude(),
                    detection.getLatitude()
            );
        }
    }
}
