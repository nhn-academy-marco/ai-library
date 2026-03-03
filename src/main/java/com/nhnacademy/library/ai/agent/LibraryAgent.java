package com.nhnacademy.library.ai.agent;

import com.nhnacademy.library.external.opennaru.dto.LibraryInfo;
import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.util.RegionCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 도서관 검색 Agent
 * <p>
 * 도서관 검색 전담 Agent입니다.
 * 지역명을 받아 해당 지역의 도서관 목록을 조회합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibraryAgent implements Agent {

    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    @Override
    public AgentType getType() {
        return AgentType.LIBRARY;
    }

    @Override
    public String getName() {
        return "LibraryAgent";
    }

    @Override
    public String getDescription() {
        return "도서관 검색 전담 Agent. 지역별 도서관 정보를 제공합니다.";
    }

    @Override
    public String execute(AgentRequest request) {
        log.info("[{}] 도서관 검색 실행: query={}", getName(), request.query());

        try {
            // 1. 지역명 추출
            String region = request.query();

            if (region == null || region.isBlank()) {
                log.warn("[{}] 지역명 없음: query={}", getName(), request.query());
                return "지역명을 입력해주세요.";
            }

            // 1.5 지역명 → 지역 코드 변환
            String regionCode = RegionCodeMapper.convert(region);
            if (regionCode == null) {
                log.warn("[{}] 지역명 → 코드 변환 실패: region={}", getName(), region);
                return String.format("'%s' 지역을 찾을 수 없습니다. 지역명을 확인해 주세요.", region);
            }

            // 2. 도서관 검색
            List<LibraryInfo> libraries = libraryInfoNaruApiClient.searchLibrariesByRegion(regionCode);

            if (libraries.isEmpty()) {
                log.info("[{}] 도서관 검색 결과 없음: region={}", getName(), region);
                return String.format("'%s' 지역의 도서관을 찾을 수 없습니다.", region);
            }

            // 3. 결과 포맷팅 (전체 표시)
            StringBuilder response = new StringBuilder();
            response.append(String.format("'%s' 지역 도서관 %d개:\n\n", region, libraries.size()));

            // 전체 도서관 표시 (libCode 포함)
            for (int i = 0; i < libraries.size(); i++) {
                LibraryInfo lib = libraries.get(i);
                response.append(String.format("%d. %s (libCode: %s)", i + 1, lib.libName(), lib.libCode()));

                // 주소 전체 표시
                if (lib.address() != null && !lib.address().isBlank()) {
                    response.append(String.format("\n   📍 %s", lib.address()));
                }

                response.append("\n");
            }

            log.info("[{}] 도서관 검색 완료: region={}, regionCode={}, count={}", getName(), region, regionCode, libraries.size());
            return response.toString();

        } catch (Exception e) {
            log.error("[{}] 도서관 검색 실패: query={}", getName(), request.query(), e);
            return "도서관 검색 중 오류가 발생했습니다.";
        }
    }
}
