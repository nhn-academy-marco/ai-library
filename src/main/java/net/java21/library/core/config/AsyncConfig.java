package net.java21.library.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리를 위한 설정
 */
@Configuration
public class AsyncConfig {

    public static final String COVER_ENRICHMENT_EXECUTOR = "coverEnrichmentExecutor";
    private static final int COVER_POOL_SIZE = 2;
    private static final int COVER_QUEUE_CAPACITY = 50;

    public static final String HYBRID_SEARCH_EXECUTOR = "hybridSearchExecutor";
    private static final int HYBRID_SEARCH_POOL_SIZE = 8;
    private static final int HYBRID_SEARCH_QUEUE_CAPACITY = 100;

    /**
     * 기본 비동기 처리를 위한 Executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 도서 표지 보강 전용 Executor
     *
     * <p>외부 API 호출이 느려도 검색 요청 스레드로 넘어오지 않도록
     * 큐가 가득 차면 작업을 버립니다(CallerRunsPolicy를 쓰지 않음).
     * 버려진 ISBN은 조회 이력에 남지 않으므로 다음 노출 때 다시 시도합니다.</p>
     */
    @Bean(name = COVER_ENRICHMENT_EXECUTOR)
    public Executor coverEnrichmentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(COVER_POOL_SIZE);
        executor.setMaxPoolSize(COVER_POOL_SIZE);
        executor.setQueueCapacity(COVER_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("cover-enrich-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 하이브리드 검색의 키워드·벡터 후보 조회를 동시에 실행하는 전용 Executor
     *
     * <p>DB 조회 작업만 맡으므로 스레드 수를 커넥션 풀보다 작게 둡니다.
     * 큐가 가득 차면 검색 요청 스레드가 직접 실행(CallerRunsPolicy)해 작업을 버리지 않습니다.</p>
     */
    @Bean(name = HYBRID_SEARCH_EXECUTOR)
    public Executor hybridSearchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(HYBRID_SEARCH_POOL_SIZE);
        executor.setMaxPoolSize(HYBRID_SEARCH_POOL_SIZE);
        executor.setQueueCapacity(HYBRID_SEARCH_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("hybrid-search-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
