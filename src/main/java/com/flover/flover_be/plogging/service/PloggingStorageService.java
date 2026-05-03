package com.flover.flover_be.plogging.service;

import com.flover.flover_be.global.storage.S3StorageService;
import com.flover.flover_be.global.storage.StorageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PloggingStorageService {

    private final S3StorageService s3StorageService;

    public StorageDto.PresignedUploadUrlResponse generateMapImagePresignedUrl(Long userId, String contentType) {
        return s3StorageService.generatePresignedUploadUrl(
                "plogging/%d/maps".formatted(userId), contentType);
    }

    public StorageDto.PresignedUploadUrlResponse generatePhotoPresignedUrl(Long userId, String contentType) {
        return s3StorageService.generatePresignedUploadUrl(
                "plogging/%d/photos".formatted(userId), contentType);
    }
}
