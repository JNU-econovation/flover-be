package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.exception.CrewErrorCode;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.service.PloggingRecordWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrewPloggingCompletionProcessor {

    private final CrewPloggingSessionRepository sessionRepository;
    private final CrewPloggingParticipantRepository participantRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final PloggingRecordWriter ploggingRecordWriter;
    private final CrewPloggingFinalizer finalizer;

    @Transactional
    public PloggingDto.CompleteResponse complete(Long userId, PloggingDto.CompleteRequest request) {
        Long sessionId = request.crewPloggingSessionId();
        CrewPloggingSession crewSession = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_PLOGGING_SESSION_NOT_FOUND));
        if (crewSession.getStatus() != CrewPloggingStatus.IN_PROGRESS
                && crewSession.getStatus() != CrewPloggingStatus.COMPLETING) {
            throw new CrewException(CrewErrorCode.SESSION_NOT_COMPLETING);
        }

        LocalDateTime now = LocalDateTime.now();
        if (crewSession.getStatus() == CrewPloggingStatus.COMPLETING
                && (crewSession.getSubmissionDeadlineAt() == null
                || !now.isBefore(crewSession.getSubmissionDeadlineAt()))) {
            throw new CrewException(CrewErrorCode.SUBMISSION_DEADLINE_EXPIRED);
        }
        crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                        crewSession.getCrew().getId(), userId, CrewMemberStatus.ACTIVE)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_MEMBER_ONLY));

        CrewPloggingParticipant participant = participantRepository
                .findBySessionIdAndUserIdForUpdate(sessionId, userId)
                .orElseThrow(() -> new CrewException(CrewErrorCode.SESSION_PARTICIPANT_ONLY));
        if (participant.getStatus() == CrewPloggingParticipantStatus.SUBMITTED
                || participant.getPloggingSession() != null) {
            throw new CrewException(CrewErrorCode.PLOGGING_ALREADY_SUBMITTED);
        }

        PloggingRecordWriter.SavedPloggingRecord saved = ploggingRecordWriter.save(
                participant.getUser(), request, crewSession);
        participant.submit(saved.session(), now);

        if (participant.isLeader() || !crewSession.hasRepresentativeSnapshot()) {
            crewSession.selectRepresentative(
                    saved.session(),
                    participant.getUser().getId(),
                    participant.getUser().getNickname()
            );
        }

        long unsubmittedCount = participantRepository.countByCrewPloggingSessionIdAndStatusIn(
                sessionId,
                List.of(
                        CrewPloggingParticipantStatus.JOINED,
                        CrewPloggingParticipantStatus.PARTICIPATING
                )
        );
        if (unsubmittedCount == 0) {
            finalizer.complete(crewSession, now);
        }
        return saved.response();
    }
}
