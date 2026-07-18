package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.crew.dto.CrewDto;
import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.exception.CrewErrorCode;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.exception.UserErrorCode;
import com.plover.plover_be.user.exception.UserException;
import com.plover.plover_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CrewService {

    private static final String JOIN_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int JOIN_CODE_LENGTH = 8;
    private static final List<CrewPloggingStatus> ACTIVE_SESSION_STATUSES = List.of(
            CrewPloggingStatus.RECRUITING,
            CrewPloggingStatus.IN_PROGRESS,
            CrewPloggingStatus.COMPLETING
    );

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewPloggingSessionRepository crewPloggingSessionRepository;
    private final UserRepository userRepository;
    private final CrewPloggingResponseMapper responseMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public CrewDto.CrewResponse createCrew(Long userId, String name) {
        User user = getUserOrThrow(userId);
        Crew crew = crewRepository.save(Crew.create(name, generateUniqueJoinCode(), user));
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
        List<CrewPloggingDto.RecordSummaryResponse> records = crewPloggingSessionRepository
                .findAllByCrewIdAndStatusOrderByEndedAtDesc(crewId, CrewPloggingStatus.COMPLETED)
                .stream()
                .map(responseMapper::toRecordSummary)
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

    private String generateUniqueJoinCode() {
        String joinCode;
        do {
            StringBuilder builder = new StringBuilder(JOIN_CODE_LENGTH);
            for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
                builder.append(JOIN_CODE_CHARACTERS.charAt(secureRandom.nextInt(JOIN_CODE_CHARACTERS.length())));
            }
            joinCode = builder.toString();
        } while (crewRepository.existsByJoinCode(joinCode));
        return joinCode;
    }

    private String normalizeAndValidateJoinCode(String rawJoinCode) {
        String joinCode = rawJoinCode == null ? "" : rawJoinCode.trim().toUpperCase(Locale.ROOT);
        if (!joinCode.matches("[A-Z0-9]{8}")) {
            throw new CrewException(CrewErrorCode.INVALID_JOIN_CODE);
        }
        return joinCode;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
