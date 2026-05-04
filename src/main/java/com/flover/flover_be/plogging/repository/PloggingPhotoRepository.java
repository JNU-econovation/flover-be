package com.flover.flover_be.plogging.repository;

import com.flover.flover_be.plogging.domain.PloggingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PloggingPhotoRepository extends JpaRepository<PloggingPhoto, Long> {

    List<PloggingPhoto> findAllByPloggingSessionIdOrderBySequenceAsc(Long ploggingSessionId);
}
