package net.java21.library.ai.function.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * AI Function 호출용 리뷰 조회 요청 DTO
 *
 * <p>Tool 입력 스키마는 JSON object여야 합니다. {@code Long}을 그대로 입력 타입으로 쓰면
 * Spring AI 1.1.2의 JsonSchemaGenerator가 {@code "type": "integer"} 스키마를 만들어
 * object 파라미터를 요구하는 LLM(Gemini 등)에서 도구 등록·호출이 실패할 수 있어 record로 감쌉니다.</p>
 *
 * @param bookId 도서 ID
 */
public record ReviewRequest(
    @JsonPropertyDescription("리뷰를 조회할 도서 ID")
    Long bookId
) {
}
