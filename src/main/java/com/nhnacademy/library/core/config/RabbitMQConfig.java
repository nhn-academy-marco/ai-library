package com.nhnacademy.library.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
