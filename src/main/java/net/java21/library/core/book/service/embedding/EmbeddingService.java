package net.java21.library.core.book.service.embedding;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * 임베딩 생성 서비스
 *
 * <p>검색어 임베딩({@link #getEmbedding(String)})은 같은 검색어가 반복될 때 임베딩 API를 다시 호출하지 않도록
 * Caffeine으로 캐시합니다. 캐시는 Spring 프록시({@code @Cacheable})가 아니라 이 클래스 안의 Caffeine 인스턴스로 처리하므로
 * 어디서 호출하든(같은 클래스 내부 호출 포함) 적용됩니다.</p>
 *
 * <p>도서 임베딩 배치({@link #getEmbeddings(List)}, {@code BookEmbeddingService})는 텍스트가 매번 달라 캐시하지 않습니다.</p>
 */
@Service
public class EmbeddingService {

    private static final String WHITESPACES = "\\s+";
    private static final String SINGLE_SPACE = " ";

    private final EmbeddingModel embeddingModel;
    private final Cache<String, float[]> queryEmbeddingCache;

    public EmbeddingService(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
                            @Value("${cache.query-embedding.ttl.minutes:60}") long ttlMinutes,
                            @Value("${cache.query-embedding.max-size:10000}") long maxSize) {
        this.embeddingModel = embeddingModel;
        this.queryEmbeddingCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }

    /**
     * 검색어 임베딩을 반환합니다. 정규화한 검색어(앞뒤 공백 제거, 소문자, 연속 공백 하나로)가 같으면 캐시된 벡터를 돌려줍니다.
     *
     * <p>캐시 미스 시에는 정규화 전 대소문자를 유지한 검색어(공백만 정리)로 임베딩합니다.
     * 빈 결과는 캐시하지 않습니다. 반환 배열은 캐시와 공유하므로 호출하는 쪽에서 수정하지 않습니다.</p>
     */
    public float[] getEmbedding(String text) {
        String key = normalizeKey(text);
        if (key == null) {
            return embedUncached(text);
        }
        float[] cached = queryEmbeddingCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        float[] result = embedUncached(text.strip().replaceAll(WHITESPACES, SINGLE_SPACE));
        if (result.length > 0) {
            queryEmbeddingCache.put(key, result);
        }
        return result;
    }

    /**
     * 여러 텍스트를 한 번에 임베딩합니다. (도서 임베딩 배치용, 캐시하지 않음)
     */
    public List<float[]> getEmbeddings(List<String> texts) {
        return embeddingModel.embed(texts);
    }

    /**
     * 검색어 임베딩 캐시 키: 앞뒤 공백 제거 → 소문자 → 연속 공백을 하나로. 내용이 없으면 null.
     */
    static String normalizeKey(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.strip().toLowerCase(Locale.ROOT).replaceAll(WHITESPACES, SINGLE_SPACE);
        return normalized.isEmpty() ? null : normalized;
    }

    private float[] embedUncached(String text) {
        float[] result = embeddingModel.embed(text);
        return result == null ? new float[0] : result;
    }
}
