package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrewDeletionService {

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewPloggingSessionRepository crewPloggingSessionRepository;
    private final CrewPloggingParticipantRepository crewPloggingParticipantRepository;
    private final PloggingPhotoRepository ploggingPhotoRepository;

    @Transactional
    public void deleteOwnedCrewsAndUserReferences(Long userId) {
        for (Long crewId : crewRepository.findIdsByLeaderId(userId)) {
            ploggingPhotoRepository.clearCrewPloggingSessionByCrewId(crewId);
            crewPloggingParticipantRepository.deleteByCrewId(crewId);
            crewPloggingSessionRepository.deleteByCrewId(crewId);
            crewMemberRepository.deleteByCrewId(crewId);
            crewRepository.deleteByCrewId(crewId);
        }

        crewPloggingSessionRepository.clearRepresentativeRecordByUserId(userId);
        crewPloggingParticipantRepository.deleteByUserId(userId);
        crewMemberRepository.deleteByUserId(userId);
    }
}
