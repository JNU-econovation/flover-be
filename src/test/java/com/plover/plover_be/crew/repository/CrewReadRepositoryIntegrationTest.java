package com.plover.plover_be.crew.repository;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.domain.PloggingPhoto;
import com.plover.plover_be.plogging.domain.PloggingSession;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrewReadRepositoryIntegrationTest {

    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewPloggingSessionRepository crewSessionRepository;
    private final CrewPloggingParticipantRepository participantRepository;
    private final PloggingSessionRepository ploggingSessionRepository;
    private final PloggingPhotoRepository photoRepository;
    private final EntityManager entityManager;

    private Long leaderId;
    private Long crewId;
    private Long sessionId;
    private Long firstMemberId;
    private Long secondMemberId;
    private Long firstParticipantId;
    private Long secondParticipantId;
    private Long firstPhotoId;
    private Long secondPhotoId;

    @Autowired
    CrewReadRepositoryIntegrationTest(
            UserRepository userRepository,
            CrewRepository crewRepository,
            CrewMemberRepository crewMemberRepository,
            CrewPloggingSessionRepository crewSessionRepository,
            CrewPloggingParticipantRepository participantRepository,
            PloggingSessionRepository ploggingSessionRepository,
            PloggingPhotoRepository photoRepository,
            EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.crewRepository = crewRepository;
        this.crewMemberRepository = crewMemberRepository;
        this.crewSessionRepository = crewSessionRepository;
        this.participantRepository = participantRepository;
        this.ploggingSessionRepository = ploggingSessionRepository;
        this.photoRepository = photoRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        User leader = user("leader");
        User member = user("member");
        userRepository.save(leader);
        userRepository.save(member);

        Crew firstCrew = crewRepository.save(Crew.create("첫 번째 크루", joinCode(), leader));
        CrewMember firstMember = crewMemberRepository.saveAndFlush(
                CrewMember.create(firstCrew, leader, CrewRole.LEADER));
        CrewMember secondMember = crewMemberRepository.saveAndFlush(
                CrewMember.create(firstCrew, member, CrewRole.MEMBER));

        Crew secondCrew = crewRepository.save(Crew.create("두 번째 크루", joinCode(), leader));
        crewMemberRepository.saveAndFlush(CrewMember.create(secondCrew, leader, CrewRole.LEADER));

        CrewPloggingSession crewSession = crewSessionRepository.save(CrewPloggingSession.create(firstCrew));
        CrewPloggingParticipant firstParticipant = participantRepository.saveAndFlush(
                CrewPloggingParticipant.create(crewSession, leader, true));
        CrewPloggingParticipant secondParticipant = participantRepository.saveAndFlush(
                CrewPloggingParticipant.create(crewSession, member, false));

        PloggingSession firstPlogging = ploggingSessionRepository.save(personalRecord(leader));
        PloggingSession secondPlogging = ploggingSessionRepository.save(personalRecord(member));
        PloggingPhoto firstPhoto = photoRepository.saveAndFlush(PloggingPhoto.create(
                firstPlogging, crewSession, 0, "https://s3.example.com/first.jpg"));
        PloggingPhoto secondPhoto = photoRepository.saveAndFlush(PloggingPhoto.create(
                secondPlogging, crewSession, 0, "https://s3.example.com/second.jpg"));

        leaderId = leader.getId();
        crewId = firstCrew.getId();
        sessionId = crewSession.getId();
        firstMemberId = firstMember.getId();
        secondMemberId = secondMember.getId();
        firstParticipantId = firstParticipant.getId();
        secondParticipantId = secondParticipant.getId();
        firstPhotoId = firstPhoto.getId();
        secondPhotoId = secondPhoto.getId();
        entityManager.clear();
    }

    @DisplayName("크루 공유 사진은 업로더 세션과 사용자를 함께 조회하고 기존 정렬을 유지한다")
    @Test
    void shared_photos_fetch_plogging_session_and_user_in_order() {
        // when
        List<PloggingPhoto> photos = photoRepository
                .findAllByCrewPloggingSessionIdOrderByCreatedAtAscIdAsc(sessionId);

        // then
        assertThat(photos).extracting(PloggingPhoto::getId)
                .containsExactly(firstPhotoId, secondPhotoId);
        assertThat(photos).allSatisfy(photo -> {
            assertThat(Hibernate.isInitialized(photo.getPloggingSession())).isTrue();
            assertThat(Hibernate.isInitialized(photo.getPloggingSession().getUser())).isTrue();
        });
    }

    @DisplayName("사진 메타데이터 벌크 조회는 세션별 최신 사진 순서를 유지한다")
    @Test
    void photo_metadata_query_keeps_latest_photo_order() {
        // when
        List<PloggingPhotoRepository.CrewPhotoMetadataView> photos =
                photoRepository.findPhotoMetadataByCrewPloggingSessionIdIn(List.of(sessionId));

        // then
        assertThat(photos).extracting(PloggingPhotoRepository.CrewPhotoMetadataView::getImageUrl)
                .containsExactly("https://s3.example.com/second.jpg", "https://s3.example.com/first.jpg");
    }

    @DisplayName("기록 상세 참가자는 사용자와 함께 조회되고 joinedAt 오름차순을 유지한다")
    @Test
    void detail_participants_fetch_user_in_joined_order() {
        // when
        List<CrewPloggingParticipant> participants = participantRepository
                .findAllWithUserByCrewPloggingSessionIdOrderByJoinedAtAsc(sessionId);

        // then
        assertThat(participants).extracting(CrewPloggingParticipant::getId)
                .containsExactly(firstParticipantId, secondParticipantId);
        assertThat(participants).allSatisfy(participant ->
                assertThat(Hibernate.isInitialized(participant.getUser())).isTrue());
    }

    @DisplayName("크루 상세 멤버는 사용자와 함께 조회되고 joinedAt 오름차순을 유지한다")
    @Test
    void crew_detail_members_fetch_user_in_joined_order() {
        // when
        List<CrewMember> members = crewMemberRepository
                .findAllByCrewIdAndStatusOrderByJoinedAtAsc(crewId, CrewMemberStatus.ACTIVE);

        // then
        assertThat(members).extracting(CrewMember::getId)
                .containsExactly(firstMemberId, secondMemberId);
        assertThat(members).allSatisfy(member ->
                assertThat(Hibernate.isInitialized(member.getUser())).isTrue());
    }

    @DisplayName("내 크루 멤버십은 크루와 함께 조회되고 joinedAt 내림차순을 유지한다")
    @Test
    void my_crew_memberships_fetch_crew_in_reverse_joined_order() {
        // when
        List<CrewMember> memberships = crewMemberRepository
                .findAllByUserIdAndStatusOrderByJoinedAtDesc(leaderId, CrewMemberStatus.ACTIVE);

        // then
        assertThat(memberships).extracting(member -> member.getCrew().getName())
                .containsExactly("두 번째 크루", "첫 번째 크루");
        assertThat(memberships).allSatisfy(member ->
                assertThat(Hibernate.isInitialized(member.getCrew())).isTrue());
    }

    private User user(String prefix) {
        String suffix = UUID.randomUUID().toString();
        return User.create(OAuthProvider.KAKAO, prefix + suffix, prefix + "@test.com", prefix + suffix, null);
    }

    private String joinCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
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
