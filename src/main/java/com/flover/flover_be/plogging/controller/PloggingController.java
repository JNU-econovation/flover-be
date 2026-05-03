package com.flover.flover_be.plogging.controller;

import com.flover.flover_be.global.auth.LoginUserId;
import com.flover.flover_be.plogging.dto.PloggingDto;
import com.flover.flover_be.plogging.service.PloggingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Plogging", description = "플로깅 API")
@RestController
@RequestMapping("/api/plogging-sessions")
@RequiredArgsConstructor
public class PloggingController {

    private final PloggingService ploggingService;

    @Operation(summary = "플로깅 완료 기록 저장",
            description = "플로깅 완료 데이터를 저장합니다. 지도 이미지와 인증샷은 S3 URL로 전달합니다.")
    @PostMapping("/complete")
    public ResponseEntity<PloggingDto.CompleteResponse> completePlogging(
            @LoginUserId Long userId,
            @RequestBody @Valid PloggingDto.CompleteRequest request
    ) {
        return ResponseEntity.ok(ploggingService.complete(userId, request));
    }
}
