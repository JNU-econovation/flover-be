package com.flover.flover_be.plogging.controller;

import com.flover.flover_be.plogging.exception.PloggingErrorCode;
import com.flover.flover_be.plogging.exception.PloggingException;
import com.flover.flover_be.plogging.service.PloggingAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "Plogging", description = "플로깅 API")
@RestController
@RequestMapping("/api/plogging")
@RequiredArgsConstructor
public class PloggingAnalysisController {

    private final PloggingAnalysisService ploggingAnalysisService;

    @Operation(summary = "플로깅 이미지 AI 분석",
            description = "플로깅 중 촬영한 이미지를 AI 서버로 전송하여 쓰레기 감지 분석을 비동기로 수행합니다. 분석 결과는 DB에 저장됩니다.")
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> analyzeImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        try {
            byte[] imageBytes = image.getBytes();
            ploggingAnalysisService.analyzeAsync(imageBytes, image.getOriginalFilename(), latitude, longitude);
        } catch (IOException e) {
            log.error("이미지 데이터 읽기 실패: {}", e.getMessage());
            throw new PloggingException(PloggingErrorCode.IMAGE_PROCESSING_FAILED);
        }
        return ResponseEntity.ok().build();
    }
}
