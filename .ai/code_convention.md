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
│   ├── domain/ ← Entity 클래스
│   └── dto/
├── order/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/ 
│   └── dto/
└── global/
    ├── exception/
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
- 도메인별 커스텀 예외는 프로젝트 공통 예외 클래스인 `BusinessException`을 상속한다.
- 예외 클래스는 HTTP 상태 코드와 메시지를 포함하도록 설계한다.
- 예외 메시지는 사용자 친화적이고 구체적으로 작성한다.
- `GlobalExceptionHandler`(`@RestControllerAdvice`)에서 모든 예외를 공통 처리한다.
- 클라이언트 응답은 `ErrorResponse` DTO로 통일한다.

## 네이밍
- 서비스 메서드: 동사 + 목적어 (예: `createUser`, `findUserById`)
- Boolean 반환 메서드: `is` / `has` / `can` 접두사 사용
- 컬렉션 반환: 복수형 사용 (예: `findActiveUsers`)

## 안티패턴 금지
- Controller에서 Repository를 직접 호출하지 않는다.
- `@Autowired` 필드 주입을 사용하지 않는다.
- `Optional`을 필드나 메서드 파라미터 타입으로 사용하지 않는다.
- 엔티티를 Controller 반환값으로 직접 사용하지 않는다.
- 엔티티에 public setter를 사용하지 않는다.
- 상태 변경은 의미 있는 도메인 메서드를 통해 수행한다. (예: changePassword)