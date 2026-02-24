package com.nhnacademy.library.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

/**
 * Jackson ObjectMapper 설정
 *
 * JSON 직렬화/역직렬화를 위한 ObjectMapper 빈 설정
 */
@Configuration
public class ObjectMapperConfig {

    /**
     * 기본 ObjectMapper 빈 (JSON용)
     *
     * @return 설정된 ObjectMapper
     */
    @Bean("customObjectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 직렬화 설정
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // pretty print
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // 역직렬화 설정
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // 알 수 없는 속성 무시
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        return objectMapper;
    }
}
