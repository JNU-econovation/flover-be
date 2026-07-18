package com.plover.plover_be.crew.repository;

import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CrewPloggingSessionRepository extends JpaRepository<CrewPloggingSession, Long> {

    Optional<CrewPloggingSession> findFirstByCrewIdAndStatusInOrderByCreatedAtDesc(
            Long crewId,
            Collection<CrewPloggingStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CrewPloggingSession s JOIN FETCH s.crew WHERE s.id = :sessionId")
    Optional<CrewPloggingSession> findByIdForUpdate(@Param("sessionId") Long sessionId);

    @Query("SELECT s.id FROM CrewPloggingSession s WHERE s.status = :status AND s.submissionDeadlineAt <= :now")
    List<Long> findIdsToFinalize(
            @Param("status") CrewPloggingStatus status,
            @Param("now") LocalDateTime now
    );

    Slice<CrewPloggingSession> findAllByCrewIdAndStatusOrderByEndedAtDesc(
            Long crewId,
            CrewPloggingStatus status,
            Pageable pageable
    );

    List<CrewPloggingSession> findAllByCrewIdAndStatusOrderByEndedAtDesc(
            Long crewId,
            CrewPloggingStatus status
    );

    @Query("""
            SELECT COUNT(s) AS count,
                   COALESCE(SUM(s.representativeStepCountSnapshot), 0) AS totalStepCount,
                   COALESCE(SUM(s.representativeDistanceMetersSnapshot), 0) AS totalDistanceMeters,
                   COALESCE(SUM(s.representativePloggingSecondsSnapshot), 0) AS totalPloggingSeconds
            FROM CrewPloggingSession s
            WHERE s.crew.id = :crewId
              AND s.status = com.plover.plover_be.crew.domain.CrewPloggingStatus.COMPLETED
              AND s.representativeStepCountSnapshot IS NOT NULL
            """)
    CrewStatsView findStatsByCrewId(@Param("crewId") Long crewId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE CrewPloggingSession s
               SET s.representativePloggingSession = null
             WHERE s.representativePloggingSession.user.id = :userId
            """)
    void clearRepresentativeRecordByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CrewPloggingSession s WHERE s.crew.id = :crewId")
    void deleteByCrewId(@Param("crewId") Long crewId);

    interface CrewStatsView {
        long getCount();
        long getTotalStepCount();
        long getTotalDistanceMeters();
        long getTotalPloggingSeconds();
    }
}
