# Задание 06. Обработчик ошибок: исключения → `{code, message}`

**Зависит от:** задания 05.

## Цель

Превратить исключения в HTTP-ответы из схемы. После этого задания API отдаёт все ошибки создания заявки:

| Исключение | HTTP | `code` |
| --- | --- | --- |
| `MethodArgumentNotValidException` (нет поля) | `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` |
| `HttpMessageNotReadableException` (битый JSON, не UUID) | `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` |
| `FriendshipOfferToSelfException` | `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` |
| `FriendshipOfferAlreadyPendingException` | `409` | `FRIENDSHIP_OFFER_ALREADY_PENDING` |
| `FriendshipOfferAlreadyAcceptedException` | `409` | `FRIENDSHIP_OFFER_ALREADY_ACCEPTED` |
| `FriendshipOfferAlreadyRejectedException` | `409` | `FRIENDSHIP_OFFER_ALREADY_REJECTED` |
| `FriendshipOfferAlreadyExistsException` (сервис не смог прочитать существующую заявку) | `204` | — (без тела, случай логируется) |

## Контекст

Повтор заявки сервис из задания 04 сообщает исключением по статусу существующей заявки (`FriendshipOfferAlready{Pending,Accepted,Rejected}Exception`), поэтому все три `409` отдаёт этот обработчик, а не контроллер.

Тело ошибки по контракту: `{"code": "...", "message": "..."}`. Сообщения в ответе — на русском, как в примерах контракта; текст исключения (английский, с UUID) клиенту не отдаётся.

## Что сделать

1. `adapter.in.web.dto.ErrorResponse(String code, String message)`.
2. `adapter.in.web.error.ErrorCode` — enum с полями `HttpStatus status` и `String message` для кодов из таблицы. Имя константы enum = значение `code`.
3. `adapter.in.web.error.GlobalExceptionHandler` (`@RestControllerAdvice`) с `@ExceptionHandler`-методами по таблице; ответ собирается из `ErrorCode`.
   - Обработчики `MethodArgumentNotValidException` / `HttpMessageNotReadableException` сейчас относятся только к create. Когда появятся accept/reject, их код (`INVALID_FRIENDSHIP_OFFER_REQUEST`) совпадает — отдельная развилка не нужна, но это стоит указать комментарием.
4. Интеграционный тест `FriendshipOfferControllerTest`: `@SpringBootTest` + `@AutoConfigureMockMvc` (в Spring Boot 4 — пакет `org.springframework.boot.webmvc.test.autoconfigure`) + `@Import(TestcontainersConfiguration.class)`. Сценарии:
   - нет `toUserId` → `400`, `code = INVALID_FRIENDSHIP_OFFER_REQUEST`;
   - `fromUserId` не UUID → `400`, тот же код;
   - `fromUserId = toUserId` → `400`, тот же код, в `friendship_offer` нет строк для пользователя;
   - новая заявка → `201`, `status = PENDING`, в БД есть заявка и подписка Алиса → Борис;
   - повтор (параметризованный тест: статус заявки выставляется через `JdbcTemplate`) → `409`, в БД по-прежнему одна заявка с тем же статусом и одна подписка:
     - `PENDING` → `code = FRIENDSHIP_OFFER_ALREADY_PENDING`;
     - `ACCEPTED` → `code = FRIENDSHIP_OFFER_ALREADY_ACCEPTED`;
     - `REJECTED` → `code = FRIENDSHIP_OFFER_ALREADY_REJECTED`;
   - во всех ошибочных случаях данные в БД не изменились.
   В каждом тесте использовать новые `UUID.randomUUID()`, чтобы тесты не зависели друг от друга (контейнер общий).

## Критерии приёмки

- `./mvnw verify` зелёный (тесты + JaCoCo + checkstyle).
- Тела ответов соответствуют контракту: проверить ответы тестов против схемы в `friendship-offer-create.yaml`.
