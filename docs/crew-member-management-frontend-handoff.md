# 크루원 관리 MVP 프론트엔드 연동 문서

## 공통 사항

- 모든 API는 `Authorization: Bearer {JWT}` 인증이 필요하다.
- 자발적 탈퇴와 크루장에 의한 강퇴는 모두 `CrewMemberStatus.WITHDRAWN`으로 처리한다.
- 탈퇴·강퇴는 멤버십 행을 삭제하지 않는다. 개인 기록, 이동 경로, 경험치, 개인 사진, 과거 같이 플로깅 참가 기록과 공유 사진은 그대로 유지된다.
- 탈퇴·강퇴된 사용자는 내 크루 목록, 현재 크루원 목록, 크루 상세와 크루 기록 조회에서 제외되거나 접근할 수 없다.
- 탈퇴·강퇴된 사용자는 기존 참여 코드로 재가입할 수 있다. 재가입하면 기존 멤버십이 `ACTIVE`로 복구되고 최초 `joinedAt`은 유지된다.
- 오류 응답은 기존 형식인 `{ "status": number, "message": string }`을 유지한다.

## 숫자 6자리 참여 코드

- `joinCode`는 `000000`부터 `999999`까지의 숫자 6자리 문자열이다.
- 앞자리 0이 의미가 있으므로 숫자로 변환하지 말고 문자열로 보관·전송한다.
- 가입 요청의 앞뒤 공백은 서버가 제거하지만, 그 외 문자는 허용하지 않는다.
- `527`, `12A456`, `1234567`은 거절된다.
- 코드는 만료·자동 재발급되지 않고 하나의 크루에 계속 귀속된다.

가입 요청:

```http
POST /api/crews/join
Authorization: Bearer {JWT}
Content-Type: application/json

{
  "joinCode": "000527"
}
```

가입 응답:

```json
{
  "crewId": 10,
  "name": "한강 지킴이",
  "joinCode": "000527",
  "role": "MEMBER"
}
```

## 크루원 목록

```http
GET /api/crews/{crewId}/members
Authorization: Bearer {JWT}
```

- 요청자가 해당 크루의 `ACTIVE` 회원이어야 한다.
- 크루장을 포함한 `ACTIVE` 회원만 최초 `joinedAt` 오름차순으로 반환한다.
- 페이지네이션은 적용하지 않는다.

```json
{
  "members": [
    {
      "userId": 11,
      "nickname": "크루장",
      "profileImageUrl": "https://example.com/leader.jpg",
      "role": "LEADER",
      "joinedAt": "2026-07-20T10:00:00"
    },
    {
      "userId": 21,
      "nickname": "플로버",
      "profileImageUrl": null,
      "role": "MEMBER",
      "joinedAt": "2026-07-20T10:30:00"
    }
  ]
}
```

## 크루원 공개 프로필

```http
GET /api/crews/{crewId}/members/{targetUserId}
Authorization: Bearer {JWT}
```

- 요청자와 대상 모두 해당 크루의 `ACTIVE` 회원이어야 한다.
- 본인도 조회할 수 있다.
- 통계에는 일반 개인 플로깅과 같이 플로깅에서 저장된 개인 기록이 모두 포함된다.
- 이메일, OAuth 정보, 로그인 식별자, 개인 이동 경로, 개인 사진과 기록 상세는 응답하지 않는다.

```json
{
  "userId": 21,
  "nickname": "플로버",
  "profileImageUrl": null,
  "level": 3,
  "experience": 1440,
  "ploggingCount": 12,
  "totalStepCount": 42000,
  "totalDistanceMeters": 28500
}
```

## 자발적 탈퇴

```http
DELETE /api/crews/{crewId}/members/me
Authorization: Bearer {JWT}
```

- `ACTIVE` 일반 크루원만 호출할 수 있다.
- 성공 응답은 `204 No Content`이며 body가 없다.
- 크루장은 탈퇴할 수 없고 크루장 위임 기능은 제공하지 않는다.
- 중복 탈퇴는 성공으로 간주하지 않는다.

## 크루원 강퇴

```http
DELETE /api/crews/{crewId}/members/{targetUserId}
Authorization: Bearer {JWT}
```

- 해당 크루의 `ACTIVE` 크루장만 호출할 수 있다.
- 대상은 `ACTIVE` 일반 크루원이어야 한다.
- 크루장 자신 또는 다른 크루장은 강퇴할 수 없다.
- 이미 `WITHDRAWN`인 대상에 대한 중복 강퇴는 `409 Conflict`다.
- 성공 응답은 `204 No Content`이며 body가 없다.

## 진행 중 세션과 탈퇴·강퇴

| 세션 상태 | 대상 참가 상태 | 처리 |
|---|---|---|
| 세션 없음 | 미참가 | 허용 |
| `RECRUITING` | `JOINED` | 허용, 참가자는 `CANCELED` |
| `RECRUITING` | `CANCELED` | 허용 |
| `IN_PROGRESS`, `COMPLETING` | `PARTICIPATING` | 409, 멤버십과 참가 상태 유지 |
| `IN_PROGRESS`, `COMPLETING` | `SUBMITTED` | 허용, 제출 기록과 사진 유지 |
| `IN_PROGRESS`, `COMPLETING` | 미참가·`CANCELED`·`NOT_SUBMITTED` | 허용 |
| `IN_PROGRESS`, `COMPLETING` | 비정상 `JOINED` | 409, 데이터 보호를 위해 변경하지 않음 |

