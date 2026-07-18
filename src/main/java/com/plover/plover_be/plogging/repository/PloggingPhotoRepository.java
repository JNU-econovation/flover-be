package com.plover.plover_be.plogging.repository;

import com.plover.plover_be.plogging.domain.PloggingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PloggingPhotoRepository extends JpaRepository<PloggingPhoto, Long> {

    List<PloggingPhoto> findAllByPloggingSessionIdOrderBySequenceAsc(Long ploggingSessionId);

    List<PloggingPhoto> findAllByCrewPloggingSessionIdOrderByCreatedAtAscIdAsc(Long crewPloggingSessionId);

    long countByCrewPloggingSessionId(Long crewPloggingSessionId);

    Optional<PloggingPhoto> findFirstByCrewPloggingSessionIdOrderByCreatedAtDescIdDesc(Long crewPloggingSessionId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PloggingPhoto p WHERE p.ploggingSession.user.id = :userId")
    void deleteByPloggingSessionUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE PloggingPhoto p
               SET p.crewPloggingSession = null
             WHERE p.crewPloggingSession.id IN (
                   SELECT s.id FROM CrewPloggingSession s WHERE s.crew.id = :crewId
             )
            """)
    void clearCrewPloggingSessionByCrewId(@Param("crewId") Long crewId);
}
