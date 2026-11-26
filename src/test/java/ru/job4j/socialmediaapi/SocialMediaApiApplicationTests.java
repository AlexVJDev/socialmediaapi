package ru.job4j.socialmediaapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.job4j.socialmediaapi.support.PostgreSqlIntegrationTest;

@SpringBootTest
@DisplayName("Приложение: запуск контекста")
class SocialMediaApiApplicationTests extends PostgreSqlIntegrationTest {

    @Test
    @DisplayName("Spring-контекст приложения поднимается без ошибок")
    void contextLoads() {
    }
}
