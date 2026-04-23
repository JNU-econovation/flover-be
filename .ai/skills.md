# 기술 스택

## Core
- Java 21
- Spring Boot 4.0.5
- Spring Data JPA
- Spring Web MVC
- MySQL
- Lombok
- Gradle (Groovy DSL)

## 테스트
- JUnit 5
- AssertJ
- Mockito
- Spring Boot Test

## 주의 사항
- `javax` 패키지 대신 `jakarta` 패키지를 사용한다.
- `@Autowired` 필드 주입을 사용하지 않는다. 생성자 주입만 사용한다. (Lombok `@RequiredArgsConstructor` 활용)
- 프로젝트에 없는 외부 라이브러리(QueryDSL, MapStruct, Security 등)는 임의로 추가하지 않는다.
