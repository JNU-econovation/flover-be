package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.exception.CrewErrorCode;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.service.PloggingStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrewPloggingCompletionService {

    private final PloggingStorageService ploggingStorageService;
    private final CrewPloggingCompletionProcessor completionProcessor;

    public PloggingDto.CompleteResponse complete(Long userId, PloggingDto.CompleteRequest request) {
        if (request.mode() != PloggingMode.FREE) {
            throw new CrewException(CrewErrorCode.CREW_PLOGGING_REQUIRES_FREE_MODE);
        }

        ploggingStorageService.validateCrewCompletionImages(
                userId,
                request.mapImageUrl(),
                request.photoUrls()
        );
        return completionProcessor.complete(userId, request);
    }
}
