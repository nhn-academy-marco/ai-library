package com.nhnacademy.library.ai.agent;

import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 도서 상세 정보 Agent
 * <p>
 * 도서 상세 정보 조회 전담 Agent입니다.
 * 도서의 상세 정보, 키워드, 이용 분석, 소장 여부 등을 조회합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookDetailAgent implements Agent {

    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    @Override
    public AgentType getType() {
        return AgentType.BOOK_DETAIL;
    }

    @Override
    public String getName() {
        return "BookDetailAgent";
    }

    @Override
    public String getDescription() {
        return "도서 상세 정보 조회 전담 Agent. 도서의 상세 정보, 키워드, 이용 분석 정보를 제공합니다.";
    }

    @Override
    public String execute(AgentRequest request) {
        log.info("[{}] 도서 상세 정보 조회 실행: query={}", getName(), request.query());

        try {
            // 1. ISBN 추출
            String isbn = extractIsbn(request.query());

            if (isbn == null || isbn.length() < 10) {
                log.warn("[{}] ISBN 추출 실패: query={}", getName(), request.query());
                return "ISBN을 찾을 수 없습니다.";
            }

            // 2. 도서 상세 정보 조회
            BookDetailInfo bookDetail = libraryInfoNaruApiClient.getBookDetail(isbn, true);

            if (bookDetail == null) {
                log.info("[{}] 도서 상세 정보 없음: isbn={}", getName(), isbn);
                return String.format("ISBN %s 도서의 상세 정보를 찾을 수 없습니다.", isbn);
            }

            // 3. 결과 포맷팅
            StringBuilder response = new StringBuilder();
            response.append(String.format("📖 도서 상세 정보 (ISBN: %s)\n\n", isbn));

            response.append(String.format("제목: %s\n", bookDetail.bookname()));
            response.append(String.format("저자: %s\n", bookDetail.authors()));
            response.append(String.format("출판사: %s\n", bookDetail.publisher()));
            response.append(String.format("출판일: %s\n", bookDetail.publicationDate()));

            if (bookDetail.description() != null && !bookDetail.description().isBlank()) {
                String desc = bookDetail.description();
                if (desc.length() > 200) {
                    desc = desc.substring(0, 200) + "...";
                }
                response.append(String.format("설명: %s\n", desc));
            }

            if (bookDetail.imageUrl() != null && !bookDetail.imageUrl().isBlank()) {
                response.append(String.format("이미지: %s\n", bookDetail.imageUrl()));
            }

            // 대출 정보
            if (bookDetail.loanCount() > 0) {
                response.append(String.format("\n📊 대출 통계\n"));
                response.append(String.format("총 대출 횟수: %d회\n", bookDetail.loanCount()));

                if (bookDetail.loanInfo() != null) {
                    var loanInfo = bookDetail.loanInfo();
                    response.append(String.format("남성: %d회, 여성: %d회\n",
                            loanInfo.maleLoanCount(), loanInfo.femaleLoanCount()));
                    response.append(String.format("10대: %d회, 20대: %d회, 30대: %d회\n",
                            loanInfo.teensLoanCount(), loanInfo.twentiesLoanCount(), loanInfo.thirtiesLoanCount()));
                }
            }

            log.info("[{}] 도서 상세 정보 조회 완료: isbn={}", getName(), isbn);
            return response.toString();

        } catch (Exception e) {
            log.error("[{}] 도서 상세 정보 조회 실패: query={}", getName(), request.query(), e);
            return "도서 상세 정보 조회 중 오류가 발생했습니다.";
        }
    }

    /**
     * 쿼리에서 ISBN 추출
     */
    private String extractIsbn(String query) {
        // ISBN 패턴 (10자리 또는 13자리 숫자)
        String isbnPattern = "\\b\\d{10,13}\\b";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(isbnPattern);
        java.util.regex.Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            return matcher.group();
        }
        return query.replaceAll("[^0-9X]", ""); // 숫자와 X만 추출 (ISBN10)
    }
}
