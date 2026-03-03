# Spring AI Function Calling 스타일로 리팩토링 계획

## 현재 문제점

AiOrchestrationService가 수동 Agent 선택 방식을 사용하고 있어서:
1. 복잡한 if-else 로직 (selectAgents, executeAgentsWithA2A)
2. LLM의 자율성 부족 (스스로 도구 선택 불가)
3. 새로운 Agent 추가 시 코드 수정 필요

## 개선 방향: Spring AI Function Calling

### 1. 기존 Agent → Function 변환

```java
// 기존: BookAgent 인터페이스
public interface Agent {
    String execute(AgentRequest request);
}

// 변경: Function 스타일
@Bean
@Description("도서관 시스템에서 도서를 검색합니다")
public Function<BookSearchRequest, BookSearchResult> searchBooks() {
    return request -> {
        // 검색 로직
        return result;
    };
}
```

### 2. AiOrchestrationService 간소화

```java
@Service
public class AiOrchestrationService {

    private final ChatClient chatClient;

    public String ask(String userMessage) {
        // LLM이 스스로 도구 선택 및 실행
        return chatClient.prompt()
            .user(userMessage)
            .tools(
                "searchBooks",           // 도서 검색
                "searchLibraries",       // 도서관 검색
                "checkBookExists"        // 소장 여부 확인
            )
            .call()
            .content();
    }
}
```

### 3. bookExist API를 Function으로 변환

```java
@Bean
@Description("도서관의 도서 소장 여부를 확인합니다")
public Function<BookExistRequest, BookExistResult> checkBookExists() {
    return request -> {
        BookExistInfo info = apiClient.checkBookExists(
            request.libCode(),
            request.isbn13()
        );
        return BookExistResult.from(info);
    };
}
```

## 장점

1. **LLM 자율성**: LLM이 스스로 필요한 도구 선택
2. **코드 간소화**: 복잡한 Agent 선택 로직 제거
3. **확장성**: 새로운 Function 추가만 하면 자동 인식
4. **표준화**: Spring AI 1.1.3 표준 패턴 따름

## 구현 순서

1. ✅ LibraryFunctions 이미 구현됨
2. ⏳ AiOrchestrationService를 ChatClient.Function Calling 스타일로 변경
3. ⏳ 기존 Agent 제거 (BookAgent, LibraryAgent 등)
4. ⏳ Telegram Bot 테스트

---

**참고**: AiLibraryAssistantService가 이미 Function Calling 스타일로 구현되어 있으므로,
AiOrchestrationService도 동일한 패턴으로 통일하는 것이 좋습니다.
