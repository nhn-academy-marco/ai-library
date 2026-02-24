package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 도서관정보나루 도서관 정보 DTO
 *
 * libSrch API 응답
 */
@Builder
public record LibraryInfo(
    String libCode,            // 도서관 코드
    String libName,            // 도서관명
    String address,            // 주소
    String tel,                // 전화번호
    String fax,                // 팩스번호
    String latitude,           // 위도
    String longitude,          // 경도
    String homepage,           // 홈페이지
    String closedDays,         // 휴관일
    String operatingHours,     // 운영시간
    String bookCount           // 장서 수
) {}
