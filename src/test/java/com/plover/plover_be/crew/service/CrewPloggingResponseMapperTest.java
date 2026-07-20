package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.domain.PloggingPhoto;
import com.plover.plover_be.plogging.domain.PloggingSession;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CrewPloggingResponseMapperTest {

    @Mock private CrewPloggingParticipantRepository participantRepository;
    @Mock private PloggingPhotoRepository photoRepository;
    @InjectMocks private CrewPloggingResponseMapper responseMapper;

    @DisplayName("조기 제출 참가자의 폴링 응답은 참가 및 기록 제출 완료 상태를 반환한다")
    @Test
    void polling_response_marks_early_submitter_as_submitted() {
        // given
        User user = User.create(OAuthProvider.KAKAO, "user", "user@test.com", "참가자", null);
        Crew crew = Crew.create("크루", "A1B2C3D4", user);
        CrewPloggingSession crewSession = CrewPloggingSession.create(crew);
        crewSession.start(LocalDateTime.now());
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, user, true);
        participant.start();
        participant.submit(personalRecord(user), LocalDateTime.now());
        given(participantRepository.findByCrewPloggingSessionIdAndUserId(crewSession.getId(), 1L))
                .willReturn(Optional.of(participant));
        given(participantRepository.countByCrewPloggingSessionIdAndStatusNot(
                crewSession.getId(), CrewPloggingParticipantStatus.CANCELED)).willReturn(2L);

        // when
        CrewPloggingDto.SessionResponse response = responseMapper.toSessionResponse(crewSession, 1L);

        // then
        assertThat(response.joinedByMe()).isTrue();
        assertThat(response.participantStatus()).isEqualTo(CrewPloggingParticipantStatus.SUBMITTED);
        assertThat(response.recordSubmittedByMe()).isTrue();
        assertThat(response.status()).isEqualTo(crewSession.getStatus());
    }

    @DisplayName("기록 요약은 벌크 조회된 사진 값으로 기존 응답 필드를 그대로 매핑한다")
    @Test
    void record_summary_maps_prefetched_photo_values() {
        // given
        User user = user("크루장", 1L);
        Crew crew = Crew.create("크루", "A1B2C3D4", user);
        CrewPloggingSession session = completedSession(crew, user, 10L);
        CrewPloggingPhotoSummaryReader.PhotoSummary photos =
                new CrewPloggingPhotoSummaryReader.PhotoSummary(3, "https://s3.example.com/latest.jpg");

        // when
        CrewPloggingDto.RecordSummaryResponse response = responseMapper.toRecordSummary(session, photos);

        // then
        assertThat(response.crewPloggingSessionId()).isEqualTo(10L);
        assertThat(response.representativeNickname()).isEqualTo("크루장");
        assertThat(response.stepCount()).isEqualTo(2000);
        assertThat(response.distanceMeters()).isEqualTo(1000);
        assertThat(response.ploggingSeconds()).isEqualTo(3600);
        assertThat(response.participantCount()).isEqualTo(1);
        assertThat(response.sharedPhotoCount()).isEqualTo(3);
        assertThat(response.representativePhotoUrl()).isEqualTo("https://s3.example.com/latest.jpg");
    }

    @DisplayName("기록 상세는 조회된 참가자와 공유 사진을 기존 DTO 및 개인정보 범위로 매핑한다")
    @Test
    void record_detail_maps_fetched_participants_and_photos() {
        // given
        User user = user("참가자", 1L);
        Crew crew = Crew.create("크루", "A1B2C3D4", user);
        CrewPloggingSession crewSession = completedSession(crew, user, 10L);
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, user, true);
        PloggingSession personalRecord = personalRecord(user);
        PloggingPhoto photo = PloggingPhoto.create(
                personalRecord, crewSession, 0, "https://s3.example.com/photo.jpg");
        LocalDateTime registeredAt = LocalDateTime.now();
        ReflectionTestUtils.setField(photo, "id", 20L);
        ReflectionTestUtils.setField(photo, "createdAt", registeredAt);
        given(participantRepository.findAllWithUserByCrewPloggingSessionIdOrderByJoinedAtAsc(10L))
                .willReturn(List.of(participant));
        given(photoRepository.findAllByCrewPloggingSessionIdOrderByCreatedAtAscIdAsc(10L))
                .willReturn(List.of(photo));

        // when
        CrewPloggingDto.RecordDetailResponse response = responseMapper.toRecordDetail(crewSession);

        // then
        assertThat(response.crewPloggingSessionId()).isEqualTo(10L);
        assertThat(response.representativeNickname()).isEqualTo("참가자");
        assertThat(response.participants()).singleElement().satisfies(result -> {
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.nickname()).isEqualTo("참가자");
        });
        assertThat(response.photos()).singleElement().satisfies(result -> {
            assertThat(result.photoId()).isEqualTo(20L);
            assertThat(result.objectUrl()).isEqualTo("https://s3.example.com/photo.jpg");
            assertThat(result.uploaderUserId()).isEqualTo(1L);
            assertThat(result.uploaderNickname()).isEqualTo("참가자");
            assertThat(result.registeredAt()).isEqualTo(registeredAt);
        });
    }

    private CrewPloggingSession completedSession(Crew crew, User representative, Long sessionId) {
        LocalDateTime now = LocalDateTime.now();
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        ReflectionTestUtils.setField(session, "id", sessionId);
        session.start(now.minusHours(1));
        session.selectRepresentative(personalRecord(representative), representative.getId(), representative.getNickname());
        session.complete(now, 1);
        assertThat(session.getStatus()).isEqualTo(CrewPloggingStatus.COMPLETED);
        return session;
    }

    private User user(String nickname, Long userId) {
        User user = User.create(OAuthProvider.KAKAO, nickname, "user@test.com", nickname, null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
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
