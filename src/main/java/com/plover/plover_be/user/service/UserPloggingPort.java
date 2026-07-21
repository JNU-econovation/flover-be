package com.plover.plover_be.user.service;

public interface UserPloggingPort {

    PloggingStats findStats(Long userId);

    void deleteByUserId(Long userId);

    record PloggingStats(long count, long totalStepCount, long totalDistanceMeters) {
    }
}
