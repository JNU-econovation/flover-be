package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CrewPloggingResponseMapper {

    private final CrewPloggingParticipantRepository participantRepository;
    private final PloggingPhotoRepository photoRepository;

    public CrewPloggingDto.SessionResponse toSessionResponse(CrewPloggingSession session, Long userId) {
        CrewPloggingParticipant participant = participantRepository
                .findByCrewPloggingSessionIdAndUserId(session.getId(), userId)
                .orElse(null);
        long participantCount = participantRepository.countByCrewPloggingSessionIdAndStatusNot(
                session.getId(), CrewPloggingParticipantStatus.CANCELED);

        return new CrewPloggingDto.SessionResponse(
                session.getId(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getSubmissionDeadlineAt(),
                participant != null && participant.getStatus() != CrewPloggingParticipantStatus.CANCELED,
                participant == null ? null : participant.getStatus(),
                participant != null && participant.getStatus() == CrewPloggingParticipantStatus.SUBMITTED,
                participantCount,
                session.getStatus() == CrewPloggingStatus.COMPLETED
        );
    }

    public CrewPloggingDto.RecordSummaryResponse toRecordSummary(
            CrewPloggingSession session,
            CrewPloggingPhotoSummaryReader.PhotoSummary photoSummary
    ) {
        return new CrewPloggingDto.RecordSummaryResponse(
                session.getId(),
                session.getStartedAt(),
                session.getRepresentativeNicknameSnapshot(),
                session.getRepresentativeStepCountSnapshot(),
                session.getRepresentativeDistanceMetersSnapshot(),
                session.getRepresentativePloggingSecondsSnapshot(),
                session.getParticipantCountSnapshot() == null ? 0 : session.getParticipantCountSnapshot(),
                photoSummary.photoCount(),
                photoSummary.representativePhotoUrl()
        );
    }

    public CrewPloggingDto.RecordDetailResponse toRecordDetail(CrewPloggingSession session) {
        List<CrewPloggingDto.ParticipantResponse> participants = participantRepository
                .findAllWithUserByCrewPloggingSessionIdOrderByJoinedAtAsc(session.getId())
                .stream()
                .filter(participant -> participant.getStatus() != CrewPloggingParticipantStatus.CANCELED)
                .map(participant -> new CrewPloggingDto.ParticipantResponse(
                        participant.getUser().getId(),
                        participant.getUser().getNickname(),
                        participant.getUser().getProfileImageUrl()
                ))
                .toList();

        List<CrewPloggingDto.PhotoResponse> photos = photoRepository
                .findAllByCrewPloggingSessionIdOrderByCreatedAtAscIdAsc(session.getId())
                .stream()
                .map(photo -> new CrewPloggingDto.PhotoResponse(
                        photo.getId(),
                        photo.getImageUrl(),
                        photo.getPloggingSession().getUser().getId(),
                        photo.getPloggingSession().getUser().getNickname(),
                        photo.getPloggingSession().getUser().getProfileImageUrl(),
                        photo.getCreatedAt()
                ))
                .toList();

        return new CrewPloggingDto.RecordDetailResponse(
                session.getId(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getRepresentativeUserIdSnapshot(),
                session.getRepresentativeNicknameSnapshot(),
                session.getRepresentativeStepCountSnapshot(),
                session.getRepresentativeDistanceMetersSnapshot(),
                session.getRepresentativePloggingSecondsSnapshot(),
                session.getParticipantCountSnapshot() == null ? 0 : session.getParticipantCountSnapshot(),
                participants,
                photos
        );
    }
}
