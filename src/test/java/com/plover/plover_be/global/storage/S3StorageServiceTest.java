package com.plover.plover_be.global.storage;

import com.plover.plover_be.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;
    @InjectMocks private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3StorageService, "bucket", "plover-be");
        ReflectionTestUtils.setField(s3StorageService, "region", "ap-northeast-2");
    }

    @DisplayName("사용자 사진 경로의 업로드된 이미지 객체를 HEAD로 검증한다")
    @Test
    void validate_uploaded_image_checks_head_metadata() {
        // given
        String url = "https://plover-be.s3.ap-northeast-2.amazonaws.com/plogging/7/photos/id.jpg";
        given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(
                HeadObjectResponse.builder()
                        .contentType("image/jpeg")
                        .contentLength(1024L)
                        .build()
        );

        // when
        s3StorageService.validateUploadedImage(url, "plogging/7/photos", 10 * 1024 * 1024L);

        // then
        verify(s3Client).headObject(any(HeadObjectRequest.class));
    }

    @DisplayName("다른 사용자의 S3 사진 경로는 HEAD 호출 전에 차단한다")
    @Test
    void validate_uploaded_image_rejects_other_user_prefix() {
        // given
        String url = "https://plover-be.s3.ap-northeast-2.amazonaws.com/plogging/8/photos/id.jpg";

        // when & then
        assertThatThrownBy(() -> s3StorageService.validateUploadedImage(
                url, "plogging/7/photos", 10 * 1024 * 1024L))
                .isInstanceOf(BusinessException.class);
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    @DisplayName("허용 크기를 넘는 S3 이미지 객체는 차단한다")
    @Test
    void validate_uploaded_image_rejects_oversized_object() {
        // given
        String url = "https://plover-be.s3.ap-northeast-2.amazonaws.com/plogging/7/photos/id.jpg";
        given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(
                HeadObjectResponse.builder()
                        .contentType("image/jpeg")
                        .contentLength(10 * 1024 * 1024L + 1)
                        .build()
        );

        // when & then
        assertThatThrownBy(() -> s3StorageService.validateUploadedImage(
                url, "plogging/7/photos", 10 * 1024 * 1024L))
                .isInstanceOf(BusinessException.class);
    }
}
