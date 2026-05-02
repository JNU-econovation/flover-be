package com.flover.flover_be.global.storage;

import com.flover.flover_be.global.exception.BusinessException;
import com.flover.flover_be.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif",
            "image/avif"
    );
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    public StorageDto.PresignedUploadUrlResponse generatePresignedUploadUrl(String keyPrefix, String contentType) {
        validateContentType(contentType);
        String key = "%s/%s%s".formatted(keyPrefix, UUID.randomUUID(), resolveExtension(contentType));

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(r -> r
                .signatureDuration(PRESIGN_DURATION)
                .putObjectRequest(p -> p
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)));

        String objectUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
        return new StorageDto.PresignedUploadUrlResponse(presigned.url().toString(), objectUrl);
    }

    // 우리 버킷 소유 URL이면 삭제. 실패해도 예외 전파 안 함 (업데이트를 막지 않음)
    public void deleteIfOwnedByBucket(String objectUrl) {
        if (!isOwnedByBucket(objectUrl)) {
            return;
        }
        try {
            String key = extractKey(objectUrl);
            s3Client.deleteObject(r -> r.bucket(bucket).key(key));
        } catch (S3Exception e) {
            log.warn("Failed to delete S3 object: {}", objectUrl, e);
        }
    }

    private boolean isOwnedByBucket(String objectUrl) {
        return objectUrl != null && objectUrl.startsWith("https://%s.s3.".formatted(bucket));
    }

    private String extractKey(String objectUrl) {
        String prefix = "https://%s.s3.%s.amazonaws.com/".formatted(bucket, region);
        return objectUrl.substring(prefix.length());
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(CommonErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            case "image/heif" -> ".heif";
            case "image/avif" -> ".avif";
            default -> "";
        };
    }
}
