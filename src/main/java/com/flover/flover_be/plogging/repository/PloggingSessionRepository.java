package com.flover.flover_be.plogging.repository;

import com.flover.flover_be.plogging.domain.PloggingSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PloggingSessionRepository extends JpaRepository<PloggingSession, Long> {

    Slice<PloggingSession> findAllByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    Optional<PloggingSession> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(p.stepCount), 0) FROM PloggingSession p WHERE p.user.id = :userId")
    long sumStepCountByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.distanceMeters), 0) FROM PloggingSession p WHERE p.user.id = :userId")
    long sumDistanceByUserId(@Param("userId") Long userId);
}
