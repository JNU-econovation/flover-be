package com.plover.plover_be.user.domain;

import com.plover.plover_be.user.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static User kakaoUser(String nickname) {
        return User.create(OAuthProvider.KAKAO, "12345", "test@test.com", nickname, null);
    }

    @DisplayName("User.create 시 provider, providerId를 포함한 필드가 초기화된다")
    @Test
    void create_initializes_fields() {
        User user = User.create(OAuthProvider.KAKAO, "12345", "test@test.com", "nickname", "http://img.url");

        assertThat(user.getProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(user.getProviderId()).isEqualTo("12345");
        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getNickname()).isEqualTo("nickname");
        assertThat(user.getProfileImageUrl()).isEqualTo("http://img.url");
        assertThat(user.getLevel()).isEqualTo(1);
        assertThat(user.getExperience()).isZero();
        assertThat(user.getTotalPloggingSeconds()).isZero();
        assertThat(user.getTitle()).isEqualTo(UserTitle.쓰봉이);
    }

    @DisplayName("닉네임과 프로필 이미지를 동시에 변경한다")
    @Test
    void updateProfile_updates_nickname_and_profile_image() {
        User user = User.create(OAuthProvider.KAKAO, "1", "a@b.com", "oldNickname", "old.jpg");

        user.updateProfile("newNickname", "new.jpg");

        assertThat(user.getNickname()).isEqualTo("newNickname");
        assertThat(user.getProfileImageUrl()).isEqualTo("new.jpg");
    }

    @DisplayName("닉네임을 변경한다")
    @Test
    void updateNickname_updates_nickname() {
        User user = User.create(OAuthProvider.KAKAO, "1", "a@b.com", "oldNickname", null);

        user.updateNickname("newNickname");

        assertThat(user.getNickname()).isEqualTo("newNickname");
    }

    @DisplayName("프로필 이미지를 변경한다")
    @Test
    void updateProfileImage_updates_profile_image() {
        User user = User.create(OAuthProvider.KAKAO, "1", "a@b.com", "nickname", "old.jpg");

        user.updateProfileImage("new.jpg");

        assertThat(user.getProfileImageUrl()).isEqualTo("new.jpg");
    }

    @DisplayName("플로깅 시간(초) 추가 시 경험치, 레벨, 칭호가 갱신된다")
    @Test
    void addPloggingTimeSeconds_updates_experience_and_level() {
        User user = kakaoUser("nickname");

        user.addPloggingTimeSeconds(3600L); // 1시간

        assertThat(user.getTotalPloggingSeconds()).isEqualTo(3600L);
        assertThat(user.getExperience()).isEqualTo(720L);  // 3600 / 5 * 1
        assertThat(user.getLevel()).isEqualTo(2);           // 720 / 720 + 1
        assertThat(user.getTitle()).isEqualTo(UserTitle.새싹_수거자);
    }

    @DisplayName("5초 미만 자투리 시간은 경험치에 반영되지 않는다")
    @Test
    void addPloggingTimeSeconds_partial_interval_not_counted() {
        User user = kakaoUser("nickname");

        user.addPloggingTimeSeconds(4L);

        assertThat(user.getExperience()).isZero();
    }

    @DisplayName("5초 단위로 경험치가 정확히 반영된다")
    @Test
    void addPloggingTimeSeconds_grants_exp_every_5_seconds() {
        User user = kakaoUser("nickname");

        user.addPloggingTimeSeconds(5L);
        assertThat(user.getExperience()).isEqualTo(1L);

        user.addPloggingTimeSeconds(5L);
        assertThat(user.getExperience()).isEqualTo(2L);
    }

    @DisplayName("레벨 기준 사이 구간에서도 현재 레벨 이하 최고 칭호가 적용된다")
    @Test
    void title_reflects_highest_applicable_for_level_in_between() {
        User user = kakaoUser("nickname");

        // 17시간 → EXP=12240 → level 18 → 쓰줍 고수 (15~19레벨 구간)
        user.addPloggingTimeSeconds(3600L * 17);

        assertThat(user.getTitle()).isEqualTo(UserTitle.쓰줍_고수);
    }

    @DisplayName("레벨 기준과 정확히 일치하면 해당 칭호가 적용된다")
    @Test
    void title_changes_exactly_at_level_threshold() {
        User user = kakaoUser("nickname");

        // 4시간 → level 5 → 쓰줍 초보자
        user.addPloggingTimeSeconds(3600L * 4);

        assertThat(user.getLevel()).isEqualTo(5);
        assertThat(user.getTitle()).isEqualTo(UserTitle.쓰줍_초보자);
    }

    @DisplayName("플로깅 시간이 0 이하이면 예외가 발생한다")
    @Test
    void addPloggingTimeSeconds_invalid_throws_exception() {
        User user = kakaoUser("nickname");

        assertThatThrownBy(() -> user.addPloggingTimeSeconds(0L))
                .isInstanceOf(UserException.class);
        assertThatThrownBy(() -> user.addPloggingTimeSeconds(-1L))
                .isInstanceOf(UserException.class);
    }
}