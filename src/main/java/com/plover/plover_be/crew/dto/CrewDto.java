package com.plover.plover_be.crew.dto;

import com.plover.plover_be.crew.domain.CrewRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CrewDto {

    public record CreateRequest(
            @NotBlank @Size(max = 100) String name
    ) {}

    public record JoinRequest(
            @NotBlank String joinCode
    ) {}

    public record CrewResponse(
            Long crewId,
            String name,
            String joinCode,
            CrewRole role
    ) {}

    public record CrewMemberResponse(
            Long userId,
            String nickname,
            String profileImageUrl,
            CrewRole role
    ) {}

    public record CrewListItemResponse(
            Long crewId,
            String name,
            long memberCount,
            CrewRole myRole,
            long completedPloggingCount,
            long totalStepCount,
            long totalDistanceMeters,
            long totalPloggingSeconds,
            boolean hasActiveSession,
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
