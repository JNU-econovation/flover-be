# k6 부하 테스트 가이드

이 구성은 로컬 PC에서 k6를 실행하고, EC2에 배포된 Spring Boot API를 대상으로 요청을 보내는 방식입니다. t2.micro급 인스턴스와 실제 사용자 10명 미만이라는 조건을 고려해 기본 테스트는 읽기 중심, 낮은 VU에서 점진 증가, 쓰기/AI API는 명시적으로 분리했습니다.

## 디렉터리 구조

```text
k6/
  data/
    locations.json
  results/
    .gitignore
  scenarios/
    smoke-test.js
    load-test.js
    stress-test.js
    ai-analysis-smoke-test.js
    common.js
    summary.js
  README.md
```

## API 선별 및 우선순위

| 우선순위 | API | 이유 | 기본 실행 |
| --- | --- | --- | --- |
| P0 | `GET /api/facilities/trash-bins` | 위치 기반 시설 조회, MySQL 거리 계산 쿼리 | 포함 |
| P0 | `GET /api/facilities/toilets` | 위치 기반 시설 조회, MySQL 거리 계산 쿼리 | 포함 |
| P0 | `GET /api/v1/routes` | 경로 추천, 외부 route engine 호출 및 재시도 | 포함, 낮은 비중 |
| P0 | `GET /api/users/me` | 앱 진입 후 자주 호출되는 인증 사용자 조회 | `ACCESS_TOKEN` 있을 때 포함 |
| P0 | `GET /api/users/me/plogging-stats` | 마이페이지 핵심 통계 집계 | `ACCESS_TOKEN` 있을 때 포함 |
| P0 | `GET /api/plogging-sessions` | 활동 기록 목록 조회 | `ACCESS_TOKEN` 있을 때 포함 |
| P0 | `GET /api/plogging-sessions/monthly` | 월간 통계 집계 | `ACCESS_TOKEN` 있을 때 포함 |
| P0 | `GET /api/plogging-sessions/weekly` | 주간 통계 집계 | `ACCESS_TOKEN` 있을 때 포함 |
| P0 | `POST /api/plogging-sessions/complete` | 세션/경로/사진 insert, 사용자 경험치 update | `INCLUDE_WRITE=true`일 때만 포함 |
| P1 | `GET /api/plogging-sessions/{id}` | 상세 조회, 사진 목록 추가 조회 | `PLOGGING_SESSION_ID` 있을 때 포함 |
| P1 | `GET /api/users/me/profile-image/upload-url` | S3 presigned URL 발급 | smoke에만 포함 |
| P1 | `GET /api/plogging-sessions/map-image/upload-url` | S3 presigned URL 발급 | smoke에만 포함 |
| P1 | `GET /api/plogging-sessions/photo/upload-url` | S3 presigned URL 발급 | smoke에만 포함 |
| P1 | `POST /api/plogging/analyze` | multipart 이미지 처리, 외부 AI API, MySQL/PostgreSQL 저장 가능 | 별도 `ai-analysis-smoke-test.js` |
| P2 | `POST /api/auth/kakao/login`, `POST /api/auth/v2/kakao/login`, `POST /api/auth/apple/login` | OAuth 외부 인증 의존, 테스트 토큰 확보가 더 안정적 | 제외 |
| P2 | `POST /api/auth/logout` | 서버 상태 변경 없음, 성능 병목 가능성 낮음 | 제외 |
| P2 | `PUT /api/users/me/nickname` | 운영 사용자 닉네임 오염 위험 | 제외 |
| P2 | `PUT /api/users/me/profile-image` | 운영 프로필 변경 및 S3 삭제 가능성 | 제외 |
| P2 | `DELETE /api/users/me` | 계정 및 연관 데이터 삭제 | 제외 |

인증은 `Authorization: Bearer <ACCESS_TOKEN>` 방식입니다. 현재 코드에는 email/password 로그인이 없고 Kakao/Apple OAuth만 있으므로, k6 스크립트는 자동 로그인 대신 `ACCESS_TOKEN` 환경변수를 사용합니다.

## k6 설치

Windows PowerShell:

```powershell
winget install k6 --source winget
k6 version
```

Chocolatey 사용 시:

```powershell
choco install k6
k6 version
```

macOS:

```bash
brew install k6
k6 version
```

## 환경변수

PowerShell에서 저장소 루트 기준으로 설정합니다.

```powershell
$env:BASE_URL = "https://api.example.com"
$env:ACCESS_TOKEN = "발급받은_JWT"
$env:TEST_DATA_PREFIX = "k6-test-20260607"
$env:STATS_YEAR = "2026"
$env:STATS_MONTH = "6"
$env:WEEK_START_DATE = "2026-06-01"
$env:PLOGGING_SESSION_ID = "1"
```

선택 변수:

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `BASE_URL` | 테스트 대상 서버 URL. 필수 | 없음 |
| `ACCESS_TOKEN` | 인증 API 호출용 JWT | 없음 |
| `TEST_USER_EMAIL` | 현재 스크립트에서는 사용하지 않음. 테스트 계정 추적용 메모 변수 | 없음 |
| `TEST_USER_PASSWORD` | 현재 스크립트에서는 사용하지 않음. email/password 로그인 도입 시 사용 가능 | 없음 |
| `TEST_DATA_PREFIX` | 쓰기 테스트 데이터 식별 prefix | `k6-test` |
| `INCLUDE_WRITE` | `true`일 때만 `POST /complete` 호출 | `false` |
| `PLOGGING_SESSION_ID` | 상세 조회 대상 세션 ID | 없음 |
| `ROUTE_TIME_MINUTES` | 경로 추천 요청 시간 | `30` |
| `ROUTE_MODE` | route engine 전달 mode | `PLOGGING` |
| `IMAGE_CONTENT_TYPE` | presigned URL 발급 smoke 테스트 content type | `image/png` |

