# 41226 — Swagger UI для OpenAPI. План

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Пункты выполняет Claude по одному, по команде пользователя; пользователь проверяет (Р-9).

Суть: springdoc отдаёт только Swagger UI, а UI показывает наш контракт `docs/api/openapi.yaml`.
Контракт публикуется как статика `/openapi/**` прямо из `docs/api` при сборке, второй копии в репозитории нет (Р-5).
Описание `/v3/api-docs`, которое springdoc строит по контроллерам, остаётся включённым — без него не работает UI; UI его не использует (Р-10).

| Итого | Новые | Изменить |
|---|---|---|
| файлов | 2 (E2E-тест, `manual-test-curl.md`) | 3 (`pom.xml`, `application.yml`, `openapi.yaml`) |

Не меняются: `docs/api/modules/*.yaml` (входы генератора), `paths/`, `schemas/`, `responses/`, Java-код модулей.

## Где будут файлы

```
pom.xml                                         (изменить) springdoc + <resources>
src/main/resources/application.yml              (изменить) springdoc.*
docs/api/openapi.yaml                           (изменить) servers, bearerAuth

target/classes/static/openapi/                  (генерируется при сборке из docs/api)
├── openapi.yaml
├── paths/*.yaml
├── schemas/*.yaml
└── responses/*.yaml

src/test/java/ru/job4j/socialmediaapi/
└── OpenApiE2ETest.java                         (новый) статика контракта + Swagger UI

docs/md/task/41226/
└── manual-test-curl.md                         (новый) curl-скрипт + чек-лист для браузера
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | `pom.xml` | сборка | изменить: версия + зависимость | Р-1 |
| 2 | `pom.xml` | сборка | изменить: `<resources>` | Р-5 |
| 3 | `src/main/resources/application.yml` | конфигурация | изменить | Р-10, Р-4 |
| 4 | `docs/api/openapi.yaml` | контракт | изменить | Р-3, Р-6 |
| 5 | `src/test/java/ru/job4j/socialmediaapi/OpenApiE2ETest.java` | тест | новый | Р-7 |
| 6 | `docs/md/task/41226/manual-test-curl.md` | ручные тесты | новый | Р-8, Р-10 |

---

## 1. `pom.xml` — зависимость springdoc (Р-1)
**Зачем:** springdoc отдаёт Swagger UI (`/swagger-ui.html`) и его конфигурацию.

- В `<properties>`: `<springdoc.version>3.1.1</springdoc.version>`.
- В `<dependencies>`:
  ```xml
  <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>${springdoc.version}</version>
  </dependency>
  ```
- Проверка: `./mvnw compile` проходит. Если springdoc 3.1.1 несовместим с Boot 4.1.1, переходим на 3.2.0 (Р-1).

## 2. `pom.xml` — публикация контракта как статики (Р-5)
**Зачем:** UI загружает `/openapi/openapi.yaml`, а потом сам по HTTP подгружает относительные `$ref`
(`/openapi/paths/...` → `/openapi/schemas/...`, `/openapi/responses/...`).

- В `<build>` добавить блок `<resources>`. Он заменяет ресурсы по умолчанию, поэтому `src/main/resources` указывается явно:
  ```xml
  <resources>
      <resource>
          <directory>src/main/resources</directory>
      </resource>
      <resource>
          <directory>docs/api</directory>
          <targetPath>static/openapi</targetPath>
          <includes>
              <include>openapi.yaml</include>
              <include>paths/**</include>
              <include>schemas/**</include>
              <include>responses/**</include>
          </includes>
      </resource>
  </resources>
  ```
- `modules/` и `*.md` не копируются. `filtering` не включаем: файлы копируются как есть.
- Проверка: после `./mvnw process-resources` в `target/classes/static/openapi/` лежат `openapi.yaml` и три каталога.

## 3. `application.yml` — настройки springdoc (Р-10, Р-4)
**Зачем:** UI берёт наш файл вместо `/v3/api-docs`, демо-URL (petstore) не показывается.

```yaml
springdoc:
  swagger-ui:
    enabled: ${SWAGGER_UI_ENABLED:true}  # Р-4
    path: /swagger-ui.html
    url: /openapi/openapi.yaml
    disable-swagger-default-url: true
    try-it-out-enabled: true
```

- `springdoc.api-docs.enabled` не задаём (Р-10): при `false` springdoc отключает и `/v3/api-docs/swagger-config`,
  и `/swagger-ui.html` отдаёт 404.
- Проверка: `/swagger-ui.html` открывается, `/v3/api-docs/swagger-config` содержит `"url":"/openapi/openapi.yaml"`.

## 4. `docs/api/openapi.yaml` — `servers` и Bearer-схема (Р-3, Р-6)
**Зачем:** Try it out отправляет запросы на тот же хост; кнопка Authorize предлагает Bearer-токен.

- После `info` добавить:
  ```yaml
  servers:
    - url: /
      description: Текущий хост
  ```
- В `components.securitySchemes`, рядом с `userIdHeader`, добавить:
  ```yaml
  bearerAuth:
    type: http
    scheme: bearer
    bearerFormat: JWT
    description: JWT-токен (проверка появится вместе с Keycloak)
  ```
- Глобальный `security` **не** добавляем, у операций `security` не меняем (Р-6).
- `modules/*.yaml` не трогаем: генератор их читает, а `servers` и Bearer нужны только UI.
- Проверка: `./mvnw compile` проходит (генератор `openapi.yaml` не читает, но файл должен остаться валидным для UI).

## 5. `OpenApiE2ETest` — автотест (Р-7)
**Зачем:** ловит поломку копирования ресурсов (п. 2) и подключения UI (п. 1, 3).

- Пакет `ru.job4j.socialmediaapi` (тест не относится к модулю), аннотация `@E2ETest`
  (в проекте это мета-аннотация, а не базовый класс), `TestRestTemplate`.
- Тесты:
  1. `GET /openapi/openapi.yaml` → 200, тело содержит `/api/v1/friendship-offer/create`;
  2. `GET /openapi/paths/friendship-offer-create.yaml` → 200;
  3. `GET /swagger-ui.html` → 200 после редиректа на `/swagger-ui/index.html`.
     Если `TestRestTemplate` редирект не проходит, проверяем 302 и `Location` с `/swagger-ui/index.html`.
- Проверка: `./mvnw test -Dtest=OpenApiE2ETest` (нужен Docker).

## 6. Ручные тесты (Р-8)
**Зачем:** проверить то, что автотест не покрывает: отключение UI, `/v3/api-docs` и работу в браузере.

`manual-test-curl.md` — готовый sh-скрипт, выводит OK/FAIL по каждой проверке:
| Проверка | Ожидание |
|---|---|
| `GET /openapi/openapi.yaml` | 200, есть `/api/v1/friendship-offer/create`, `bearerAuth`, `servers` |
| `GET /openapi/schemas/friendship-offer.yaml` | 200 |
| `GET /swagger-ui.html` (`-L`) | 200 |
| `GET /v3/api-docs` | 200 — включён, UI его не использует (Р-10) |
| `POST /api/v1/friendship-offer/create` | 201 — тот же запрос, что делает Try it out |
| перезапуск с `SWAGGER_UI_ENABLED=false`: `/swagger-ui.html` | 404; `/openapi/openapi.yaml` — 200 (Р-4) |

Чек-лист для браузера (`http://localhost:8080/swagger-ui.html`):
- в адресной строке UI — `/openapi/openapi.yaml`, а не petstore и не `/v3/api-docs`;
- видны все разделы (friendships, subscriptions, posts), схемы раскрываются: значит, `$ref` подгрузились;
- Servers: `/`;
- Authorize показывает `userIdHeader` и `bearerAuth`;
- Try it out → create offer → 201.

Bruno не делаем: нового API нет (Р-8).

---

После выполнения: `./mvnw verify`, ручные тесты и коммит (делает пользователь).
`ApplicationModulesTest` не затрагивается (модули не меняются), но его можно прогнать по команде.
