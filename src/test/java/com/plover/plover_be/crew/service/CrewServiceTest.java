package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.crew.dto.CrewDto;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrewServiceTest {

    @Mock private CrewRepository crewRepository;
    @Mock private CrewMemberRepository crewMemberRepository;
    @Mock private CrewPloggingSessionRepository crewPloggingSessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CrewPloggingResponseMapper responseMapper;
    @InjectMocks private CrewService crewService;

    @DisplayName("크루 생성자는 크루장 멤버십으로 함께 저장된다")
    @Test
    void create_crew_saves_leader_membership() {
        // given
        Long userId = 1L;
        User user = user();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(crewRepository.existsByJoinCode(any())).willReturn(false);
        given(crewRepository.save(any(Crew.class))).willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<CrewMember> memberCaptor = ArgumentCaptor.forClass(CrewMember.class);

        // when
        CrewDto.CrewResponse response = crewService.createCrew(userId, "우리 크루");

        // then
        verify(crewMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(CrewRole.LEADER);
        assertThat(memberCaptor.getValue().getStatus()).isEqualTo(CrewMemberStatus.ACTIVE);
        assertThat(response.joinCode()).matches("[A-Z0-9]{8}");
    }

    @DisplayName("활성 크루원이 같은 참여 코드로 다시 가입하면 예외가 발생한다")
    @Test
    void join_crew_rejects_active_member() {
        // given
        Long userId = 1L;
        User user = user();
        Crew crew = Crew.create("우리 크루", "A1B2C3D4", user);
        CrewMember member = CrewMember.create(crew, user, CrewRole.MEMBER);
        given(crewRepository.findByJoinCode("A1B2C3D4")).willReturn(Optional.of(crew));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(crew.getId(), userId))
                .willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> crewService.joinCrew(userId, "a1b2c3d4"))
                .isInstanceOf(CrewException.class);
    }

    @DisplayName("탈퇴한 크루원은 기존 멤버십을 활성화해 재가입한다")
    @Test
    void join_crew_reactivates_withdrawn_member() {
        // given
        Long userId = 1L;
        User user = user();
        Crew crew = Crew.create("우리 크루", "A1B2C3D4", user);
        CrewMember member = CrewMember.create(crew, user, CrewRole.MEMBER);
        member.withdraw(LocalDateTime.now());
        given(crewRepository.findByJoinCode("A1B2C3D4")).willReturn(Optional.of(crew));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(crew.getId(), userId))
                .willReturn(Optional.of(member));

        // when
        crewService.joinCrew(userId, "A1B2C3D4");

        // then
        assertThat(member.getStatus()).isEqualTo(CrewMemberStatus.ACTIVE);
        assertThat(member.getLeftAt()).isNull();
    }

    private User user() {
        return User.create(OAuthProvider.KAKAO, "provider-id", "test@test.com", "닉네임", null);
    }
}
