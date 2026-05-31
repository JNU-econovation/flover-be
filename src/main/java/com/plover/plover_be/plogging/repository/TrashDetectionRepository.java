package com.plover.plover_be.plogging.repository;

import com.plover.plover_be.plogging.domain.TrashDetection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrashDetectionRepository extends JpaRepository<TrashDetection, Long> {
}