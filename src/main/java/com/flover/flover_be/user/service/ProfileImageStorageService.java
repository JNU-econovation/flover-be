package com.flover.flover_be.user.service;

import com.flover.flover_be.global.storage.S3StorageService;
import com.flover.flover_be.global.storage.StorageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileImageStorageService {

    private final S3StorageService s3StorageService;

    public StorageDto.PresignedUploadUrlResponse generatePresignedUploadUrl(Long userId, String contentType) {
        return s3StorageService.generatePresignedUploadUrl(
                "users/%d/profile".formatted(userId), contentType);
    }

    public void deleteIfOwnedByBucket(String objectUrl) {
        s3StorageService.deleteIfOwnedByBucket(objectUrl);
    }
}
