# CLAUDE.md
CLAUDE.md комитить не нужно.
Никогда не производи commit изменений. Это должен делать только пользователь.

Отвечай пользователю на русском языке (пользователь может писать и на английском). Документация и Javadoc в проекте — на русском.

## Главные правила работы над задачей (действуют всегда)
1. **Источник правды — `docs/md/<task-number>/decisions.md`.** Порядок приоритета:
   последнее указание пользователя в чате → `decisions.md` → код пользователя в ветке → `fix.md` / `Readme*.md` / `task-*.md`.
   Если черновики (fix, Readme, task-manual) противоречат `decisions.md` — они устарели, а не код.
2. **Любое указание или исправление пользователя в чате** сразу записывается в `decisions.md`
   (новая строка или статус «заменено Р-x» у старой). В ответе — одна строка: «записал как Р-…».
   Если правило общее для проекта, а не для задачи, — предложить сохранить его в memory.
3. **Код, написанный пользователем, не «чинить» обратно по плану.** Расхождение кода с `fix.md`/`task-*.md` —
   это вопрос пользователю, а не замечание к исправлению. Код-ревью сверяется с `decisions.md`.
4. **Устаревшие документы** (переделанный план, старые task-manual) переносятся в `docs/md/<task-number>/archive/`
   и не используются как основание для решений.
5. **Расхождение с образцом** (параметры, формат ответа, обёртка, структура) — отдельный вопрос с рекомендацией,
   а не молчаливый выбор другого образца.
6. Процесс задачи (fix.md → decisions.md → task-manual → ручные тесты) описан в skill `/task-workflow`.

## Ограничения
- Общие файлы README.md, docs/architecture.md, *.kts не редактируем без отдельной команды.
- `var` — только в тестовых классах. В новом коде и примерах — явный тип.
- Тесты минимальные: архитектура + happy-path нового функционала.
- В код, Javadoc и пояснения не вставлять ID запросов ESB (вида «ESB-SYNCH-LTEMPL-ххх»).
- `ApplicationModulesTest` запускать только по команде пользователя; можно напоминать после перемещения классов,
  окончания доработки или рефакторинга.

## Команды
```bash
./gradlew build                                   # сборка + тесты + checkstyle
./gradlew test                                    # все тесты (нужен запущенный Docker — Testcontainers)
./gradlew :test --tests "uz.kapitalbank.loanlineapplications.architecture.ApplicationModulesTest"
./gradlew :test --tests "*LoanLineApplicationSigningServiceTest.someMethod"
# с --tests используйте :test — иначе фильтр применится и к external-clients и сборка упадёт с "No tests found"
./gradlew checkstyleMain checkstyleTest           # конфиг: checkstyle/checkstyle.xml
./gradlew :external-clients:api:build :external-clients:spring-boot-starter:build

docker compose up -d                              # Postgres :5432, Temporal :7233 (UI :8233), WireMock :8443
./gradlew bootRun --args='--spring.profiles.active=local'
```

Swagger локально: http://localhost:8080/swagger-ui/index.html. API-сценарии для IntelliJ/Amplicode Connekt — `scripts/connekt` (окружение `local`).

## Стек
Java 21, Spring Boot 3.4, Spring Modulith, Temporal, JPA + Flyway (PostgreSQL), Kafka, OpenFeign + Resilience4j, MapStruct, Lombok, Keycloak/OAuth2 resource server. Логирование — Log4j2 (spring-boot-starter-logging исключён).

## Архитектура
Модульный монолит на Spring Modulith. Модули — прямые подпакеты `uz.kapitalbank.loanlineapplications`: `applications` (основной), `loanlines`, `loantemplates`, `documents`, `stopfactors`, `currencies`, `durablesteps`, `dictionaries`. Допустимые межмодульные зависимости — в `package-info.java` модуля (`@ApplicationModule(allowedDependencies = ...)`); новую зависимость прописывать там.
Архитектурный стиль: DDD + гексагональная структура.

**Перед изменением структуры модулей прочитай `docs/architecture.md`.** Кратко:
- `<module>/api` — публичный контракт (интерфейсы, модели, события, ошибки); другие модули обращаются только к нему.
- `<module>/internal/domain` → `internal/application` (use cases, сервисы, порты) → `internal/infrastructure` (web, persistence, integration, temporal, event-листенеры, config). Domain не зависит от application/infrastructure; application работает с портами; контроллеры не ходят в persistence напрямую.
- Типы событий для `@ApplicationModuleListener` — всегда в `<module>.api.event`.
- Для эндпоинтов `/internal/**` контроллерные тесты по умолчанию не пишутся.

Правила проверяет `architecture/ApplicationModulesTest` (Modulith + Spoon).

**Legacy-код.** Пакет `shared` и часть тестов в корневых пакетах (`service/impl`, `client`, `controller`, …) — наследие до перехода на модули, постепенно переносится (коммиты `Refactor-N`). `shared` не зависит от бизнес-модулей; legacy-код обращается к модулям только через `api`.

**Статусная модель заявки** (PREPARING → STOP_FACTOR_* → COORDINATION → LOAN_DEAL_PREPARING → SIGNING_BY_BANK_* → SIGNING_BY_CLIENT_* → SIGNED → COMPLETE, с переходами в REJECTED) — в `README.md`. Недопустимый переход → `InvalidStatusTransitionException`.

**Долгие процессы.** Проверка стоп-факторов (`applications/internal/infrastructure/temporal/stopfactors`) и протокол подписания (`documents/internal/infrastructure/temporal`) — Temporal workflow. Общие настройки activity и retry — `shared/temporal`. `durablesteps` (`DurableSteps.run(StepKey, Class, supplier)`) сохраняет результаты шагов в `durable_step` (JSONB); контракт — в Javadoc `DurableSteps`.

**Внешние интеграции.** Gradle-подмодули `external-clients/api` (контракты, DTO) и `external-clients/spring-boot-starter` (Feign-бины, автоконфигурация, ключи `client-properties.*`), пакет `uz.kapitalbank.integration`, от приложения не зависят. Бизнес-адаптеры, маппинг, retry/CircuitBreaker и прикладные исключения — в `integration/` модулей приложения. Описание — `external-clients/README.md`.

**БД.** Flyway-миграции — `src/main/resources/db/migration`, `V<N>__описание.sql` (следующий номер после последнего). Тестовые данные для dev — `db/dev`.

## Тесты
- Интеграционные тесты наследуют `IntegrationTest` (`@SpringBootTest` + Testcontainers: Postgres, Kafka, Keycloak; `TestSecurityConfiguration`).
- HTTP-клиенты — через WireMock (`AbstractFeignClientTest`); ожидаемые JSON — в `src/test/resources/<область>/...`, сравнение через json-unit.
- Temporal — `temporal-testing`.

## Git
Ветки `feature/UB-<номер>`, базовая ветка — `dev`. Сообщения коммитов: `UB-12689 <описание>`.
