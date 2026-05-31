package com.plover.plover_be.plogging.controller;

import com.plover.plover_be.global.auth.LoginUserId;
import com.plover.plover_be.global.storage.StorageDto;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.service.PloggingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Plogging", description = "플로깅 API")
@Validated
@RestController
@RequestMapping("/api/plogging-sessions")
@RequiredArgsConstructor
public class PloggingController {

    private final PloggingService ploggingService;

    @Operation(summary = "플로깅 기록 전체 조회", description = "로그인한 사용자의 플로깅 기록 목록을 최신순으로 반환합니다. 무한 스크롤을 위한 페이징을 지원합니다.")
    @GetMapping
    public ResponseEntity<PloggingDto.SessionListResponse> getSessions(
            @LoginUserId Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ploggingService.findSessions(userId, pageable));
    }

    @Operation(summary = "플로깅 기록 단건 조회", description = "로그인한 사용자의 특정 플로깅 기록 상세 정보를 조회합니다.")
    @GetMapping("/{ploggingSessionId}")
    public ResponseEntity<PloggingDto.SessionDetailResponse> getSession(
            @LoginUserId Long userId,
            @PathVariable Long ploggingSessionId
    ) {
        return ResponseEntity.ok(ploggingService.findSession(userId, ploggingSessionId));
    }

    @Operation(summary = "월간 플로깅 통계 조회", description = "지정한 연월의 플로깅 누적 통계를 반환합니다.")
    @GetMapping("/monthly")
    public ResponseEntity<PloggingDto.MonthlyStatsResponse> getMonthlyStats(
            @LoginUserId Long userId,
            @RequestParam @Min(1) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        return ResponseEntity.ok(ploggingService.findMonthlyStats(userId, year, month));
    }

    @Operation(summary = "주간 플로깅 통계 조회", description = "startDate 기준 7일간의 날짜별 플로깅 통계를 반환합니다.")
    @GetMapping("/weekly")
    public ResponseEntity<PloggingDto.WeeklyStatsResponse> getWeeklyStats(
            @LoginUserId Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return ResponseEntity.ok(ploggingService.findWeeklyStats(userId, startDate));
    }

    @Operation(summary = "플로깅 완료 기록 저장",
            description = "플로깅 완료 데이터를 저장합니다. 지도 이미지와 인증샷은 S3 URL로 전달합니다.")
    @PostMapping("/complete")
    public ResponseEntity<PloggingDto.CompleteResponse> completePlogging(
            @LoginUserId Long userId,
            @RequestBody @Valid PloggingDto.CompleteRequest request
    ) {
        return ResponseEntity.ok(ploggingService.complete(userId, request));
    }

    @Operation(summary = "지도 이미지 업로드 URL 발급",
            description = "플로깅 경로 지도 이미지를 S3에 직접 업로드하기 위한 Presigned URL을 발급합니다.")
    @GetMapping("/map-image/upload-url")
    public ResponseEntity<StorageDto.PresignedUploadUrlResponse> getMapImageUploadUrl(
            @LoginUserId Long userId,
            @RequestParam String contentType
    ) {
        return ResponseEntity.ok(ploggingService.generateMapImagePresignedUrl(userId, contentType));
    }

    @Operation(summary = "플로깅 인증샷 업로드 URL 발급",
            description = "플로깅 인증샷을 S3에 직접 업로드하기 위한 Presigned URL을 발급합니다.")
    @GetMapping("/photo/upload-url")
    public ResponseEntity<StorageDto.PresignedUploadUrlResponse> getPhotoUploadUrl(
            @LoginUserId Long userId,
            @RequestParam String contentType
    ) {
        return ResponseEntity.ok(ploggingService.generatePhotoPresignedUrl(userId, contentType));
    }
}
