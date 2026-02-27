package com.nhnacademy.library.core.book.service.personalization;

import com.nhnacademy.library.core.book.dto.BookSearchResponse;

import java.util.List;

/**
 * 개인화 추천 서비스 인터페이스
 *
 * <p>사용자의 피드백 데이터를 분석하여 선호도를 학습하고,
 * 검색 결과에 개인화된 순위를 적용합니다.</p>
 */
public interface PersonalizationService {

    /**
     * 사용자 선호도 벡터를 계산합니다.
     *
     * <p>GOOD 피드백을 받은 도서들의 임베딩 벡터를 평균내어
     * 사용자의 취향을 대표하는 벡터를 생성합니다.
     * 결과는 24시간 동안 캐싱됩니다.</p>
     *
     * @param chatId 사용자 chatId
     * @return 선호도 벡터 (1024차원), 피드백 부족 시 null
     */
    float[] calculateUserPreferenceVector(Long chatId);

    /**
     * 검색 결과를 개인화된 순서로 재정렬합니다.
     *
     * <p>사용자 선호도 벡터와 각 도서의 임베딩 간의 코사인 유사도를 계산하고,
     * 기존 RRF 점수와 조합하여 최종 순위를 결정합니다.</p>
     *
     * @param results 기존 검색 결과
     * @param chatId  사용자 chatId
     * @return 개인화된 검색 결과
     */
    List<BookSearchResponse> personalizedSearch(
        List<BookSearchResponse> results,
        Long chatId
    );

    /**
     * 선호도 벡터 캐시를 무효화합니다.
     *
     * <p>사용자가 새로운 피드백을 남긴 경우,
     * 캐시를 삭제하여 다음 검색时 재계산되도록 합니다.</p>
     *
     * @param chatId 사용자 chatId
     */
    void evictUserPreferenceCache(Long chatId);
}
