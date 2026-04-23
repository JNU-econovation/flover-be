# 개발 워크플로우

## 기능 개발 순서
1. Domain Entity 정의 (`domain/`)
2. Repository 인터페이스 작성 (`repository/`)
3. Service 레이어 구현 (`service/`)
4. DTO 작성 (`dto/` — Record 사용)
5. Controller 구현 (`controller/`)
6. 테스트 코드 작성 (단위 테스트 → 통합 테스트)

## 브랜치 전략
- `feature/{도메인}/{기능명}` 형식으로 브랜치를 생성한다.
- 예: `feature/user/signup`, `feature/order/create`

## 커밋 메시지
- Conventional Commits를 따른다.
- 접두사: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`
- 형식: `[type]: 한글 설명`
- 예: `[feat]: 회원 가입 API 구현`, `[fix]: 주문 조회 NPE 수정`

## PR 규칙
- PR 제목은 커밋 메시지 형식을 따른다.
- 관련 이슈를 반드시 연결한다 (`close #이슈번호`).
- 로컬 동작 확인 및 테스트 통과 후 PR을 올린다.
