package com.nhnacademy.library.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 *
 * <p>Spring Data Redis를 사용하기 위한 범용 설정입니다.
 * 캐시, 세션 저장소 등 다양한 용도로 사용할 수 있습니다.
 *
 * <p>Java 8 시간 타입(LocalDate, LocalDateTime, Instant 등)을 직렬화하기 위해
 * Jackson JavaTimeModule을 사용합니다.
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig {

    /**
     * RedisTemplate Bean 등록
     *
     * <p>Redis와의 상호작용을 위한 범용 RedisTemplate입니다.
     * Java 8 시간 타입 직렬화를 위해 JavaTimeModule이 포함된 ObjectMapper를 사용합니다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 직렬화 (String)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 직렬화 (JSON with JavaTimeModule)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
