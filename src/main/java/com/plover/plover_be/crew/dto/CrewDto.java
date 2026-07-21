package com.plover.plover_be.crew.dto;

import com.plover.plover_be.crew.domain.CrewRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class CrewDto {

    public record CreateRequest(
            @Schema(description = "크루 이름", example = "한강 지킴이")
            @NotBlank @Size(max = 100) String name
    ) {}

    public record JoinRequest(
            @Schema(description = "숫자 6자리 참여 코드. 앞뒤 공백은 제거됩니다.", example = "000527")
            @NotBlank String joinCode
    ) {}

    public record CrewResponse(
            @Schema(description = "크루 ID", example = "10") Long crewId,
            @Schema(description = "크루 이름", example = "한강 지킴이") String name,
            @Schema(description = "영구 귀속된 숫자 6자리 참여 코드", example = "000527") String joinCode,
            @Schema(description = "로그인 사용자의 크루 역할", example = "MEMBER") CrewRole role
    ) {}

    public record CrewMemberResponse(
            @Schema(description = "사용자 ID", example = "21") Long userId,
            @Schema(description = "공개 닉네임", example = "플로버") String nickname,
            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", nullable = true) String profileImageUrl,
            @Schema(description = "크루 역할", example = "MEMBER") CrewRole role
    ) {}

    public record CrewMemberListItemResponse(
            @Schema(description = "사용자 ID", example = "21") Long userId,
            @Schema(description = "공개 닉네임", example = "플로버") String nickname,
            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", nullable = true) String profileImageUrl,
            @Schema(description = "크루 역할", example = "MEMBER") CrewRole role,
            @Schema(description = "최초 가입 시각. 재가입해도 변경되지 않습니다.", example = "2026-07-20T10:30:00") LocalDateTime joinedAt
    ) {}

    public record CrewMemberListResponse(
            @Schema(description = "가입 시각 오름차순 ACTIVE 크루원 목록") List<CrewMemberListItemResponse> members
    ) {}

    public record CrewMemberProfileResponse(
            @Schema(description = "사용자 ID", example = "21") Long userId,
            @Schema(description = "공개 닉네임", example = "플로버") String nickname,
            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", nullable = true) String profileImageUrl,
            @Schema(description = "현재 레벨", example = "3") int level,
            @Schema(description = "현재 경험치", example = "1440") long experience,
            @Schema(description = "전체 개인 플로깅 횟수", example = "12") long ploggingCount,
            @Schema(description = "전체 개인 플로깅 누적 걸음 수", example = "42000") long totalStepCount,
            @Schema(description = "전체 개인 플로깅 누적 거리(m)", example = "28500") long totalDistanceMeters
    ) {}

    public record CrewListItemResponse(
            @Schema(description = "크루 ID", example = "10") Long crewId,
            @Schema(description = "크루 이름", example = "한강 지킴이") String name,
            @Schema(description = "ACTIVE 크루원 수", example = "5") long memberCount,
            @Schema(description = "로그인 사용자의 역할", example = "MEMBER") CrewRole myRole,
            @Schema(description = "대표 기록이 존재하는 완료 세션 수", example = "8") long completedPloggingCount,
            @Schema(description = "대표 기록 누적 걸음 수", example = "32000") long totalStepCount,
            @Schema(description = "대표 기록 누적 거리(m)", example = "21000") long totalDistanceMeters,
            @Schema(description = "대표 기록 누적 플로깅 시간(초)", example = "14400") long totalPloggingSeconds,
            @Schema(description = "활성 같이 플로깅 세션 존재 여부", example = "true") boolean hasActiveSession,
            @Schema(description = "활성 세션 상태", example = "RECRUITING", nullable = true)
            com.plover.plover_be.crew.domain.CrewPloggingStatus activeSessionStatus
    ) {}

    public record CrewListResponse(
            List<CrewListItemResponse> crews
    ) {}

    public record CrewDetailResponse(
            Long crewId,
            String name,
            String joinCode,
            long memberCount,
            List<CrewMemberResponse> members,
            CrewRole myRole,
            boolean leader,
            long completedPloggingCount,
            long totalStepCount,
            long totalDistanceMeters,
            long totalPloggingSeconds,
            CrewPloggingDto.SessionResponse activeSession,
            List<CrewPloggingDto.RecordSummaryResponse> completedRecords
    ) {}
}
