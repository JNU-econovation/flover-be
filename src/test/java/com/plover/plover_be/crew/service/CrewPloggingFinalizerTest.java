package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CrewPloggingFinalizerTest {

    @Mock private CrewPloggingSessionRepository sessionRepository;
    @Mock private CrewPloggingParticipantRepository participantRepository;
    @InjectMocks private CrewPloggingFinalizer finalizer;

    @DisplayName("제출 유예가 만료되면 미제출 참가자를 처리하고 세션을 완료한다")
    @Test
    void finalize_expired_session_marks_not_submitted_and_completes() {
        // given
        User user = User.create(OAuthProvider.KAKAO, "id", "test@test.com", "닉네임", null);
        Crew crew = Crew.create("크루", "A1B2C3D4", user);
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        LocalDateTime now = LocalDateTime.now();
        session.start(now.minusHours(2));
        session.end(now.minusHours(1), now.minusMinutes(1));
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(session, user, true);
        participant.start();
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));
        given(participantRepository.findAllByCrewPloggingSessionIdOrderByJoinedAtAsc(10L))
                .willReturn(List.of(participant));
        given(participantRepository.countByCrewPloggingSessionIdAndStatusNot(
                session.getId(), CrewPloggingParticipantStatus.CANCELED)).willReturn(1L);

        // when
        finalizer.finalizeExpiredSession(10L, now);

        // then
        assertThat(participant.getStatus()).isEqualTo(CrewPloggingParticipantStatus.NOT_SUBMITTED);
        assertThat(session.getStatus()).isEqualTo(CrewPloggingStatus.COMPLETED);
        assertThat(session.getParticipantCountSnapshot()).isEqualTo(1);
    }

    @DisplayName("IN_PROGRESS에서 전원 조기 제출 시 서버 완료 시각과 참가자 스냅샷을 저장한다")
    @Test
    void complete_early_submitted_session() {
        // given
        User user = User.create(OAuthProvider.KAKAO, "id", "test@test.com", "닉네임", null);
        Crew crew = Crew.create("크루", "A1B2C3D4", user);
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        LocalDateTime now = LocalDateTime.now();
        session.start(now.minusHours(1));
        given(participantRepository.countByCrewPloggingSessionIdAndStatusNot(
                session.getId(), CrewPloggingParticipantStatus.CANCELED)).willReturn(3L);

        // when
        finalizer.complete(session, now);

        // then
        assertThat(session.getStatus()).isEqualTo(CrewPloggingStatus.COMPLETED);
        assertThat(session.getEndedAt()).isEqualTo(now);
        assertThat(session.getParticipantCountSnapshot()).isEqualTo(3);
    }
}
