package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.crew.dto.CrewDto;
import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.exception.CrewErrorCode;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.exception.UserErrorCode;
import com.plover.plover_be.user.exception.UserException;
import com.plover.plover_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CrewService {

    private static final List<CrewPloggingStatus> ACTIVE_SESSION_STATUSES = List.of(
            CrewPloggingStatus.RECRUITING,
            CrewPloggingStatus.IN_PROGRESS,
            CrewPloggingStatus.COMPLETING
    );

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewPloggingSessionRepository crewPloggingSessionRepository;
    private final CrewPloggingParticipantRepository crewPloggingParticipantRepository;
    private final UserRepository userRepository;
    private final PloggingSessionRepository ploggingSessionRepository;
    private final CrewPloggingResponseMapper responseMapper;
    private final CrewPloggingPhotoSummaryReader photoSummaryReader;
    private final CrewJoinCodeGenerator joinCodeGenerator;

    @Transactional
    public CrewDto.CrewResponse createCrew(Long userId, String name) {
        User user = getUserOrThrow(userId);
        Crew crew;
        try {
            crew = crewRepository.saveAndFlush(Crew.create(name, joinCodeGenerator.generate(), user));
        } catch (DataIntegrityViolationException e) {
            throw new CrewException(CrewErrorCode.JOIN_CODE_CONFLICT);
        }
        crewMemberRepository.save(CrewMember.create(crew, user, CrewRole.LEADER));
        return new CrewDto.CrewResponse(crew.getId(), crew.getName(), crew.getJoinCode(), CrewRole.LEADER);
    }

    @Transactional
    public CrewDto.CrewResponse joinCrew(Long userId, String rawJoinCode) {
        String joinCode = normalizeAndValidateJoinCode(rawJoinCode);
        Crew crew = crewRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_NOT_FOUND));
        User user = getUserOrThrow(userId);

        Optional<CrewMember> existing = crewMemberRepository.findByCrewIdAndUserIdForUpdate(crew.getId(), userId);
        if (existing.isPresent()) {
            CrewMember membership = existing.get();
            if (membership.getStatus() == CrewMemberStatus.ACTIVE) {
                throw new CrewException(CrewErrorCode.ALREADY_CREW_MEMBER);
            }
            membership.reactivate();
            return new CrewDto.CrewResponse(crew.getId(), crew.getName(), crew.getJoinCode(), membership.getRole());
        }

        try {
            crewMemberRepository.saveAndFlush(CrewMember.create(crew, user, CrewRole.MEMBER));
        } catch (DataIntegrityViolationException e) {
            throw new CrewException(CrewErrorCode.ALREADY_CREW_MEMBER);
        }
        return new CrewDto.CrewResponse(crew.getId(), crew.getName(), crew.getJoinCode(), CrewRole.MEMBER);
    }

    @Transactional(readOnly = true)
    public CrewDto.CrewListResponse findMyCrews(Long userId) {
        getUserOrThrow(userId);
        List<CrewDto.CrewListItemResponse> crews = crewMemberRepository
                .findAllByUserIdAndStatusOrderByJoinedAtDesc(userId, CrewMemberStatus.ACTIVE)
                .stream()
                .map(member -> toListItem(member, userId))
                .toList();
        return new CrewDto.CrewListResponse(crews);
    }

    @Transactional(readOnly = true)
    public CrewDto.CrewDetailResponse findCrew(Long userId, Long crewId) {
        CrewMember me = getActiveMemberOrThrow(crewId, userId);
        Crew crew = me.getCrew();
        List<CrewDto.CrewMemberResponse> members = crewMemberRepository
                .findAllByCrewIdAndStatusOrderByJoinedAtAsc(crewId, CrewMemberStatus.ACTIVE)
                .stream()
                .map(member -> new CrewDto.CrewMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getNickname(),
                        member.getUser().getProfileImageUrl(),
                        member.getRole()
                ))
                .toList();
        CrewPloggingSessionRepository.CrewStatsView stats = crewPloggingSessionRepository.findStatsByCrewId(crewId);
        CrewPloggingDto.SessionResponse activeSession = crewPloggingSessionRepository
                .findFirstByCrewIdAndStatusInOrderByCreatedAtDesc(crewId, ACTIVE_SESSION_STATUSES)
                .map(session -> responseMapper.toSessionResponse(session, userId))
                .orElse(null);
        List<CrewPloggingSession> completedSessions = crewPloggingSessionRepository
                .findAllByCrewIdAndStatusOrderByEndedAtDesc(crewId, CrewPloggingStatus.COMPLETED);
        Map<Long, CrewPloggingPhotoSummaryReader.PhotoSummary> photoSummaries = photoSummaryReader.findBySessionIds(
                completedSessions.stream().map(CrewPloggingSession::getId).toList()
        );
        List<CrewPloggingDto.RecordSummaryResponse> records = completedSessions
                .stream()
                .map(session -> responseMapper.toRecordSummary(
                        session,
                        photoSummaries.getOrDefault(
                                session.getId(),
                                CrewPloggingPhotoSummaryReader.PhotoSummary.empty()
                        )
                ))
                .toList();

        return new CrewDto.CrewDetailResponse(
                crew.getId(),
                crew.getName(),
                crew.getJoinCode(),
                members.size(),
                members,
                me.getRole(),
                me.getRole() == CrewRole.LEADER,
                stats.getCount(),
                stats.getTotalStepCount(),
                stats.getTotalDistanceMeters(),
                stats.getTotalPloggingSeconds(),
                activeSession,
                records
        );
    }

    @Transactional(readOnly = true)
    public CrewDto.CrewMemberListResponse findMembers(Long userId, Long crewId) {
        getActiveMemberOrThrow(crewId, userId);
        List<CrewDto.CrewMemberListItemResponse> members = crewMemberRepository
                .findAllByCrewIdAndStatusOrderByJoinedAtAsc(crewId, CrewMemberStatus.ACTIVE)
                .stream()
                .map(member -> new CrewDto.CrewMemberListItemResponse(
                        member.getUser().getId(),
                        member.getUser().getNickname(),
                        member.getUser().getProfileImageUrl(),
                        member.getRole(),
                        member.getJoinedAt()
                ))
                .toList();
        return new CrewDto.CrewMemberListResponse(members);
    }

    @Transactional(readOnly = true)
    public CrewDto.CrewMemberProfileResponse findMemberProfile(Long userId, Long crewId, Long targetUserId) {
        getActiveMemberOrThrow(crewId, userId);
        CrewMember target = getActiveMemberOrThrow(crewId, targetUserId);
        User targetUser = target.getUser();
        PloggingSessionRepository.PloggingStatsView stats = ploggingSessionRepository
                .findStatsByUserId(targetUserId);
        return new CrewDto.CrewMemberProfileResponse(
                targetUser.getId(),
                targetUser.getNickname(),
                targetUser.getProfileImageUrl(),
                targetUser.getLevel(),
                targetUser.getExperience(),
                stats.getCount(),
                stats.getTotalStepCount(),
                stats.getTotalDistanceMeters()
        );
    }

    @Transactional
    public void withdraw(Long userId, Long crewId) {
        MembershipChangeContext context = lockMembershipChangeContext(crewId, userId, userId, false);
        if (context.target().getRole() == CrewRole.LEADER) {
            throw new CrewException(CrewErrorCode.CREW_LEADER_CANNOT_WITHDRAW);
        }
        withdraw(context);
    }

    @Transactional
    public void removeMember(Long userId, Long crewId, Long targetUserId) {
        MembershipChangeContext context = lockMembershipChangeContext(crewId, userId, targetUserId, true);
        if (userId.equals(targetUserId)) {
            throw new CrewException(CrewErrorCode.CREW_LEADER_CANNOT_BE_WITHDRAWN);
        }
        if (context.target().getRole() == CrewRole.LEADER) {
            throw new CrewException(CrewErrorCode.CREW_LEADER_CANNOT_BE_WITHDRAWN);
        }
        withdraw(context);
    }

    @Transactional(readOnly = true)
    public CrewMember getActiveMemberOrThrow(Long crewId, Long userId) {
        return crewMemberRepository.findByCrewIdAndUserIdAndStatus(crewId, userId, CrewMemberStatus.ACTIVE)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_MEMBER_ONLY));
    }

    private CrewDto.CrewListItemResponse toListItem(CrewMember member, Long userId) {
        Long crewId = member.getCrew().getId();
        CrewPloggingSessionRepository.CrewStatsView stats = crewPloggingSessionRepository.findStatsByCrewId(crewId);
        Optional<CrewPloggingSession> activeSession = crewPloggingSessionRepository
                .findFirstByCrewIdAndStatusInOrderByCreatedAtDesc(crewId, ACTIVE_SESSION_STATUSES);
        return new CrewDto.CrewListItemResponse(
                crewId,
                member.getCrew().getName(),
                crewMemberRepository.countByCrewIdAndStatus(crewId, CrewMemberStatus.ACTIVE),
                member.getRole(),
                stats.getCount(),
                stats.getTotalStepCount(),
                stats.getTotalDistanceMeters(),
                stats.getTotalPloggingSeconds(),
                activeSession.isPresent(),
                activeSession.map(CrewPloggingSession::getStatus).orElse(null)
        );
    }

    private MembershipChangeContext lockMembershipChangeContext(
            Long crewId,
            Long requesterUserId,
            Long targetUserId,
            boolean leaderRequired
    ) {
        crewRepository.findByIdForUpdate(crewId)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_NOT_FOUND));
        Optional<CrewPloggingSession> activeSession = crewPloggingSessionRepository
                .findActiveByCrewIdForUpdate(crewId, ACTIVE_SESSION_STATUSES);

        CrewMember requester = crewMemberRepository
                .findByCrewIdAndUserIdForUpdate(crewId, requesterUserId)
                .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_MEMBER_ONLY));
        validateActiveMember(requester);
        if (leaderRequired && requester.getRole() != CrewRole.LEADER) {
            throw new CrewException(CrewErrorCode.CREW_LEADER_ONLY);
        }

        CrewMember target = requesterUserId.equals(targetUserId)
                ? requester
                : crewMemberRepository.findByCrewIdAndUserIdForUpdate(crewId, targetUserId)
                        .orElseThrow(() -> new CrewException(CrewErrorCode.CREW_MEMBER_NOT_FOUND));
        validateActiveTarget(target);

        Optional<CrewPloggingParticipant> participant = activeSession.flatMap(session ->
                crewPloggingParticipantRepository.findBySessionIdAndUserIdForUpdate(
                        session.getId(), targetUserId));
        return new MembershipChangeContext(activeSession.orElse(null), target, participant.orElse(null));
    }

    private void withdraw(MembershipChangeContext context) {
        CrewPloggingSession session = context.session();
        CrewPloggingParticipant participant = context.participant();
        if (session != null && participant != null) {
            validateAndUpdateParticipant(session, participant);
        }
        context.target().withdraw(LocalDateTime.now());
    }

    private void validateAndUpdateParticipant(
            CrewPloggingSession session,
            CrewPloggingParticipant participant
    ) {
        if (session.getStatus() == CrewPloggingStatus.RECRUITING) {
            if (participant.getStatus() == CrewPloggingParticipantStatus.JOINED) {
                participant.cancel();
                return;
            }
            if (participant.getStatus() == CrewPloggingParticipantStatus.CANCELED) {
                return;
            }
            throw new CrewException(CrewErrorCode.INVALID_PARTICIPANT_STATE_FOR_WITHDRAWAL);
        }

        if (participant.getStatus() == CrewPloggingParticipantStatus.PARTICIPATING) {
            throw new CrewException(CrewErrorCode.ACTIVE_PARTICIPANT_CANNOT_WITHDRAW);
        }
        if (participant.getStatus() == CrewPloggingParticipantStatus.JOINED) {
            throw new CrewException(CrewErrorCode.INVALID_PARTICIPANT_STATE_FOR_WITHDRAWAL);
        }
    }

    private void validateActiveMember(CrewMember member) {
        if (member.getStatus() != CrewMemberStatus.ACTIVE) {
            throw new CrewException(CrewErrorCode.CREW_MEMBER_ONLY);
        }
    }

    private void validateActiveTarget(CrewMember member) {
        if (member.getStatus() != CrewMemberStatus.ACTIVE) {
            throw new CrewException(CrewErrorCode.CREW_MEMBER_ALREADY_WITHDRAWN);
        }
    }

    private record MembershipChangeContext(
            CrewPloggingSession session,
            CrewMember target,
            CrewPloggingParticipant participant
    ) {}

    private String normalizeAndValidateJoinCode(String rawJoinCode) {
        String joinCode = rawJoinCode == null ? "" : rawJoinCode.trim();
        if (!joinCode.matches("[0-9]{6}")) {
            throw new CrewException(CrewErrorCode.INVALID_JOIN_CODE);
        }
        return joinCode;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
