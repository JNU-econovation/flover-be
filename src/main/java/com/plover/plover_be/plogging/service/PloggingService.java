package com.plover.plover_be.plogging.service;

import com.plover.plover_be.global.storage.StorageDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import com.plover.plover_be.plogging.domain.PloggingPhoto;
import com.plover.plover_be.plogging.domain.PloggingSession;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.exception.PloggingErrorCode;
import com.plover.plover_be.plogging.exception.PloggingException;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.exception.UserErrorCode;
import com.plover.plover_be.user.exception.UserException;
import com.plover.plover_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PloggingService {

    private final UserRepository userRepository;
    private final PloggingSessionRepository ploggingSessionRepository;
    private final PloggingPhotoRepository ploggingPhotoRepository;
    private final PloggingStorageService ploggingStorageService;
    private final PloggingRecordWriter ploggingRecordWriter;

    @Transactional
    public PloggingDto.CompleteResponse complete(Long userId, PloggingDto.CompleteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        return ploggingRecordWriter.save(user, request, null).response();
    }

    @Transactional(readOnly = true)
    public PloggingDto.SessionListResponse findSessions(Long userId, Pageable pageable) {
        validateUserExists(userId);
        Slice<PloggingSession> slice = ploggingSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId, pageable);
        List<PloggingDto.SessionSummaryResponse> content = slice.getContent().stream()
                .map(session -> new PloggingDto.SessionSummaryResponse(
                        session.getId(),
                        session.getMode(),
                        session.getPlaceName(),
                        session.getStartedAt(),
                        session.getFinishedAt(),
                        session.getDistanceMeters()
                ))
                .toList();
        return new PloggingDto.SessionListResponse(content, slice.hasNext());
    }

    @Transactional(readOnly = true)
    public PloggingDto.SessionDetailResponse findSession(Long userId, Long ploggingSessionId) {
        validateUserExists(userId);
        PloggingSession session = ploggingSessionRepository.findByIdAndUserId(ploggingSessionId, userId)
                .orElseThrow(() -> new PloggingException(PloggingErrorCode.PLOGGING_SESSION_NOT_FOUND));
        List<String> photoUrls = ploggingPhotoRepository
                .findAllByPloggingSessionIdOrderBySequenceAsc(ploggingSessionId)
                .stream()
                .map(PloggingPhoto::getImageUrl)
                .toList();
        return new PloggingDto.SessionDetailResponse(
                session.getId(),
                session.getMode(),
                session.getStartedAt(),
                session.getFinishedAt(),
                session.getPlaceName(),
                session.getDistanceMeters(),
                session.getStepCount(),
                session.getCaloriesBurned(),
                session.getPloggingSeconds(),
                session.getRestSeconds(),
                session.getMapImageUrl(),
                photoUrls
        );
    }

    @Transactional(readOnly = true)
    public PloggingDto.MonthlyStatsResponse findMonthlyStats(Long userId, int year, int month) {
        validateUserExists(userId);
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime end = start.plusMonths(1);
        List<PloggingSessionRepository.SessionStatsView> sessions = fetchPeriodStats(userId, start, end);
        SessionAggregate agg = SessionAggregate.from(sessions);
        return new PloggingDto.MonthlyStatsResponse(
                year, month,
                agg.stepCount(), agg.distanceMeters(), agg.caloriesBurned(),
                sessions.size(), agg.ploggingSeconds()
        );
    }

    @Transactional(readOnly = true)
    public PloggingDto.WeeklyStatsResponse findWeeklyStats(Long userId, LocalDate startDate) {
        validateUserExists(userId);
        LocalDate endDate = startDate.plusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        List<PloggingSessionRepository.SessionStatsView> sessions = fetchPeriodStats(userId, start, end);

        Map<LocalDate, List<PloggingSessionRepository.SessionStatsView>> byDate = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getFinishedAt().toLocalDate()));

        List<PloggingDto.DailyStatsResponse> dailyStats = IntStream.range(0, 7)
                .mapToObj(startDate::plusDays)
                .map(date -> {
                    List<PloggingSessionRepository.SessionStatsView> daySessions = byDate.getOrDefault(date, List.of());
                    SessionAggregate agg = SessionAggregate.from(daySessions);
                    return new PloggingDto.DailyStatsResponse(
                            date, date.getDayOfWeek(),
                            agg.stepCount(), agg.distanceMeters(), agg.caloriesBurned(),
                            daySessions.size(), agg.ploggingSeconds()
                    );
                })
                .toList();

        return new PloggingDto.WeeklyStatsResponse(startDate, endDate, dailyStats);
    }

    private List<PloggingSessionRepository.SessionStatsView> fetchPeriodStats(Long userId, LocalDateTime start, LocalDateTime end) {
        return ploggingSessionRepository.findSessionStatsInPeriod(userId, start, end);
    }

    private record SessionAggregate(long stepCount, long distanceMeters, long caloriesBurned, long ploggingSeconds) {
        static SessionAggregate from(List<PloggingSessionRepository.SessionStatsView> sessions) {
            long stepCount = 0, distanceMeters = 0, caloriesBurned = 0, ploggingSeconds = 0;
            for (PloggingSessionRepository.SessionStatsView s : sessions) {
                stepCount += s.getStepCount();
                distanceMeters += s.getDistanceMeters();
                caloriesBurned += s.getCaloriesBurned();
                ploggingSeconds += s.getPloggingSeconds();
            }
            return new SessionAggregate(stepCount, distanceMeters, caloriesBurned, ploggingSeconds);
        }
    }

    @Transactional(readOnly = true)
    public StorageDto.PresignedUploadUrlResponse generateMapImagePresignedUrl(Long userId, String contentType) {
        validateUserExists(userId);
        return ploggingStorageService.generateMapImagePresignedUrl(userId, contentType);
    }

    @Transactional(readOnly = true)
    public StorageDto.PresignedUploadUrlResponse generatePhotoPresignedUrl(Long userId, String contentType) {
        validateUserExists(userId);
        return ploggingStorageService.generatePhotoPresignedUrl(userId, contentType);
    }

    private void validateUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

}
