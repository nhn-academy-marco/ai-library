package com.nhnacademy.library.core.book.service.personalization.impl;

import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.service.personalization.PersonalizationService;
import com.nhnacademy.library.core.book.util.VectorUtils;
import com.nhnacademy.library.external.telegram.dto.FeedbackType;
import com.nhnacademy.library.external.telegram.entity.SearchFeedback;
import com.nhnacademy.library.external.telegram.repository.SearchFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 개인화 추천 서비스 구현체
 *
 * <p>사용자의 피드백 데이터를 기반으로 선호도를 학습하고,
 * 검색 결과에 개인화된 순위를 적용합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizationServiceImpl implements PersonalizationService {

    /**
     * 개인화 적용 최소 피드백 수
     * <p>피드백이 이 수보다 적으면 콜드 스타트로 간주하여 개인화를 적용하지 않습니다.</p>
     */
    private static final int MIN_FEEDBACK_COUNT = 3;

    /**
     * 선호도 계산에 사용할 최대 피드백 수
     * <p>최신 피드백부터 이 수만큼만 사용하여 선호도를 계산합니다.</p>
     */
    private static final int MAX_FEEDBACK_COUNT = 20;

    /**
     * 개인화 점수 가중치
     * <p>최종 점수 = RRF 점수 + (코사인 유사도 × 이 가중치)</p>
     */
    private static final double PERSONALIZATION_WEIGHT = 0.3;

    private final BookRepository bookRepository;
    private final SearchFeedbackRepository feedbackRepository;

    @Override
    @Cacheable(
        value = "userPreferenceVectors",
        key = "#chatId",
        unless = "#result == null",
        cacheManager = "personalizationCacheManager"
    )
    public float[] calculateUserPreferenceVector(Long chatId) {
        log.info("[PERSONALIZATION] Calculating preference vector for chatId: {}", chatId);

        // 1. GOOD 피드백만 추출 (최신순)
        List<Long> likedBookIds = getLikedBookIds(chatId);

        // 2. 콜드 스타트 체크
        if (likedBookIds.size() < MIN_FEEDBACK_COUNT) {
            log.info("[PERSONALIZATION] Cold start for chatId: {} (feedback count: {})",
                chatId, likedBookIds.size());
            return null;
        }

        // 3. 임베딩 조회
        List<float[]> embeddings = bookRepository.findEmbeddingsByIds(likedBookIds);

        if (embeddings.isEmpty()) {
            log.warn("[PERSONALIZATION] No embeddings found for chatId: {}", chatId);
            return null;
        }

        // 4. 임베딩 수와 피드백 수가 다른 경우 로깅 (임베딩이 없는 도서가 있을 수 있음)
        if (embeddings.size() < likedBookIds.size()) {
            log.debug("[PERSONALIZATION] Some books have no embedding: {}/{}",
                embeddings.size(), likedBookIds.size());
        }

        // 5. 평균 벡터 계산
        float[][] embeddingArray = embeddings.toArray(new float[0][]);
        float[] preferenceVector = VectorUtils.averageVector(embeddingArray);

        log.info("[PERSONALIZATION] Preference vector calculated for chatId: {} (based on {} books)",
            chatId, embeddings.size());

        return preferenceVector;
    }

    @Override
    public List<BookSearchResponse> personalizedSearch(
        List<BookSearchResponse> results,
        Long chatId
    ) {
        // 1. 선호도 벡터 조회
        float[] preferenceVector = calculateUserPreferenceVector(chatId);

        // 2. 선호도 벡터가 없으면 기존 결과 반환
        if (preferenceVector == null) {
            log.debug("[PERSONALIZATION] No preference vector for chatId: {}, skipping personalization", chatId);
            return results;
        }

        log.info("[PERSONALIZATION] Applying personalization for chatId: {}", chatId);

        // 3. 각 도서별 코사인 유사도 계산 및 최종 점수 계산
        return results.stream()
            .map(response -> {
                // Book 엔티티에서 임베딩 조회
                return bookRepository.findById(response.getId())
                    .map(book -> {
                        float[] bookEmbedding = book.getEmbedding();
                        if (bookEmbedding == null) {
                            return response;
                        }

                        // 코사인 유사도 계산
                        double similarity = VectorUtils.calculateCosineSimilarity(preferenceVector, bookEmbedding);

                        // 최종 점수 계산: 기존 RRF 점수 + (유사도 × 가중치)
                        double rrfScore = response.getRrfScore() != null ? response.getRrfScore() : 0.0;
                        double finalScore = rrfScore + (similarity * PERSONALIZATION_WEIGHT);

                        // 점수 업데이트 (임시로 similarity 필드에 개인화 점수 저장)
                        response.setSimilarity(similarity);
                        response.setRrfScore(finalScore);

                        log.debug("[PERSONALIZATION] Book: {}, Similarity: {:.4f}, Final Score: {:.4f}",
                            response.getTitle(), similarity, finalScore);

                        return response;
                    })
                    .orElse(response);
            })
            .sorted((a, b) -> {
                double scoreA = a.getRrfScore() != null ? a.getRrfScore() : 0.0;
                double scoreB = b.getRrfScore() != null ? b.getRrfScore() : 0.0;
                return Double.compare(scoreB, scoreA); // 내림차순
            })
            .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(
        value = "userPreferenceVectors",
        key = "#chatId",
        cacheManager = "personalizationCacheManager"
    )
    public void evictUserPreferenceCache(Long chatId) {
        log.info("[PERSONALIZATION] Cache evicted for chatId: {}", chatId);
    }

    /**
     * 사용자가 좋아한 도서 ID 목록을 추출합니다.
     *
     * <p>GOOD 피드백만 필터링하고 중복을 제거하며,
     * 최신 피드백 순으로 제한된 수만큼 반환합니다.</p>
     *
     * @param chatId 사용자 chatId
     * @return 좋아한 도서 ID 목록
     */
    private List<Long> getLikedBookIds(Long chatId) {
        return feedbackRepository.findByChatIdOrderByCreatedAtDesc(chatId).stream()
            .filter(feedback -> feedback.getType() == FeedbackType.GOOD)
            .map(SearchFeedback::getBookId)
            .distinct() // 중복 제거
            .limit(MAX_FEEDBACK_COUNT)
            .collect(Collectors.toList());
    }
}
