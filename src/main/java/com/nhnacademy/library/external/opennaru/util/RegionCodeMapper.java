package com.nhnacademy.library.external.opennaru.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 한국 지역명을 도서관정보나루 API 지역 코드로 변환하는 유틸리티 클래스
 * <p>
 * 도서관정보나루 API는 지역명이 아닌 지역 코드를 요구합니다.
 * 예: "서울" → "11", "부산" → "21", "광주" → "24"
 * </p>
 * <p>
 * 지역 코드 참고 (도서관정보나루 API 기준):
 * 11=서울, 21=부산, 22=대구, 23=인천, 24=광주, 25=대전, 26=울산,
 * 29=세종, 31=경기, 32=강원, 33=충북, 34=충남, 35=전북, 36=전남,
 * 37=경북, 38=경남, 39=제주
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
        // 도서관정보나루 API 지역 코드
        // 11=서울, 21=부산, 22=대구, 23=인천, 24=광주, 25=대전, 26=울산
        // 29=세종, 31=경기, 32=강원, 33=충북, 34=충남, 35=전북, 36=전남
        // 37=경북, 38=경남, 39=제주
        REGION_CODE_MAP.put("부산", "21");
        REGION_CODE_MAP.put("부산광역시", "21");
        REGION_CODE_MAP.put("busan", "21");

        REGION_CODE_MAP.put("대구", "22");
        REGION_CODE_MAP.put("대구광역시", "22");
        REGION_CODE_MAP.put("daegu", "22");

        REGION_CODE_MAP.put("인천", "23");
        REGION_CODE_MAP.put("인천광역시", "23");
        REGION_CODE_MAP.put("incheon", "23");

        REGION_CODE_MAP.put("광주", "24");
        REGION_CODE_MAP.put("광주광역시", "24");
        REGION_CODE_MAP.put("gwangju", "24");

        REGION_CODE_MAP.put("대전", "25");
        REGION_CODE_MAP.put("대전광역시", "25");
        REGION_CODE_MAP.put("daejeon", "25");

        REGION_CODE_MAP.put("울산", "26");
        REGION_CODE_MAP.put("울산광역시", "26");
        REGION_CODE_MAP.put("ulsan", "26");

        REGION_CODE_MAP.put("세종", "29");
        REGION_CODE_MAP.put("세종특별자치시", "29");
        REGION_CODE_MAP.put("sejong", "29");

        // 도
        REGION_CODE_MAP.put("경기", "31");
        REGION_CODE_MAP.put("경기도", "31");
        REGION_CODE_MAP.put("gyeonggi", "31");

        REGION_CODE_MAP.put("강원", "32");
        REGION_CODE_MAP.put("강원도", "32");
        REGION_CODE_MAP.put("강원특별자치도", "32");
        REGION_CODE_MAP.put("gangwon", "32");

        REGION_CODE_MAP.put("충북", "33");
        REGION_CODE_MAP.put("충청북도", "33");
        REGION_CODE_MAP.put("chungbuk", "33");

        REGION_CODE_MAP.put("충남", "34");
        REGION_CODE_MAP.put("충청남도", "34");
        REGION_CODE_MAP.put("chungnam", "34");

        REGION_CODE_MAP.put("전북", "35");
        REGION_CODE_MAP.put("전라북도", "35");
        REGION_CODE_MAP.put("jeonbuk", "35");

        REGION_CODE_MAP.put("전남", "36");
        REGION_CODE_MAP.put("전라남도", "36");
        REGION_CODE_MAP.put("jeonnam", "36");

        REGION_CODE_MAP.put("경북", "37");
        REGION_CODE_MAP.put("경상북도", "37");
        REGION_CODE_MAP.put("gyeongbuk", "37");

        REGION_CODE_MAP.put("경남", "38");
        REGION_CODE_MAP.put("경상남도", "38");
        REGION_CODE_MAP.put("gyeongnam", "38");

        REGION_CODE_MAP.put("제주", "39");
        REGION_CODE_MAP.put("제주도", "39");
        REGION_CODE_MAP.put("제주특별자치도", "39");
        REGION_CODE_MAP.put("jeju", "39");

        // 주요 시/군 (상세 지역 코드)
        REGION_CODE_MAP.put("강남구", "680");
        REGION_CODE_MAP.put("gangnam", "680");

        REGION_CODE_MAP.put("판교", "688");
        REGION_CODE_MAP.put("pangyo", "688");

        // 광주 남구 (24030) - 강남구(680)보다 구체적이므로 먼저 체크
        REGION_CODE_MAP.put("광주 남구", "24030");
        REGION_CODE_MAP.put("광주 남구도", "24030");
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
