# 4123 — заметки анализа

Урок: сервис + контроллер `POST /api/offer-friendships/create` поверх JPA-репозитория и интеграционный тест
контроллера (`@SpringBootTest` + MockMvc) на общем PostgreSQL-контейнере (базовый класс `PostgreSqlIntegrationTest`).

## Что у нас уже есть
| Урок | У нас |
|---|---|
| `OfferFriendshipService.createOfferFriendship` | `CreateFriendshipOffer` (+ подписка, ошибки по статусу заявки пары) |
| `OfferFriendshipController` | `FriendshipOfferController` (реализует сгенерированный `FriendshipOfferApi`) |
| обработчики в контроллере | `FriendshipOfferExceptionHandler` (advice модуля) |
| `OfferFriendshipControllerIntegrationTest` (2 теста) | `FriendshipOfferControllerTest` (MockMvc + контейнер, ~20 тестов) и `FriendshipOfferE2ETest` |
| проверка в БД через Spring Data JPA | через `JdbcTemplate` |

## Расхождения API
| | Урок | У нас (контракт `docs/api`) |
|---|---|---|
| путь | `/api/offer-friendships/create` | `/api/v1/friendship-offer/create` |
| отправитель | `fromUserId` в теле | заголовок `X-User-Id` |
| повтор, заявка PENDING | 204 | 200 `FRIENDSHIP_OFFER_ALREADY_PENDING` |
| повтор, заявка ACCEPTED | 204 | 409 `FRIENDSHIP_OFFER_ALREADY_ACCEPTED` |
| повтор, заявка REJECTED | 409 `FRIEND_REQUEST_REJECTED` | 409 `FRIENDSHIP_OFFER_ALREADY_REJECTED` |
| заявка самому себе | 409 `SELF_FRIEND_REQUEST` | 400 `VALIDATION_ERROR` |
| встречная заявка | новая заявка (уникальна направленная пара) | ошибка по статусу заявки пары (Р1-4) |

## Контейнеры в тестах сейчас
- `TestcontainersConfiguration` (`@Bean @ServiceConnection PostgreSQLContainer`) — свой контейнер на каждый
  Spring-контекст. Разных контекстов несколько (`@SpringBootTest` + MockMvc, `@E2ETest` с другими часами,
  `@DataJpaTest`, тесты адаптеров) → несколько контейнеров за прогон.
- `FriendshipOfferPersistenceAdapterTest` — свой `@Container` (ещё один контейнер на класс).
- Урок: один контейнер на весь прогон (singleton), тесты не чистят базу — у каждого свои UUID.

## Время
Урок усекает `Instant.now()` до микросекунд, чтобы JSON-ответ совпал с БД. У нас ответ строится из строки,
перечитанной из БД после вставки (`findById`), — совпадает без усечения.
