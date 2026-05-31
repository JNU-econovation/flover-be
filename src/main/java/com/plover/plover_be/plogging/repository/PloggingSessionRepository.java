package com.plover.plover_be.plogging.repository;

import com.plover.plover_be.plogging.domain.PloggingSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PloggingSessionRepository extends JpaRepository<PloggingSession, Long> {

    Slice<PloggingSession> findAllByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    Optional<PloggingSession> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(p) AS count, COALESCE(SUM(p.stepCount), 0) AS totalStepCount, COALESCE(SUM(p.distanceMeters), 0) AS totalDistanceMeters FROM PloggingSession p WHERE p.user.id = :userId")
    PloggingStatsView findStatsByUserId(@Param("userId") Long userId);

    @Query("SELECT p.finishedAt AS finishedAt, p.stepCount AS stepCount, p.distanceMeters AS distanceMeters, p.caloriesBurned AS caloriesBurned, p.ploggingSeconds AS ploggingSeconds FROM PloggingSession p WHERE p.user.id = :userId AND p.finishedAt >= :start AND p.finishedAt < :end")
    List<SessionStatsView> findSessionStatsInPeriod(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    interface PloggingStatsView {
        long getCount();
        long getTotalStepCount();
        long getTotalDistanceMeters();
    }

    interface SessionStatsView {
        LocalDateTime getFinishedAt();
        int getStepCount();
        int getDistanceMeters();
        int getCaloriesBurned();
        int getPloggingSeconds();
    }

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PloggingSession s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
