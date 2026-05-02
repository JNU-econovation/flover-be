package com.flover.flover_be.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserTitle {

    쓰봉이(1, "쓰봉이"),
    새싹_수거자(2, "새싹 수거자"),
    쓰줍_초보자(5, "쓰줍 초보자"),
    쓰줍_중급자(10, "쓰줍 중급자"),
    쓰줍_고수(15, "쓰줍 고수"),
    거리의_청소부(20, "거리의 청소부"),
    거리_구조대(30, "거리 구조대"),
    도시_정화자(40, "도시 정화자"),
    플로깅_모험가(50, "플로깅 모험가"),
    플로깅_탐험가(60, "플로깅 탐험가"),
    플로깅_대장(70, "플로깅 대장"),
    플로깅_레전드(80, "플로깅 레전드"),
    환경_수호자(90, "환경 수호자"),
    환경의_신(100, "환경의 신"),
    ONE_TRASH_ONE_FOLLOW(150, "1trash1follow"),
    환경부장관(200, "환경부장관");

    private final int minLevel;
    private final String displayName;

    // 현재 레벨 이하에서 가장 높은 칭호 반환
    public static String of(int level) {
        String result = 쓰봉이.displayName;
        for (UserTitle title : values()) {
            if (level >= title.minLevel) {
                result = title.displayName;
            } else {
                break;
            }
        }
        return result;
    }
}
