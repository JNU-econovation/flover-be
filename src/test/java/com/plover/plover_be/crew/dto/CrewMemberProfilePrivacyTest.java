package com.plover.plover_be.crew.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CrewMemberProfilePrivacyTest {

    @DisplayName("크루원 공개 프로필 DTO는 인증 정보와 개인 기록 상세 필드를 노출하지 않는다")
    @Test
    void member_profile_does_not_expose_sensitive_fields() {
        assertThat(Arrays.stream(CrewDto.CrewMemberProfileResponse.class.getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly(
                        "userId",
                        "nickname",
                        "profileImageUrl",
                        "level",
                        "experience",
                        "ploggingCount",
                        "totalStepCount",
                        "totalDistanceMeters"
                )
                .doesNotContain(
                        "email",
                        "provider",
                        "providerId",
                        "routePoints",
                        "photoUrls",
                        "ploggingSessions"
                );
    }
}
