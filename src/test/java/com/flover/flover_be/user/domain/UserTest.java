package com.flover.flover_be.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void create_필드_정상_초기화() {
        User user = User.create(12345L, "test@test.com", "닉네임", "http://img.url");

        assertThat(user.getKakaoId()).isEqualTo(12345L);
        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getNickname()).isEqualTo("닉네임");
        assertThat(user.getProfileImageUrl()).isEqualTo("http://img.url");
    }

    @Test
    void updateProfile_닉네임과_이미지_변경() {
        User user = User.create(1L, "a@b.com", "기존닉네임", "old.jpg");

        user.updateProfile("새닉네임", "new.jpg");

        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(user.getProfileImageUrl()).isEqualTo("new.jpg");
    }

}
