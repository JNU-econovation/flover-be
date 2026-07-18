package com.plover.plover_be.plogging.service;

import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.plogging.domain.PloggingPhoto;
import com.plover.plover_be.plogging.domain.PloggingRoutePoint;
import com.plover.plover_be.plogging.domain.PloggingSession;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import com.plover.plover_be.plogging.repository.PloggingRoutePointRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PloggingRecordWriter {

    private final PloggingSessionRepository ploggingSessionRepository;
    private final PloggingRoutePointRepository ploggingRoutePointRepository;
    private final PloggingPhotoRepository ploggingPhotoRepository;

    public SavedPloggingRecord save(
            User user,
            PloggingDto.CompleteRequest request,
            CrewPloggingSession crewPloggingSession
    ) {
        PloggingSession session = PloggingSession.create(
                user, request.mode(),
                request.startedAt(), request.finishedAt(),
                request.distanceMeters(), request.stepCount(), request.caloriesBurned(),
                request.ploggingSeconds(), request.restSeconds(),
                request.placeName(),
                request.startLatitude(), request.startLongitude(),
                request.endLatitude(), request.endLongitude(),
                request.mapImageUrl()
        );
        PloggingSession savedSession = ploggingSessionRepository.save(session);

        saveRoutePoints(savedSession, request.routePoints());
        savePhotos(savedSession, crewPloggingSession, request.photoUrls());

        long previousExperience = user.getExperience();
        int previousLevel = user.getLevel();
        user.addPloggingTimeSeconds(request.ploggingSeconds());

        return new SavedPloggingRecord(
                savedSession,
                new PloggingDto.CompleteResponse(
                        savedSession.getId(),
                        previousExperience,
                        user.getExperience(),
                        previousLevel,
                        user.getLevel()
                )
        );
    }

    public record SavedPloggingRecord(
            PloggingSession session,
            PloggingDto.CompleteResponse response
    ) {}

    private void saveRoutePoints(PloggingSession session, List<PloggingDto.RoutePointRequest> routePoints) {
        List<PloggingRoutePoint> points = new ArrayList<>();
        for (int i = 0; i < routePoints.size(); i++) {
            PloggingDto.RoutePointRequest rp = routePoints.get(i);
            points.add(PloggingRoutePoint.create(session, i, rp.latitude(), rp.longitude()));
        }
        ploggingRoutePointRepository.saveAll(points);
    }

    private void savePhotos(
            PloggingSession session,
            CrewPloggingSession crewPloggingSession,
            List<String> photoUrls
    ) {
        List<PloggingPhoto> photos = new ArrayList<>();
        for (int i = 0; i < photoUrls.size(); i++) {
            photos.add(PloggingPhoto.create(session, crewPloggingSession, i, photoUrls.get(i)));
        }
        ploggingPhotoRepository.saveAll(photos);
    }
}
