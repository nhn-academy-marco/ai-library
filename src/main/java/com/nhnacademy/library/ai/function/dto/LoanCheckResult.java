package com.nhnacademy.library.ai.function.dto;

import java.util.List;

/**
 * AI Function 호출용 대출 가능 여부 조회 결과 DTO
 *
 * @param isbn           ISBN
 * @param totalLibraries 전체 대출 가능 도서관 수
 * @param libraries      대출 가능 도서관 목록
 */
public record LoanCheckResult(
    String isbn,
    int totalLibraries,
    List<LibraryInfo> libraries
) {
    /**
     * AI Function 호출용 도서관 정보 DTO
     *
     * @param name            도서관명
     * @param address         주소
     * @param distance        거리 (km, 위도/경도 제공 시 계산)
     * @param phone           전화번호
     * @param loanAvailable   대출 가능 여부
     * @param operatingHours  운영 시간
     */
    public record LibraryInfo(
        String name,
        String address,
        Double distance,
        String phone,
        boolean loanAvailable,
        String operatingHours
    ) {
        /**
         * 외부 API 응답을 변환합니다.
         *
         * @param name           도서관명
         * @param address        주소
         * @param distance       거리
         * @param phone          전화번호
         * @param loanAvailable  대출 가능 여부
         * @param operatingHours 운영 시간
         * @return LibraryInfo
         */
        public static LibraryInfo of(String name, String address, Double distance, String phone,
                                     boolean loanAvailable, String operatingHours) {
            return new LibraryInfo(name, address, distance, phone, loanAvailable, operatingHours);
        }
    }

    /**
     * 빈 결과를 생성합니다.
     *
     * @param isbn ISBN
     * @return LoanCheckResult
     */
    public static LoanCheckResult empty(String isbn) {
        return new LoanCheckResult(isbn, 0, List.of());
    }

    /**
     * 결과를 생성합니다.
     *
     * @param isbn           ISBN
     * @param totalLibraries 전체 대출 가능 도서관 수
     * @param libraries      대출 가능 도서관 목록
     * @return LoanCheckResult
     */
    public static LoanCheckResult of(String isbn, int totalLibraries, List<LibraryInfo> libraries) {
        return new LoanCheckResult(isbn, totalLibraries, libraries);
    }
}
