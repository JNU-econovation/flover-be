package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CrewPloggingFinalizationScheduler {

    private final CrewPloggingSessionRepository sessionRepository;
    private final CrewPloggingFinalizer finalizer;

    @Scheduled(fixedDelayString = "${crew.plogging.finalization-check-interval-ms:60000}")
    public void finalizeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.findIdsToFinalize(CrewPloggingStatus.COMPLETING, now)
                .forEach(sessionId -> finalizer.finalizeExpiredSession(sessionId, now));
    }
}
