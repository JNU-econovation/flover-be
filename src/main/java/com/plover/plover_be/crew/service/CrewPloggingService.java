package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.exception.CrewErrorCode;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CrewPloggingService {

    private static final List<CrewPloggingStatus> ACTIVE_SESSION_STATUSES = List.of(
            CrewPloggingStatus.RECRUITING,
            CrewPloggingStatus.IN_PROGRESS,
            CrewPloggingStatus.COMPLETING
    );

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewPloggingSessionRepository sessionRepository;
    private final CrewPloggingParticipantRepository participantRepository;
    private final CrewPloggingResponseMapper responseMapper;
    private final CrewPloggingPhotoSummaryReader photoSummaryReader;
    private final CrewPloggingFinalizer finalizer;

    @Value("${crew.plogging.submission-grace-hours:24}")
    private long submissionGraceHours;

    @Transactional
    public CrewPloggingDto.SessionResponse createSession(Long userId, Long crewId) {
        Crew crew = crewRepository.findByIdForUpdate(crewId)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_NOT_FOUND));
        CrewMember member = getActiveMemberOrThrow(crewId, userId);
        validateLeader(member);

        Optional<CrewPloggingSession> active = sessionRepository
                .findFirstByCrewIdAndStatusInOrderByCreatedAtDesc(crewId, ACTIVE_SESSION_STATUSES);
        if (active.isPresent()) {
            return responseMapper.toSessionResponse(active.get(), userId);
        }

        CrewPloggingSession session = sessionRepository.save(CrewPloggingSession.create(crew));
        participantRepository.save(CrewPloggingParticipant.create(session, member.getUser(), true));
        return responseMapper.toSessionResponse(session, userId);
    }

    @Transactional
    public CrewPloggingDto.SessionResponse joinSession(Long userId, Long sessionId) {
        CrewPloggingSession session = getSessionForUpdate(sessionId);
        if (session.getStatus() != CrewPloggingStatus.RECRUITING) {
            throw new CrewException(CrewErrorCode.SESSION_NOT_RECRUITING);
        }
        CrewMember member = getActiveMemberOrThrow(session.getCrew().getId(), userId);

        Optional<CrewPloggingParticipant> existing = participantRepository
                .findBySessionIdAndUserIdForUpdate(sessionId, userId);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == CrewPloggingParticipantStatus.CANCELED) {
                existing.get().rejoin();
            }
            return responseMapper.toSessionResponse(session, userId);
        }

        participantRepository.save(CrewPloggingParticipant.create(session, member.getUser(), false));
        return responseMapper.toSessionResponse(session, userId);
    }

    @Transactional
    public CrewPloggingDto.SessionResponse cancelParticipation(Long userId, Long sessionId) {
        CrewPloggingSession session = getSessionForUpdate(sessionId);
        if (session.getStatus() != CrewPloggingStatus.RECRUITING) {
            throw new CrewException(CrewErrorCode.SESSION_NOT_RECRUITING);
        }
        CrewPloggingParticipant participant = participantRepository
                .findBySessionIdAndUserIdForUpdate(sessionId, userId)
                .orElseThrow(() -> new CrewException(CrewErrorCode.SESSION_PARTICIPANT_ONLY));
        if (participant.getStatus() != CrewPloggingParticipantStatus.CANCELED) {
            participant.cancel();
        }
        return responseMapper.toSessionResponse(session, userId);
    }

    @Transactional
    public CrewPloggingDto.SessionResponse startSession(Long userId, Long sessionId) {
        CrewPloggingSession session = getSessionForUpdate(sessionId);
        CrewMember member = getActiveMemberOrThrow(session.getCrew().getId(), userId);
        validateLeader(member);
        if (session.getStatus() == CrewPloggingStatus.IN_PROGRESS) {
            return responseMapper.toSessionResponse(session, userId);
        }

        session.start(LocalDateTime.now());
        participantRepository.findAllByCrewPloggingSessionIdOrderByJoinedAtAsc(sessionId)
                .forEach(CrewPloggingParticipant::start);
        return responseMapper.toSessionResponse(session, userId);
    }

    @Transactional
    public CrewPloggingDto.SessionResponse cancelSession(Long userId, Long sessionId) {
        CrewPloggingSession session = getSessionForUpdate(sessionId);
        CrewMember member = getActiveMemberOrThrow(session.getCrew().getId(), userId);
        validateLeader(member);
        if (session.getStatus() == CrewPloggingStatus.CANCELED) {
            return responseMapper.toSessionResponse(session, userId);
        }

        session.cancel(LocalDateTime.now());
        participantRepository.findAllByCrewPloggingSessionIdOrderByJoinedAtAsc(sessionId)
                .forEach(CrewPloggingParticipant::cancelBySession);
        return responseMapper.toSessionResponse(session, userId);
    }

    @Transactional
    public CrewPloggingDto.SessionResponse endSession(Long userId, Long sessionId) {
        CrewPloggingSession session = getSessionForUpdate(sessionId);
        CrewMember member = getActiveMemberOrThrow(session.getCrew().getId(), userId);
        validateLeader(member);
        if (session.getStatus() == CrewPloggingStatus.COMPLETING
                || session.getStatus() == CrewPloggingStatus.COMPLETED) {
            return responseMapper.toSessionResponse(session, userId);
        }

        LocalDateTime endedAt = LocalDateTime.now();
        session.end(endedAt, endedAt.plusHours(submissionGraceHours));
        long unsubmittedCount = participantRepository.countByCrewPloggingSessionIdAndStatusIn(
                sessionId,
                List.of(
                        CrewPloggingParticipantStatus.JOINED,
                        CrewPloggingParticipantStatus.PARTICIPATING
                )
        );
        if (unsubmittedCount == 0) {
            finalizer.complete(session, endedAt);
        }
        return responseMapper.toSessionResponse(session, userId);
    }

    @Transactional(readOnly = true)
    public CrewPloggingDto.SessionResponse findActiveSession(Long userId, Long crewId) {
        getActiveMemberOrThrow(crewId, userId);
        return sessionRepository.findFirstByCrewIdAndStatusInOrderByCreatedAtDesc(crewId, ACTIVE_SESSION_STATUSES)
                .map(session -> responseMapper.toSessionResponse(session, userId))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CrewPloggingDto.SessionResponse findSession(Long userId, Long sessionId) {
        CrewPloggingSession session = getSessionOrThrow(sessionId);
        getActiveMemberOrThrow(session.getCrew().getId(), userId);
        return responseMapper.toSessionResponse(session, userId);
    }

    @Transactional(readOnly = true)
    public CrewPloggingDto.RecordListResponse findRecords(
            Long userId,
            Long crewId,
            Pageable pageable
    ) {
        getActiveMemberOrThrow(crewId, userId);
        Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Slice<CrewPloggingSession> sessions = sessionRepository
                .findAllByCrewIdAndStatusOrderByEndedAtDesc(crewId, CrewPloggingStatus.COMPLETED, pageOnly);
        Map<Long, CrewPloggingPhotoSummaryReader.PhotoSummary> photoSummaries = photoSummaryReader.findBySessionIds(
                sessions.getContent().stream().map(CrewPloggingSession::getId).toList()
        );
        return new CrewPloggingDto.RecordListResponse(
                sessions.getContent().stream()
                        .map(session -> responseMapper.toRecordSummary(
                                session,
                                photoSummaries.getOrDefault(
                                        session.getId(),
                                        CrewPloggingPhotoSummaryReader.PhotoSummary.empty()
                                )
                        ))
                        .toList(),
                sessions.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public CrewPloggingDto.RecordDetailResponse findRecord(Long userId, Long crewId, Long sessionId) {
        getActiveMemberOrThrow(crewId, userId);
        CrewPloggingSession session = getSessionOrThrow(sessionId);
        if (!session.getCrew().getId().equals(crewId) || session.getStatus() != CrewPloggingStatus.COMPLETED) {
            throw new CrewException(CrewErrorCode.CREW_PLOGGING_SESSION_NOT_FOUND);
        }
        return responseMapper.toRecordDetail(session);
    }

    private CrewPloggingSession getSessionForUpdate(Long sessionId) {
        return sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_PLOGGING_SESSION_NOT_FOUND));
    }

    private CrewPloggingSession getSessionOrThrow(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_PLOGGING_SESSION_NOT_FOUND));
    }

    private CrewMember getActiveMemberOrThrow(Long crewId, Long userId) {
        return crewMemberRepository.findByCrewIdAndUserIdAndStatus(crewId, userId, CrewMemberStatus.ACTIVE)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_MEMBER_ONLY));
    }

    private void validateLeader(CrewMember member) {
        if (member.getRole() != CrewRole.LEADER) {
            throw new CrewException(CrewErrorCode.CREW_LEADER_ONLY);
        }
    }
}
