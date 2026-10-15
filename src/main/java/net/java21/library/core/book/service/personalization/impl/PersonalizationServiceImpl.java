package net.java21.library.core.book.service.personalization.impl;

import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.repository.BookRepository;
import net.java21.library.core.book.service.personalization.PersonalizationService;
import net.java21.library.core.book.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * 개인화 점수 가중치
     * <p>최종 점수 = RRF 점수 + (코사인 유사도 × 이 가중치)</p>
     */
    @Value("${app.personalization.weight:0.3}")
    private double personalizationWeight;

    private final BookRepository bookRepository;
    private final UserPreferenceVectorCalculator userPreferenceVectorCalculator;

    @Override
    public float[] calculateUserPreferenceVector(Long chatId) {
        // 캐시가 적용되도록 별도 빈(프록시)을 통해 호출
        return userPreferenceVectorCalculator.calculate(chatId);
    }

    @Override
    public List<BookSearchResponse> personalizedSearch(
        List<BookSearchResponse> results,
        Long chatId
    ) {
        // 1. 선호도 벡터 조회
        float[] preferenceVector = userPreferenceVectorCalculator.calculate(chatId);

        // 2. 선호도 벡터가 없으면 기존 결과 반환
        if (preferenceVector == null) {
            log.debug("[PERSONALIZATION] No preference vector for chatId: {}, skipping personalization", chatId);
            return results;
        }

        log.info("[PERSONALIZATION] Applying personalization for chatId: {}", chatId);

        // 3. N+1 쿼리 방지: 한 번에 모든 임베딩 조회
        List<Long> bookIds = results.stream()
            .map(BookSearchResponse::getId)
            .collect(Collectors.toList());

        // IN 쿼리 결과는 입력 순서를 보장하지 않으므로 (id, embedding) 쌍으로 조회
        List<Object[]> rows = bookRepository.findIdAndEmbeddingsByIds(bookIds);

        // bookId와 임베딩을 매핑 (Map 생성)
        Map<Long, float[]> embeddingMap = new HashMap<>();
        for (Object[] row : rows) {
            embeddingMap.put((Long) row[0], (float[]) row[1]);
        }

        // 4. 각 도서별 코사인 유사도 계산 및 최종 점수 계산 (메모리 상에서 처리)
        return results.stream()
            .map(response -> {
                float[] bookEmbedding = embeddingMap.get(response.getId());
                if (bookEmbedding == null) {
                    return response;
                }

                // 코사인 유사도 계산
                double similarity = VectorUtils.calculateCosineSimilarity(preferenceVector, bookEmbedding);

                // 최종 점수 계산: 기존 RRF 점수 + (유사도 × 가중치)
                double rrfScore = response.getRrfScore() != null ? response.getRrfScore() : 0.0;
                double finalScore = rrfScore + (similarity * personalizationWeight);

                // 입력 객체는 시맨틱 캐시가 보관 중인 객체일 수 있으므로 직접 수정하지 않고 복사본에 점수를 적용
                // (원본을 수정하면 같은 캐시 결과로 검색할 때마다 점수가 누적됨)
                BookSearchResponse personalized = response.copy();
                personalized.setSimilarity(similarity);
                personalized.setRrfScore(finalScore);

                if (log.isDebugEnabled()) {
                    log.debug("[PERSONALIZATION] Book: {}, Similarity: {}, Final Score: {}",
                        response.getTitle(), String.format("%.4f", similarity), String.format("%.4f", finalScore));
                }

                return personalized;
            })
            .sorted((a, b) -> {
                double scoreA = a.getRrfScore() != null ? a.getRrfScore() : 0.0;
                double scoreB = b.getRrfScore() != null ? b.getRrfScore() : 0.0;
                return Double.compare(scoreB, scoreA); // 내림차순
            })
            .collect(Collectors.toList());
    }

    @Override
    public void evictUserPreferenceCache(Long chatId) {
        userPreferenceVectorCalculator.evict(chatId);
    }
}
