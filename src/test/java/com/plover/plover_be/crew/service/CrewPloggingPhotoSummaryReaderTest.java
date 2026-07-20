package com.plover.plover_be.crew.service;

import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrewPloggingPhotoSummaryReaderTest {

    @Mock private PloggingPhotoRepository photoRepository;
    @InjectMocks private CrewPloggingPhotoSummaryReader photoSummaryReader;

    @DisplayName("여러 세션의 사진 개수와 최신 사진을 섞지 않고 한 번에 집계한다")
    @Test
    void find_photo_summaries_groups_count_and_latest_photo_by_session() {
        // given
        List<Long> sessionIds = List.of(1L, 2L, 3L);
        given(photoRepository.findPhotoMetadataByCrewPloggingSessionIdIn(sessionIds)).willReturn(List.of(
                photo(1L, "session-1-latest.jpg"),
                photo(1L, "session-1-old.jpg"),
                photo(2L, "session-2-latest.jpg")
        ));

        // when
        Map<Long, CrewPloggingPhotoSummaryReader.PhotoSummary> result =
                photoSummaryReader.findBySessionIds(sessionIds);

        // then
        assertThat(result.get(1L).photoCount()).isEqualTo(2);
        assertThat(result.get(1L).representativePhotoUrl()).isEqualTo("session-1-latest.jpg");
        assertThat(result.get(2L).photoCount()).isEqualTo(1);
        assertThat(result.get(2L).representativePhotoUrl()).isEqualTo("session-2-latest.jpg");
        assertThat(result).doesNotContainKey(3L);
        verify(photoRepository).findPhotoMetadataByCrewPloggingSessionIdIn(sessionIds);
    }

    @DisplayName("조회할 세션이 없으면 사진 쿼리를 실행하지 않는다")
    @Test
    void find_photo_summaries_skips_query_for_empty_sessions() {
        // when
        Map<Long, CrewPloggingPhotoSummaryReader.PhotoSummary> result =
                photoSummaryReader.findBySessionIds(List.of());

        // then
        assertThat(result).isEmpty();
        verify(photoRepository, never()).findPhotoMetadataByCrewPloggingSessionIdIn(List.of());
    }

    private PloggingPhotoRepository.CrewPhotoMetadataView photo(Long sessionId, String imageUrl) {
        return new PloggingPhotoRepository.CrewPhotoMetadataView() {
            @Override
            public Long getCrewPloggingSessionId() {
                return sessionId;
            }

            @Override
            public String getImageUrl() {
                return imageUrl;
            }
        };
    }
}
