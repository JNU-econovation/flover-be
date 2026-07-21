package com.plover.plover_be.crew.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참가자 상태: JOINED(모집 참가), PARTICIPATING(측정 중), SUBMITTED(개인 기록 제출), NOT_SUBMITTED(유예 만료 미제출), CANCELED(참가 또는 모집 취소)")
public enum CrewPloggingParticipantStatus {
    JOINED,
    PARTICIPATING,
    SUBMITTED,
    NOT_SUBMITTED,
    CANCELED
}
