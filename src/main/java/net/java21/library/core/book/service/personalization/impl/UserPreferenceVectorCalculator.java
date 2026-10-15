package net.java21.library.core.book.service.personalization.impl;

import net.java21.library.core.book.repository.BookRepository;
import net.java21.library.core.book.util.VectorUtils;
import net.java21.library.external.telegram.dto.FeedbackType;
import net.java21.library.external.telegram.entity.SearchFeedback;
import net.java21.library.external.telegram.repository.SearchFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 선호도 벡터 계산 및 캐싱 담당 컴포넌트
 *
 * <p>{@link PersonalizationServiceImpl} 내부에서 직접 호출하면 프록시를 거치지 않아
 * {@code @Cacheable}이 적용되지 않으므로(self-invocation), 캐시 대상 메서드를 별도 빈으로 분리했습니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserPreferenceVectorCalculator {

    /**
     * 개인화 적용 최소 피드백 수
     * <p>피드백이 이 수보다 적으면 콜드 스타트로 간주하여 개인화를 적용하지 않습니다.</p>
     */
    @Value("${app.personalization.min-feedback-count:3}")
    private int minFeedbackCount;

    /**
     * 선호도 계산에 사용할 최대 피드백 수
     * <p>최신 피드백부터 이 수만큼만 사용하여 선호도를 계산합니다.</p>
     */
    @Value("${app.personalization.max-feedback-count:20}")
    private int maxFeedbackCount;

    private final BookRepository bookRepository;
    private final SearchFeedbackRepository feedbackRepository;

    @Cacheable(
        value = "userPreferenceVectors",
        key = "#chatId",
        unless = "#result == null",
        cacheManager = "personalizationCacheManager"
    )
    public float[] calculate(Long chatId) {
        log.info("[PERSONALIZATION] Calculating preference vector for chatId: {}", chatId);

        // 1. GOOD 피드백만 추출 (최신순)
        List<Long> likedBookIds = getLikedBookIds(chatId);

        // 2. 콜드 스타트 체크
        if (likedBookIds.size() < minFeedbackCount) {
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

    @CacheEvict(
        value = "userPreferenceVectors",
        key = "#chatId",
        cacheManager = "personalizationCacheManager"
    )
    public void evict(Long chatId) {
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
            .limit(maxFeedbackCount)
            .collect(Collectors.toList());
    }
}
