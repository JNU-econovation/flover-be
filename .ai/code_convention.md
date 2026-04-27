# 코드 컨벤션

## 패키지 구조
- 도메인 기반 패키지 구조를 사용한다.
- 루트 패키지: `com.flover.flover_be`
- 각 도메인 패키지 하위에 `controller`, `service`, `repository`, `domain`, `dto` 패키지를 둔다.

```
com.flover.flover_be
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/    ← Entity 클래스
│   ├── exception/ ← 도메인 ErrorCode enum, 도메인 Exception 클래스
│   └── dto/
├── order/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/
│   ├── exception/
│   └── dto/
└── global/
    ├── exception/ ← ErrorCode(interface), CommonErrorCode, BusinessException, GlobalExceptionHandler
    └── response/
```

## DTO
- Request/Response DTO는 Java Record로 작성한다.
- DTO에 Jakarta Validation 어노테이션을 직접 선언한다.
- 요청과 응답 DTO를 분리한다.
- Inner class 패턴으로 관련 DTO를 그룹핑한다.

```java
public class UserDto {
    public record Request(@NotBlank String name, @Email String email) {}
    public record Response(Long id, String name, String email) {}
}
```

## Entity
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용한다.
- 생성자는 정적 팩토리 메서드(`of`, `create`)로 대체한다.
- 엔티티를 API 응답으로 직접 노출하지 않는다.

## 빌더 패턴
- 필드가 많거나 생성자가 복잡한 경우 빌더 패턴 사용을 고려한다.
- 가독성과 유지보수를 위해 의미 있는 필드만 선택적으로 설정할 수 있도록 한다.
- DTO 또는 테스트 객체 생성 시 적극적으로 활용한다.
- 엔티티의 생성 규칙을 우회하는 무분별한 빌더 사용은 지양한다.

## 예외 처리
- `ErrorCode`는 `global/exception/`에 **인터페이스**로 선언한다. (`getStatus()`, `getMessage()` 정의)
- 에러 코드는 도메인별로 분리하여 각 도메인의 `exception/` 패키지에 enum으로 관리한다.
  - 도메인 공통 에러: `global/exception/CommonErrorCode`
  - 도메인별 에러: `{domain}/exception/{Domain}ErrorCode` (예: `user/exception/AuthErrorCode`)
- 도메인별 커스텀 예외는 `BusinessException`을 상속하며 `ErrorCode`를 인자로 받는다.
- 커스텀 예외 클래스는 해당 도메인의 `exception/` 하위에 위치한다. (`global/exception/`에 두지 않는다.)
- `GlobalExceptionHandler`(`@RestControllerAdvice`)에서 모든 예외를 공통 처리한다.
- 에러 응답은 `ErrorResponse` DTO로 통일한다.

```java
// global: ErrorCode 인터페이스
public interface ErrorCode {
    HttpStatus getStatus();
    String getMessage();
}

// global: 서버 공통 에러
public enum CommonErrorCode implements ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
}

// user: 도메인 에러코드
public enum AuthErrorCode implements ErrorCode {
    KAKAO_TOKEN_FAILED(HttpStatus.UNAUTHORIZED, "카카오 토큰 발급에 실패했습니다.");
}

// user: 도메인 예외는 ErrorCode를 받아 부모에게 위임
public class KakaoAuthException extends BusinessException {
    public KakaoAuthException(ErrorCode errorCode) { super(errorCode); }
}
```

## 네이밍
- 서비스 메서드: 동사 + 목적어 (예: `createUser`, `findUserById`)
- Boolean 반환 메서드: `is` / `has` / `can` 접두사 사용
- 컬렉션 반환: 복수형 사용 (예: `findActiveUsers`)

## 환경변수 관리
- DB 비밀번호, JWT secret, OAuth 키 등 민감한 설정값은 `.env` 파일에 관리한다.
- `application.properties`에서 `${VAR_NAME}` 형식으로 참조한다.
- `.env`는 `.gitignore`에 추가한다.

## API 응답 형식
- 성공 응답은 Controller에서 데이터를 직접 반환한다. (`ResponseEntity<T>` 또는 `T`)
- 공통 래퍼(`ApiResponse<T>`)를 사용하지 않는다.
- 에러 응답만 `ErrorResponse` DTO로 통일한다.

## 안티패턴 금지
- Controller에서 Repository를 직접 호출하지 않는다.
- `@Autowired` 필드 주입을 사용하지 않는다.
- `Optional`을 필드나 메서드 파라미터 타입으로 사용하지 않는다.
- 엔티티를 Controller 반환값으로 직접 사용하지 않는다.
- 엔티티에 public setter를 사용하지 않는다.
- 상태 변경은 의미 있는 도메인 메서드를 통해 수행한다. (예: changePassword)