package com.plover.plover_be.plogging.repository;

import com.plover.plover_be.plogging.domain.PloggingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PloggingPhotoRepository extends JpaRepository<PloggingPhoto, Long> {

    List<PloggingPhoto> findAllByPloggingSessionIdOrderBySequenceAsc(Long ploggingSessionId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PloggingPhoto p WHERE p.ploggingSession.user.id = :userId")
    void deleteByPloggingSessionUserId(@Param("userId") Long userId);
}
