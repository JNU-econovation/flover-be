package com.plover.plover_be.crew.dto;

import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;

import java.time.LocalDateTime;
import java.util.List;

public class CrewPloggingDto {

    public record SessionResponse(
            Long crewPloggingSessionId,
            CrewPloggingStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            LocalDateTime submissionDeadlineAt,
            boolean joinedByMe,
            CrewPloggingParticipantStatus participantStatus,
            boolean recordSubmittedByMe,
            long participantCount,
            boolean crewRecordCompleted
    ) {}

    public record ParticipantResponse(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {}

    public record PhotoResponse(
            Long photoId,
            String objectUrl,
            Long uploaderUserId,
            String uploaderNickname,
            String uploaderProfileImageUrl,
            LocalDateTime registeredAt
    ) {}

    public record RecordSummaryResponse(
            Long crewPloggingSessionId,
            LocalDateTime ploggingDate,
            String representativeNickname,
            Integer stepCount,
            Integer distanceMeters,
            Integer ploggingSeconds,
            int participantCount,
            long sharedPhotoCount,
            String representativePhotoUrl
    ) {}

    public record RecordListResponse(
            List<RecordSummaryResponse> content,
            boolean hasNext
    ) {}

    public record RecordDetailResponse(
            Long crewPloggingSessionId,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Long representativeUserId,
            String representativeNickname,
            Integer stepCount,
            Integer distanceMeters,
            Integer ploggingSeconds,
            int participantCount,
            List<ParticipantResponse> participants,
            List<PhotoResponse> photos
    ) {}
}
