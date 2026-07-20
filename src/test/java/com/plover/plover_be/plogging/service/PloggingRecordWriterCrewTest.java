package com.plover.plover_be.plogging.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.domain.PloggingPhoto;
import com.plover.plover_be.plogging.domain.PloggingSession;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import com.plover.plover_be.plogging.repository.PloggingRoutePointRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PloggingRecordWriterCrewTest {

    @Mock private PloggingSessionRepository sessionRepository;
    @Mock private PloggingRoutePointRepository routePointRepository;
    @Mock private PloggingPhotoRepository photoRepository;
    @InjectMocks private PloggingRecordWriter recordWriter;

    @DisplayName("크루 개인 완료 사진은 한 행에서 개인 기록과 크루 공유 앨범을 함께 참조한다")
    @Test
    void crew_completion_saves_one_photo_row_with_both_relations() {
        // given
        User user = User.create(OAuthProvider.KAKAO, "user", "user@test.com", "참가자", null);
        Crew crew = Crew.create("크루", "A1B2C3D4", user);
        CrewPloggingSession crewSession = CrewPloggingSession.create(crew);
        PloggingDto.CompleteRequest request = request(List.of("https://s3.example.com/photo.jpg"));
        given(sessionRepository.save(any(PloggingSession.class))).willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<List<PloggingPhoto>> photos = ArgumentCaptor.forClass(List.class);

        // when
        recordWriter.save(user, request, crewSession);

        // then
        verify(photoRepository).saveAll(photos.capture());
        assertThat(photos.getValue()).hasSize(1);
        assertThat(photos.getValue().get(0).getPloggingSession()).isNotNull();
        assertThat(photos.getValue().get(0).getCrewPloggingSession()).isSameAs(crewSession);
        assertThat(photos.getValue().get(0).getImageUrl()).isEqualTo("https://s3.example.com/photo.jpg");
        assertThat(user.getExperience()).isPositive();
    }

    private PloggingDto.CompleteRequest request(List<String> photoUrls) {
        return new PloggingDto.CompleteRequest(
                PloggingMode.FREE,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                1000, 2000, 100, 3600, 0, "공원",
                37.5, 127.0, 37.6, 127.1,
                List.of(), null, photoUrls, 10L
        );
    }
}
