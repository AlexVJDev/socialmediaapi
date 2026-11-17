# 41226 — журнал решений

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р-1 | springdoc: `org.springdoc:springdoc-openapi-starter-webmvc-ui` версии **3.1.1** (как в уроке; собран на Spring Boot 4.1.0, совместим с нашим 4.1.1). 3.2.0 — только если сборка с 3.1.1 упадёт. | чат 2026-11-17 | принято |
| Р-2 | `/v3/api-docs` отключаем (`springdoc.api-docs.enabled: false`) — контракт только `docs/api/openapi.yaml`. Если после этого `/swagger-ui.html` не открывается или не грузит наш `url` — оставляем включённым. | fix.md в.4 + чат 2026-11-17 | принято (проверить при запуске) |
| Р-3 | `servers` в `openapi.yaml`: `url: /` (запросы Try it out идут на хост, с которого открыт UI), вместо `http://localhost:8080` из урока. | fix.md в.3 + чат 2026-11-17 | принято |
| Р-4 | Отключение UI — как в уроке: только `springdoc.swagger-ui.enabled: ${SWAGGER_UI_ENABLED:true}`; статический `/openapi/openapi.yaml` остаётся доступен (контракт не секрет). | fix.md в.5 + чат 2026-11-17 | принято |
| Р-5 | Публикация контракта: `openapi.yaml` + `paths/`, `schemas/`, `responses/` копируются в `static/openapi` с сохранением структуры (`modules/` и `*.md` — нет); Swagger UI сам подгружает относительные `$ref`. Исходник один — `docs/api`. | fix.md в.1 + чат 2026-11-17 | принято |
| Р-6 | Bearer: `bearerAuth` добавляется в `components.securitySchemes` `openapi.yaml`, глобальный `security` не задаётся (операции не меняются). | fix.md в.2 + чат 2026-11-17 | принято |
| Р-7 | Автотест: один E2E-класс (наследник `E2ETest`): `GET /openapi/openapi.yaml` → 200 и содержит `/api/v1/friendship-offer/create`; `GET /openapi/paths/friendship-offer-create.yaml` → 200; `GET /swagger-ui.html` → 200 (после редиректа). | fix.md в.6 + чат 2026-11-17 | принято |
| Р-8 | Ручные тесты: `manual-test-curl.md` (файл, UI, `/v3/api-docs`, create offer) + чек-лист в браузере (Authorize, Try it out); Bruno не делаем. | fix.md в.7 + чат 2026-11-17 | принято |
| Р-9 | Код пишет Claude по пунктам плана, по одному, по команде пользователя; пользователь проверяет (как в 41224). | fix.md в.8 + чат 2026-11-17 | принято |
