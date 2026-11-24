# 4122 — заметки анализа

## Урок vs проект

| Что | Урок 4122 | У нас сейчас |
|---|---|---|
| доступ к БД | Spring Data JPA | `JdbcTemplate`, нативный SQL, record `*Row` + `RowMapper` |
| пакеты | `ru.job4j.socialmedia.domain`, `.repository` | модуль `friendships`: `internal/domain`, `internal/application/port`, `internal/infrastructure/persistence` |
| enum статуса | новый `OfferFriendshipStatus` | доменный `FriendshipOfferStatus` (PENDING/ACCEPTED/REJECTED) |
| id | `@GeneratedValue(UUID)` | задаёт домен `FriendshipOffer.create` |
| создание | `ON CONFLICT (from_user_id, to_user_id) DO NOTHING`, 0/1 | `INSERT … RETURNING` в savepoint (`NESTED`), дубль → `FriendshipOfferAlreadyExistsException` по имени constraint `uq_friendship_offer_pair` |
| уникальность | направленная пара | неупорядоченная пара (009, индекс `LEAST/GREATEST`) — `ON CONFLICT` с целью из урока не сработает |
| репозиторий | `JpaRepository` | порт `FriendshipOfferRepository` (8 методов) + адаптер |
| тест | `@DataJpaTest`, `PostgreSqlIntegrationTest` | `@SpringBootTest` + `@Testcontainers`, 21 тест через порт |
| зависимости | data-jpa, data-jpa-test, Lombok | starter-jdbc; Lombok и MapStruct уже подключены |

## Почему уходим от savepoint
Hibernate после исключения при flush помечает сессию непригодной; savepoint в `JpaTransactionManager` работает
только для JDBC-операций. `ON CONFLICT DO NOTHING` не порождает ошибку — транзакция остаётся рабочей,
и `CreateFriendshipOffer` по-прежнему читает существующую заявку через `findByPair`.

## Методы порта → JPA
| Метод порта | Реализация |
|---|---|
| `createFriendshipOffer` | нативный `INSERT … ON CONFLICT DO NOTHING` → 0 ⇒ исключение; 1 ⇒ `findById` |
| `findByPair` (FOR UPDATE) | JPQL `where (from=a and to=b) or (from=b and to=a)` + `@Lock(PESSIMISTIC_WRITE)` |
| `updateStatusIfPending` | `@Modifying(clearAutomatically = true)` JPQL `update … where id, toUserId, status = PENDING` → 1 ⇒ `findById` |
| `findByIdAndRecipient` | производный `findByIdAndToUserId` |
| `findById` | `findById` |
| `findPage` / `findPageAfter` | 4 нативных запроса (incoming/outgoing × первая/после курсора), `limit + 1` строк |

Все запросы ограничены ключом/владельцем (`id`, `to_user_id`, `from_user_id`) — правило «никаких запросов по всей таблице».
