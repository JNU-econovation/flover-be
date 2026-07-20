package com.plover.plover_be.crew.controller;

import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.service.CrewPloggingService;
import com.plover.plover_be.global.auth.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CrewPloggingController {

    private final CrewPloggingService crewPloggingService;

    @Operation(summary = "같이 플로깅 세션 생성",
            description = "크루장만 호출할 수 있습니다. 활성 세션(RECRUITING/IN_PROGRESS/COMPLETING)이 있으면 새로 만들지 않고 기존 세션을 반환하는 멱등 동작입니다.")
    @PostMapping("/crews/{crewId}/plogging-sessions")
    public ResponseEntity<CrewPloggingDto.SessionResponse> createSession(
            @LoginUserId Long userId,
            @Parameter(description = "크루 ID", example = "10") @PathVariable Long crewId
    ) {
        return ResponseEntity.ok(crewPloggingService.createSession(userId, crewId));
    }

    @Operation(summary = "크루 활성 같이 플로깅 세션 조회",
            description = "ACTIVE 크루원만 조회할 수 있습니다. RECRUITING, IN_PROGRESS, COMPLETING만 반환하며 CANCELED와 COMPLETED는 제외합니다. 참가 전 폴링에 사용합니다.")
    @GetMapping("/crews/{crewId}/plogging-sessions/active")
    public ResponseEntity<CrewPloggingDto.SessionResponse> getActiveSession(
            @LoginUserId Long userId,
            @Parameter(description = "크루 ID", example = "10") @PathVariable Long crewId
    ) {
        return ResponseEntity.ok(crewPloggingService.findActiveSession(userId, crewId));
    }

    @Operation(summary = "같이 플로깅 상태 폴링",
            description = "ACTIVE 크루원이 참가 후 sessionId로 폴링합니다. CANCELED 세션도 조회되며 status와 로그인 사용자의 participantStatus에 CANCELED가 반영됩니다. recordSubmittedByMe가 true이면 측정이나 완료 요청을 반복하지 마세요.")
    @GetMapping("/crew-plogging-sessions/{sessionId}")
    public ResponseEntity<CrewPloggingDto.SessionResponse> getSession(
            @LoginUserId Long userId,
            @Parameter(description = "같이 플로깅 세션 ID", example = "100") @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.findSession(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 참가",
            description = "ACTIVE 크루원이 RECRUITING 세션에 참가합니다. 취소했던 참가자는 기존 참가 행을 JOINED로 복구하며, 이미 참가한 요청은 기존 상태를 반환합니다.")
    @PostMapping("/crew-plogging-sessions/{sessionId}/participants/me")
    public ResponseEntity<CrewPloggingDto.SessionResponse> joinSession(
            @LoginUserId Long userId,
            @Parameter(description = "같이 플로깅 세션 ID", example = "100") @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.joinSession(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 참가 취소",
            description = "RECRUITING의 일반 참가자만 취소할 수 있습니다. 크루장은 취소할 수 없고, 이미 CANCELED이면 기존 상태를 반환합니다.")
    @DeleteMapping("/crew-plogging-sessions/{sessionId}/participants/me")
    public ResponseEntity<CrewPloggingDto.SessionResponse> cancelParticipation(
            @LoginUserId Long userId,
            @Parameter(description = "같이 플로깅 세션 ID", example = "100") @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.cancelParticipation(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 시작",
            description = "크루장이 RECRUITING 세션을 IN_PROGRESS로 전환하고 JOINED 참가자를 PARTICIPATING으로 변경합니다. 이미 IN_PROGRESS이면 기존 상태를 반환합니다.")
    @PostMapping("/crew-plogging-sessions/{sessionId}/start")
    public ResponseEntity<CrewPloggingDto.SessionResponse> startSession(
            @LoginUserId Long userId,
            @Parameter(description = "같이 플로깅 세션 ID", example = "100") @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.startSession(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 모집 전체 취소",
            description = "크루장만 RECRUITING 세션을 CANCELED로 전환할 수 있으며 JOINED 참가자도 CANCELED가 됩니다. 이미 CANCELED이면 현재 상태를 반환하는 멱등 동작입니다. 취소 후 active 조회에서는 제외되지만 단건 폴링에서는 조회됩니다.")
    @PostMapping("/crew-plogging-sessions/{sessionId}/cancel")
    public ResponseEntity<CrewPloggingDto.SessionResponse> cancelSession(
            @LoginUserId Long userId,
            @Parameter(description = "같이 플로깅 세션 ID", example = "100") @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.cancelSession(userId, sessionId));
    }

    @Operation(summary = "같이 플로깅 전체 종료",
            description = "크루장이 IN_PROGRESS 세션의 공통 종료 시각을 저장합니다. 미제출 PARTICIPATING 참가자가 있으면 COMPLETING, 전원이 SUBMITTED이면 즉시 COMPLETED가 됩니다. 이미 COMPLETING/COMPLETED이면 현재 상태를 반환합니다.")
    @PostMapping("/crew-plogging-sessions/{sessionId}/end")
    public ResponseEntity<CrewPloggingDto.SessionResponse> endSession(
            @LoginUserId Long userId,
            @Parameter(description = "같이 플로깅 세션 ID", example = "100") @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.endSession(userId, sessionId));
    }

    @Operation(summary = "크루 플로깅 기록 목록 조회",
            description = "현재 ACTIVE 크루원만 완료된 크루 기록을 최신순으로 조회할 수 있습니다. 대표 기록 스냅샷과 공유 사진 수·최신 사진 URL을 반환합니다.")
    @GetMapping("/crews/{crewId}/plogging-records")
    public ResponseEntity<CrewPloggingDto.RecordListResponse> getRecords(
            @LoginUserId Long userId,
            @Parameter(description = "크루 ID", example = "10") @PathVariable Long crewId,
            @ParameterObject
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(crewPloggingService.findRecords(userId, crewId, pageable));
    }

    @Operation(summary = "크루 플로깅 기록 상세 조회",
            description = "현재 ACTIVE 크루원만 조회할 수 있습니다. 대표 수치 스냅샷, 과거 참가자 공개 정보와 참가자 전체 공유 사진을 반환하며 개인 경로·경험치·개인 상세 기록은 노출하지 않습니다.")
    @GetMapping("/crews/{crewId}/plogging-records/{sessionId}")
    public ResponseEntity<CrewPloggingDto.RecordDetailResponse> getRecord(
            @LoginUserId Long userId,
            @Parameter(description = "크루 ID", example = "10") @PathVariable Long crewId,
            @Parameter(description = "완료된 같이 플로깅 세션 ID", example = "100") @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(crewPloggingService.findRecord(userId, crewId, sessionId));
    }
}
