# 테스트 컨벤션

## 프레임워크
- JUnit 5 + AssertJ
- Mockito (단위 테스트)
- `@SpringBootTest` (통합 테스트)

## 테스트 구조
- Given-When-Then 패턴을 따른다.
- `@DisplayName`에 한글로 테스트 의도를 명시한다.
- 메서드명은 영문 snake_case로 작성한다.

```java
@DisplayName("존재하지 않는 사용자 조회 시 예외가 발생한다")
@Test
void find_user_by_id_throws_exception_when_not_found() {
    // given
    Long userId = 999L;
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.findById(userId))
        .isInstanceOf(UserNotFoundException.class);
}
```
## 테스트
- 단순 비즈니스 로직 검증은 Mockito 기반 단위 테스트를 우선한다.
- Repository 계층 테스트는 필요 시 `@DataJpaTest`를 우선 고려한다.
- 하나의 테스트는 하나의 행위/결과를 검증하는 데 집중한다.
- 테스트 데이터는 각 테스트 내부에서 명확하게 준비한다.
- 매직 넘버/하드코딩 문자열은 의미 있는 변수명으로 추출한다.
- 테스트 메서드에서 다른 테스트 메서드에 의존하지 않는다.
- 테스트 이름, DisplayName, 검증 내용을 통해 실패 원인을 쉽게 파악할 수 있도록 작성한다.
- 테스트 커버리지는 높게 유지하되, 수치보다 핵심 비즈니스 로직 검증을 우선한다.
- 의미 없는 테스트(예: Lombok 생성자, 단순 getter/setter)는 작성하지 않는다.

## 단위 테스트
- `@ExtendWith(MockitoExtension.class)` 사용
- 의존성은 `@Mock` / `@InjectMocks`로 주입
- Service 레이어를 중점적으로 단위 테스트한다.

### `@Mock` vs `@Spy`
- `@Mock`: 완전한 가짜 객체. 모든 메서드가 기본값(null, 0, false)을 반환하며 실제 로직은 실행되지 않는다.
- `@Spy`: 실제 객체를 감싼다. 명시적으로 stub하지 않은 메서드는 실제 구현이 그대로 실행된다.
- **`@Spy`를 사용하는 경우**: 의존 객체의 실제 동작이 테스트 검증에 필요할 때. 예: `ObjectMapper`를 `@Mock`으로 두면 JSON 직렬화가 null을 반환해 필터 에러 응답 테스트가 의미 없어진다.
- 원칙: 의존성을 완전히 격리하고 싶으면 `@Mock`, 실제 동작이 필요하면 `@Spy`.

## 통합 테스트
- `@SpringBootTest` + `@Transactional` 조합으로 DB 롤백을 보장한다.
- 테스트용 프로퍼티는 `src/test/resources/application.properties`에 분리한다.
- 통합 테스트가 꼭 필요한 경우에만 `@SpringBootTest`를 사용한다.

## 안티패턴 금지
- 프로덕션 코드를 테스트만을 위해 수정하지 않는다.
- `@Autowired`로 구체 구현체를 직접 주입해 결합도를 높이지 않는다.
