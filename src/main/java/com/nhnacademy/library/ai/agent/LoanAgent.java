package com.nhnacademy.library.ai.agent;

import com.nhnacademy.library.ai.function.dto.LoanCheckRequest;
import com.nhnacademy.library.ai.function.dto.LoanCheckResult;
import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.LoanItemInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 대출 가능 확인 Agent
 * <p>
 * 대출 가능 확인 전담 Agent입니다.
 * ISBN을 받아 전국 도서관의 대출 가능 현황을 조회합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoanAgent implements Agent {

    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    @Override
    public AgentType getType() {
        return AgentType.LOAN;
    }

    @Override
    public String getName() {
        return "LoanAgent";
    }

    @Override
    public String getDescription() {
        return "대출 가능 확인 전담 Agent. ISBN으로 도서관 대출 가능 여부를 조회합니다.";
    }

    @Override
    public String execute(AgentRequest request) {
        log.info("[{}] 대출 가능 확인 실행: query={}", getName(), request.query());

        try {
            // 1. ISBN 추출
            String isbn = extractIsbn(request.query());

            if (isbn == null || isbn.length() < 10) {
                log.warn("[{}] ISBN 추출 실패: query={}", getName(), request.query());
                return "ISBN을 찾을 수 없습니다.";
            }

            // 2. 지역 파라미터 추출 (선택 사항)
            String region = extractRegion(request);
            log.info("[{}] 지역 코드 파라미터: region={}, params={}", getName(), region, request.params());

            // 3. 도서관정보나루 API 호출
            List<LoanItemInfo> loanItems;
            if (region != null) {
                log.info("[{}] 지역 코드로 대출 가능 조회: isbn={}, region={}", getName(), isbn, region);
                loanItems = libraryInfoNaruApiClient.searchLoanItemsByIsbn(isbn, region);
            } else {
                log.info("[{}] 전체 대출 가능 조회: isbn={}", getName(), isbn);
                loanItems = libraryInfoNaruApiClient.searchLoanItemsByIsbn(isbn);
            }

            if (loanItems.isEmpty()) {
                log.info("[{}] 대출 가능 도서관 없음: isbn={}", getName(), isbn);
                return String.format("ISBN %s 도서는 대출 가능한 도서관이 없습니다.", isbn);
            }

            // 4. 결과 포맷팅 (상위 5개)
            StringBuilder response = new StringBuilder();
            response.append(String.format("ISBN %s 대출 가능 도서관 (%d개):\n\n", isbn, loanItems.size()));

            int displayCount = Math.min(5, loanItems.size());
            for (int i = 0; i < displayCount; i++) {
                LoanItemInfo item = loanItems.get(i);
                boolean loanAvailable = "Y".equalsIgnoreCase(item.loanAvailable());
                String status = loanAvailable ? "✅ 대출 가능" : "❌ 대출 불가";

                response.append(String.format("%d. %s\n", i + 1, item.libraryName()));
                if (item.location() != null) {
                    response.append(String.format("   주소: %s\n", item.location()));
                }
                response.append(String.format("   상태: %s\n\n", status));
            }

            log.info("[{}] 대출 가능 확인 완료: isbn={}, count={}", getName(), isbn, loanItems.size());
            return response.toString();

        } catch (Exception e) {
            log.error("[{}] 대출 가능 확인 실패: query={}", getName(), request.query(), e);
            return "대출 가능 확인 중 오류가 발생했습니다.";
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

    /**
     * AgentRequest에서 지역 추출
     */
    private String extractRegion(AgentRequest request) {
        Object params = request.params();
        if (params instanceof String) {
            return (String) params;
        }
        return null;
    }
}
