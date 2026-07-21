package com.plover.plover_be.crew.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "크루 역할: LEADER(크루장), MEMBER(일반 크루원)")
public enum CrewRole {
    LEADER,
    MEMBER
}
