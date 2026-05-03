package com.flover.flover_be.plogging.service;

import com.flover.flover_be.global.storage.StorageDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import com.flover.flover_be.plogging.domain.PloggingPhoto;
import com.flover.flover_be.plogging.domain.PloggingRoutePoint;
import com.flover.flover_be.plogging.domain.PloggingSession;
import com.flover.flover_be.plogging.dto.PloggingDto;
import com.flover.flover_be.plogging.repository.PloggingPhotoRepository;
import com.flover.flover_be.plogging.repository.PloggingRoutePointRepository;
import com.flover.flover_be.plogging.repository.PloggingSessionRepository;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.exception.UserErrorCode;
import com.flover.flover_be.user.exception.UserException;
import com.flover.flover_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PloggingService {

    private final UserRepository userRepository;
    private final PloggingSessionRepository ploggingSessionRepository;
    private final PloggingRoutePointRepository ploggingRoutePointRepository;
    private final PloggingPhotoRepository ploggingPhotoRepository;
    private final PloggingStorageService ploggingStorageService;

    @Transactional
    public PloggingDto.CompleteResponse complete(Long userId, PloggingDto.CompleteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

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
        savePhotos(savedSession, request.photoUrls());

        user.addPloggingTimeSeconds(request.ploggingSeconds());

        return new PloggingDto.CompleteResponse(savedSession.getId());
    }

    @Transactional(readOnly = true)
    public PloggingDto.SessionListResponse findSessions(Long userId, Pageable pageable) {
        Slice<PloggingSession> slice = ploggingSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId, pageable);
        List<PloggingDto.SessionSummaryResponse> content = slice.getContent().stream()
                .map(session -> new PloggingDto.SessionSummaryResponse(
                        session.getId(),
                        session.getMode(),
                        session.getPlaceName(),
                        session.getStartedAt(),
                        session.getFinishedAt(),
                        session.getDistanceMeters()
                ))
                .toList();
        return new PloggingDto.SessionListResponse(content, slice.hasNext());
    }

    @Transactional(readOnly = true)
    public StorageDto.PresignedUploadUrlResponse generateMapImagePresignedUrl(Long userId, String contentType) {
        validateUserExists(userId);
        return ploggingStorageService.generateMapImagePresignedUrl(userId, contentType);
    }

    @Transactional(readOnly = true)
    public StorageDto.PresignedUploadUrlResponse generatePhotoPresignedUrl(Long userId, String contentType) {
        validateUserExists(userId);
        return ploggingStorageService.generatePhotoPresignedUrl(userId, contentType);
    }

    private void validateUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    private void saveRoutePoints(PloggingSession session, List<PloggingDto.RoutePointRequest> routePoints) {
        List<PloggingRoutePoint> points = new ArrayList<>();
        for (int i = 0; i < routePoints.size(); i++) {
            PloggingDto.RoutePointRequest rp = routePoints.get(i);
            points.add(PloggingRoutePoint.create(session, i, rp.latitude(), rp.longitude()));
        }
        ploggingRoutePointRepository.saveAll(points);
    }

    private void savePhotos(PloggingSession session, List<String> photoUrls) {
        List<PloggingPhoto> photos = new ArrayList<>();
        for (int i = 0; i < photoUrls.size(); i++) {
            photos.add(PloggingPhoto.create(session, i, photoUrls.get(i)));
        }
        ploggingPhotoRepository.saveAll(photos);
    }
}
