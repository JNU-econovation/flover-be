package com.plover.plover_be.crew.exception;

import com.plover.plover_be.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CrewErrorCode implements ErrorCode {

    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "크루를 찾을 수 없습니다."),
    CREW_MEMBER_ONLY(HttpStatus.FORBIDDEN, "현재 크루원만 접근할 수 있습니다."),
    CREW_LEADER_ONLY(HttpStatus.FORBIDDEN, "크루장만 수행할 수 있습니다."),
    CREW_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "크루원을 찾을 수 없습니다."),
    CREW_MEMBER_ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "이미 탈퇴한 크루원입니다."),
    CREW_LEADER_CANNOT_WITHDRAW(HttpStatus.CONFLICT, "크루장은 크루에서 탈퇴할 수 없습니다."),
    CREW_LEADER_CANNOT_BE_WITHDRAWN(HttpStatus.CONFLICT, "크루장은 강퇴할 수 없습니다."),
    ACTIVE_PARTICIPANT_CANNOT_WITHDRAW(HttpStatus.CONFLICT, "개인 기록을 제출하지 않은 진행 중 참가자는 탈퇴하거나 강퇴할 수 없습니다."),
    INVALID_PARTICIPANT_STATE_FOR_WITHDRAWAL(HttpStatus.CONFLICT, "현재 참가자 상태에서는 탈퇴하거나 강퇴할 수 없습니다."),
    INVALID_JOIN_CODE(HttpStatus.BAD_REQUEST, "참여 코드는 숫자로 이루어진 6자리여야 합니다."),
    JOIN_CODE_CONFLICT(HttpStatus.CONFLICT, "참여 코드 생성 중 충돌이 발생했습니다. 다시 요청해 주세요."),
    ALREADY_CREW_MEMBER(HttpStatus.CONFLICT, "이미 가입한 크루입니다."),
    ACTIVE_SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 활성 같이 플로깅 세션이 있습니다."),
    CREW_PLOGGING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "같이 플로깅 세션을 찾을 수 없습니다."),
    SESSION_NOT_RECRUITING(HttpStatus.CONFLICT, "참가자를 모집 중인 세션이 아닙니다."),
    SESSION_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "진행 중인 세션이 아닙니다."),
    SESSION_NOT_COMPLETING(HttpStatus.CONFLICT, "개인 기록을 제출할 수 있는 세션 상태가 아닙니다."),
    ALREADY_SESSION_PARTICIPANT(HttpStatus.CONFLICT, "이미 같이 플로깅에 참가했습니다."),
    SESSION_PARTICIPANT_ONLY(HttpStatus.FORBIDDEN, "같이 플로깅 참가자만 수행할 수 있습니다."),
    LEADER_CANNOT_CANCEL_PARTICIPATION(HttpStatus.BAD_REQUEST, "크루장은 참가를 취소할 수 없습니다."),
    PLOGGING_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 개인 플로깅 기록을 제출했습니다."),
    SUBMISSION_DEADLINE_EXPIRED(HttpStatus.CONFLICT, "개인 기록 제출 가능 시간이 만료되었습니다."),
    CREW_PLOGGING_REQUIRES_FREE_MODE(HttpStatus.BAD_REQUEST, "같이 플로깅은 자유 플로깅 모드만 사용할 수 있습니다."),
    INVALID_SESSION_STATE(HttpStatus.CONFLICT, "현재 세션 상태에서는 요청을 처리할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
