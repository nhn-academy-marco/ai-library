package com.nhnacademy.library.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.library.ai.agent.dto.ParsedQuery;
import com.nhnacademy.library.ai.agent.dto.ParsedQuery.SearchIntent;
import com.nhnacademy.library.external.opennaru.util.RegionCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 쿼리 파싱 Agent
 * <p>
 * 사용자의 자연어 쿼리를 분석하여 구조화된 정보를 추출합니다.
 * <b>llama3-korean-blossom</b> LLM을 활용하여 도서명, 지역명, 검색 의도 등을 정확하게 파악합니다.
 * </p>
 *
 * <p>**역할:**</p>
 * <ul>
 *   <li>도서명 추출: "토비의 스프링" → bookName="토비의 스프링"</li>
 *   <li>지역명 추출: "광주에서" → regionName="광주"</li>
 *   <li>의도 파악: "빌릴까?" → intent=CHECK_AVAILABILITY</li>
 *   <li>키워드 추출: "3.1 버전" → keywords=["3.1", "버전"]</li>
 * </ul>
 *
 * <p>**사용 모델:** llama3-korean-blossom (한국어 쿼리 파싱 특화)</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueryParserAgent {

    private final ChatClient llama3ChatClient;  // llama3-korean-blossom 전용
    private final ObjectMapper objectMapper;

    /**
     * 사용자 쿼리를 파싱하여 구조화된 정보를 추출합니다.
     *
     * @param userMessage 사용자의 자연어 쿼리
     * @return 파싱된 쿼리 정보
     */
    public ParsedQuery parse(String userMessage) {
        long startTime = System.currentTimeMillis();
        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [QueryParserAgent] 🔍 쿼리 파싱 시작                   │");
        log.info("│ 모델: llama3-korean-blossom                           │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 입력 메시지:                                            │");
        log.info("│   \"{}\"                                              │", userMessage);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            String systemPrompt = buildSystemPrompt();
            log.debug("[QueryParserAgent] System Prompt 길이: {}字符", systemPrompt.length());

            // llama3-korean-blossom 사용
            long llmStart = System.currentTimeMillis();
            log.info("[QueryParserAgent] LLM 호출 시작...");

            String response = llama3ChatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

            long llmElapsed = System.currentTimeMillis() - llmStart;
            log.info("[QueryParserAgent] ✅ LLM 응답 수신 (소요시간: {}ms)", llmElapsed);
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [QueryParserAgent] LLM 응답 (Raw):                     │");
            log.info("├─────────────────────────────────────────────────────┤");
            log.info("│ {}                                                    │", response);
            log.info("└─────────────────────────────────────────────────────┘");

            // JSON 파싱
            long parseStart = System.currentTimeMillis();
            ParsedQuery parsed = parseJsonResponse(response, userMessage);
            long parseElapsed = System.currentTimeMillis() - parseStart;

            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [QueryParserAgent] ✅ 파싱 완료                        │");
            log.info("├─────────────────────────────────────────────────────┤");
            log.info("│ 추출된 엔티티:                                        │");
            log.info("│   - bookName: {}                                     │", parsed.bookName());
            log.info("│   - authorName: {}                                   │", parsed.authorName());
            log.info("│   - regionName: {}                                   │", parsed.regionName());
            log.info("│   - intent: {}                                        │", parsed.intent());
            log.info("│   - keywords: {}                                      │", parsed.keywords());
            log.info("│   - JSON 파싱 소요시간: {}ms                           │", parseElapsed);
            log.info("└─────────────────────────────────────────────────────┘");

            long totalElapsed = System.currentTimeMillis() - startTime;
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [QueryParserAgent] ⏱️ 총 소요시간: {}ms               │", totalElapsed);
            log.info("└─────────────────────────────────────────────────────┘");

            return parsed;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [QueryParserAgent] ❌ 파싱 실패 ({}ms)                │", elapsed);
            log.error("│ 에러: {}                                                │", e.getMessage());
            log.error("│ Fallback으로 전환합니다...                            │");
            log.error("└─────────────────────────────────────────────────────┘");
            return fallbackParse(userMessage);
        }
    }

    /**
     * System Prompt를 생성합니다.
     */
    private String buildSystemPrompt() {
        return """
            당신은 도서관 시스템을 위한 쿼리 분석 전문가입니다.

            **작업:**
            사용자 메시지에서 다음 정보를 추출하고 JSON 형식으로 반환하세요.

            **출력 형식 (JSON):**
            {
              "bookName": "도서명 (없으면 null)",
              "authorName": "저자명 (없으면 null)",
              "regionName": "지역명 (예: 서울, 광주, 부산, 대구, 인천, 대전, 울산, 세종, 경기, 강원 등 - 없으면 null)",
              "intent": "SEARCH | CHECK_AVAILABILITY | RECOMMEND | LIBRARY_SEARCH | UNKNOWN",
              "keywords": ["키워드1", "키워드2"]
            }

            **Intent 정의:**
            - SEARCH: 도서 검색 (예: "자바 책 찾아줘")
            - CHECK_AVAILABILITY: 대출 가능 여부 확인 (예: "빌릴까?", "있어?", "대출 가능?")
            - RECOMMEND: 도서 추천 요청 (예: "추천해줘", "어떤 책 좋아?")
            - LIBRARY_SEARCH: 도서관 검색 (예: "근처 도서관", "광주 도서관")
            - UNKNOWN: 의도 불명확

            **지역명 표준화:**
            - "서울역", "서울시", "서울특별시" → "서울"
            - "광주광역시", "광주" → "광주"
            - **중요:** "시/도 + 구/군" 형태는 보존 (예: "광주 남구", "부산 남구", "서울 강남구")
            - "판교", "강남" → "경기" (구체적인 구/군만 단독으로 표현될 때만 상위 시/도로 변환)

            **예시:**

            1. "광주에서 토비의 스프링 빌릴까?"
            → {
                "bookName": "토비의 스프링",
                "authorName": null,
                "regionName": "광주",
                "intent": "CHECK_AVAILABILITY",
                "keywords": ["빌릴까", "대출 가능"]
              }

            2. "자바의 정석 책 추천해줘"
            → {
                "bookName": "자바의 정석",
                "authorName": null,
                "regionName": null,
                "intent": "RECOMMEND",
                "keywords": ["추천"]
              }

            3. "이일민 책"
            → {
                "bookName": null,
                "authorName": "이일민",
                "regionName": null,
                "intent": "SEARCH",
                "keywords": []
              }

            4. "서울역 근처 도서관"
            → {
                "bookName": null,
                "authorName": null,
                "regionName": "서울",
                "intent": "LIBRARY_SEARCH",
                "keywords": ["근처", "역"]
              }

            **중요:**
            - 반드시 유효한 JSON만 반환하세요 (markdown 코드 블록 제외)
            - 값이 없으면 null로 설정하세요
            - 지역명은 표준 지역명(시/도 단위)으로 변환하세요
            """;
    }

    /**
     * LLM 응답을 ParsedQuery로 변환합니다.
     */
    private ParsedQuery parseJsonResponse(String jsonResponse, String originalQuery) {
        try {
            // markdown 코드 블록 제거 (```json ... ```)
            String cleanedJson = jsonResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

            log.debug("[QueryParserAgent] 정제된 JSON: {}", cleanedJson);

            ParsedQueryJson json = objectMapper.readValue(cleanedJson, ParsedQueryJson.class);

            // 지역명 → 지역 코드 변환
            String regionCode = null;
            if (json.regionName() != null && !json.regionName().isBlank()) {
                regionCode = RegionCodeMapper.convert(json.regionName());
                log.debug("[QueryParserAgent] 지역 코드 변환: {} → {}", json.regionName(), regionCode);
            }

            return new ParsedQuery(
                json.bookName(),
                json.authorName(),
                json.regionName(),
                regionCode,
                parseIntent(json.intent()),
                json.keywords() != null ? json.keywords() : List.of(),
                originalQuery
            );

        } catch (JsonProcessingException e) {
            log.error("[QueryParserAgent] JSON 파싱 실패: {}", jsonResponse, e);
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }

    /**
     * Intent 문자열을 Enum으로 변환합니다.
     */
    private SearchIntent parseIntent(String intentStr) {
        if (intentStr == null || intentStr.isBlank()) {
            return SearchIntent.UNKNOWN;
        }

        try {
            return SearchIntent.valueOf(intentStr);
        } catch (IllegalArgumentException e) {
            log.warn("[QueryParserAgent] 알 수 없는 intent: {}", intentStr);
            return SearchIntent.UNKNOWN;
        }
    }

    /**
     * LLM 파싱 실패 시 Fallback 파싱 (규칙 기반)
     */
    private ParsedQuery fallbackParse(String userMessage) {
        log.info("[QueryParserAgent] Fallback 파싱 실행");

        String lowerMessage = userMessage.toLowerCase();

        // Intent 추출
        SearchIntent intent = SearchIntent.SEARCH;
        if (lowerMessage.contains("빌릴") || lowerMessage.contains("대출") || lowerMessage.contains("있어")) {
            intent = SearchIntent.CHECK_AVAILABILITY;
        } else if (lowerMessage.contains("추천")) {
            intent = SearchIntent.RECOMMEND;
        } else if (lowerMessage.contains("도서관")) {
            intent = SearchIntent.LIBRARY_SEARCH;
        }

        // 지역명 추출
        String regionName = extractRegionName(userMessage);

        // 지역명 → 지역 코드 변환
        String regionCode = null;
        if (regionName != null && !regionName.isBlank()) {
            regionCode = RegionCodeMapper.convert(regionName);
            log.debug("[QueryParserAgent] Fallback 지역 코드 변환: {} → {}", regionName, regionCode);
        }

        return new ParsedQuery(
            null, // bookName (추출 불가)
            null, // authorName (추출 불가)
            regionName,
            regionCode,
            intent,
            List.of(),
            userMessage
        );
    }

    /**
     * Fallback용 지역명 추출
     * "시/도 + 구/군" 형태를 우선 추출합니다 (예: "광주 남구")
     */
    private String extractRegionName(String message) {
        // 먼저 "시/도 + 구/군" 형태를 추출 (구체적인 지역명 우선)
        String[] detailedRegions = {
            "서울 강남구", "서울 강서구", "서울 강동구", "서울 강북구", "서울 관악구", "서울 광진구",
            "서울 구로구", "서울 금천구", "서울 노원구", "서울 도봉구", "서울 동대문구", "서울 동작구",
            "서울 마포구", "서울 서대문구", "서울 서초구", "서울 성동구", "서울 성북구", "서울 송파구",
            "서울 양천구", "서울 영등포구", "서울 은평구", "서울 종로구", "서울 중구", "서울 중랑구",
            "서울 용산구", "서울 연산구",
            "부산 강서구", "부산 금정구", "부산 남구", "부산 동구", "부산 동래구", "부산 부산진구",
            "부산 사상구", "부산 사하구", "부산 서구", "부산 수영구", "부산 연제구", "부산 영도구",
            "부산 중구", "부산 해운대구",
            "대구 남구", "대구 북구", "대구 동구", "대구 서구", "대구 중구", "대구 수성구", "대구 달서구",
            "인천 남구", "인천 부평구", "인천 서구", "인천 연수구", "인천 중구",
            "광주 남구", "광주 북구", "광주 동구", "광주 서구", "광주 광산구",
            "대전 남구", "대전 동구", "대전 서구", "대전 중구", "대전 유성구", "대전 대덕구",
            "울산 남구", "울산 동구", "울산 북구", "울산 중구", "울산 울주구",
            "세종시",
            "경기 수원시", "경기 성남시", "경기 고양시", "경기 용인시", "경기 부천시",
            "경기 안산시", "경기 안양시", "경기 김포시", "경기 평택시", "경기 화성시",
            "경기 파주시", "경기 의정부시", "경기 광명시", "경기 군포시", "경기 시흥구"
        };

        for (String region : detailedRegions) {
            if (message.contains(region)) {
                return region;
            }
        }

        // 단순 시/도 단위 추출
        String[] regions = {
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
        };

        for (String region : regions) {
            if (message.contains(region)) {
                return region;
            }
        }
        return null;
    }

    /**
     * JSON 파싱용 내부 Record
     */
    private record ParsedQueryJson(
        String bookName,
        String authorName,
        String regionName,
        String intent,
        List<String> keywords
    ) {}
}
