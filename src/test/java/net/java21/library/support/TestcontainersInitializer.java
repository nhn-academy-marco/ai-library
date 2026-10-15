package net.java21.library.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 모든 Spring Context 테스트에 일회용 PostgreSQL(pgvector)·RabbitMQ 컨테이너를 연결합니다.
 *
 * <p>테스트 설정이 원격 공용 DB와 운영 RabbitMQ에 연결되면, 스키마가 지워지거나 운영 큐의 메시지를
 * 테스트 앱이 소비하는 사고가 납니다. 이 초기화기는 src/test/resources/META-INF/spring.factories 로
 * 등록되어 별도 애노테이션 없이 모든 {@code @SpringBootTest}에 적용됩니다.</p>
 *
 * <p>컨테이너는 JVM당 한 번만 띄우고(싱글턴) 여러 테스트 컨텍스트가 공유합니다.</p>
 */
public class TestcontainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");
    private static final DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:3.13-alpine");

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("library_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/init-test.sql");

    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(RABBITMQ_IMAGE);

    static {
        POSTGRES.start();
        RABBITMQ.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRES.getJdbcUrl() + "&stringtype=unspecified",
                "spring.datasource.username=" + POSTGRES.getUsername(),
                "spring.datasource.password=" + POSTGRES.getPassword(),
                "spring.rabbitmq.host=" + RABBITMQ.getHost(),
                "spring.rabbitmq.port=" + RABBITMQ.getAmqpPort(),
                "spring.rabbitmq.username=" + RABBITMQ.getAdminUsername(),
                "spring.rabbitmq.password=" + RABBITMQ.getAdminPassword()
        ).applyTo(context.getEnvironment());
    }
}
