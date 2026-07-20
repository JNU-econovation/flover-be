package com.plover.plover_be.crew.controller;

import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.service.CrewPloggingService;
import com.plover.plover_be.global.auth.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Crew Plogging", description = "같이 플로깅 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CrewPloggingController {

    private final CrewPloggingService crewPloggingService;

    @Operation(summary = "같이 플로깅 세션 생성")
    @PostMapping("/crews/{crewId}/plogging-sessions")
    public ResponseEntity<CrewPloggingDto.SessionResponse> createSession(
            @LoginUserId Long userId,
            @PathVariable Long crewId
    ) {
        return ResponseEntity.ok(crewPloggingService.createSession(userId, crewId));
    }

    @Operation(summary = "크루 활성 같이 플로깅 세션 조회")
    @GetMapping("/crews/{crewId}/plogging-sessions/active")
    public ResponseEntity<CrewPloggingDto.SessionResponse> getActiveSession(
            @LoginUserId Long userId,
            @PathVariable Long crewId
    ) {
        return ResponseEntity.ok(crewPloggingService.findActiveSession(userId, crewId));
    }

    @Operation(summary = "같이 플로깅 상태 폴링")
    @GetMapping("/crew-plogging-sessions/{sessionId}")
    public ResponseEntity<CrewPloggingDto.SessionResponse> getSession(
            @LoginUserId Long userId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.findSession(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 참가")
    @PostMapping("/crew-plogging-sessions/{sessionId}/participants/me")
    public ResponseEntity<CrewPloggingDto.SessionResponse> joinSession(
            @LoginUserId Long userId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.joinSession(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 참가 취소")
    @DeleteMapping("/crew-plogging-sessions/{sessionId}/participants/me")
    public ResponseEntity<CrewPloggingDto.SessionResponse> cancelParticipation(
            @LoginUserId Long userId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.cancelParticipation(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 시작")
    @PostMapping("/crew-plogging-sessions/{sessionId}/start")
    public ResponseEntity<CrewPloggingDto.SessionResponse> startSession(
            @LoginUserId Long userId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.startSession(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 세션 전체 취소")
    @PostMapping("/crew-plogging-sessions/{sessionId}/cancel")
    public ResponseEntity<CrewPloggingDto.SessionResponse> cancelSession(
            @LoginUserId Long userId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.cancelSession(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 전체 종료")
    @PostMapping("/crew-plogging-sessions/{sessionId}/end")
    public ResponseEntity<CrewPloggingDto.SessionResponse> endSession(
            @LoginUserId Long userId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.endSession(userId, sessionId));
    }

    @Operation(summary = "크루 플로깅 기록 목록 조회")
    @GetMapping("/crews/{crewId}/plogging-records")
    public ResponseEntity<CrewPloggingDto.RecordListResponse> getRecords(
            @LoginUserId Long userId,
            @PathVariable Long crewId,
            @ParameterObject
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(crewPloggingService.findRecords(userId, crewId, pageable));
    }

    @Operation(summary = "크루 플로깅 기록 상세 조회")
    @GetMapping("/crews/{crewId}/plogging-records/{sessionId}")
    public ResponseEntity<CrewPloggingDto.RecordDetailResponse> getRecord(
            @LoginUserId Long userId,
            @PathVariable Long crewId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.findRecord(userId, crewId, sessionId));
    }
}
