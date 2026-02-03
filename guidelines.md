# Guidelines


## 용어 사용 Guidelines

* 가능한 경우 **공식 용어**를 사용합니다.
* 영어 표현이 더 적절하거나 일반적으로 사용되는 경우에는 영어 원문을 그대로 사용합니다.
* 선택한 용어는 모든 문서와 코드 주석에서 **일관되게 사용**합니다.

## 코드 작성 공통 Guidelines

* **상수화**
    * 코드 구현 시 **매직 넘버(Magic Number)** 등 의미 있는 값은 반드시 **상수(Constant)**로 정의하여 사용합니다.
    * 상수 이름은 **대문자와 언더스코어(`UPPER_SNAKE_CASE`)** 표기법을 사용합니다.
    * 상수는 가능한 경우 **관련 클래스/인터페이스 내부**나 **별도 상수 클래스(Constant Class)**에 정의하여 재사용성을 높입니다.

* **주석**
    * 필요 시 **Javadoc** 작성
    * 로직이 복잡하면 코드 내부 주석 추가
    * 의미가 명확하지 않은 값은 주석(Comment)으로 설명

## Backend 작성 Guidelines

* **Spring Boot 사용**
    * 모듈별 구조를 명확히 하고 패키지 규칙 준수
    * **Controller → Service → Repository** 계층 분리

* **REST API 설계**
    * HTTP Status Code를 표준에 맞게 반환
    * DTO를 사용해 요청(Request)/응답(Response) 정의
        * 요청 객체 예시: `BookRequest`
        * 응답 객체 예시: `BookResponse`
* Dto Validation 처리 
  * 대상 : 모든 Request dto

* **예외 처리**
    * Global Exception Handler 사용
    * 예외들은 `RuntimeException`을 상속한 **Custom Exception** 작성
    * Exception 클래스는 해당 패키지 하위에 `exception` 패키지를 만들어 관리
        * 예시: `com.nhnacademy.library.book.exception`
    * 의미 있는 메시지와 상태 코드 반환

* **로깅**
    * `@Slf4j` annotation 활용
    * 로깅 정책은 `src/main/resources/logback-spring.xml` 파일에서 관리
    * 로그 목적에 따라 레벨 사용
        * 디버깅용: `log.debug()`
        * 정보 전달용: `log.info()`
        * 에러: `log.error()`


## Frontend 작성 Guidelines

* **UI Framework**
    * [Tabler](https://tabler.io/admin-template), Bootstrap 5 기반을 사용합니다.

* **Layout 구조**
    * 모든 페이지는 **Thymeleaf** 기반으로 작성합니다.

* **Layout 활용**
    * `pom.xml`에 포함된 `thymeleaf-layout-dialect` 라이브러리를 활용합니다.
    * 이미 정의된 레이아웃을 사용하여, 사용자가 구현하는 페이지는 레이아웃 구조를 그대로 사용하고 **페이지 내용(content)**만 HTML로 작성할 수 있도록 합니다.
