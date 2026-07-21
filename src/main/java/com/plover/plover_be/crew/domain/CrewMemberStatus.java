package com.plover.plover_be.crew.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "크루 멤버십 상태: ACTIVE(현재 크루원), WITHDRAWN(자발적 탈퇴 또는 강퇴). 두 탈퇴 유형은 구분하지 않습니다.")
public enum CrewMemberStatus {
    ACTIVE,
    WITHDRAWN
}
