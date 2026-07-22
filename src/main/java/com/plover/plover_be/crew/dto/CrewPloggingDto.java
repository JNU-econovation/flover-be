package com.plover.plover_be.crew.dto;

import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.plogging.domain.PloggingMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class CrewPloggingDto {

    public record SessionResponse(
            @Schema(description = "같이 플로깅 세션 ID", example = "100") Long crewPloggingSessionId,
            @Schema(description = "세션 상태", example = "RECRUITING") CrewPloggingStatus status,
            @Schema(description = "서버 기준 공통 시작 시각", nullable = true) LocalDateTime startedAt,
            @Schema(description = "서버 기준 공통 종료 시각", nullable = true) LocalDateTime endedAt,
            @Schema(description = "미제출 참가자의 제출 마감 시각", nullable = true) LocalDateTime submissionDeadlineAt,
            @Schema(description = "로그인 사용자의 세션 참가 이력 여부", example = "true") boolean joinedByMe,
            @Schema(description = "로그인 사용자의 참가 상태", example = "JOINED", nullable = true) CrewPloggingParticipantStatus participantStatus,
            @Schema(description = "로그인 사용자의 개인 기록 제출 여부", example = "false") boolean recordSubmittedByMe,
            @Schema(description = "취소되지 않은 참가자 수", example = "3") long participantCount,
            @Schema(description = "크루 대표 기록 최종화 여부", example = "false") boolean crewRecordCompleted
    ) {}

    public record ParticipantResponse(
            @Schema(description = "과거 참가자 사용자 ID", example = "21") Long userId,
            @Schema(description = "참가자 공개 닉네임", example = "플로버") String nickname,
            @Schema(description = "참가자 프로필 이미지 URL", nullable = true) String profileImageUrl
    ) {}

    public record PhotoResponse(
            @Schema(description = "사진 ID", example = "301") Long photoId,
            @Schema(description = "기존 S3 객체 URL") String objectUrl,
            @Schema(description = "업로더 사용자 ID", example = "21") Long uploaderUserId,
            @Schema(description = "업로더 공개 닉네임", example = "플로버") String uploaderNickname,
            @Schema(description = "업로더 프로필 이미지 URL", nullable = true) String uploaderProfileImageUrl,
            @Schema(description = "사진 등록 시각") LocalDateTime registeredAt
    ) {}

    public record RecordSummaryResponse(
            @Schema(description = "같이 플로깅 세션 ID", example = "100") Long crewPloggingSessionId,
            @Schema(description = "크루 플로깅 날짜") LocalDateTime ploggingDate,
            @Schema(description = "대표 기록 닉네임 스냅샷", nullable = true) String representativeNickname,
            @Schema(description = "대표 걸음 수 스냅샷", nullable = true) Integer stepCount,
            @Schema(description = "대표 거리(m) 스냅샷", nullable = true) Integer distanceMeters,
            @Schema(description = "대표 플로깅 시간(초) 스냅샷", nullable = true) Integer ploggingSeconds,
            @Schema(description = "최종 참가자 수", example = "3") int participantCount,
            @Schema(description = "공유 사진 수", example = "4") long sharedPhotoCount,
            @Schema(description = "가장 최근 공유 사진 URL", nullable = true) String representativePhotoUrl
    ) {}

    public record RecordListResponse(
            List<RecordSummaryResponse> content,
            boolean hasNext
    ) {}

    public record RecordDetailResponse(
            Long crewPloggingSessionId,
            PloggingMode mode,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String placeName,
            Long representativeUserId,
            String representativeNickname,
            Integer stepCount,
            Integer distanceMeters,
            Integer caloriesBurned,
            Integer ploggingSeconds,
            String mapImageUrl,
            int participantCount,
            List<ParticipantResponse> participants,
            List<PhotoResponse> photos
    ) {}
}
