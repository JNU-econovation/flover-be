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
import org.springframework.dao.DataIntegrityViolationException;

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
    @Mock private CrewPloggingPhotoSummaryReader photoSummaryReader;
    @Mock private CrewJoinCodeGenerator joinCodeGenerator;
    @InjectMocks private CrewService crewService;

    @DisplayName("크루 생성자는 크루장 멤버십으로 함께 저장된다")
    @Test
    void create_crew_saves_leader_membership() {
        // given
        Long userId = 1L;
        User user = user();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(joinCodeGenerator.generate()).willReturn("000527");
        given(crewRepository.saveAndFlush(any(Crew.class))).willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<CrewMember> memberCaptor = ArgumentCaptor.forClass(CrewMember.class);

        // when
        CrewDto.CrewResponse response = crewService.createCrew(userId, "우리 크루");

        // then
        verify(crewMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(CrewRole.LEADER);
        assertThat(memberCaptor.getValue().getStatus()).isEqualTo(CrewMemberStatus.ACTIVE);
        assertThat(response.joinCode()).isEqualTo("000527");
    }

    @DisplayName("사전 검사 후 INSERT에서 참여 코드가 충돌하면 409 비즈니스 예외로 변환한다")
    @Test
    void create_crew_converts_database_join_code_collision() {
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(user()));
        given(joinCodeGenerator.generate()).willReturn("123456");
        given(crewRepository.saveAndFlush(any(Crew.class)))
                .willThrow(new DataIntegrityViolationException("duplicate join code"));

        assertThatThrownBy(() -> crewService.createCrew(userId, "우리 크루"))
                .isInstanceOf(CrewException.class);
    }

    @DisplayName("활성 크루원이 같은 참여 코드로 다시 가입하면 예외가 발생한다")
    @Test
    void join_crew_rejects_active_member() {
        // given
        Long userId = 1L;
        User user = user();
        Crew crew = Crew.create("우리 크루", "123456", user);
        CrewMember member = CrewMember.create(crew, user, CrewRole.MEMBER);
        given(crewRepository.findByJoinCode("123456")).willReturn(Optional.of(crew));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(crew.getId(), userId))
                .willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> crewService.joinCrew(userId, " 123456 "))
                .isInstanceOf(CrewException.class);
    }

    @DisplayName("숫자 6자리 참여 코드의 앞뒤 공백을 제거해 신규 크루원으로 가입한다")
    @Test
    void join_crew_accepts_trimmed_six_digit_code() {
        Long userId = 2L;
        User leader = user();
        User joiningUser = User.create(
                OAuthProvider.KAKAO, "joining-user", "join@test.com", "가입자", null);
        Crew crew = Crew.create("우리 크루", "000527", leader);
        given(crewRepository.findByJoinCode("000527")).willReturn(Optional.of(crew));
        given(userRepository.findById(userId)).willReturn(Optional.of(joiningUser));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(crew.getId(), userId))
                .willReturn(Optional.empty());
        given(crewMemberRepository.saveAndFlush(any(CrewMember.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        CrewDto.CrewResponse response = crewService.joinCrew(userId, " 000527 ");

        assertThat(response.joinCode()).isEqualTo("000527");
        assertThat(response.role()).isEqualTo(CrewRole.MEMBER);
        verify(crewRepository).findByJoinCode("000527");
    }

    @DisplayName("탈퇴한 크루원은 기존 멤버십을 활성화해 재가입한다")
    @Test
    void join_crew_reactivates_withdrawn_member() {
        // given
        Long userId = 1L;
        User user = user();
        Crew crew = Crew.create("우리 크루", "123456", user);
        CrewMember member = CrewMember.create(crew, user, CrewRole.MEMBER);
        member.withdraw(LocalDateTime.now());
        given(crewRepository.findByJoinCode("123456")).willReturn(Optional.of(crew));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(crew.getId(), userId))
                .willReturn(Optional.of(member));

        // when
        crewService.joinCrew(userId, "123456");

        // then
        assertThat(member.getStatus()).isEqualTo(CrewMemberStatus.ACTIVE);
        assertThat(member.getLeftAt()).isNull();
    }

    @DisplayName("6자리 숫자가 아닌 참여 코드는 거절한다")
    @Test
    void join_crew_rejects_invalid_join_code() {
        assertThatThrownBy(() -> crewService.joinCrew(1L, "12A456"))
                .isInstanceOf(CrewException.class);
        assertThatThrownBy(() -> crewService.joinCrew(1L, "12345"))
                .isInstanceOf(CrewException.class);
        assertThatThrownBy(() -> crewService.joinCrew(1L, "1234567"))
                .isInstanceOf(CrewException.class);
    }

    private User user() {
        return User.create(OAuthProvider.KAKAO, "provider-id", "test@test.com", "닉네임", null);
    }
}
