package com.nhnacademy.library.batch.init.properties;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "init")
public class InitProperties {
    private String bookFile;
    private boolean enable;
    private int batchSize;
}