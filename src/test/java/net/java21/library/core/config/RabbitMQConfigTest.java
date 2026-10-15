package net.java21.library.core.config;

import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RabbitMQ 재시도/DLQ 설정 검증 (브로커 연결 없이 빈 구성만 확인)
 */
@DisplayName("RabbitMQConfig 재시도/DLQ 설정 테스트")
class RabbitMQConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
        .withUserConfiguration(RabbitMQConfig.class)
        .withPropertyValues(
            "rabbitmq.queue.review-summary=test-review",
            "spring.rabbitmq.listener.simple.retry.enabled=true",
            "spring.rabbitmq.listener.simple.retry.max-attempts=3",
            "spring.rabbitmq.listener.simple.default-requeue-rejected=false"
        );

    @Test
    @DisplayName("기존 큐는 인자 없이 유지하고, DLQ와 DLQ 바인딩을 선언해야 한다")
    void declaresDeadLetterQueue() {
        contextRunner.run(context -> {
            Queue main = context.getBean("reviewSummaryQueue", Queue.class);
            Queue dlq = context.getBean("reviewSummaryDeadLetterQueue", Queue.class);
            Binding binding = context.getBean("deadLetterBinding", Binding.class);

            assertThat(main.getArguments()).isEmpty();
            assertThat(dlq.getName()).isEqualTo("test-review.dlq");
            assertThat(dlq.isDurable()).isTrue();
            assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.DLQ_ROUTING_KEY);
        });
    }

    @Test
    @DisplayName("리스너 컨테이너에 재시도 인터셉터가 적용되고, 소진 시 DLQ로 재발행해야 한다")
    void listenerFactoryHasRetryAndRecoverer() {
        contextRunner.run(context -> {
            assertThat(context.getBean(MessageRecoverer.class)).isInstanceOf(RepublishMessageRecoverer.class);

            SimpleRabbitListenerContainerFactory factory =
                context.getBean("rabbitListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);
            Advice[] adviceChain = factory.getAdviceChain();
            assertThat(adviceChain).isNotEmpty();
            assertThat(ReflectionTestUtils.getField(factory, "defaultRequeueRejected")).isEqualTo(false);
        });
    }
}
