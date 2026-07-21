package com.plover.plover_be.crew.controller;

import com.plover.plover_be.crew.dto.CrewDto;
import com.plover.plover_be.crew.service.CrewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrewControllerTest {

    @Mock private CrewService crewService;
    @InjectMocks private CrewController crewController;

    @DisplayName("크루원 목록은 로그인 사용자 ID로 조회한다")
    @Test
    void get_members_uses_authenticated_user_id() {
        CrewDto.CrewMemberListResponse response = new CrewDto.CrewMemberListResponse(List.of());
        given(crewService.findMembers(1L, 10L)).willReturn(response);

        assertThat(crewController.getMembers(1L, 10L).getBody()).isEqualTo(response);
        verify(crewService).findMembers(1L, 10L);
    }

    @DisplayName("크루원 프로필은 로그인 사용자 ID와 대상 ID로 조회한다")
    @Test
    void get_member_profile_uses_authenticated_user_id() {
        CrewDto.CrewMemberProfileResponse response = new CrewDto.CrewMemberProfileResponse(
                2L, "닉네임", null, 1, 0, 0, 0, 0
        );
        given(crewService.findMemberProfile(1L, 10L, 2L)).willReturn(response);

        assertThat(crewController.getMemberProfile(1L, 10L, 2L).getBody()).isEqualTo(response);
        verify(crewService).findMemberProfile(1L, 10L, 2L);
    }

    @DisplayName("자발적 탈퇴는 로그인 사용자 ID를 사용하고 204를 반환한다")
    @Test
    void withdraw_uses_authenticated_user_id() {
        assertThat(crewController.withdraw(2L, 10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(crewService).withdraw(2L, 10L);
    }

    @DisplayName("강퇴는 로그인 크루장 ID와 대상 ID를 사용하고 204를 반환한다")
    @Test
    void remove_member_uses_authenticated_user_id() {
        assertThat(crewController.removeMember(1L, 10L, 2L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(crewService).removeMember(1L, 10L, 2L);
    }
}
