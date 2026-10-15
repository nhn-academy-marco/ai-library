package com.nhnacademy.library.core.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReviewSummaryTask가 RabbitMQ JSON 컨버터로 직렬화/역직렬화되는지 검증
 */
@DisplayName("ReviewSummaryTask 직렬화 테스트")
class ReviewSummaryTaskSerializationTest {

    @Test
    @DisplayName("Jackson2JsonMessageConverter로 왕복 변환 시 bookId와 enqueueTime이 보존되어야 한다")
    void roundTrip_PreservesFields() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        ReviewSummaryTask task = new ReviewSummaryTask(42L);
        long originalEnqueueTime = task.getEnqueueTime();

        Message message = converter.toMessage(task, new MessageProperties());
        Object converted = converter.fromMessage(message);

        assertThat(converted).isInstanceOf(ReviewSummaryTask.class);
        ReviewSummaryTask restored = (ReviewSummaryTask) converted;
        assertThat(restored.getBookId()).isEqualTo(42L);
        assertThat(restored.getEnqueueTime()).isEqualTo(originalEnqueueTime);
    }

    @Test
    @DisplayName("발행 측이 기록한 enqueueTime이 역직렬화 후에도 유지되어야 한다")
    void deserialize_KeepsEnqueueTimeFromPayload() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("__TypeId__", ReviewSummaryTask.class.getName());
        Message message = new Message("{\"bookId\":7,\"enqueueTime\":1000}".getBytes(), properties);

        ReviewSummaryTask restored = (ReviewSummaryTask) converter.fromMessage(message);

        assertThat(restored.getBookId()).isEqualTo(7L);
        assertThat(restored.getEnqueueTime()).isEqualTo(1000L);
    }
}
