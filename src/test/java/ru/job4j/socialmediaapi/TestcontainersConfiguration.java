package ru.job4j.socialmediaapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Общий PostgreSQL для всех интеграционных тестов: один контейнер на весь прогон (JVM),
 * сколько бы Spring-контекстов ни создали тесты ({@code @SpringBootTest}, {@code @E2ETest}, {@code @DataJpaTest}).
 *
 * <p>Контейнер запускается один раз при загрузке класса. Бин возвращает тот же экземпляр,
 * {@code destroyMethod = ""} не даёт закрытию контекста остановить общий контейнер — его останавливает
 * Testcontainers (Ryuk) после завершения JVM. Liquibase применяет миграции в первом контексте,
 * в остальных база уже актуальна.
 *
 * <p>База общая и не очищается: каждый тест работает со своими {@code UUID.randomUUID()}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** Единственный контейнер PostgreSQL на прогон тестов. */
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }

    /**
     * @return общий запущенный контейнер; параметры подключения Spring Boot берёт через {@link ServiceConnection}
     */
    @Bean(destroyMethod = "")
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
