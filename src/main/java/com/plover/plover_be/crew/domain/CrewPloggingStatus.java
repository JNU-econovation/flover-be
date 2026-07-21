package com.plover.plover_be.crew.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "같이 플로깅 상태: RECRUITING(모집), IN_PROGRESS(진행), COMPLETING(제출 유예), COMPLETED(완료), CANCELED(모집 취소)")
public enum CrewPloggingStatus {
    RECRUITING,
    IN_PROGRESS,
    COMPLETING,
    COMPLETED,
    CANCELED
}
