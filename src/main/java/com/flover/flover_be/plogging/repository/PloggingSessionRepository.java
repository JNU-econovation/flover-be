package com.flover.flover_be.plogging.repository;

import com.flover.flover_be.plogging.domain.PloggingSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PloggingSessionRepository extends JpaRepository<PloggingSession, Long> {

    Slice<PloggingSession> findAllByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);
}
