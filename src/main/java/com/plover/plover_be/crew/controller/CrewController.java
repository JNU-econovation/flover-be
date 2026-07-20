package com.plover.plover_be.crew.controller;

import com.plover.plover_be.crew.dto.CrewDto;
import com.plover.plover_be.crew.service.CrewService;
import com.plover.plover_be.global.auth.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Crew", description = "크루 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;

    @Operation(
            summary = "크루 생성",
            description = "인증 사용자가 크루장이 되는 크루를 생성하고 숫자 6자리 영구 참여 코드를 발급합니다. 참여 코드 동시 충돌은 409이며 클라이언트가 다시 요청해야 합니다."
    )
    @PostMapping
    public ResponseEntity<CrewDto.CrewResponse> createCrew(
            @LoginUserId Long userId,
            @RequestBody @Valid CrewDto.CreateRequest request
    ) {
        return ResponseEntity.ok(crewService.createCrew(userId, request.name()));
    }

    @Operation(
            summary = "참여 코드로 크루 가입",
            description = "앞뒤 공백을 제거한 숫자 6자리 참여 코드로 가입합니다. WITHDRAWN 멤버십은 최초 joinedAt을 유지한 채 ACTIVE로 복구하며, 이미 ACTIVE이면 409입니다."
    )
    @PostMapping("/join")
    public ResponseEntity<CrewDto.CrewResponse> joinCrew(
            @LoginUserId Long userId,
            @RequestBody @Valid CrewDto.JoinRequest request
    ) {
        return ResponseEntity.ok(crewService.joinCrew(userId, request.joinCode()));
    }

    @Operation(
            summary = "내 크루 목록 조회",
            description = "로그인 사용자의 ACTIVE 멤버십만 joinedAt 내림차순으로 반환합니다. 탈퇴 또는 강퇴된 크루는 포함하지 않습니다."
    )
    @GetMapping
    public ResponseEntity<CrewDto.CrewListResponse> getMyCrews(@LoginUserId Long userId) {
        return ResponseEntity.ok(crewService.findMyCrews(userId));
    }

    @Operation(
            summary = "크루 상세 조회",
            description = "현재 ACTIVE 크루원만 조회할 수 있습니다. 기존 응답 계약대로 현재 크루원, 누적 통계, 활성 세션과 완료 기록을 함께 반환합니다."
    )
    @GetMapping("/{crewId}")
    public ResponseEntity<CrewDto.CrewDetailResponse> getCrew(
            @LoginUserId Long userId,
            @Parameter(description = "크루 ID", example = "10") @PathVariable Long crewId
    ) {
        return ResponseEntity.ok(crewService.findCrew(userId, crewId));
    }

    @Operation(
            summary = "크루원 목록 조회",
            description = "현재 ACTIVE 크루원만 호출할 수 있으며 크루장을 포함한 ACTIVE 크루원을 최초 joinedAt 오름차순으로 반환합니다. WITHDRAWN 회원은 제외됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ACTIVE 크루원 목록 조회 성공"),
            @ApiResponse(responseCode = "403", description = "요청자가 현재 ACTIVE 크루원이 아님")
    })
    @GetMapping("/{crewId}/members")
    public ResponseEntity<CrewDto.CrewMemberListResponse> getMembers(
            @LoginUserId Long userId,
            @Parameter(description = "크루 ID", example = "10") @PathVariable Long crewId
    ) {
        return ResponseEntity.ok(crewService.findMembers(userId, crewId));
    }

    @Operation(
            summary = "크루원 프로필 조회",
            description = "요청자와 대상 모두 해당 크루의 ACTIVE 회원이어야 합니다. 공개 프로필과 DB에서 집계한 전체 개인 플로깅 통계만 반환하며 이메일, OAuth 정보, 경로, 사진은 공개하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공개 프로필과 개인 플로깅 집계 조회 성공"),
            @ApiResponse(responseCode = "403", description = "요청자 또는 대상이 현재 ACTIVE 크루원이 아님")
    })
    @GetMapping("/{crewId}/members/{targetUserId}")
    public ResponseEntity<CrewDto.CrewMemberProfileResponse> getMemberProfile(
            @LoginUserId Long userId,
            @Parameter(description = "크루 ID", example = "10") @PathVariable Long crewId,
            @Parameter(description = "조회 대상 사용자 ID", example = "21") @PathVariable Long targetUserId
    ) {
        return ResponseEntity.ok(crewService.findMemberProfile(userId, crewId, targetUserId));
    }

    @Operation(
            summary = "크루 자발적 탈퇴",
            description = "ACTIVE 일반 크루원의 멤버십을 WITHDRAWN으로 변경합니다. 크루장은 탈퇴할 수 없습니다. RECRUITING의 JOINED 참가자는 CANCELED로 바뀌며, IN_PROGRESS/COMPLETING의 PARTICIPATING 또는 비정상 JOINED 참가자는 409입니다. 과거 기록과 사진은 유지되고 같은 코드로 재가입할 수 있습니다. 이미 WITHDRAWN이면 ACTIVE 크루원 권한이 없어 403입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 성공. 응답 body 없음"),
            @ApiResponse(responseCode = "403", description = "현재 ACTIVE 크루원이 아님"),
            @ApiResponse(responseCode = "409", description = "크루장 또는 탈퇴 불가능한 진행 참가 상태")
    })
    @DeleteMapping("/{crewId}/members/me")
    public ResponseEntity<Void> withdraw(
            @LoginUserId Long userId,
            @Parameter(description = "탈퇴할 크루 ID", example = "10") @PathVariable Long crewId
    ) {
        crewService.withdraw(userId, crewId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "크루원 강퇴",
            description = "크루장만 ACTIVE 일반 크루원을 WITHDRAWN으로 변경할 수 있습니다. 자발적 탈퇴와 같은 상태·세션 정책을 사용하고 과거 기록 및 공유 사진을 보존합니다. 크루장, 자기 자신, 이미 WITHDRAWN인 대상 또는 미제출 진행 참가자는 409입니다. 멱등 API가 아닙니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "강퇴 성공. 응답 body 없음"),
            @ApiResponse(responseCode = "403", description = "요청자가 ACTIVE 크루장 또는 크루원이 아님"),
            @ApiResponse(responseCode = "404", description = "대상 크루원을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "크루장·자기 자신·이미 WITHDRAWN인 대상 또는 탈퇴 불가능한 진행 참가 상태")
    })
    @DeleteMapping("/{crewId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @LoginUserId Long userId,
            @Parameter(description = "크루 ID", example = "10") @PathVariable Long crewId,
            @Parameter(description = "강퇴 대상 사용자 ID", example = "21") @PathVariable Long targetUserId
    ) {
        crewService.removeMember(userId, crewId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}
