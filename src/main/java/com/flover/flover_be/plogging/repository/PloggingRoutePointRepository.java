package com.flover.flover_be.plogging.repository;

import com.flover.flover_be.plogging.domain.PloggingRoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PloggingRoutePointRepository extends JpaRepository<PloggingRoutePoint, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PloggingRoutePoint r WHERE r.ploggingSession.user.id = :userId")
    void deleteByPloggingSessionUserId(@Param("userId") Long userId);
}
