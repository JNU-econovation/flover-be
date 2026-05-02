package com.flover.flover_be.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.flover.flover_be.user.exception.UserException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @DisplayName("User.create initializes fields")
    @Test
    void create_initializes_fields() {
        User user = User.create(12345L, "test@test.com", "nickname", "http://img.url");

        assertThat(user.getKakaoId()).isEqualTo(12345L);
        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getNickname()).isEqualTo("nickname");
        assertThat(user.getProfileImageUrl()).isEqualTo("http://img.url");
        assertThat(user.getLevel()).isEqualTo(1);
        assertThat(user.getExperience()).isZero();
        assertThat(user.getTotalPloggingMinutes()).isZero();
    }

    @DisplayName("Update nickname and profile image")
    @Test
    void updateProfile_updates_nickname_and_profile_image() {
        User user = User.create(1L, "a@b.com", "oldNickname", "old.jpg");

        user.updateProfile("newNickname", "new.jpg");

        assertThat(user.getNickname()).isEqualTo("newNickname");
        assertThat(user.getProfileImageUrl()).isEqualTo("new.jpg");
    }

    @DisplayName("Update nickname")
    @Test
    void updateNickname_updates_nickname() {
        User user = User.create(1L, "a@b.com", "oldNickname", null);

        user.updateNickname("newNickname");

        assertThat(user.getNickname()).isEqualTo("newNickname");
    }

    @DisplayName("Update profile image")
    @Test
    void updateProfileImage_updates_profile_image() {
        User user = User.create(1L, "a@b.com", "nickname", "old.jpg");

        user.updateProfileImage("new.jpg");

        assertThat(user.getProfileImageUrl()).isEqualTo("new.jpg");
    }

    @DisplayName("Add plogging time updates experience and level")
    @Test
    void addPloggingTimeMinutes_updates_experience_and_level() {
        User user = User.create(1L, "a@b.com", "nickname", null);

        user.addPloggingTimeMinutes(600L);

        assertThat(user.getTotalPloggingMinutes()).isEqualTo(600L);
        assertThat(user.getExperience()).isEqualTo(1_000L);
        assertThat(user.getLevel()).isEqualTo(2);
    }

    @DisplayName("플로깅 시간이 0 이하이면 예외가 발생한다")
    @Test
    void addPloggingTimeMinutes_invalid_throws_exception() {
        User user = User.create(1L, "a@b.com", "nickname", null);

        assertThatThrownBy(() -> user.addPloggingTimeMinutes(0L))
                .isInstanceOf(UserException.class);
        assertThatThrownBy(() -> user.addPloggingTimeMinutes(-1L))
                .isInstanceOf(UserException.class);
    }
}
