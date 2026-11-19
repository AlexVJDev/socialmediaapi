# 41226 — заметки

## Описание урока (descr.pdf, «Swagger UI для OpenAPI» #505790)
PDF — скриншоты страницы, текст извлекается только из колонтитулов. Суть:
- зависимость `org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdoc.version}` (3.1.1);
- `<build><resources>`: `src/main/resources` + `docs/api` → `static/openapi`, include `openapi.yaml`;
- `springdoc.swagger-ui`: `path: /swagger-ui.html`, `url: /openapi/openapi.yaml`, `disable-swagger-default-url: true`,
  `try-it-out-enabled: true`, `enabled: ${SWAGGER_UI_ENABLED:true}`;
- в `openapi.yaml`: `servers` (`http://localhost:8080`), `components.securitySchemes.bearerAuth` (http/bearer/JWT),
  глобальный `security: - bearerAuth: []`;
- не дублировать контракт аннотациями `@Operation`/`@ApiResponse`/`@Schema`;
- задание: 1) зависимость; 2) копирование без второй копии; 3) url + отключить demo-URL; 4) проверить
  `/openapi/openapi.yaml` и `/swagger-ui.html`; 5) UI грузит наш файл, не `/v3/api-docs`, показывает API дружбы;
  6) Try it out — create offer; 7) Bearer + кнопка Authorize; 8) отключение переменной окружения; 9) коммит.

## Состояние проекта
- Spring Boot 4.1.1, Maven, `application.yml`; Spring Security нет.
- `docs/api/openapi.yaml` — многофайловый: `paths/*.yaml` → `../schemas/*.yaml`, `../responses/*.yaml`.
  `docs/api/modules/*.yaml` — входы генератора по модулям (дублируют пути), для UI не нужны.
- `securitySchemes.userIdHeader` (apiKey, `X-User-Id`) — в `openapi.yaml` и `modules/posts|subscriptions.yaml`;
  применяется в путях posts/feed/subscribes; friendships — без `security`.
- `servers` в контракте нет.
- E2E-база — `src/test/java/ru/job4j/socialmediaapi/E2ETest.java` (`RANDOM_PORT`, Testcontainers).

## Почему копировать каталог, а не один файл (вопрос 1)
Swagger UI (swagger-client) разрешает внешние относительные `$ref` HTTP-запросами относительно URL спецификации:
`/openapi/openapi.yaml` → `/openapi/paths/...` → `/openapi/schemas/...`. Значит, достаточно опубликовать
дерево `docs/api` (без `modules/`, `*.md`) с той же структурой.

## Проверка Р-2 (19.11.2026, springdoc 3.1.1)
- `api-docs.enabled: false` → `/swagger-ui.html` 404, `/v3/api-docs/swagger-config` 404 (UI не регистрируется).
- без этой настройки → `/swagger-ui.html` 302 → `/swagger-ui/index.html` 200; swagger-config:
  `"url":"/openapi/openapi.yaml"`, `"tryItOutEnabled":true` — UI грузит наш файл. Отсюда Р-10.
