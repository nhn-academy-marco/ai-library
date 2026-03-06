package com.nhnacademy.library.external.opennaru.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 한국 지역명을 도서관정보나루 API 지역 코드로 변환하는 유틸리티 클래스
 * <p>
 * 도서관정보나루 API는 지역명이 아닌 지역 코드를 요구합니다.
 * 예: "서울" → "11", "부산" → "21", "종로구" → "11010"
 * </p>
 * <p>
 * 지역 코드 참고 (도서관정보나루 API 기준 v20250306):
 * - 시/도 2자리: 11=서울, 21=부산, 22=대구, 23=인천, 24=광주, 25=대전, 26=울산
 * - 시/군/구 5자리: 11010=서울 종로구, 24030=광주 남구
 * </p>
 */
@Slf4j
public class RegionCodeMapper {

    /**
     * 지역명 → 지역 코드 매핑 테이블
     * <p>
     * 시/도 2자리 코드 + 시/군/구 3자리 코드를 포함
     * </p>
     */
    private static final Map<String, String> REGION_CODE_MAP = new HashMap<>();

    static {
        // ========== 시/도 단위 (2자리 코드) ==========

        // 서울특별시 (11)
        REGION_CODE_MAP.put("서울", "11");
        REGION_CODE_MAP.put("서울특별시", "11");
        REGION_CODE_MAP.put("seoul", "11");

        // 부산광역시 (21)
        REGION_CODE_MAP.put("부산", "21");
        REGION_CODE_MAP.put("부산광역시", "21");
        REGION_CODE_MAP.put("busan", "21");

        // 대구광역시 (22)
        REGION_CODE_MAP.put("대구", "22");
        REGION_CODE_MAP.put("대구광역시", "22");
        REGION_CODE_MAP.put("daegu", "22");

        // 인천광역시 (23)
        REGION_CODE_MAP.put("인천", "23");
        REGION_CODE_MAP.put("인천광역시", "23");
        REGION_CODE_MAP.put("incheon", "23");

        // 광주광역시 (24)
        REGION_CODE_MAP.put("광주", "24");
        REGION_CODE_MAP.put("광주광역시", "24");
        REGION_CODE_MAP.put("gwangju", "24");

        // 대전광역시 (25)
        REGION_CODE_MAP.put("대전", "25");
        REGION_CODE_MAP.put("대전광역시", "25");
        REGION_CODE_MAP.put("daejeon", "25");

        // 울산광역시 (26)
        REGION_CODE_MAP.put("울산", "26");
        REGION_CODE_MAP.put("울산광역시", "26");
        REGION_CODE_MAP.put("ulsan", "26");

        // 세종특별자치시 (29)
        REGION_CODE_MAP.put("세종", "29");
        REGION_CODE_MAP.put("세종특별자치시", "29");
        REGION_CODE_MAP.put("sejong", "29");

        // 경기도 (31)
        REGION_CODE_MAP.put("경기", "31");
        REGION_CODE_MAP.put("경기도", "31");
        REGION_CODE_MAP.put("gyeonggi", "31");

        // 강원특별자치도 (32)
        REGION_CODE_MAP.put("강원", "32");
        REGION_CODE_MAP.put("강원도", "32");
        REGION_CODE_MAP.put("강원특별자치도", "32");
        REGION_CODE_MAP.put("gangwon", "32");

        // 충청북도 (33)
        REGION_CODE_MAP.put("충북", "33");
        REGION_CODE_MAP.put("충청북도", "33");
        REGION_CODE_MAP.put("chungbuk", "33");

        // 충청남도 (34)
        REGION_CODE_MAP.put("충남", "34");
        REGION_CODE_MAP.put("충청남도", "34");
        REGION_CODE_MAP.put("chungnam", "34");

        // 전북특별자치도 (35)
        REGION_CODE_MAP.put("전북", "35");
        REGION_CODE_MAP.put("전라북도", "35");
        REGION_CODE_MAP.put("jeonbuk", "35");

        // 전라남도 (36)
        REGION_CODE_MAP.put("전남", "36");
        REGION_CODE_MAP.put("전라남도", "36");
        REGION_CODE_MAP.put("jeonnam", "36");

        // 경상북도 (37)
        REGION_CODE_MAP.put("경북", "37");
        REGION_CODE_MAP.put("경상북도", "37");
        REGION_CODE_MAP.put("gyeongbuk", "37");

        // 경상남도 (38)
        REGION_CODE_MAP.put("경남", "38");
        REGION_CODE_MAP.put("경상남도", "38");
        REGION_CODE_MAP.put("gyeongnam", "38");

        // 제주특별자치도 (39)
        REGION_CODE_MAP.put("제주", "39");
        REGION_CODE_MAP.put("제주도", "39");
        REGION_CODE_MAP.put("제주특별자치도", "39");
        REGION_CODE_MAP.put("jeju", "39");

        // ========== 시/군/구 단위 (5자리 코드) ==========

        // 서울특별시 구 (11xxx)
        REGION_CODE_MAP.put("종로구", "11010");
        REGION_CODE_MAP.put("중구", "11020");
        REGION_CODE_MAP.put("용산구", "11030");
        REGION_CODE_MAP.put("성동구", "11040");
        REGION_CODE_MAP.put("광진구", "11050");
        REGION_CODE_MAP.put("동대문구", "11060");
        REGION_CODE_MAP.put("중랑구", "11070");
        REGION_CODE_MAP.put("성북구", "11080");
        REGION_CODE_MAP.put("강북구", "11090");
        REGION_CODE_MAP.put("도봉구", "11100");
        REGION_CODE_MAP.put("노원구", "11110");
        REGION_CODE_MAP.put("은평구", "11120");
        REGION_CODE_MAP.put("서대문구", "11130");
        REGION_CODE_MAP.put("마포구", "11140");
        REGION_CODE_MAP.put("양천구", "11150");
        REGION_CODE_MAP.put("강서구", "11160");
        REGION_CODE_MAP.put("구로구", "11170");
        REGION_CODE_MAP.put("금천구", "11180");
        REGION_CODE_MAP.put("영등포구", "11190");
        REGION_CODE_MAP.put("동작구", "11200");
        REGION_CODE_MAP.put("관악구", "11210");
        REGION_CODE_MAP.put("서초구", "11220");
        REGION_CODE_MAP.put("강남구", "11230");
        REGION_CODE_MAP.put("송파구", "11240");
        REGION_CODE_MAP.put("강동구", "11250");

        // 부산광역시 구 (21xxx)
        REGION_CODE_MAP.put("중구", "21010");
        REGION_CODE_MAP.put("서구", "21020");
        REGION_CODE_MAP.put("동구", "21030");
        REGION_CODE_MAP.put("영도구", "21040");
        REGION_CODE_MAP.put("부산진구", "21050");
        REGION_CODE_MAP.put("동래구", "21060");
        REGION_CODE_MAP.put("남구", "21070");
        REGION_CODE_MAP.put("북구", "21080");
        REGION_CODE_MAP.put("해운대구", "21090");
        REGION_CODE_MAP.put("사하구", "21100");
        REGION_CODE_MAP.put("금정구", "21110");
        REGION_CODE_MAP.put("강서구", "21120");
        REGION_CODE_MAP.put("연제구", "21130");
        REGION_CODE_MAP.put("수영구", "21140");
        REGION_CODE_MAP.put("사상구", "21150");
        REGION_CODE_MAP.put("기장군", "21310");

        // 대구광역시 구 (22xxx)
        REGION_CODE_MAP.put("중구", "22010");
        REGION_CODE_MAP.put("동구", "22020");
        REGION_CODE_MAP.put("서구", "22030");
        REGION_CODE_MAP.put("남구", "22040");
        REGION_CODE_MAP.put("북구", "22050");
        REGION_CODE_MAP.put("수성구", "22060");
        REGION_CODE_MAP.put("달서구", "22070");
        REGION_CODE_MAP.put("달성군", "22310");
        REGION_CODE_MAP.put("군위군", "22320");

        // 인천광역시 구 (23xxx)
        REGION_CODE_MAP.put("중구", "23010");
        REGION_CODE_MAP.put("동구", "23020");
        REGION_CODE_MAP.put("남구", "23030");
        REGION_CODE_MAP.put("연수구", "23040");
        REGION_CODE_MAP.put("남동구", "23050");
        REGION_CODE_MAP.put("부평구", "23060");
        REGION_CODE_MAP.put("계양구", "23070");
        REGION_CODE_MAP.put("서구", "23080");
        REGION_CODE_MAP.put("강화군", "23310");
        REGION_CODE_MAP.put("옹진군", "23320");

        // 광주광역시 구 (24xxx) - 중요: 남구 포함
        REGION_CODE_MAP.put("동구", "24010");
        REGION_CODE_MAP.put("서구", "24020");
        REGION_CODE_MAP.put("남구", "24030");
        REGION_CODE_MAP.put("북구", "24040");
        REGION_CODE_MAP.put("광산구", "24050");

        // 대전광역시 구 (25xxx)
        REGION_CODE_MAP.put("동구", "25010");
        REGION_CODE_MAP.put("중구", "25020");
        REGION_CODE_MAP.put("서구", "25040");
        REGION_CODE_MAP.put("유성구", "25030");
        REGION_CODE_MAP.put("대덕구", "25050");

        // 울산광역시 구 (26xxx)
        REGION_CODE_MAP.put("중구", "26010");
        REGION_CODE_MAP.put("남구", "26020");
        REGION_CODE_MAP.put("동구", "26030");
        REGION_CODE_MAP.put("북구", "26040");
        REGION_CODE_MAP.put("울주군", "26310");

        // 세종특별자치시 (29xxx)
        REGION_CODE_MAP.put("세종시", "29010");

        // 경기도 시/군/구 (31xxx)
        REGION_CODE_MAP.put("수원시", "31010");
        REGION_CODE_MAP.put("수원시 장안구", "31011");
        REGION_CODE_MAP.put("수원시 권선구", "31012");
        REGION_CODE_MAP.put("수원시 팔달구", "31013");
        REGION_CODE_MAP.put("수원시 영통구", "31014");
        REGION_CODE_MAP.put("성남시", "31020");
        REGION_CODE_MAP.put("성남시 수정구", "31021");
        REGION_CODE_MAP.put("성남시 중원구", "31022");
        REGION_CODE_MAP.put("성남시 분당구", "31023");
        REGION_CODE_MAP.put("의정부시", "31030");
        REGION_CODE_MAP.put("안양시", "31040");
        REGION_CODE_MAP.put("안양시 만안구", "31041");
        REGION_CODE_MAP.put("안양시 동안구", "31042");
        REGION_CODE_MAP.put("부천시", "31050");
        REGION_CODE_MAP.put("광명시", "31060");
        REGION_CODE_MAP.put("평택시", "31070");
        REGION_CODE_MAP.put("동두천시", "31080");
        REGION_CODE_MAP.put("안산시", "31090");
        REGION_CODE_MAP.put("안산시 상록구", "31091");
        REGION_CODE_MAP.put("안산시 단원구", "31092");
        REGION_CODE_MAP.put("고양시", "31100");
        REGION_CODE_MAP.put("고양시 덕양구", "31101");
        REGION_CODE_MAP.put("고양시 일산동구", "31103");
        REGION_CODE_MAP.put("고양시 일산서구", "31104");
        REGION_CODE_MAP.put("과천시", "31110");
        REGION_CODE_MAP.put("구리시", "31120");
        REGION_CODE_MAP.put("남양주시", "31130");
        REGION_CODE_MAP.put("오산시", "31140");
        REGION_CODE_MAP.put("시흥시", "31150");
        REGION_CODE_MAP.put("군포시", "31160");
        REGION_CODE_MAP.put("의왕시", "31170");
        REGION_CODE_MAP.put("하남시", "31180");
        REGION_CODE_MAP.put("용인시", "31190");
        REGION_CODE_MAP.put("용인시 처인구", "31191");
        REGION_CODE_MAP.put("용인시 기흥구", "31192");
        REGION_CODE_MAP.put("용인시 수지구", "31193");
        REGION_CODE_MAP.put("파주시", "31200");
        REGION_CODE_MAP.put("이천시", "31210");
        REGION_CODE_MAP.put("안성시", "31220");
        REGION_CODE_MAP.put("김포시", "31230");
        REGION_CODE_MAP.put("화성시", "31240");
        REGION_CODE_MAP.put("광주시", "31250");
        REGION_CODE_MAP.put("양주시", "31260");
        REGION_CODE_MAP.put("포천시", "31270");
        REGION_CODE_MAP.put("여주시", "31280");
        REGION_CODE_MAP.put("연천군", "31350");
        REGION_CODE_MAP.put("가평군", "31370");
        REGION_CODE_MAP.put("양평군", "31380");

        // 강원특별자치도 시/군 (32xxx)
        REGION_CODE_MAP.put("춘천시", "32010");
        REGION_CODE_MAP.put("원주시", "32020");
        REGION_CODE_MAP.put("강릉시", "32030");
        REGION_CODE_MAP.put("동해시", "32040");
        REGION_CODE_MAP.put("태백시", "32050");
        REGION_CODE_MAP.put("속초시", "32060");
        REGION_CODE_MAP.put("삼척시", "32070");
        REGION_CODE_MAP.put("홍천군", "32310");
        REGION_CODE_MAP.put("횡성군", "32320");
        REGION_CODE_MAP.put("영월군", "32330");
        REGION_CODE_MAP.put("평창군", "32340");
        REGION_CODE_MAP.put("정선군", "32350");
        REGION_CODE_MAP.put("철원군", "32360");
        REGION_CODE_MAP.put("화천군", "32370");
        REGION_CODE_MAP.put("양구군", "32380");
        REGION_CODE_MAP.put("인제군", "32390");
        REGION_CODE_MAP.put("고성군", "32400");
        REGION_CODE_MAP.put("양양군", "32410");

        // 충청북도 시/군/구 (33xxx)
        REGION_CODE_MAP.put("충주시", "33020");
        REGION_CODE_MAP.put("제천시", "33030");
        REGION_CODE_MAP.put("청주시", "33040");
        REGION_CODE_MAP.put("청주시 상당구", "33041");
        REGION_CODE_MAP.put("청주시 서원구", "33042");
        REGION_CODE_MAP.put("청주시 흥덕구", "33043");
        REGION_CODE_MAP.put("청주시 청원구", "33044");
        REGION_CODE_MAP.put("보은군", "33320");
        REGION_CODE_MAP.put("옥천군", "33330");
        REGION_CODE_MAP.put("영동군", "33340");
        REGION_CODE_MAP.put("진천군", "33350");
        REGION_CODE_MAP.put("괴산군", "33360");
        REGION_CODE_MAP.put("음성군", "33370");
        REGION_CODE_MAP.put("단양군", "33380");
        REGION_CODE_MAP.put("증평군", "33390");

        // 충청남도 시/군/구 (34xxx)
        REGION_CODE_MAP.put("천안시", "34010");
        REGION_CODE_MAP.put("천안시 동남구", "34011");
        REGION_CODE_MAP.put("천안시 서북구", "34012");
        REGION_CODE_MAP.put("공주시", "34020");
        REGION_CODE_MAP.put("보령시", "34030");
        REGION_CODE_MAP.put("아산시", "34040");
        REGION_CODE_MAP.put("서산시", "34050");
        REGION_CODE_MAP.put("논산시", "34060");
        REGION_CODE_MAP.put("계룡시", "34070");
        REGION_CODE_MAP.put("당진시", "34080");
        REGION_CODE_MAP.put("금산군", "34310");
        REGION_CODE_MAP.put("부여군", "34330");
        REGION_CODE_MAP.put("서천시", "34340");
        REGION_CODE_MAP.put("청양군", "34350");
        REGION_CODE_MAP.put("홍성군", "34360");
        REGION_CODE_MAP.put("예산군", "34370");
        REGION_CODE_MAP.put("태안군", "34380");

        // 전북특별자치도 시/군 (35xxx)
        REGION_CODE_MAP.put("전주시", "35010");
        REGION_CODE_MAP.put("전주시 완산구", "35011");
        REGION_CODE_MAP.put("전주시 덕진구", "35012");
        REGION_CODE_MAP.put("군산시", "35020");
        REGION_CODE_MAP.put("익산시", "35030");
        REGION_CODE_MAP.put("정읍시", "35040");
        REGION_CODE_MAP.put("남원시", "35050");
        REGION_CODE_MAP.put("김제시", "35060");
        REGION_CODE_MAP.put("완주군", "35310");
        REGION_CODE_MAP.put("진안군", "35320");
        REGION_CODE_MAP.put("무주군", "35330");
        REGION_CODE_MAP.put("장수군", "35340");
        REGION_CODE_MAP.put("임실군", "35350");
        REGION_CODE_MAP.put("순창군", "35360");
        REGION_CODE_MAP.put("고창군", "35370");
        REGION_CODE_MAP.put("부안군", "35380");

        // 전라남도 시/군 (36xxx)
        REGION_CODE_MAP.put("목포시", "36010");
        REGION_CODE_MAP.put("여수시", "36020");
        REGION_CODE_MAP.put("순천시", "36030");
        REGION_CODE_MAP.put("나주시", "36040");
        REGION_CODE_MAP.put("광양시", "36060");
        REGION_CODE_MAP.put("담양군", "36310");
        REGION_CODE_MAP.put("곡성군", "36320");
        REGION_CODE_MAP.put("구례군", "36330");
        REGION_CODE_MAP.put("고흽군", "36350");
        REGION_CODE_MAP.put("보성군", "36360");
        REGION_CODE_MAP.put("화순군", "36370");
        REGION_CODE_MAP.put("장흥군", "36380");
        REGION_CODE_MAP.put("강진군", "36390");
        REGION_CODE_MAP.put("해남군", "36400");
        REGION_CODE_MAP.put("영암군", "36410");
        REGION_CODE_MAP.put("무안군", "36420");
        REGION_CODE_MAP.put("함평군", "36430");
        REGION_CODE_MAP.put("영광군", "36440");
        REGION_CODE_MAP.put("장성군", "36450");
        REGION_CODE_MAP.put("완도군", "36460");
        REGION_CODE_MAP.put("진도군", "36470");
        REGION_CODE_MAP.put("신안군", "36480");

        // 경상북도 시/군/구 (37xxx)
        REGION_CODE_MAP.put("포항시", "37010");
        REGION_CODE_MAP.put("포항시 남구", "37011");
        REGION_CODE_MAP.put("포항시 북구", "37012");
        REGION_CODE_MAP.put("경주시", "37020");
        REGION_CODE_MAP.put("김천시", "37030");
        REGION_CODE_MAP.put("안동시", "37040");
        REGION_CODE_MAP.put("구미시", "37050");
        REGION_CODE_MAP.put("영주시", "37060");
        REGION_CODE_MAP.put("상주시", "37080");
        REGION_CODE_MAP.put("문경시", "37090");
        REGION_CODE_MAP.put("경산시", "37100");
        REGION_CODE_MAP.put("군위군", "37310");
        REGION_CODE_MAP.put("의성군", "37320");
        REGION_CODE_MAP.put("청송군", "37330");
        REGION_CODE_MAP.put("영양군", "37340");
        REGION_CODE_MAP.put("영덕군", "37350");
        REGION_CODE_MAP.put("청도군", "37360");
        REGION_CODE_MAP.put("고령군", "37370");
        REGION_CODE_MAP.put("성주시", "37380");
        REGION_CODE_MAP.put("칠곡군", "37390");
        REGION_CODE_MAP.put("예천시", "37400");
        REGION_CODE_MAP.put("봉화군", "37410");
        REGION_CODE_MAP.put("울진군", "37420");
        REGION_CODE_MAP.put("울릉군", "37430");

        // 경상남도 시/군/구 (38xxx)
        REGION_CODE_MAP.put("진주시", "38030");
        REGION_CODE_MAP.put("통영시", "38050");
        REGION_CODE_MAP.put("사천시", "38060");
        REGION_CODE_MAP.put("김해시", "38070");
        REGION_CODE_MAP.put("밀양시", "38080");
        REGION_CODE_MAP.put("거제시", "38090");
        REGION_CODE_MAP.put("양산시", "38100");
        REGION_CODE_MAP.put("창원시", "38110");
        REGION_CODE_MAP.put("창원시 의창구", "38111");
        REGION_CODE_MAP.put("창원시 성산구", "38112");
        REGION_CODE_MAP.put("창원시 마산합포구", "38113");
        REGION_CODE_MAP.put("창원시 마산회원구", "38114");
        REGION_CODE_MAP.put("창원시 진해구", "38115");
        REGION_CODE_MAP.put("의령군", "38310");
        REGION_CODE_MAP.put("함안군", "38320");
        REGION_CODE_MAP.put("창녕군", "38330");
        REGION_CODE_MAP.put("고성군", "38340");
        REGION_CODE_MAP.put("남해군", "38350");
        REGION_CODE_MAP.put("하동군", "38360");
        REGION_CODE_MAP.put("산청군", "38370");
        REGION_CODE_MAP.put("함양군", "38380");
        REGION_CODE_MAP.put("거창군", "38390");
        REGION_CODE_MAP.put("합천군", "38400");

        // 제주특별자치도 (39xxx)
        REGION_CODE_MAP.put("제주시", "39010");
        REGION_CODE_MAP.put("서귀포시", "39020");

        // ========== 일반적 지역명 중복 방지 ==========
        // "강남구", "남구" 등의 중복을 피하기 위해
        // 구체적인 "시/도 + 구/군" 형태를 우선적으로 매핑

        // 강남구 (서울) vs 남구 (부산/대구/울산) 중복 해결
        // - "광주 남구" → 24030 (이미 위에서 추가됨)
        // - "부산 남구" → 21070
        // - "대구 남구" → 22040
        // - "울산 남구" → 26020

        // 강남구 (서울) - 명시적 매핑 추가
        REGION_CODE_MAP.put("강남구", "11230");
        REGION_CODE_MAP.put("서울 강남구", "11230");

        // 광주 남구
        REGION_CODE_MAP.put("광주 남구", "24030");

        // 부산 남구
        REGION_CODE_MAP.put("부산 남구", "21070");

        // 대구 남구
        REGION_CODE_MAP.put("대구 남구", "22040");

        // 울산 남구
        REGION_CODE_MAP.put("울산 남구", "26020");
    }

