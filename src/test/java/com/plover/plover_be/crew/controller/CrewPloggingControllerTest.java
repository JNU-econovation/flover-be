package com.plover.plover_be.crew.controller;

import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.service.CrewPloggingService;
import com.plover.plover_be.global.auth.LoginUserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CrewPloggingControllerTest {

    @Mock private CrewPloggingService crewPloggingService;
    @InjectMocks private CrewPloggingController crewPloggingController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver loginUserIdResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(LoginUserId.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return 1L;
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(crewPloggingController)
                .setCustomArgumentResolvers(
                        loginUserIdResolver,
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @DisplayName("세션 전체 취소는 요청 본문 없이 로그인 사용자 ID를 서비스에 전달한다")
    @Test
    void cancel_session_uses_authenticated_user_id() {
        // given
        CrewPloggingDto.SessionResponse response = new CrewPloggingDto.SessionResponse(
                10L, CrewPloggingStatus.CANCELED, null, null, null,
                false, null, false, 0, false
        );
        given(crewPloggingService.cancelSession(1L, 10L)).willReturn(response);

        // when
        CrewPloggingDto.SessionResponse body = crewPloggingController.cancelSession(1L, 10L).getBody();

        // then
        assertThat(body).isEqualTo(response);
        verify(crewPloggingService).cancelSession(1L, 10L);
    }

    @DisplayName("크루 기록 목록은 page, size, sort 쿼리 파라미터를 Pageable로 바인딩한다")
    @Test
    void get_records_binds_individual_pageable_query_parameters() throws Exception {
        given(crewPloggingService.findRecords(eq(1L), eq(10L), any(Pageable.class)))
                .willReturn(new CrewPloggingDto.RecordListResponse(List.of(), false));

        mockMvc.perform(get("/api/crews/{crewId}/plogging-records", 10L)
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "string"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(crewPloggingService).findRecords(eq(1L), eq(10L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("string")).isNotNull();
    }
}
