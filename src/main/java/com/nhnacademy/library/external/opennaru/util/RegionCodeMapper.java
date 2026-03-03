package com.nhnacademy.library.external.opennaru.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 한국 지역명을 도서관정보나루 API 지역 코드로 변환하는 유틸리티 클래스
 * <p>
 * 도서관정보나루 API는 지역명이 아닌 지역 코드를 요구합니다.
 * 예: "서울" → "11", "부산" → "26", "광주" → "29"
 * </p>
 * <p>
 * 지역 코드 참고 (도서관정보나루 API 기준):
 * 11=서울, 26=부산, 27=대구, 28=인천, 29=광주, 30=대전, 31=울산,
 * 36=세종, 41=경기, 42=강원, 43=충북, 44=충남, 45=전북, 46=전남,
 * 47=경북, 48=경남, 50=제주
 * </p>
 */
@Slf4j
public class RegionCodeMapper {

    /**
     * 지역명 → 지역 코드 매핑 테이블
     * <p>
     * 참고: 도서관정보나루 API 지역 코드 기준
     * </p>
     */
    private static final Map<String, String> REGION_CODE_MAP = new HashMap<>();

    static {
        // 특별시
        REGION_CODE_MAP.put("서울", "11");
        REGION_CODE_MAP.put("서울특별시", "11");
        REGION_CODE_MAP.put("seoul", "11");

        // 광역시
        // 도서관정보나루 API 지역 코드: 26=부산, 27=대구, 28=인천, 29=광주, 30=대전, 31=울산, 36=세종
        REGION_CODE_MAP.put("부산", "26");
        REGION_CODE_MAP.put("부산광역시", "26");
        REGION_CODE_MAP.put("busan", "26");

        REGION_CODE_MAP.put("대구", "27");
        REGION_CODE_MAP.put("대구광역시", "27");
        REGION_CODE_MAP.put("daegu", "27");

        REGION_CODE_MAP.put("인천", "28");
        REGION_CODE_MAP.put("인천광역시", "28");
        REGION_CODE_MAP.put("incheon", "28");

        REGION_CODE_MAP.put("광주", "29");
        REGION_CODE_MAP.put("광주광역시", "29");
        REGION_CODE_MAP.put("gwangju", "29");

        REGION_CODE_MAP.put("대전", "30");
        REGION_CODE_MAP.put("대전광역시", "30");
        REGION_CODE_MAP.put("daejeon", "30");

        REGION_CODE_MAP.put("울산", "31");
        REGION_CODE_MAP.put("울산광역시", "31");
        REGION_CODE_MAP.put("ulsan", "31");

        REGION_CODE_MAP.put("세종", "36");
        REGION_CODE_MAP.put("세종특별자치시", "36");
        REGION_CODE_MAP.put("sejong", "36");

        // 도
        REGION_CODE_MAP.put("경기", "41");
        REGION_CODE_MAP.put("경기도", "41");
        REGION_CODE_MAP.put("gyeonggi", "41");

        REGION_CODE_MAP.put("강원", "42");
        REGION_CODE_MAP.put("강원도", "42");
        REGION_CODE_MAP.put("강원특별자치도", "42");
        REGION_CODE_MAP.put("gangwon", "42");

        REGION_CODE_MAP.put("충북", "43");
        REGION_CODE_MAP.put("충청북도", "43");
        REGION_CODE_MAP.put("chungbuk", "43");

        REGION_CODE_MAP.put("충남", "44");
        REGION_CODE_MAP.put("충청남도", "44");
        REGION_CODE_MAP.put("chungnam", "44");

        REGION_CODE_MAP.put("전북", "45");
        REGION_CODE_MAP.put("전라북도", "45");
        REGION_CODE_MAP.put("jeonbuk", "45");

        REGION_CODE_MAP.put("전남", "46");
        REGION_CODE_MAP.put("전라남도", "46");
        REGION_CODE_MAP.put("jeonnam", "46");

        REGION_CODE_MAP.put("경북", "47");
        REGION_CODE_MAP.put("경상북도", "47");
        REGION_CODE_MAP.put("gyeongbuk", "47");

        REGION_CODE_MAP.put("경남", "48");
        REGION_CODE_MAP.put("경상남도", "48");
        REGION_CODE_MAP.put("gyeongnam", "48");

        REGION_CODE_MAP.put("제주", "50");
        REGION_CODE_MAP.put("제주도", "50");
        REGION_CODE_MAP.put("제주특별자치도", "50");
        REGION_CODE_MAP.put("jeju", "50");

        // 주요 시/군 (상세 지역 코드)
        REGION_CODE_MAP.put("강남구", "680");
        REGION_CODE_MAP.put("gangnam", "680");

        REGION_CODE_MAP.put("판교", "688");
        REGION_CODE_MAP.put("pangyo", "688");
    }

    /**
     * 지역명을 지역 코드로 변환합니다.
     *
     * @param regionName 지역명 (예: "서울", "광주", "경기도")
     * @return 지역 코드 (예: "11", "29", "41"), 변환할 수 없는 경우 null
     */
    public static String toRegionCode(String regionName) {
        if (regionName == null || regionName.isBlank()) {
            log.warn("[RegionCodeMapper] 지역명이 비어있습니다.");
            return null;
        }

        // 공백 제거
        String normalizedRegion = regionName.trim();

        // 직접 매칭
        String code = REGION_CODE_MAP.get(normalizedRegion);

        if (code != null) {
            log.info("[RegionCodeMapper] 지역명 → 코드 변환: {} → {}", normalizedRegion, code);
            return code;
        }

        // 부분 매칭 시도 (예: "광주" 포함된 문자열)
        for (Map.Entry<String, String> entry : REGION_CODE_MAP.entrySet()) {
            if (normalizedRegion.contains(entry.getKey())) {
                log.info("[RegionCodeMapper] 부분 매칭으로 지역명 → 코드 변환: {} ({}) → {}",
                        normalizedRegion, entry.getKey(), entry.getValue());
                return entry.getValue();
            }
        }

        log.warn("[RegionCodeMapper] 지역명을 코드로 변환할 수 없습니다: {}", regionName);
        return null;
    }

    /**
     * 입력값이 이미 지역 코드 형식인지 확인합니다.
     *
     * @param value 입력값
     * @return 지역 코드 형식이면 true
     */
    public static boolean isRegionCode(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        // 숫자 2~3자리 형식 확인 (예: "11", "680")
        return value.matches("\\d{2,3}");
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
}