    /**
     * 지역명을 지역 코드로 변환합니다.
     *
     * @param regionName 지역명 (예: "서울", "광주", "종로구", "광주 남구")
     * @return 지역 코드 (예: "11", "24030", "11010"), 변환할 수 없는 경우 null
     */
    public static String toRegionCode(String regionName) {
        if (regionName == null || regionName.isBlank()) {
            log.warn("[RegionCodeMapper] 지역명이 비어있습니다.");
            return null;
        }

        // 공백 제거
        String normalizedRegion = regionName.trim();

        // 직접 매칭 (구체적인 매핑을 먼저 체크)
        String code = REGION_CODE_MAP.get(normalizedRegion);

        if (code != null) {
            log.info("[RegionCodeMapper] 지역명 → 코드 변환: {} → {}", normalizedRegion, code);
            return code;
        }

        // 부분 매칭 시도 (예: "광주" 포함된 문자열)
        // 더 구체적인 매핑이 우선되도록, 긴이 경로 순회
        String bestMatch = null;
        int bestMatchLength = 0;

        for (Map.Entry<String, String> entry : REGION_CODE_MAP.entrySet()) {
            String key = entry.getKey();
            if (normalizedRegion.contains(key)) {
                // 더 구체적(더 긴) 매핑이 우선
                if (key.length() > bestMatchLength) {
                    bestMatch = entry.getValue();
                    bestMatchLength = key.length();
                }
            }
        }

        if (bestMatch != null) {
            log.info("[RegionCodeMapper] 부분 매칭으로 지역명 → 코드 변환: {} → (matched: {}) → {}",
                    normalizedRegion, bestMatchLength, bestMatch);
            return bestMatch;
        }

        log.warn("[RegionCodeMapper] 지역명을 코드로 변환할 수 없습니다: {}", regionName);
        return null;
    }

