package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.domain.PloggingPhoto;
import com.plover.plover_be.plogging.domain.PloggingRoutePoint;
import com.plover.plover_be.plogging.domain.PloggingSession;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import com.plover.plover_be.plogging.repository.PloggingRoutePointRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrewMemberManagementIntegrationTest {

    @Autowired private CrewService crewService;
    @Autowired private UserRepository userRepository;
    @Autowired private CrewRepository crewRepository;
    @Autowired private CrewMemberRepository crewMemberRepository;
    @Autowired private CrewPloggingSessionRepository crewSessionRepository;
    @Autowired private CrewPloggingParticipantRepository participantRepository;
    @Autowired private PloggingSessionRepository ploggingSessionRepository;
    @Autowired private PloggingRoutePointRepository routePointRepository;
    @Autowired private PloggingPhotoRepository photoRepository;
    @Autowired private EntityManager entityManager;

    @DisplayName("강퇴는 멤버십만 WITHDRAWN으로 변경하고 과거 개인·참가·사진·대표 기록을 보존한다")
    @Test
    void remove_member_preserves_historical_records_and_shared_photo() {
        User leader = userRepository.save(user("leader"));
        User member = userRepository.save(user("member"));
        member.addPloggingTimeSeconds(100);
        Crew crew = crewRepository.save(Crew.create("크루", joinCode(), leader));
        crewMemberRepository.save(CrewMember.create(crew, leader, CrewRole.LEADER));
        CrewMember membership = crewMemberRepository.saveAndFlush(
                CrewMember.create(crew, member, CrewRole.MEMBER));

        LocalDateTime now = LocalDateTime.now();
        CrewPloggingSession crewSession = crewSessionRepository.save(CrewPloggingSession.create(crew));
        crewSession.start(now.minusHours(1));
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, member, false);
        participant.start();
        participantRepository.save(participant);
        PloggingSession personalRecord = ploggingSessionRepository.save(personalRecord(member, now));
        participant.submit(personalRecord, now);
        crewSession.selectRepresentative(personalRecord, member.getId(), member.getNickname());
        crewSession.complete(now, 1);
        PloggingRoutePoint routePoint = routePointRepository.save(
                PloggingRoutePoint.create(personalRecord, 0, 37.5, 127.0));
        PloggingPhoto photo = photoRepository.saveAndFlush(
                PloggingPhoto.create(personalRecord, crewSession, 0, "https://example.com/photo.jpg"));
        long experience = member.getExperience();

        crewService.removeMember(leader.getId(), crew.getId(), member.getId());
        entityManager.flush();
        entityManager.clear();

        CrewMember withdrawn = crewMemberRepository
                .findByCrewIdAndUserIdForUpdate(crew.getId(), member.getId()).orElseThrow();
        CrewPloggingSession savedCrewSession = crewSessionRepository.findById(crewSession.getId()).orElseThrow();
        assertThat(withdrawn.getStatus()).isEqualTo(CrewMemberStatus.WITHDRAWN);
        assertThat(participantRepository.findById(participant.getId())).isPresent();
        assertThat(ploggingSessionRepository.findById(personalRecord.getId())).isPresent();
        assertThat(routePointRepository.findById(routePoint.getId())).isPresent();
        assertThat(photoRepository.findById(photo.getId()).orElseThrow().getCrewPloggingSession().getId())
                .isEqualTo(crewSession.getId());
        assertThat(savedCrewSession.getRepresentativeNicknameSnapshot()).isEqualTo(member.getNickname());
        assertThat(userRepository.findById(member.getId()).orElseThrow().getExperience()).isEqualTo(experience);
        assertThat(ploggingSessionRepository.findStatsByUserId(member.getId()).getCount()).isEqualTo(1);
    }

    @DisplayName("자발적 탈퇴 후 같은 코드로 재가입하면 기존 멤버십과 최초 joinedAt을 유지한다")
    @Test
    void withdrawn_member_rejoins_with_existing_membership_and_original_joined_at() {
        User leader = userRepository.save(user("leader"));
        User member = userRepository.save(user("member"));
        Crew crew = crewRepository.save(Crew.create("크루", joinCode(), leader));
        crewMemberRepository.save(CrewMember.create(crew, leader, CrewRole.LEADER));
        CrewMember membership = crewMemberRepository.saveAndFlush(
                CrewMember.create(crew, member, CrewRole.MEMBER));
        Long membershipId = membership.getId();
        LocalDateTime joinedAt = membership.getJoinedAt();

        crewService.withdraw(member.getId(), crew.getId());
        crewService.joinCrew(member.getId(), "  " + crew.getJoinCode() + "  ");
        entityManager.flush();
        entityManager.clear();

        CrewMember rejoined = crewMemberRepository
                .findByCrewIdAndUserIdForUpdate(crew.getId(), member.getId()).orElseThrow();
        assertThat(rejoined.getId()).isEqualTo(membershipId);
        assertThat(rejoined.getStatus()).isEqualTo(CrewMemberStatus.ACTIVE);
        assertThat(rejoined.getJoinedAt()).isEqualTo(joinedAt);
        assertThat(rejoined.getLeftAt()).isNull();
    }

    private User user(String prefix) {
        String suffix = UUID.randomUUID().toString();
        return User.create(OAuthProvider.KAKAO, prefix + suffix, prefix + "@test.com", prefix, null);
    }

    private String joinCode() {
        return String.format("%06d", Math.floorMod(UUID.randomUUID().hashCode(), 1_000_000));
    }

    private PloggingSession personalRecord(User user, LocalDateTime now) {
        return PloggingSession.create(
                user, PloggingMode.FREE, now.minusHours(1), now,
                1000, 2000, 100, 3600, 0, "공원",
                37.5, 127.0, 37.6, 127.1, null
        );
    }
}
