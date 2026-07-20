package com.plover.plover_be.crew.service;

import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CrewPloggingPhotoSummaryReader {

    private final PloggingPhotoRepository photoRepository;

    public Map<Long, PhotoSummary> findBySessionIds(Collection<Long> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, PhotoSummary> summaries = new HashMap<>();
        for (PloggingPhotoRepository.CrewPhotoMetadataView photo
                : photoRepository.findPhotoMetadataByCrewPloggingSessionIdIn(sessionIds)) {
            summaries.compute(photo.getCrewPloggingSessionId(), (sessionId, existing) ->
                    existing == null
                            ? new PhotoSummary(1, photo.getImageUrl())
                            : new PhotoSummary(existing.photoCount() + 1, existing.representativePhotoUrl())
            );
        }
        return summaries;
    }

    public record PhotoSummary(long photoCount, String representativePhotoUrl) {

        public static PhotoSummary empty() {
            return new PhotoSummary(0, null);
        }
    }
}
