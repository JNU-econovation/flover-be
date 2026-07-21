package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
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
class CrewDeletionServiceIntegrationTest {

    @Autowired private CrewDeletionService crewDeletionService;
    @Autowired private CrewRepository crewRepository;
    @Autowired private CrewMemberRepository crewMemberRepository;
    @Autowired private CrewPloggingSessionRepository crewSessionRepository;
    @Autowired private CrewPloggingParticipantRepository participantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PloggingSessionRepository ploggingSessionRepository;
    @Autowired private PloggingPhotoRepository photoRepository;
    @Autowired private PloggingRoutePointRepository routePointRepository;
    @Autowired private EntityManager entityManager;

    @DisplayName("크루장 탈퇴 정리는 크루만 삭제하고 다른 크루원의 개인 기록, 경로, 사진, 경험치를 보존한다")
    @Test
    void deleting_owned_crew_preserves_other_members_personal_data() {
        // given
        User leader = user("leader");
        User member = user("member");
        member.addPloggingTimeSeconds(3600);
        userRepository.save(leader);
        userRepository.save(member);
        long memberExperience = member.getExperience();

        Crew crew = crewRepository.save(Crew.create("크루", joinCode(), leader));
        crewMemberRepository.save(CrewMember.create(crew, leader, CrewRole.LEADER));
        crewMemberRepository.save(CrewMember.create(crew, member, CrewRole.MEMBER));
        CrewPloggingSession crewSession = crewSessionRepository.save(startedSession(crew));
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, member, false);
        participant.start();
        PloggingSession personal = ploggingSessionRepository.save(personalRecord(member));
        participant.submit(personal, LocalDateTime.now());
        participantRepository.save(participant);
        PloggingPhoto photo = photoRepository.save(PloggingPhoto.create(
                personal, crewSession, 0, "https://s3.example.com/member-photo.jpg"));
        PloggingRoutePoint routePoint = routePointRepository.save(
                PloggingRoutePoint.create(personal, 0, 37.5, 127.0));
        Long crewId = crew.getId();
        Long personalId = personal.getId();
        Long photoId = photo.getId();
        Long routePointId = routePoint.getId();
        Long memberId = member.getId();
        entityManager.flush();
        entityManager.clear();

        // when
        crewDeletionService.deleteOwnedCrewsAndUserReferences(leader.getId());
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(crewRepository.findById(crewId)).isEmpty();
        assertThat(ploggingSessionRepository.findById(personalId)).isPresent();
        assertThat(routePointRepository.findById(routePointId)).isPresent();
        PloggingPhoto preservedPhoto = photoRepository.findById(photoId).orElseThrow();
        assertThat(preservedPhoto.getPloggingSession().getId()).isEqualTo(personalId);
        assertThat(preservedPhoto.getCrewPloggingSession()).isNull();
        assertThat(userRepository.findById(memberId).orElseThrow().getExperience()).isEqualTo(memberExperience);
        assertThat(ploggingSessionRepository.findStatsByUserId(memberId).getCount()).isEqualTo(1L);
    }

    @DisplayName("일반 크루원 참조 정리는 크루와 대표 스냅샷 통계를 보존하고 대표 개인 기록 FK만 해제한다")
    @Test
    void deleting_member_reference_preserves_crew_snapshots_and_stats() {
        // given
        User leader = user("leader");
        User member = user("member");
        userRepository.save(leader);
        userRepository.save(member);
        Crew crew = crewRepository.save(Crew.create("크루", joinCode(), leader));
        crewMemberRepository.save(CrewMember.create(crew, leader, CrewRole.LEADER));
        crewMemberRepository.save(CrewMember.create(crew, member, CrewRole.MEMBER));
        CrewPloggingSession crewSession = startedSession(crew);
        PloggingSession personal = ploggingSessionRepository.save(personalRecord(member));
        crewSession.selectRepresentative(personal, member.getId(), member.getNickname());
        LocalDateTime now = LocalDateTime.now();
        crewSession.end(now, now.plusHours(24));
        crewSession.complete(now, 2);
        crewSessionRepository.save(crewSession);
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, member, false);
        participant.start();
        participant.submit(personal, now);
        participantRepository.save(participant);
        Long crewId = crew.getId();
        Long crewSessionId = crewSession.getId();
        Long memberId = member.getId();
        entityManager.flush();
        entityManager.clear();

        // when
        crewDeletionService.deleteOwnedCrewsAndUserReferences(memberId);
        entityManager.flush();
        entityManager.clear();

        // then
        CrewPloggingSession preserved = crewSessionRepository.findById(crewSessionId).orElseThrow();
        assertThat(crewRepository.findById(crewId)).isPresent();
        assertThat(preserved.getStatus()).isEqualTo(CrewPloggingStatus.COMPLETED);
        assertThat(preserved.getRepresentativePloggingSession()).isNull();
        assertThat(preserved.getRepresentativeNicknameSnapshot()).isEqualTo(member.getNickname());
        assertThat(preserved.getRepresentativeStepCountSnapshot()).isEqualTo(personal.getStepCount());
        assertThat(preserved.getParticipantCountSnapshot()).isEqualTo(2);
        assertThat(crewSessionRepository.findStatsByCrewId(crewId).getCount()).isEqualTo(1L);
        assertThat(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crewId, memberId, CrewMemberStatus.ACTIVE)).isEmpty();
        assertThat(participantRepository.findByCrewPloggingSessionIdAndUserId(
                crewSessionId, memberId)).isEmpty();
    }

    private User user(String prefix) {
        String suffix = UUID.randomUUID().toString();
        return User.create(OAuthProvider.KAKAO, prefix + suffix, prefix + "@test.com", prefix + suffix, null);
    }

    private String joinCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private CrewPloggingSession startedSession(Crew crew) {
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        session.start(LocalDateTime.now().minusHours(1));
        return session;
    }

    private PloggingSession personalRecord(User user) {
        LocalDateTime now = LocalDateTime.now();
        return PloggingSession.create(
                user, PloggingMode.FREE, now.minusHours(1), now,
                1000, 2000, 100, 3600, 0, "공원",
                37.5, 127.0, 37.6, 127.1, null
        );
    }
}