## 실행 명령

smoke test: 1 VU로 API 정상 호출 여부를 확인합니다.

```powershell
k6 run k6/scenarios/smoke-test.js
```

load test: t2.micro 기준 보수적으로 1 -> 5 -> 10 -> 20 -> 30 VU까지 올립니다.

```powershell
k6 run k6/scenarios/load-test.js
```

stress test: 임계점을 찾기 위해 10 -> 20 -> 30 -> 50 VU까지 올립니다. 기본 구성에서는 100 VU를 사용하지 않습니다.

```powershell
k6 run k6/scenarios/stress-test.js
```

쓰기 API를 포함하려면 반드시 테스트 계정과 테스트 데이터 prefix를 확인한 뒤 실행합니다.

```powershell
$env:INCLUDE_WRITE = "true"
$env:TEST_DATA_PREFIX = "k6-test-20260607"
k6 run k6/scenarios/smoke-test.js
Remove-Item Env:INCLUDE_WRITE
```

AI 분석 API는 일반 load/stress에서 분리되어 있습니다. `k6/data/sample-trash-image.jpg` 같은 테스트 이미지를 직접 둔 뒤 실행합니다.

```powershell
$env:RUN_HEAVY_API = "true"
k6 run k6/scenarios/ai-analysis-smoke-test.js
Remove-Item Env:RUN_HEAVY_API
```

AI 분석 테스트 이미지는 반드시 `k6/data/sample-trash-image.jpg` 경로에 JPEG 파일로 둡니다. k6의 `open()`은 init context에서 실행되므로 동적 환경변수 경로 대신 스크립트 안의 정적 문자열 리터럴 경로를 사용합니다.

## 결과 저장

각 스크립트는 종료 시 자동으로 결과를 저장합니다.

```text
k6/results/smoke-summary-YYYYMMDD-HHmmss.json
k6/results/smoke-summary-YYYYMMDD-HHmmss.html
k6/results/load-summary-YYYYMMDD-HHmmss.json
k6/results/load-summary-YYYYMMDD-HHmmss.html
k6/results/stress-summary-YYYYMMDD-HHmmss.json
k6/results/stress-summary-YYYYMMDD-HHmmss.html
```

k6 기본 JSON export도 같이 남기고 싶다면 다음처럼 실행할 수 있습니다.

```powershell
k6 run --summary-export k6/results/load-summary-export.json k6/scenarios/load-test.js
```

## 지표 해석

`http_req_failed`는 HTTP 요청 실패율입니다. 기본 load 테스트에서는 1% 미만을 목표로 잡았습니다. 0.01은 1%를 의미합니다.

`http_req_duration`은 요청 전체 응답 시간입니다. p95는 전체 요청 중 95%가 이 시간 이내에 끝났다는 뜻이고, p99는 상위 1%의 느린 요청까지 보는 지표입니다.

`http_reqs`의 `rate`가 TPS 또는 RPS에 해당합니다. 예를 들어 `http_reqs rate = 18.5/s`라면 초당 약 18.5개의 HTTP 요청을 처리한 것입니다.

VU 단계별로 `http_req_failed`가 증가하거나 p95/p99가 급격히 증가하는 구간이 한계 후보입니다. 예: “20 VU까지 실패율 0%, p95 420ms였으나 30 VU부터 p95가 1.8s로 상승해 t2.micro 환경의 안정 처리 구간은 20~30 VU 사이로 추정됩니다.”

route API만 느리다면 `api=route_recommendation` 태그의 응답 시간을 따로 봅니다. 이 경우 Spring Boot 자체보다 route engine 또는 네트워크 호출이 병목일 가능성이 큽니다.

쓰기 API를 포함한 경우 `api=plogging_complete_write` 지표를 읽습니다. 실패율이 낮아도 DB에 테스트 세션이 남기 때문에 운영 데이터와 분리된 테스트 계정으로만 실행해야 합니다.

## 테스트 주의사항

삭제 API(`DELETE /api/users/me`)는 기본 테스트에서 제외했습니다.

프로필 이미지 변경 API(`PUT /api/users/me/profile-image`)는 기존 S3 객체 삭제가 발생할 수 있어 제외했습니다.

닉네임 변경 API(`PUT /api/users/me/nickname`)는 운영 사용자 프로필 오염 위험이 있어 제외했습니다.

AI 분석 API는 외부 AI 서버 호출과 DB 저장이 발생하므로 일반 load/stress에 넣지 않았습니다.

쓰기 테스트는 `INCLUDE_WRITE=true`가 없으면 실행되지 않습니다. 실행 시 `TEST_DATA_PREFIX`를 설정하고 테스트 계정으로만 수행합니다.

EC2 t2.micro는 CPU credit 영향을 크게 받습니다. 같은 VU에서도 테스트 시간대와 CPU credit 잔량에 따라 결과가 달라질 수 있으므로 smoke -> load -> stress 순서로 실행하고, 운영 피크 시간에는 stress를 피합니다.

## 발표용 요약 문장

“전체 API를 무작정 호출하지 않고, 위치 기반 조회, 사용자/플로깅 통계, 경로 추천, 세션 완료 쓰기처럼 실제 사용자 흐름과 병목 가능성이 높은 API를 선별했다. t2.micro 환경을 고려해 기본 부하는 30 VU, stress는 50 VU까지 보수적으로 증가시켰고, 삭제/프로필 변경/AI 분석은 운영 데이터 오염과 외부 의존성 위험 때문에 기본 부하 테스트에서 분리했다.”
