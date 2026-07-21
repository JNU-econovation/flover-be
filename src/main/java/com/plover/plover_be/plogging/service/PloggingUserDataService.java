package com.plover.plover_be.plogging.service;

import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import com.plover.plover_be.plogging.repository.PloggingRoutePointRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.service.UserPloggingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PloggingUserDataService implements UserPloggingPort {

    private final PloggingSessionRepository ploggingSessionRepository;
    private final PloggingPhotoRepository ploggingPhotoRepository;
    private final PloggingRoutePointRepository ploggingRoutePointRepository;

    @Override
    public PloggingStats findStats(Long userId) {
        PloggingSessionRepository.PloggingStatsView stats =
                ploggingSessionRepository.findStatsByUserId(userId);
        return new PloggingStats(stats.getCount(), stats.getTotalStepCount(), stats.getTotalDistanceMeters());
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        ploggingPhotoRepository.deleteByPloggingSessionUserId(userId);
        ploggingRoutePointRepository.deleteByPloggingSessionUserId(userId);
        ploggingSessionRepository.deleteByUserId(userId);
    }
}
