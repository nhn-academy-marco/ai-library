package net.java21.library.ai.function.dto;

/**
 * 도서 소장 여부 조회 결과
 *
 * @param libCode 도서관 코드
 * @param libName 도서관명
 * @param isbn13 ISBN-13
 * @param exists 소장 여부
 * @param loanAvailable 대출 가능 여부 ("Y" or "N")
 * @param callNumber 청구기호
 * @param location 소장 위치
 */
public record BookExistResult(
    String libCode,
    String libName,
    String isbn13,
    boolean exists,
    String loanAvailable,
    String callNumber,
    String location
) {
    /**
     * BookExistInfo로부터 BookExistResult 생성
     */
    public static BookExistResult from(
        String libCode,
        String libName,
        net.java21.library.external.opennaru.dto.BookExistInfo info
    ) {
        if (info == null) {
            return new BookExistResult(libCode, libName, "", false, null, null, null);
        }
        return new BookExistResult(
            libCode,
            libName != null ? libName : info.libName(),
            info.isbn13(),
            info.exists(),
            info.loanAvailable(),
            info.callNumber(),
            info.location()
        );
    }

    /**
     * 대출 가능한지 확인
     */
    public boolean isLoanAvailable() {
        return exists && "Y".equals(loanAvailable);
    }
}
