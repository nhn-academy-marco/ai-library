package net.java21.library.ai.function;

import net.java21.library.ai.function.dto.LoanCheckRequest;
import net.java21.library.ai.function.dto.LoanCheckResult;
import net.java21.library.external.opennaru.client.LibraryInfoNaruApiClient;
import net.java21.library.external.opennaru.dto.LoanItemInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 대출 가능 여부 조회 Function (Spring AI 1.1.2 FunctionToolCallback 방식)
 * <p>
 * AI Agent가 도서의 대출 가능 여부를 조회할 때 호출하는 함수입니다.
 * ISBN을 받아 전국 도서관의 대출 가능 현황을 반환합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoanCheckFunction {

    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    /**
     * 대출 가능 확인 실행 메서드 (외부에서 호출)
     */
    public LoanCheckResult checkLoanAvailability(LoanCheckRequest request) {
        log.info("[AI Function] 대출 가능 여부 조회 시작: isbn={}", request.isbn());

        try {
            // 1. 도서관정보나루 API 호출
            List<LoanItemInfo> loanItems = libraryInfoNaruApiClient.searchLoanItemsByIsbn(
                request.isbn()
            );

            if (loanItems.isEmpty()) {
                log.info("[AI Function] 대출 가능 도서관 없음: isbn={}", request.isbn());
                return LoanCheckResult.empty(request.isbn());
            }

            // 2. 지역 필터링 (선택 사항)
            List<LoanItemInfo> filteredItems = filterByRegion(loanItems, request.region());

            // 3. 거리 기반 정렬 (위도/경도 제공 시)
            if (request.latitude() != null && request.longitude() != null) {
                filteredItems = sortByDistance(
                    filteredItems,
                    request.latitude(),
                    request.longitude()
                );
            }

            // 4. 상위 N개 추출
            List<LoanItemInfo> topItems = filteredItems.stream()
                .limit(request.limit())
                .toList();

            // 5. 결과 변환
            List<LoanCheckResult.LibraryInfo> libraries = topItems.stream()
                .map(this::toLibraryInfo)
                .toList();

            log.info("[AI Function] 대출 가능 도서관 조회 완료: isbn={}, count={}",
                request.isbn(), libraries.size());

            return LoanCheckResult.of(
                request.isbn(),
                loanItems.size(),
                libraries
            );

        } catch (Exception e) {
            log.error("[AI Function] 대출 가능 여부 조회 실패: isbn={}", request.isbn(), e);
            return LoanCheckResult.empty(request.isbn());
        }
    }

    /**
     * 지역 기반 필터링
     *
     * @param loanItems 대출 도서 목록
     * @param region    지역
     * @return 필터링된 목록
     */
    private List<LoanItemInfo> filterByRegion(List<LoanItemInfo> loanItems, String region) {
        if (region == null || region.isBlank()) {
            return loanItems;
        }
        return loanItems.stream()
            .filter(item -> item.location() != null && item.location().contains(region))
            .collect(Collectors.toList());
    }

    /**
     * 거리 기반 정렬 (하버사인 공식)
     * <p>
     * 참고: 현재 LoanItemInfo에는 위도/경도 정보가 없으므로,
     * 지역 필터링만 수행하고 정렬은 생략합니다.
     * </p>
     *
     * @param loanItems 대출 도서 목록
     * @param latitude  위도
     * @param longitude 경도
     * @return 정렬된 목록
     */
    private List<LoanItemInfo> sortByDistance(List<LoanItemInfo> loanItems,
                                              Double latitude,
                                              Double longitude) {
        // TODO: 도서관 위도/경도 정보가 API 응답에 포함되면 거리 계산 구현
        // 현재는 지역 필터링된 목록을 그대로 반환
        log.debug("[AI Function] 거리 기반 정렬 요청되었으나, 위도/경도 정보 부족으로 생략");
        return loanItems;
    }

    /**
     * LoanItemInfo를 LibraryInfo로 변환합니다.
     *
     * @param item LoanItemInfo
     * @return LoanCheckResult.LibraryInfo
     */
    private LoanCheckResult.LibraryInfo toLibraryInfo(LoanItemInfo item) {
        boolean loanAvailable = "Y".equalsIgnoreCase(item.loanAvailable());

        return LoanCheckResult.LibraryInfo.of(
            item.libraryName(),
            item.location(),
            null, // 거리 정보 (현재 미지원)
            "",   // 전화번호 (현재 미지원)
            loanAvailable,
            ""    // 운영 시간 (현재 미지원)
        );
    }
}
