package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CrewPloggingFinalizer {

    private final CrewPloggingSessionRepository sessionRepository;
    private final CrewPloggingParticipantRepository participantRepository;

    @Transactional
    public void finalizeExpiredSession(Long sessionId, LocalDateTime now) {
        CrewPloggingSession session = sessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null
                || session.getStatus() != CrewPloggingStatus.COMPLETING
                || session.getSubmissionDeadlineAt() == null
                || session.getSubmissionDeadlineAt().isAfter(now)) {
            return;
        }

        participantRepository.findAllByCrewPloggingSessionIdOrderByJoinedAtAsc(sessionId)
                .forEach(participant -> {
                    if (participant.getStatus() == CrewPloggingParticipantStatus.PARTICIPATING) {
                        participant.markNotSubmitted();
                    }
                });
        complete(session, now);
    }

    public void complete(CrewPloggingSession session, LocalDateTime completedAt) {
        int participantCount = Math.toIntExact(participantRepository.countByCrewPloggingSessionIdAndStatusNot(
                session.getId(), CrewPloggingParticipantStatus.CANCELED));
        session.complete(completedAt, participantCount);
    }
}
