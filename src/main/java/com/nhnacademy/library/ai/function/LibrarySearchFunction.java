package com.nhnacademy.library.ai.function;

import com.nhnacademy.library.ai.function.dto.LibrarySearchRequest;
import com.nhnacademy.library.ai.function.dto.LibrarySearchResult;
import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.LibraryInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 도서관 검색 Function (Spring AI 1.1.2 FunctionToolCallback 방식)
 * <p>
 * AI Agent가 지역별 도서관을 검색할 때 호출하는 함수입니다.
 * 지역명, 위도/경도를 받아 도서관 목록을 반환합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibrarySearchFunction {

    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    /**
     * 도서관 검색 실행 메서드 (외부에서 호출)
     *
     * @param request 도서관 검색 요청
     * @return 도서관 검색 결과
     */
    public LibrarySearchResult searchLibraries(LibrarySearchRequest request) {
        log.info("[AI Function] 도서관 검색 시작: region={}", request.region());

        try {
            List<LibraryInfo> allLibraries;

            // 1. 지역별 또는 전체 도서관 검색
            if (request.region() != null && !request.region().isBlank()) {
                allLibraries = libraryInfoNaruApiClient.searchLibrariesByRegion(request.region());
            } else {
                allLibraries = libraryInfoNaruApiClient.searchAllLibraries();
            }

            if (allLibraries.isEmpty()) {
                log.info("[AI Function] 도서관 검색 결과 없음");
                return LibrarySearchResult.empty();
            }

            // 2. 거리 기반 정렬 (위도/경도 제공 시)
            List<LibraryInfo> filteredLibraries = allLibraries;
            if (request.latitude() != null && request.longitude() != null) {
                filteredLibraries = sortByDistance(
                    allLibraries,
                    request.latitude(),
                    request.longitude()
                );
            }

            // 3. 상위 N개 추출
            List<LibraryInfo> topLibraries = filteredLibraries.stream()
                .limit(request.limit())
                .collect(Collectors.toList());

            log.info("[AI Function] 도서관 검색 완료: count={}", topLibraries.size());

            // 4. 결과 변환
            List<LibrarySearchResult.LibraryItem> libraryItems = topLibraries.stream()
                .map(this::toLibraryItem)
                .collect(Collectors.toList());

            return LibrarySearchResult.of(
                request.region() != null ? request.region() : "전체",
                allLibraries.size(),
                libraryItems
            );

        } catch (Exception e) {
            log.error("[AI Function] 도서관 검색 실패: region={}", request.region(), e);
            return LibrarySearchResult.empty();
        }
    }

    /**
     * 거리 기반 정렬 (하버사인 공식)
     * <p>
     * 참고: LibraryInfo에는 위도/경도 정보가 있으므로 실제 거리 계산 가능.
     * </p>
     *
     * @param libraries 도서관 목록
     * @param latitude  위도
     * @param longitude 경도
     * @return 정렬된 목록
     */
    private List<LibraryInfo> sortByDistance(List<LibraryInfo> libraries,
                                             Double latitude,
                                             Double longitude) {
        // TODO: 도서관 위도/경도 정보로 거리 계산 구현
        log.debug("[AI Function] 거리 기반 정렬 요청됨");
        return libraries;
    }

    /**
     * LibraryInfo를 LibraryItem으로 변환합니다.
     *
     * @param library LibraryInfo
     * @return LibrarySearchResult.LibraryItem
     */
    private LibrarySearchResult.LibraryItem toLibraryItem(LibraryInfo library) {
        return LibrarySearchResult.LibraryItem.of(
            library.libName(),
            library.address() != null ? library.address() : "주소 정보 없음",
            library.homepage() != null ? library.homepage() : ""
        );
    }
}
