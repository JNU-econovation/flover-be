package com.plover.plover_be.plogging.controller;

import com.plover.plover_be.global.exception.GlobalExceptionHandler;
import com.plover.plover_be.plogging.exception.PloggingErrorCode;
import com.plover.plover_be.plogging.exception.PloggingException;
import com.plover.plover_be.plogging.service.PloggingAnalysisService;
import com.plover.plover_be.plogging.service.PloggingImageValidator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PloggingAnalysisControllerTest {

    @Mock
    private PloggingAnalysisService ploggingAnalysisService;

    @Mock
    private PloggingImageValidator ploggingImageValidator;

    @InjectMocks
    private PloggingAnalysisController controller;

    private MockMvc mockMvc;

    private static final MockMultipartFile VALID_IMAGE = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", "fake-image-data".getBytes()
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @DisplayName("정상 요청 시 200 OK를 반환한다")
    @Test
    void analyze_image_성공_200_반환() throws Exception {
        // given
        doNothing().when(ploggingAnalysisService)
                .analyzeAsync(any(), anyString(), anyDouble(), anyDouble(), any(LocalDateTime.class));

        // when & then
        mockMvc.perform(multipart("/api/plogging/analyze")
                        .file(VALID_IMAGE)
                        .param("latitude", "37.5")
                        .param("longitude", "127.0"))
                .andExpect(status().isOk());
    }

    @DisplayName("정상 요청 시 서비스의 analyzeAsync가 호출된다")
    @Test
    void analyze_image_서비스_호출() throws Exception {
        // given
        doNothing().when(ploggingAnalysisService)
                .analyzeAsync(any(), anyString(), anyDouble(), anyDouble(), any(LocalDateTime.class));

        // when
        mockMvc.perform(multipart("/api/plogging/analyze")
                        .file(VALID_IMAGE)
                        .param("latitude", "37.5")
                        .param("longitude", "127.0"))
                .andExpect(status().isOk());

        // then
        verify(ploggingAnalysisService)
                .analyzeAsync(any(), anyString(), anyDouble(), anyDouble(), any(LocalDateTime.class));
    }

    @DisplayName("이미지 검증 실패 시 400을 반환하고 분석을 시작하지 않는다")
    @Test
    void analyze_image_검증_실패_400_반환() throws Exception {
        // given
        doThrow(new PloggingException(PloggingErrorCode.INVALID_IMAGE_FORMAT))
                .when(ploggingImageValidator).validate(any());

        // when & then
        mockMvc.perform(multipart("/api/plogging/analyze")
                        .file(VALID_IMAGE)
                        .param("latitude", "37.5")
                        .param("longitude", "127.0"))
                .andExpect(status().isBadRequest());

        verify(ploggingAnalysisService, never())
                .analyzeAsync(any(), anyString(), anyDouble(), anyDouble(), any(LocalDateTime.class));
    }

    @DisplayName("image 파트 없이 요청하면 400을 반환한다")
    @Test
    void analyze_image_파트_누락_400_반환() throws Exception {
        // when & then
        mockMvc.perform(multipart("/api/plogging/analyze")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0"))
                .andExpect(status().isBadRequest());

        verify(ploggingAnalysisService, never()).analyzeAsync(any(), any(), any(), any(), any());
    }

    @DisplayName("latitude 파라미터 없이 요청하면 400을 반환한다")
    @Test
    void analyze_image_latitude_누락_400_반환() throws Exception {
        // when & then
        mockMvc.perform(multipart("/api/plogging/analyze")
                        .file(VALID_IMAGE)
                        .param("longitude", "127.0"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("longitude 파라미터 없이 요청하면 400을 반환한다")
    @Test
    void analyze_image_longitude_누락_400_반환() throws Exception {
        // when & then
        mockMvc.perform(multipart("/api/plogging/analyze")
                        .file(VALID_IMAGE)
                        .param("latitude", "37.5"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("이미지 읽기 실패 시 PloggingException이 발생한다")
    @Test
    void analyze_image_읽기_실패_예외_발생() throws Exception {
        // given
        MockMultipartFile brokenFile = new MockMultipartFile(
                "image", "broken.jpg", "image/jpeg", (byte[]) null
        ) {
            @Override
            public byte[] getBytes() throws java.io.IOException {
                throw new java.io.IOException("강제 읽기 오류");
            }
        };

        // when & then
        mockMvc.perform(multipart("/api/plogging/analyze")
                        .file(brokenFile)
                        .param("latitude", "37.5")
                        .param("longitude", "127.0"))
                .andExpect(status().isInternalServerError());

        verify(ploggingAnalysisService, never()).analyzeAsync(any(), any(), any(), any(), any());
    }
}
