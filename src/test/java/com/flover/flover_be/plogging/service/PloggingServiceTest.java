package com.flover.flover_be.plogging.service;

import com.flover.flover_be.plogging.domain.PloggingMode;
import com.flover.flover_be.plogging.domain.PloggingPhoto;
import com.flover.flover_be.plogging.domain.PloggingRoutePoint;
import com.flover.flover_be.plogging.domain.PloggingSession;
import com.flover.flover_be.plogging.dto.PloggingDto;
import com.flover.flover_be.plogging.repository.PloggingPhotoRepository;
import com.flover.flover_be.plogging.repository.PloggingRoutePointRepository;
import com.flover.flover_be.plogging.repository.PloggingSessionRepository;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.exception.UserException;
import com.flover.flover_be.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PloggingServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PloggingSessionRepository ploggingSessionRepository;
    @Mock private PloggingRoutePointRepository ploggingRoutePointRepository;
    @Mock private PloggingPhotoRepository ploggingPhotoRepository;
    @InjectMocks private PloggingService ploggingService;

    @DisplayName("플로깅 완료 기록을 정상적으로 저장한다")
    @Test
    void complete_성공() {
        // given
        Long userId = 1L;
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        PloggingDto.CompleteRequest request = buildRequest(List.of(
                new PloggingDto.RoutePointRequest(37.1, 127.1),
                new PloggingDto.RoutePointRequest(37.2, 127.2)
        ), List.of("https://s3.example.com/photo1.jpg"));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.save(any(PloggingSession.class))).willAnswer(i -> i.getArgument(0));
        given(ploggingRoutePointRepository.saveAll(any())).willReturn(List.of());
        given(ploggingPhotoRepository.saveAll(any())).willReturn(List.of());

        // when
        PloggingDto.CompleteResponse result = ploggingService.complete(userId, request);

        // then
        assertThat(result.ploggingSessionId()).isNull(); // ID는 DB 채번이므로 null
        verify(ploggingSessionRepository).save(any(PloggingSession.class));
    }

    @DisplayName("존재하지 않는 유저로 플로깅 완료 저장 시 예외가 발생한다")
    @Test
    void complete_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());
        PloggingDto.CompleteRequest request = buildRequest(List.of(), List.of());

        // when & then
        assertThatThrownBy(() -> ploggingService.complete(1L, request))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("경로 좌표에 요청 순서대로 sequence 0부터 부여된다")
    @Test
    @SuppressWarnings("unchecked")
    void complete_경로포인트_시퀀스_부여() {
        // given
        Long userId = 1L;
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        List<PloggingDto.RoutePointRequest> routePoints = List.of(
                new PloggingDto.RoutePointRequest(37.1, 127.1),
                new PloggingDto.RoutePointRequest(37.2, 127.2),
                new PloggingDto.RoutePointRequest(37.3, 127.3)
        );
        PloggingDto.CompleteRequest request = buildRequest(routePoints, List.of());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ploggingPhotoRepository.saveAll(any())).willReturn(List.of());

        ArgumentCaptor<List<PloggingRoutePoint>> captor = ArgumentCaptor.forClass(List.class);
        given(ploggingRoutePointRepository.saveAll(captor.capture())).willReturn(List.of());

        // when
        ploggingService.complete(userId, request);

        // then
        List<PloggingRoutePoint> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved.get(0).getSequence()).isEqualTo(0);
        assertThat(saved.get(1).getSequence()).isEqualTo(1);
        assertThat(saved.get(2).getSequence()).isEqualTo(2);
        assertThat(saved.get(0).getLatitude()).isEqualTo(37.1);
        assertThat(saved.get(2).getLongitude()).isEqualTo(127.3);
    }

    @DisplayName("인증샷에 요청 순서대로 sequence 0부터 부여된다")
    @Test
    @SuppressWarnings("unchecked")
    void complete_사진_시퀀스_부여() {
        // given
        Long userId = 1L;
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        List<String> photoUrls = List.of(
                "https://s3.example.com/photo0.jpg",
                "https://s3.example.com/photo1.jpg"
        );
        PloggingDto.CompleteRequest request = buildRequest(List.of(), photoUrls);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ploggingRoutePointRepository.saveAll(any())).willReturn(List.of());

        ArgumentCaptor<List<PloggingPhoto>> captor = ArgumentCaptor.forClass(List.class);
        given(ploggingPhotoRepository.saveAll(captor.capture())).willReturn(List.of());

        // when
        ploggingService.complete(userId, request);

        // then
        List<PloggingPhoto> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getSequence()).isEqualTo(0);
        assertThat(saved.get(1).getSequence()).isEqualTo(1);
        assertThat(saved.get(0).getImageUrl()).isEqualTo("https://s3.example.com/photo0.jpg");
    }

    @DisplayName("플로깅 완료 시 유저의 총 플로깅 시간이 갱신된다")
    @Test
    void complete_유저_플로깅시간_갱신() {
        // given
        Long userId = 1L;
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        int ploggingSeconds = 600;
        PloggingDto.CompleteRequest request = buildRequest(List.of(), List.of());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ploggingRoutePointRepository.saveAll(any())).willReturn(List.of());
        given(ploggingPhotoRepository.saveAll(any())).willReturn(List.of());

        // when
        ploggingService.complete(userId, request);

        // then
        assertThat(user.getTotalPloggingSeconds()).isEqualTo(ploggingSeconds);
    }

    @DisplayName("경로 좌표와 인증샷이 없어도 정상 저장된다")
    @Test
    void complete_빈_목록_성공() {
        // given
        Long userId = 1L;
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        PloggingDto.CompleteRequest request = buildRequest(List.of(), List.of());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ploggingRoutePointRepository.saveAll(any())).willReturn(List.of());
        given(ploggingPhotoRepository.saveAll(any())).willReturn(List.of());

        // when
        PloggingDto.CompleteResponse result = ploggingService.complete(userId, request);

        // then
        assertThat(result.ploggingSessionId()).isNull();
        verify(ploggingRoutePointRepository).saveAll(List.of());
        verify(ploggingPhotoRepository).saveAll(List.of());
    }

    @DisplayName("플로깅 기록 목록을 최신순으로 조회한다")
    @Test
    void find_sessions_성공() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 5, 4, 10, 0, 0);
        User user = User.create(12345L, "test@test.com", "닉네임", null);

        PloggingSession older = PloggingSession.create(
                user, PloggingMode.FREE,
                now.minusDays(1), now.minusDays(1).plusHours(1),
                1000, 2000, 50, 300, 0, "장소A",
                37.5, 127.0, 37.51, 127.01, null);
        PloggingSession newer = PloggingSession.create(
                user, PloggingMode.RECOMMENDED,
                now, now.plusHours(1),
                2000, 4000, 100, 600, 0, "장소B",
                37.5, 127.0, 37.51, 127.01, null);

        given(ploggingSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId))
                .willReturn(List.of(newer, older));

        // when
        List<PloggingDto.SessionSummaryResponse> result = ploggingService.findSessions(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).placeName()).isEqualTo("장소B");
        assertThat(result.get(1).placeName()).isEqualTo("장소A");
        assertThat(result.get(0).mode()).isEqualTo(PloggingMode.RECOMMENDED);
        assertThat(result.get(0).distanceMeters()).isEqualTo(2000);
    }

    @DisplayName("플로깅 기록이 없으면 빈 리스트를 반환한다")
    @Test
    void find_sessions_빈_결과() {
        // given
        Long userId = 1L;
        given(ploggingSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId))
                .willReturn(List.of());

        // when
        List<PloggingDto.SessionSummaryResponse> result = ploggingService.findSessions(userId);

        // then
        assertThat(result).isEmpty();
    }

    private PloggingDto.CompleteRequest buildRequest(
            List<PloggingDto.RoutePointRequest> routePoints,
            List<String> photoUrls
    ) {
        return new PloggingDto.CompleteRequest(
                PloggingMode.FREE,
                LocalDateTime.of(2026, 5, 3, 10, 0, 0),
                LocalDateTime.of(2026, 5, 3, 10, 10, 0),
                1500,
                2000,
                80,
                600,
                0,
                "한강공원",
                37.5, 127.0,
                37.51, 127.01,
                routePoints,
                "https://s3.example.com/map.jpg",
                photoUrls
        );
    }
}
