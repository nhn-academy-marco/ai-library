package net.java21.library.core.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 설정(src/main/resources/application.properties)의 DBCP2 풀 설정이 BasicDataSource에 실제로 바인딩되는지 확인합니다.
 * (테스트 클래스패스의 application.properties가 운영 설정을 가리므로 파일을 직접 읽습니다. DB에는 연결하지 않습니다.)
 */
class DataSourcePoolPropertiesTest {

    private static final String MAIN_PROPERTIES = "src/main/resources/application.properties";

    @Test
    @DisplayName("풀 크기 20, 대여 시 검사 대신 유휴 검사(30초 주기), 최대 대기 5초가 바인딩된다")
    void dbcp2PoolSettings() throws Exception {
        Properties properties = PropertiesLoaderUtils.loadProperties(new FileSystemResource(MAIN_PROPERTIES));
        Binder binder = new Binder(new MapConfigurationPropertySource(properties));

        BasicDataSource dataSource = new BasicDataSource();
        binder.bind("spring.datasource.dbcp2", Bindable.ofInstance(dataSource));

        assertThat(dataSource.getMaxTotal()).isEqualTo(20);
        assertThat(dataSource.getMinIdle()).isEqualTo(5);
        assertThat(dataSource.getTestOnBorrow()).isFalse();
        assertThat(dataSource.getTestWhileIdle()).isTrue();
        assertThat(dataSource.getValidationQuery()).isEqualTo("SELECT 1");
        assertThat(dataSource.getDurationBetweenEvictionRuns()).isEqualTo(Duration.ofSeconds(30));
        assertThat(dataSource.getMaxWaitDuration()).isEqualTo(Duration.ofSeconds(5));
        assertThat(dataSource.getMaxConnDuration()).isEqualTo(Duration.ofMinutes(30));
        assertThat(dataSource.getValidationQueryTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
    }
}
