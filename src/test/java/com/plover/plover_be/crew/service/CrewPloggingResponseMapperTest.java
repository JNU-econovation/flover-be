package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.plogging.domain.PloggingMode;
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

import java.time.LocalDateTime;
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

    private PloggingSession personalRecord(User user) {
        LocalDateTime now = LocalDateTime.now();
        return PloggingSession.create(
                user, PloggingMode.FREE, now.minusHours(1), now,
                1000, 2000, 100, 3600, 0, "공원",
                37.5, 127.0, 37.6, 127.1, null
        );
    }
}
