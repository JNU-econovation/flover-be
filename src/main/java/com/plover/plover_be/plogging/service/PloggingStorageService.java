package com.plover.plover_be.plogging.service;

import com.plover.plover_be.global.exception.BusinessException;
import com.plover.plover_be.global.exception.CommonErrorCode;
import com.plover.plover_be.global.storage.S3StorageService;
import com.plover.plover_be.global.storage.StorageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PloggingStorageService {

    private static final String MAP_IMAGE_PATH_TEMPLATE = "plogging/%d/maps";
    private static final String PHOTO_PATH_TEMPLATE = "plogging/%d/photos";
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;

    private final S3StorageService s3StorageService;

    public StorageDto.PresignedUploadUrlResponse generateMapImagePresignedUrl(Long userId, String contentType) {
        return s3StorageService.generatePresignedUploadUrl(
                MAP_IMAGE_PATH_TEMPLATE.formatted(userId), contentType);
    }

    public StorageDto.PresignedUploadUrlResponse generatePhotoPresignedUrl(Long userId, String contentType) {
        return s3StorageService.generatePresignedUploadUrl(
                PHOTO_PATH_TEMPLATE.formatted(userId), contentType);
    }

    public void validateCrewCompletionImages(Long userId, String mapImageUrl, List<String> photoUrls) {
        if (mapImageUrl != null && !mapImageUrl.isBlank()) {
            s3StorageService.validateUploadedImage(
                    mapImageUrl,
                    MAP_IMAGE_PATH_TEMPLATE.formatted(userId),
                    MAX_FILE_SIZE_BYTES
            );
        }

        Set<String> uniqueUrls = new HashSet<>();
        for (String photoUrl : photoUrls) {
            if (!uniqueUrls.add(photoUrl)) {
                throw new BusinessException(CommonErrorCode.INVALID_IMAGE_URL);
            }
            s3StorageService.validateUploadedImage(
                    photoUrl,
                    PHOTO_PATH_TEMPLATE.formatted(userId),
                    MAX_FILE_SIZE_BYTES
            );
        }
    }
}
