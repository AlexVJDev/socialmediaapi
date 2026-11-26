package ru.job4j.socialmediaapi.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Базовый класс интеграционных тестов с PostgreSQL: один контейнер на весь прогон (JVM),
 * сколько бы Spring-контекстов ни создали наследники ({@code @SpringBootTest}, {@code @E2ETest}, {@code @DataJpaTest}).
 *
 * <p>Контейнер запускается один раз при загрузке класса и не останавливается при закрытии контекста —
 * его удаляет Testcontainers (Ryuk) после завершения JVM. Параметры подключения Spring получает
 * через {@link #configureDataSource(DynamicPropertyRegistry)}; они заменяют {@code spring.datasource.*}
 * из {@code application.yml}. Liquibase применяет миграции в первом контексте, в остальных база уже актуальна.
 *
 * <p>База общая и не очищается: каждый тест работает со своими {@code UUID.randomUUID()}.
 */
public abstract class PostgreSqlIntegrationTest {

    /** Единственный контейнер PostgreSQL на прогон тестов. */
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17").withDatabaseName("social_media");

    static {
        POSTGRES.start();
    }

    /**
     * Передаёт Spring параметры подключения к контейнеру (Docker назначает случайный порт).
     *
     * @param registry реестр динамических свойств тестового контекста
     */
    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
