package net.java21.library.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 설정
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.review-summary}")
    private String reviewSummaryQueueName;

    private static final String EXCHANGE = "nhnacademy-library-exchange";
    private static final String ROUTING_KEY = "review.summary";
    static final String DLQ_ROUTING_KEY = "review.summary.dlq";

    /**
     * 리뷰 요약 큐
     */
    @Bean
    public Queue reviewSummaryQueue() {
        return QueueBuilder.durable(reviewSummaryQueueName).build();
    }

    /**
     * Exchange (Direct 방식)
     */
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    /**
     * Queue와 Exchange 바인딩
     */
    @Bean
    public Binding binding(Queue reviewSummaryQueue, DirectExchange exchange) {
        return BindingBuilder.bind(reviewSummaryQueue)
                .to(exchange)
                .with(ROUTING_KEY);
    }

    /**
     * 리뷰 요약 실패 메시지 보관 큐 (DLQ)
     *
     * <p>기존 큐에 x-dead-letter-* 인자를 추가하면 이미 선언된 큐와 인자가 달라 재선언에 실패하므로,
     * 기존 큐는 그대로 두고 재시도 소진 시 {@link #messageRecoverer}가 이 큐로 재발행합니다.</p>
     */
    @Bean
    public Queue reviewSummaryDeadLetterQueue() {
        return QueueBuilder.durable(reviewSummaryQueueName + ".dlq").build();
    }

    /**
     * DLQ와 Exchange 바인딩
     */
    @Bean
    public Binding deadLetterBinding(Queue reviewSummaryDeadLetterQueue, DirectExchange exchange) {
        return BindingBuilder.bind(reviewSummaryDeadLetterQueue)
                .to(exchange)
                .with(DLQ_ROUTING_KEY);
    }

    /**
     * 재시도 소진 후 메시지 처리기
     *
     * <p>spring.rabbitmq.listener.simple.retry.* 설정으로 재시도한 뒤에도 실패하면
     * 원본 큐로 되돌리지 않고(무한 재전달 방지) 예외 정보를 헤더에 담아 DLQ로 재발행합니다.</p>
     */
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, EXCHANGE, DLQ_ROUTING_KEY);
    }

    /**
     * JSON 메시지 컨버터
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON 컨버터
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
