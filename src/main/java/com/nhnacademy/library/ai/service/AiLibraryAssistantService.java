package com.nhnacademy.library.ai.service;

import com.nhnacademy.library.ai.function.BookSearchFunction;
import com.nhnacademy.library.ai.function.LoanCheckFunction;
import com.nhnacademy.library.ai.function.ReviewFunction;
import com.nhnacademy.library.ai.function.dto.BookSearchRequest;
import com.nhnacademy.library.ai.function.dto.BookSearchResult;
import com.nhnacademy.library.ai.function.dto.LoanCheckRequest;
import com.nhnacademy.library.ai.function.dto.LoanCheckResult;
import com.nhnacademy.library.ai.function.dto.ReviewResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

/**
 * Library Assistant Agent 메인 서비스
 * <p>
 * AI Agent와 사용자 간의 상호작용을 담당합니다.
 * Function Calling을 통해 AI가 도서 검색, 리뷰 조회, 대출 가능 확인을 수행합니다.
 * </p>
 * <p>
 * 순환 의존성을 방지하기 위해 FunctionToolCallback을 Spring Bean으로 등록하지 않고,
 * 서비스 내부에서 프로그래밍 방식으로 생성합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiLibraryAssistantService {

    private final ChatModel chatModel;
    private final BookSearchFunction bookSearchFunction;
    private final ReviewFunction reviewFunction;
    private final LoanCheckFunction loanCheckFunction;

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

        // FunctionToolCallback 프로그래밍 방식 생성 (순환 의존성 방지)
        FunctionToolCallback<BookSearchRequest, BookSearchResult> bookSearchTool =
            FunctionToolCallback.builder("BookSearch", bookSearchFunction::searchBooks)
                .description("도서관 시스템에서 도서를 검색합니다. " +
                            "제목, 저자명으로 검색할 수 있으며, 검색어를 받아 도서 목록을 반환합니다.")
                .inputType(BookSearchRequest.class)
                .build();

        FunctionToolCallback<Long, ReviewResult> reviewTool =
            FunctionToolCallback.builder("GetReviews", reviewFunction::getReviews)
                .description("도서의 리뷰 목록을 조회합니다. " +
                            "도서 ID를 받아 리뷰 요약, 평점, 주요 리뷰를 제공합니다.")
                .inputType(Long.class)
                .build();

        FunctionToolCallback<LoanCheckRequest, LoanCheckResult> loanCheckTool =
            FunctionToolCallback.builder("CheckLoanAvailability", loanCheckFunction::checkLoanAvailability)
                .description("도서의 대출 가능 여부를 조회합니다. " +
                            "ISBN을 받아 전국 도서관의 대출 가능 현황을 제공합니다. " +
                            "지역, 위도, 경도를 제공하면 해당 지역의 가까운 도서관을 우선으로 보여줍니다.")
                .inputType(LoanCheckRequest.class)
                .build();

        // ChatClient 생성 (Function Tool 등록)
        ChatClient chatClient = ChatClient.builder(chatModel)
            .defaultTools(bookSearchTool, reviewTool, loanCheckTool)
            .build();

        String response = chatClient.prompt()
            .user(userMessage)
            .call()
            .content();

        log.info("[AI Service] AI 응답 생성 완료");
        return response;
    }
}
