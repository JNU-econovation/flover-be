package com.plover.plover_be.crew.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CrewPloggingPrivacyTest {

    @DisplayName("크루 기록 상세 응답은 개인 경로, 경험치, 리포트 필드를 노출하지 않는다")
    @Test
    void record_detail_does_not_expose_private_personal_fields() {
        // given
        Set<String> fieldNames = Arrays.stream(CrewPloggingDto.RecordDetailResponse.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .collect(Collectors.toSet());

        // when & then
        assertThat(fieldNames)
                .noneMatch(name -> name.contains("route"))
                .noneMatch(name -> name.contains("experience"))
                .noneMatch(name -> name.contains("report"))
                .noneMatch(name -> name.contains("personalploggingsession"));
    }
}
