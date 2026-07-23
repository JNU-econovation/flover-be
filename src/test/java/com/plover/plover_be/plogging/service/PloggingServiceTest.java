package com.plover.plover_be.plogging.service;

import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.domain.PloggingPhoto;
import com.plover.plover_be.plogging.domain.PloggingRoutePoint;
import com.plover.plover_be.plogging.domain.PloggingSession;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.exception.PloggingException;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import com.plover.plover_be.plogging.repository.PloggingRoutePointRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.exception.UserException;
import com.plover.plover_be.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    @Mock private PloggingStorageService ploggingStorageService;
    private PloggingService ploggingService;

    @BeforeEach
    void setUp() {
        PloggingRecordWriter ploggingRecordWriter = new PloggingRecordWriter(
                ploggingSessionRepository,
                ploggingRoutePointRepository,
                ploggingPhotoRepository
        );
        ploggingService = new PloggingService(
                userRepository,
                ploggingSessionRepository,
                ploggingPhotoRepository,
                ploggingStorageService,
                ploggingRecordWriter
        );
    }

    @DisplayName("플로깅 완료 기록을 정상적으로 저장한다")
    @Test
    void complete_성공() {
        // given
        Long userId = 1L;
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
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

    @DisplayName("플로깅 완료 응답에 이전/현재 경험치와 레벨이 포함된다")
    @Test
    void complete_경험치_이전후_반환() {
        // given
        Long userId = 1L;
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
        // ploggingSeconds=600 → experience = (600/5)*1 = 120, level = 1
        PloggingDto.CompleteRequest request = buildRequest(List.of(), List.of());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ploggingRoutePointRepository.saveAll(any())).willReturn(List.of());
        given(ploggingPhotoRepository.saveAll(any())).willReturn(List.of());

        // when
        PloggingDto.CompleteResponse result = ploggingService.complete(userId, request);

        // then
        assertThat(result.previousExperience()).isZero();
        assertThat(result.currentExperience()).isEqualTo(120L);
        assertThat(result.previousLevel()).isEqualTo(1);
        assertThat(result.currentLevel()).isEqualTo(1);
    }

    @DisplayName("플로깅 완료 후 레벨업이 발생하면 이전/현재 레벨이 다르다")
    @Test
    void complete_레벨업_시_이전후_레벨_반환() {
        // given
        Long userId = 1L;
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
        // 미리 3300초 적립 → experience=660, level=1
        user.addPloggingTimeSeconds(3300);
        // ploggingSeconds=600 추가 → totalPloggingSeconds=3900, experience=780, level=2
        PloggingDto.CompleteRequest request = buildRequest(List.of(), List.of());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ploggingRoutePointRepository.saveAll(any())).willReturn(List.of());
        given(ploggingPhotoRepository.saveAll(any())).willReturn(List.of());

        // when
        PloggingDto.CompleteResponse result = ploggingService.complete(userId, request);

        // then
        assertThat(result.previousExperience()).isEqualTo(660L);
        assertThat(result.currentExperience()).isEqualTo(780L);
        assertThat(result.previousLevel()).isEqualTo(1);
        assertThat(result.currentLevel()).isEqualTo(2);
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
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
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
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
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
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
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
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
        PloggingDto.CompleteRequest request = buildRequest(List.of(), List.of());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ploggingRoutePointRepository.saveAll(any())).willReturn(List.of());
        given(ploggingPhotoRepository.saveAll(any())).willReturn(List.of());

        // when
        PloggingDto.CompleteResponse result = ploggingService.complete(userId, request);

        // then
        assertThat(result.ploggingSessionId()).isNull();
        assertThat(result.previousExperience()).isNotNegative();
        assertThat(result.currentExperience()).isGreaterThanOrEqualTo(result.previousExperience());
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
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);

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
        assertThat(result.content().get(0).startedAt()).isEqualTo(now.toInstant(ZoneOffset.UTC));
        assertThat(result.content().get(0).finishedAt()).isEqualTo(now.plusHours(1).toInstant(ZoneOffset.UTC));
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("다음 페이지가 있으면 hasNext가 true다")
    @Test
    void find_sessions_hasNext_true() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 1);
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
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
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
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
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);

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
        assertThat(result.startedAt()).isEqualTo(Instant.parse("2026-05-04T10:00:00Z"));
        assertThat(result.finishedAt()).isEqualTo(finishedAt.toInstant(ZoneOffset.UTC));
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
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
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
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
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

    @DisplayName("월간 통계를 정상적으로 집계해 반환한다")
    @Test
    void find_monthly_stats_성공() {
        // given
        Long userId = 1L;
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
        List<PloggingSessionRepository.SessionStatsView> sessions = List.of(
                mockSessionStats(LocalDateTime.of(2026, 4, 10, 9, 0), 4000, 3000, 150, 1800),
                mockSessionStats(LocalDateTime.of(2026, 4, 20, 9, 0), 5000, 4000, 200, 2400)
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findSessionStatsInPeriod(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(sessions);

        // when
        PloggingDto.MonthlyStatsResponse result = ploggingService.findMonthlyStats(userId, 2026, 4);

        // then
        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.month()).isEqualTo(4);
        assertThat(result.totalPloggingCount()).isEqualTo(2);
        assertThat(result.totalStepCount()).isEqualTo(9000L);
        assertThat(result.totalDistanceMeters()).isEqualTo(7000L);
        assertThat(result.totalCaloriesBurned()).isEqualTo(350L);
        assertThat(result.totalPloggingSeconds()).isEqualTo(4200L);
    }

    @DisplayName("플로깅 기록이 없는 달은 모든 통계가 0이다")
    @Test
    void find_monthly_stats_기록없음_모두_0() {
        // given
        Long userId = 1L;
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findSessionStatsInPeriod(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(List.of());

        // when
        PloggingDto.MonthlyStatsResponse result = ploggingService.findMonthlyStats(userId, 2026, 4);

        // then
        assertThat(result.totalPloggingCount()).isZero();
        assertThat(result.totalStepCount()).isZero();
        assertThat(result.totalDistanceMeters()).isZero();
        assertThat(result.totalCaloriesBurned()).isZero();
        assertThat(result.totalPloggingSeconds()).isZero();
    }

    @DisplayName("존재하지 않는 유저로 월간 통계 조회 시 예외가 발생한다")
    @Test
    void find_monthly_stats_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> ploggingService.findMonthlyStats(1L, 2026, 4))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("주간 통계는 7개의 날짜 데이터를 반환한다")
    @Test
    void find_weekly_stats_7개_날짜_반환() {
        // given
        Long userId = 1L;
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
        LocalDate startDate = LocalDate.of(2026, 4, 14);
        List<PloggingSessionRepository.SessionStatsView> sessions = List.of(
                mockSessionStats(LocalDateTime.of(2026, 4, 15, 9, 0), 4800, 3900, 220, 3600)
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findSessionStatsInPeriod(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(sessions);

        // when
        PloggingDto.WeeklyStatsResponse result = ploggingService.findWeeklyStats(userId, startDate);

        // then
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 4, 14));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(result.dailyStats()).hasSize(7);
        assertThat(result.dailyStats().get(0).dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
    }

    @DisplayName("플로깅 기록이 없는 날은 0으로 채워진다")
    @Test
    void find_weekly_stats_기록없는_날_0으로_채워짐() {
        // given
        Long userId = 1L;
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "닉네임", null);
        LocalDate startDate = LocalDate.of(2026, 4, 14);
        List<PloggingSessionRepository.SessionStatsView> sessions = List.of(
                mockSessionStats(LocalDateTime.of(2026, 4, 15, 9, 0), 4800, 3900, 220, 3600)
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findSessionStatsInPeriod(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).willReturn(sessions);

        // when
        PloggingDto.WeeklyStatsResponse result = ploggingService.findWeeklyStats(userId, startDate);

        // then
        PloggingDto.DailyStatsResponse monday = result.dailyStats().get(0);   // 4/14 월요일
        PloggingDto.DailyStatsResponse tuesday = result.dailyStats().get(1);  // 4/15 화요일
        assertThat(monday.stepCount()).isZero();
        assertThat(monday.ploggingCount()).isZero();
        assertThat(tuesday.stepCount()).isEqualTo(4800L);
        assertThat(tuesday.ploggingCount()).isEqualTo(1L);
        assertThat(tuesday.caloriesBurned()).isEqualTo(220L);
    }

    @DisplayName("존재하지 않는 유저로 주간 통계 조회 시 예외가 발생한다")
    @Test
    void find_weekly_stats_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> ploggingService.findWeeklyStats(1L, LocalDate.of(2026, 4, 14)))
                .isInstanceOf(UserException.class);
    }

    private PloggingSessionRepository.SessionStatsView mockSessionStats(
            LocalDateTime finishedAt, int stepCount, int distanceMeters, int caloriesBurned, int ploggingSeconds
    ) {
        return new PloggingSessionRepository.SessionStatsView() {
            public LocalDateTime getFinishedAt() { return finishedAt; }
            public int getStepCount() { return stepCount; }
            public int getDistanceMeters() { return distanceMeters; }
            public int getCaloriesBurned() { return caloriesBurned; }
            public int getPloggingSeconds() { return ploggingSeconds; }
        };
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
                photoUrls,
                null
        );
    }
}
