package com.plover.plover_be.plogging.service;

import com.plover.plover_be.global.storage.S3StorageService;
import com.plover.plover_be.global.storage.StorageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PloggingStorageService {

    private static final String MAP_IMAGE_PATH_TEMPLATE = "plogging/%d/maps";
    private static final String PHOTO_PATH_TEMPLATE = "plogging/%d/photos";

    private final S3StorageService s3StorageService;

    public StorageDto.PresignedUploadUrlResponse generateMapImagePresignedUrl(Long userId, String contentType) {
        return s3StorageService.generatePresignedUploadUrl(
                MAP_IMAGE_PATH_TEMPLATE.formatted(userId), contentType);
    }

    public StorageDto.PresignedUploadUrlResponse generatePhotoPresignedUrl(Long userId, String contentType) {
        return s3StorageService.generatePresignedUploadUrl(
                PHOTO_PATH_TEMPLATE.formatted(userId), contentType);
    }
}
