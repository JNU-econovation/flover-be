package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.service.PloggingStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class CrewPloggingCompletionServiceTest {

    @Mock private PloggingStorageService ploggingStorageService;
    @Mock private CrewPloggingCompletionProcessor completionProcessor;
    @InjectMocks private CrewPloggingCompletionService completionService;

    @DisplayName("크루 완료는 S3 객체 검증 후 DB 완료 처리를 호출한다")
    @Test
    void complete_validates_s3_before_database_processor() {
        // given
        PloggingDto.CompleteRequest request = request(PloggingMode.FREE);

        // when
        completionService.complete(1L, request);

        // then
        InOrder inOrder = inOrder(ploggingStorageService, completionProcessor);
        inOrder.verify(ploggingStorageService)
                .validateCrewCompletionImages(1L, request.mapImageUrl(), request.photoUrls());
        inOrder.verify(completionProcessor).complete(1L, request);
    }

    @DisplayName("크루 완료 요청은 자유 플로깅 모드만 허용한다")
    @Test
    void complete_rejects_recommended_mode() {
        // given
        PloggingDto.CompleteRequest request = request(PloggingMode.RECOMMENDED);

        // when & then
        assertThatThrownBy(() -> completionService.complete(1L, request))
                .isInstanceOf(CrewException.class);
    }

    private PloggingDto.CompleteRequest request(PloggingMode mode) {
        return new PloggingDto.CompleteRequest(
                mode,
                LocalDateTime.of(2026, 7, 18, 10, 0),
                LocalDateTime.of(2026, 7, 18, 11, 0),
                1000, 2000, 100, 3600, 0, "공원",
                37.5, 127.0, 37.6, 127.1,
                List.of(),
                null,
                List.of(),
                10L
        );
    }
}
