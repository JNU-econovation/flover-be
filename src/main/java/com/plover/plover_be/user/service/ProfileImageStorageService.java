package com.plover.plover_be.user.service;

import com.plover.plover_be.global.storage.S3StorageService;
import com.plover.plover_be.global.storage.StorageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileImageStorageService {

    private static final String PROFILE_IMAGE_PATH_TEMPLATE = "users/%d/profile";

    private final S3StorageService s3StorageService;

    public StorageDto.PresignedUploadUrlResponse generatePresignedUploadUrl(Long userId, String contentType) {
        return s3StorageService.generatePresignedUploadUrl(
                PROFILE_IMAGE_PATH_TEMPLATE.formatted(userId), contentType);
    }

    public void deleteIfOwnedByBucket(String objectUrl) {
        s3StorageService.deleteIfOwnedByBucket(objectUrl);
    }
}
