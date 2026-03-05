package com.nhnacademy.library.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Library Assistant Agent 메인 서비스
 * <p>
 * AI Agent와 사용자 간의 상호작용을 담당합니다.
 * Function Calling을 통해 AI가 도서 검색, 리뷰 조회, 대출 가능 확인을 수행합니다.
 * </p>
 * <p>
 * <b>리팩토링:</b> ChatClient와 FunctionToolCallback을 Bean으로 등록하여 재사용합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiLibraryAssistantService {

    @Qualifier("libraryAssistantChatClient")
    private final ChatClient chatClient;

    /**
     * 사용자 요청을 처리하고 AI 응답을 반환합니다.
     * <p>
     * AI가 필요에 따라 다음 Function들을 호출할 수 있습니다:
     * - BookSearch: 도서 검색
     * - GetReviews: 리뷰 조회
     * - CheckLoanAvailability: 대출 가능 확인
     * </p>
     *
     * @param userMessage 사용자 메시지
     * @return AI 응답
     */
    public String ask(String userMessage) {
        log.info("[AI Service] 사용자 요청: {}", userMessage);

        // System Prompt
        String systemPrompt = """
            당신은 도서관 도우미 AI입니다.

            **중요: 반드시 한국어로만 답변하세요. 중국어, 일본어, 영어 등 다른 언어를 사용하지 마세요.**

            사용자의 요청을 처리할 때:
            1. 필요한 도구(함수)를 호출하여 정보를 수집하세요
            2. 도구 호출 결과를 받으면 반드시 자연스러운 한국어로 답변하세요
            3. 검색 결과, 리뷰, 대출 가능 정보를 종합해서 친절하게 설명하세요

            **지역 기반 검색 예시:**
            사용자: "강남구 근처 도서관에서 빌릴 수 있는 자바 책 알려줘"
            1. SearchLibraries("강남구") → 강남구 도서관 목록 확보
            2. BookSearch("자바") → 자바 도서 목록 확보 (ISBN)
            3. CheckLoanAvailability(isbn, "강남구") → 강남구 도서관 대출 가능 확인
            4. 종합 답변: "강남구 도서관에서 빌릴 수 있는 자바 책 5권을 찾았습니다..."

            예시 응답:
            "토비의 스프링 책을 찾았습니다! 📚

            **도서 정보**
            - 제목: 토비의 스프링
            - 저자: 크레이그 워즈
            - 출판사: 책만
            - 평점: 4.5/5.0

            이 책은 스프링 프레임워크의 핵심 개념을 명확하게 설명하는 입문서로,
            초보자도 쉽게 이해할 수 있습니다."
            """;

        String response = chatClient.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call()
            .content();

        log.info("[AI Service] AI 응답 생성 완료");
        return response;
    }
}
