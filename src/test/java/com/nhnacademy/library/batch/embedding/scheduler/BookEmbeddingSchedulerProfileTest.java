package com.nhnacademy.library.batch.embedding.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

class BookEmbeddingSchedulerProfileTest {

    @SpringBootTest
    @ActiveProfiles("default") // 기본 프로필 명시
    @Nested
    class DefaultProfileTest {
        @Autowired
        private ApplicationContext applicationContext;

        @Test
        @DisplayName("기본 프로필에서는 BookEmbeddingScheduler 빈이 등록되지 않아야 한다")
        void testSchedulerDisabledInDefaultProfile() {
            boolean beanExists = applicationContext.containsBean("bookEmbeddingScheduler");
            assertThat(beanExists).isFalse();
        }
    }

    @SpringBootTest
    @ActiveProfiles("prod")
    @Nested
    class ProdProfileTest {
        @Autowired
        private ApplicationContext applicationContext;

        @Test
        @DisplayName("prod 프로필에서는 BookEmbeddingScheduler 빈이 등록되어야 한다")
        void testSchedulerEnabledInProdProfile() {
            boolean beanExists = applicationContext.containsBean("bookEmbeddingScheduler");
            assertThat(beanExists).isTrue();
        }
    }
}
