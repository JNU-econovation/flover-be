package com.plover.plover_be.crew.repository;

import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CrewPloggingParticipantRepository extends JpaRepository<CrewPloggingParticipant, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM CrewPloggingParticipant p
            JOIN FETCH p.user
            WHERE p.crewPloggingSession.id = :sessionId AND p.user.id = :userId
            """)
    Optional<CrewPloggingParticipant> findBySessionIdAndUserIdForUpdate(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId
    );

    Optional<CrewPloggingParticipant> findByCrewPloggingSessionIdAndUserId(Long sessionId, Long userId);

    List<CrewPloggingParticipant> findAllByCrewPloggingSessionIdOrderByJoinedAtAsc(Long sessionId);

    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT p FROM CrewPloggingParticipant p
            WHERE p.crewPloggingSession.id = :sessionId
            ORDER BY p.joinedAt ASC
            """)
    List<CrewPloggingParticipant> findAllWithUserByCrewPloggingSessionIdOrderByJoinedAtAsc(
            @Param("sessionId") Long sessionId
    );

    long countByCrewPloggingSessionIdAndStatusNot(Long sessionId, CrewPloggingParticipantStatus status);

    long countByCrewPloggingSessionIdAndStatusIn(
            Long sessionId,
            Collection<CrewPloggingParticipantStatus> statuses
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CrewPloggingParticipant p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM CrewPloggingParticipant p
             WHERE p.crewPloggingSession.id IN (
                   SELECT s.id FROM CrewPloggingSession s WHERE s.crew.id = :crewId
             )
            """)
    void deleteByCrewId(@Param("crewId") Long crewId);
}
