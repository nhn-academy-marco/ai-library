package net.java21.library.core.book.service.search;

/**
 * 한 검색 요청 안에서 단계별 소요 시간을 넘겨주기 위한 스레드 범위 저장소
 *
 * <p>질의 임베딩은 {@code BookSearchService}가, 키워드·벡터·RRF는 {@code HybridSearchStrategy}가 수행하므로
 * 하이브리드 검색의 단계별 시간을 한 줄 로그로 남기려면 임베딩 시간을 전략까지 전달해야 합니다.
 * 같은 요청 스레드에서 기록·조회하고, 기록한 쪽이 끝날 때 {@link #clear()}로 지웁니다.</p>
 */
public final class SearchTimings {

    /** 이번 요청에서 임베딩을 생성하지 않았음을 나타내는 값 */
    public static final long NOT_MEASURED = -1L;

    private static final ThreadLocal<Long> EMBEDDING_MILLIS = new ThreadLocal<>();

    private SearchTimings() {
    }

    public static void recordEmbedding(long millis) {
        EMBEDDING_MILLIS.set(millis);
    }

    /**
     * 이번 요청의 질의 임베딩 소요 시간(ms). 측정하지 않았으면 {@link #NOT_MEASURED}.
     */
    public static long embeddingMillis() {
        Long millis = EMBEDDING_MILLIS.get();
        return millis == null ? NOT_MEASURED : millis;
    }

    public static void clear() {
        EMBEDDING_MILLIS.remove();
    }
}
