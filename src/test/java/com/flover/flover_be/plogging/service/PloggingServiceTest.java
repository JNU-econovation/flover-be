package com.flover.flover_be.plogging.service;

import com.flover.flover_be.plogging.domain.PloggingMode;
import com.flover.flover_be.plogging.domain.PloggingPhoto;
import com.flover.flover_be.plogging.domain.PloggingRoutePoint;
import com.flover.flover_be.plogging.domain.PloggingSession;
import com.flover.flover_be.plogging.dto.PloggingDto;
import com.flover.flover_be.plogging.exception.PloggingException;
import com.flover.flover_be.plogging.repository.PloggingPhotoRepository;
import com.flover.flover_be.plogging.repository.PloggingRoutePointRepository;
import com.flover.flover_be.plogging.repository.PloggingSessionRepository;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.exception.UserException;
import com.flover.flover_be.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
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
        Pageable pageable = PageRequest.of(0, 20);
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

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId, pageable))
                .willReturn(new SliceImpl<>(List.of(newer, older), pageable, false));

        // when
        PloggingDto.SessionListResponse result = ploggingService.findSessions(userId, pageable);

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).placeName()).isEqualTo("장소B");
        assertThat(result.content().get(1).placeName()).isEqualTo("장소A");
        assertThat(result.content().get(0).mode()).isEqualTo(PloggingMode.RECOMMENDED);
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("다음 페이지가 있으면 hasNext가 true다")
    @Test
    void find_sessions_hasNext_true() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 1);
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        PloggingSession session = PloggingSession.create(
                user, PloggingMode.FREE,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                1000, 2000, 50, 300, 0, "장소A",
                37.5, 127.0, 37.51, 127.01, null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId, pageable))
                .willReturn(new SliceImpl<>(List.of(session), pageable, true));

        // when
        PloggingDto.SessionListResponse result = ploggingService.findSessions(userId, pageable);

        // then
        assertThat(result.hasNext()).isTrue();
    }

    @DisplayName("존재하지 않는 유저로 플로깅 기록 조회 시 예외가 발생한다")
    @Test
    void find_sessions_유저없음_예외() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> ploggingService.findSessions(1L, pageable))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("플로깅 기록이 없으면 빈 리스트를 반환한다")
    @Test
    void find_sessions_빈_결과() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId, pageable))
                .willReturn(new SliceImpl<>(List.of(), pageable, false));

        // when
        PloggingDto.SessionListResponse result = ploggingService.findSessions(userId, pageable);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("플로깅 기록 단건 조회 시 상세 정보를 반환한다")
    @Test
    void find_session_성공() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 4, 10, 0, 0);
        LocalDateTime finishedAt = LocalDateTime.of(2026, 5, 4, 10, 30, 0);
        User user = User.create(12345L, "test@test.com", "닉네임", null);

        PloggingSession session = PloggingSession.create(
                user, PloggingMode.FREE,
                startedAt, finishedAt,
                3000, 4000, 150, 1800, 120,
                "한강공원", 37.5, 127.0, 37.52, 127.02,
                "https://s3.example.com/map.jpg"
        );

        PloggingPhoto photo1 = PloggingPhoto.create(session, 0, "https://s3.example.com/photo0.jpg");
        PloggingPhoto photo2 = PloggingPhoto.create(session, 1, "https://s3.example.com/photo1.jpg");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findByIdAndUserId(sessionId, userId)).willReturn(Optional.of(session));
        given(ploggingPhotoRepository.findAllByPloggingSessionIdOrderBySequenceAsc(sessionId))
                .willReturn(List.of(photo1, photo2));

        // when
        PloggingDto.SessionDetailResponse result = ploggingService.findSession(userId, sessionId);

        // then
        assertThat(result.mode()).isEqualTo(PloggingMode.FREE);
        assertThat(result.startedAt()).isEqualTo(startedAt);
        assertThat(result.finishedAt()).isEqualTo(finishedAt);
        assertThat(result.placeName()).isEqualTo("한강공원");
        assertThat(result.distanceMeters()).isEqualTo(3000);
        assertThat(result.stepCount()).isEqualTo(4000);
        assertThat(result.caloriesBurned()).isEqualTo(150);
        assertThat(result.ploggingSeconds()).isEqualTo(1800);
        assertThat(result.restSeconds()).isEqualTo(120);
        assertThat(result.mapImageUrl()).isEqualTo("https://s3.example.com/map.jpg");
        assertThat(result.photoUrls()).containsExactly(
                "https://s3.example.com/photo0.jpg",
                "https://s3.example.com/photo1.jpg"
        );
    }

    @DisplayName("존재하지 않거나 본인 기록이 아닌 플로깅 세션 조회 시 예외가 발생한다")
    @Test
    void find_session_없거나_타인기록_예외() {
        // given
        Long userId = 1L;
        Long sessionId = 999L;
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findByIdAndUserId(sessionId, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> ploggingService.findSession(userId, sessionId))
                .isInstanceOf(PloggingException.class);
    }

    @DisplayName("인증샷 URL이 sequence 오름차순으로 반환된다")
    @Test
    void find_session_인증샷_sequence_순서() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        PloggingSession session = PloggingSession.create(
                user, PloggingMode.FREE,
                LocalDateTime.of(2026, 5, 4, 9, 0, 0), LocalDateTime.of(2026, 5, 4, 9, 30, 0),
                1000, 2000, 80, 600, 0,
                "공원", 37.5, 127.0, 37.51, 127.01, null
        );

        PloggingPhoto first = PloggingPhoto.create(session, 0, "https://s3.example.com/first.jpg");
        PloggingPhoto second = PloggingPhoto.create(session, 1, "https://s3.example.com/second.jpg");
        PloggingPhoto third = PloggingPhoto.create(session, 2, "https://s3.example.com/third.jpg");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findByIdAndUserId(sessionId, userId)).willReturn(Optional.of(session));
        given(ploggingPhotoRepository.findAllByPloggingSessionIdOrderBySequenceAsc(sessionId))
                .willReturn(List.of(first, second, third));

        // when
        PloggingDto.SessionDetailResponse result = ploggingService.findSession(userId, sessionId);

        // then
        assertThat(result.photoUrls()).hasSize(3);
        assertThat(result.photoUrls().get(0)).isEqualTo("https://s3.example.com/first.jpg");
        assertThat(result.photoUrls().get(1)).isEqualTo("https://s3.example.com/second.jpg");
        assertThat(result.photoUrls().get(2)).isEqualTo("https://s3.example.com/third.jpg");
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
