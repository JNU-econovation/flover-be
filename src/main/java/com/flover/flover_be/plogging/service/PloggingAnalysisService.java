package com.flover.flover_be.plogging.service;

import com.flover.flover_be.plogging.domain.TrashDetection;
import com.flover.flover_be.plogging.dto.AiResponseDto;
import com.flover.flover_be.plogging.repository.TrashDetectionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class PloggingAnalysisService {

    private final RestClient restClient;
    private final TrashDetectionRepository trashDetectionRepository;
    private final String aiServerUrl;
    private final String aiApiKey;

    public PloggingAnalysisService(
            RestClient restClient,
            TrashDetectionRepository trashDetectionRepository,
            @Value("${ai.server.url}") String aiServerUrl,
            @Value("${ai.api.key}") String aiApiKey
    ) {
        this.restClient = restClient;
        this.trashDetectionRepository = trashDetectionRepository;
        this.aiServerUrl = aiServerUrl;
        this.aiApiKey = aiApiKey;
    }

    @Async("aiAnalysisExecutor")
    public void analyzeAsync(byte[] imageBytes, String filename, Double latitude, Double longitude) {
        log.info("AI 이미지 분석 요청 시작: 위도={}, 경도={}", latitude, longitude);
        try {
            AiResponseDto response = callAiServer(imageBytes, filename, latitude, longitude);

            if (response == null || !"success".equals(response.status())) {
                log.warn("AI 서버로부터 유효하지 않은 응답 수신: status={}", response != null ? response.status() : "null");
                return;
            }
            saveDetections(response, latitude, longitude);
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

    private void saveDetections(AiResponseDto response, Double latitude, Double longitude) {
        Map<String, Integer> counts = response.counts();
        List<TrashDetection> detections = new ArrayList<>();
        for (AiResponseDto.Detection detection : response.detections()) {
            int count = counts.getOrDefault(detection.type(), 1);
            detections.add(TrashDetection.create(latitude, longitude, detection.type(), count, detection.confidence()));
        }
        trashDetectionRepository.saveAll(detections);
        log.info("쓰레기 감지 결과 저장 완료: {}건", detections.size());
    }
}
