package ru.job4j.socialmediaapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import ru.job4j.socialmediaapi.support.PostgreSqlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E публикации контракта и Swagger UI: сборка копирует {@code docs/api} в {@code static/openapi},
 * приложение отдаёт контракт как статику {@code /openapi/**}, а springdoc — страницу Swagger UI.
 * Лежит в корневом пакете: проверяет конфигурацию приложения, а не модуль.
 */
@E2ETest
@DisplayName("E2E: контракт OpenAPI и Swagger UI")
class OpenApiE2ETest extends PostgreSqlIntegrationTest {

    /** HTTP-клиент, привязанный к случайному порту запущенного приложения. */
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /openapi/openapi.yaml — 200, контракт содержит API дружбы")
    void whenGetContractThenOkWithFriendshipApi() {
        var response = restTemplate.getForEntity("/openapi/openapi.yaml", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("/api/v1/friendship-offer/create");
    }

    @Test
    @DisplayName("GET /openapi/paths/friendship-offer-create.yaml — 200: файлы $ref опубликованы рядом с контрактом")
    void whenGetReferencedPathThenOk() {
        var response = restTemplate.getForEntity("/openapi/paths/friendship-offer-create.yaml", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /openapi/** — Cache-Control: no-cache: браузер не держит старый контракт после изменений")
    void whenGetContractThenNoCache() {
        var response = restTemplate.getForEntity("/openapi/paths/friendship-offer-create.yaml", String.class);

        assertThat(response.getHeaders().getCacheControl()).contains("no-cache");
    }

    @Test
    @DisplayName("GET /swagger-ui.html — 200 после редиректа на страницу Swagger UI")
    void whenGetSwaggerUiThenOk() {
        var response = restTemplate.getForEntity("/swagger-ui.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Swagger UI");
    }
}
