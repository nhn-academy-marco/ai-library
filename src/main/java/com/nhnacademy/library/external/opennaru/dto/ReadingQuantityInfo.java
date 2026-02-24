package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 독서량/독서율 정보 DTO
 *
 * readQt API 응답
 */
@Builder
public record ReadingQuantityInfo(
    String region,             // 지역 코드
    String regionName,         // 지역명
    String dtlRegion,          // 상세 지역 코드
    String dtlRegionName,      // 상세 지역명
    double readingQuantity,    // 독서량 (권)
    double readingRate        // 독서율 (%)
) {}
