package com.flover.flover_be.plogging.repository;

import com.flover.flover_be.plogging.domain.PloggingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PloggingSessionRepository extends JpaRepository<PloggingSession, Long> {

    List<PloggingSession> findAllByUserIdOrderByStartedAtDesc(Long userId);
}
