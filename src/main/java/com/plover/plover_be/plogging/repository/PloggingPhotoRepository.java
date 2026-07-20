package com.plover.plover_be.plogging.repository;

import com.plover.plover_be.plogging.domain.PloggingPhoto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PloggingPhotoRepository extends JpaRepository<PloggingPhoto, Long> {

    List<PloggingPhoto> findAllByPloggingSessionIdOrderBySequenceAsc(Long ploggingSessionId);

    @EntityGraph(attributePaths = {"ploggingSession", "ploggingSession.user"})
    List<PloggingPhoto> findAllByCrewPloggingSessionIdOrderByCreatedAtAscIdAsc(Long crewPloggingSessionId);

    @Query("""
            SELECT p.crewPloggingSession.id AS crewPloggingSessionId,
                   p.imageUrl AS imageUrl
            FROM PloggingPhoto p
            WHERE p.crewPloggingSession.id IN :sessionIds
            ORDER BY p.crewPloggingSession.id ASC, p.createdAt DESC, p.id DESC
            """)
    List<CrewPhotoMetadataView> findPhotoMetadataByCrewPloggingSessionIdIn(
            @Param("sessionIds") Collection<Long> sessionIds
    );

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

    interface CrewPhotoMetadataView {
        Long getCrewPloggingSessionId();
        String getImageUrl();
    }
}
