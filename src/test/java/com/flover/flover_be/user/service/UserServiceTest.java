package com.flover.flover_be.user.service;

import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.dto.UserDto;
import com.flover.flover_be.user.exception.UserException;
import com.flover.flover_be.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @DisplayName("닉네임을 정상적으로 변경한다")
    @Test
    void update_nickname_성공() {
        // given
        Long userId = 1L;
        String newNickname = "새닉네임";
        User user = User.create(1L, "test@test.com", "기존닉네임", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname(newNickname)).willReturn(false);

        // when
        UserDto.NicknameResponse result = userService.updateNickname(userId, newNickname);

        // then
        assertThat(result.nickname()).isEqualTo(newNickname);
    }

    @DisplayName("존재하지 않는 유저의 닉네임 변경 시 예외가 발생한다")
    @Test
    void update_nickname_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(1L, "새닉네임"))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("이미 사용 중인 닉네임으로 변경 시 예외가 발생한다")
    @Test
    void update_nickname_중복_닉네임_예외() {
        // given
        Long userId = 1L;
        String duplicateNickname = "중복닉네임";
        User user = User.create(1L, "test@test.com", "기존닉네임", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname(duplicateNickname)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(userId, duplicateNickname))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("현재 닉네임과 동일한 닉네임으로 변경 시 중복 검사를 건너뛴다")
    @Test
    void update_nickname_같은_닉네임이면_중복_검사_건너뜀() {
        // given
        Long userId = 1L;
        String sameNickname = "기존닉네임";
        User user = User.create(1L, "test@test.com", sameNickname, null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.updateNickname(userId, sameNickname);

        // then
        verify(userRepository, never()).existsByNickname(anyString());
    }
}