    /**
     * 입력값이 이미 지역 코드 형식인지 확인합니다.
     *
     * @param value 입력값
     * @return 지역 코드 형식이면 true (2자리 또는 5자리 숫자)
     */
    public static boolean isRegionCode(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        // 숫자 2자리(시/도) 또는 5자리(시/군/구) 형식 확인 (예: "11", "11010", "24030")
        return value.matches("\\d{2}|\\d{5}");
    }

    /**
     * 지역명을 지역 코드로 변환합니다.
     * 이미 지역 코드 형식이면 그대로 반환합니다.
     *
     * @param regionInput 지역명 또는 지역 코드
     * @return 지역 코드, 변환할 수 없는 경우 null
     */
    public static String convert(String regionInput) {
        if (regionInput == null || regionInput.isBlank()) {
            return null;
        }

        // 이미 지역 코드 형식이면 그대로 반환
        if (isRegionCode(regionInput)) {
            log.info("[RegionCodeMapper] 이미 지역 코드 형식: {}", regionInput);
            return regionInput;
        }

        // 지역명 → 코드 변환
        return toRegionCode(regionInput);
    }

    /**
     * 5자리 지역 코드에서 구/군 이름을 추출합니다.
     * 예: 24030 → "남구", 11010 → "종로구"
     *
     * @param regionCode 5자리 지역 코드
     * @return 구/군 이름 (예: "남구", "종로구"), 변환할 수 없는 경우 null
     */
    public static String extractDistrictName(String regionCode) {
        if (regionCode == null || regionCode.length() != 5) {
            return null;
        }

        // 코드 → 지역명 역매핑
        for (Map.Entry<String, String> entry : REGION_CODE_MAP.entrySet()) {
            if (entry.getValue().equals(regionCode)) {
                String regionName = entry.getKey();
                // "광주 남구" → "남구" 추출
                if (regionName.contains(" ")) {
                    return regionName.substring(regionName.lastIndexOf(" ") + 1);
                }
                // "남구" → "남구"
                return regionName;
            }
        }

        return null;
    }

    /**
     * 5자리 지역 코드를 2자리 코드로 변환합니다.
     * 도서관정보나루 API는 libSrch에서 2자리 코드만 지원합니다.
     * 예: 24030 → "24", 11010 → "11"
     *
     * @param regionCode 지역 코드 (2자리 또는 5자리)
     * @return 2자리 지역 코드
     */
    public static String toTwoDigitCode(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return null;
        }

        // 이미 2자리면 그대로 반환
        if (regionCode.length() == 2) {
            return regionCode;
        }

        // 5자리면 앞 2자리만 반환
        if (regionCode.length() == 5) {
            return regionCode.substring(0, 2);
        }

        log.warn("[RegionCodeMapper] 알 수 없는 지역 코드 형식: {}", regionCode);
        return null;
    }
}