## 역할별 버튼 노출

- `myRole == LEADER`
  - 일반 `MEMBER` 행에 강퇴 버튼을 표시할 수 있다.
  - 본인 행에는 탈퇴·강퇴 버튼을 표시하지 않는다.
- `myRole == MEMBER`
  - 본인용 크루 탈퇴 버튼만 표시한다.
  - 다른 회원의 강퇴 버튼은 표시하지 않는다.
- 진행 중 세션의 대상 참가 상태를 화면이 알고 있다면 `PARTICIPATING`일 때 탈퇴·강퇴 버튼을 비활성화한다. 최종 권한과 상태 검증은 서버가 수행한다.

## 모집 취소 폴링

참가 전에는 크루 단위 활성 세션을 폴링한다.

```http
GET /api/crews/{crewId}/plogging-sessions/active
```

참가 후에는 저장한 sessionId로 단건 상태를 폴링한다.

```http
GET /api/crew-plogging-sessions/{sessionId}
```

- `CANCELED` 세션은 active 조회에서 제외된다.
- `CANCELED` 세션도 단건 조회에서는 404가 아니라 다음과 같이 반환된다.

```json
{
  "crewPloggingSessionId": 100,
  "status": "CANCELED",
  "startedAt": null,
  "endedAt": null,
  "submissionDeadlineAt": null,
  "joinedByMe": true,
  "participantStatus": "CANCELED",
  "recordSubmittedByMe": false,
  "participantCount": 0,
  "crewRecordCompleted": false
}
```

`RECRUITING → CANCELED` 감지 시 프론트 처리 순서:

1. 단건 폴링을 중지한다.
2. 참가 대기 화면을 종료한다.
3. “크루장이 같이 플로깅 모집을 취소했습니다” 팝업을 표시한다.
4. 로컬 sessionId와 참가 상태를 초기화한다.
5. 크루 상세 화면을 다시 조회한다.

## 주요 오류와 권장 메시지

| HTTP | 상황 | 권장 사용자 메시지 |
|---|---|---|
| 400 | 참여 코드가 숫자 6자리가 아님 | “참여 코드는 숫자 6자리로 입력해 주세요.” |
| 403 | 현재 ACTIVE 크루원이 아님 | “현재 크루원만 이용할 수 있습니다.” |
| 403 | 일반 크루원이 강퇴 요청 | “크루장만 크루원을 내보낼 수 있습니다.” |
| 404 | 크루·대상 크루원·세션이 없음 | “요청한 정보를 찾을 수 없습니다.” |
| 409 | 이미 ACTIVE로 가입함 | “이미 가입한 크루입니다.” |
| 409 | 이미 WITHDRAWN인 대상 재강퇴 | “이미 탈퇴한 크루원입니다.” |
| 409 | 크루장 탈퇴 또는 크루장 강퇴 | “크루장은 탈퇴하거나 강퇴할 수 없습니다.” |
| 409 | 미제출 진행 참가자 탈퇴·강퇴 | “플로깅 기록을 먼저 완료해 주세요.” |
| 409 | 참여 코드 생성 동시 충돌 | “크루 생성에 실패했습니다. 다시 시도해 주세요.” |

## 기존 프론트 수정 사항

- 참여 코드 입력 UI를 영문 포함 8자리에서 숫자 6자리 문자열로 변경한다.
- 크루원 목록 화면은 `GET /api/crews/{crewId}/members`를 연동한다.
- 목록 행 선택 시 공개 프로필 API를 연동한다.
- 크루장/일반 회원 역할에 따라 탈퇴·강퇴 버튼을 구분한다.
- `DELETE` 성공 시 JSON을 파싱하지 말고 `204`를 성공으로 처리한다.
- 모집 참가 후에는 active API가 아니라 sessionId 단건 API로 폴링을 전환한다.
- `recordSubmittedByMe == true`이면 GPS 측정, 개인 완료 요청과 사진 등록을 반복하지 않는다.

기존 개인 플로깅 완료, 개인 기록 조회, 사진 Presigned URL API와 기존 크루·같이 플로깅 API의 URL 및 JSON 필드는 변경되지 않았다.

## QA 체크리스트

- [ ] `000527`의 앞자리 0이 입력·저장·표시 과정에서 유지된다.
- [ ] 잘못된 길이와 영문 포함 코드는 가입할 수 없다.
- [ ] 목록에 크루장이 포함되고 WITHDRAWN 회원은 보이지 않는다.
- [ ] 비회원과 WITHDRAWN 회원은 목록·프로필·크루 기록을 조회할 수 없다.
- [ ] 일반 회원은 강퇴 버튼을 볼 수 없다.
- [ ] 크루장은 자신과 크루장을 강퇴할 수 없다.
- [ ] 탈퇴·강퇴 후 과거 완료 기록과 공유 사진이 그대로 보존된다.
- [ ] 탈퇴·강퇴 후 같은 코드로 재가입할 수 있다.
- [ ] RECRUITING 참가자는 탈퇴·강퇴 시 대기 화면에서 제외된다.
- [ ] PARTICIPATING 참가자의 탈퇴·강퇴가 409로 차단된다.
- [ ] SUBMITTED 참가자는 탈퇴·강퇴할 수 있고 기록이 유지된다.
- [ ] 취소된 모집이 active 조회에서 사라지고 단건 폴링에서는 CANCELED로 확인된다.
- [ ] 취소 감지 후 팝업, 로컬 상태 초기화와 상세 재조회가 순서대로 수행된다.
