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
        assertThat(user.getTotalPloggingSeconds()).isZero();
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

    @DisplayName("플로깅 시간(초) 추가 시 경험치와 레벨이 갱신된다")
    @Test
    void addPloggingTimeSeconds_updates_experience_and_level() {
        User user = User.create(1L, "a@b.com", "nickname", null);

        user.addPloggingTimeSeconds(3600L); // 1시간

        assertThat(user.getTotalPloggingSeconds()).isEqualTo(3600L);
        assertThat(user.getExperience()).isEqualTo(720L);  // 3600 / 5 * 1
        assertThat(user.getLevel()).isEqualTo(2);           // 720 / 720 + 1
    }

    @DisplayName("5초 미만 자투리 시간은 경험치에 반영되지 않는다")
    @Test
    void addPloggingTimeSeconds_partial_interval_not_counted() {
        User user = User.create(1L, "a@b.com", "nickname", null);

        user.addPloggingTimeSeconds(4L); // 5초 미만

        assertThat(user.getExperience()).isZero();
    }

    @DisplayName("5초 단위로 경험치가 정확히 반영된다")
    @Test
    void addPloggingTimeSeconds_grants_exp_every_5_seconds() {
        User user = User.create(1L, "a@b.com", "nickname", null);

        user.addPloggingTimeSeconds(5L);
        assertThat(user.getExperience()).isEqualTo(1L);

        user.addPloggingTimeSeconds(5L);
        assertThat(user.getExperience()).isEqualTo(2L);
    }

    @DisplayName("플로깅 시간이 0 이하이면 예외가 발생한다")
    @Test
    void addPloggingTimeSeconds_invalid_throws_exception() {
        User user = User.create(1L, "a@b.com", "nickname", null);

        assertThatThrownBy(() -> user.addPloggingTimeSeconds(0L))
                .isInstanceOf(UserException.class);
        assertThatThrownBy(() -> user.addPloggingTimeSeconds(-1L))
                .isInstanceOf(UserException.class);
    }
}
