package com.plover.plover_be.plogging.service;

import com.plover.plover_be.global.storage.S3StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PloggingStorageServiceTest {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;

    @Mock private S3StorageService s3StorageService;
    @InjectMocks private PloggingStorageService ploggingStorageService;

    @DisplayName("크루 완료 사진은 로그인 사용자 사진 경로로 검증한다")
    @Test
    void validate_crew_completion_images_uses_user_photo_prefix() {
        // given
        Long userId = 7L;
        String photoUrl = "https://bucket.example/plogging/7/photos/photo.jpg";

        // when
        ploggingStorageService.validateCrewCompletionImages(userId, null, List.of(photoUrl));

        // then
        verify(s3StorageService).validateUploadedImage(
                photoUrl,
                "plogging/7/photos",
                MAX_FILE_SIZE_BYTES
        );
    }
}
