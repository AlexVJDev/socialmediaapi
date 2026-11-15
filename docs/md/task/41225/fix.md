Режим Ментора: ДА

# 41225 — API публикаций и ленты: вопросы

## Раунд 3
Ответы перенесены в decisions.md Р3-1…Р3-4.

<details><summary>вопросы раунда 3</summary>


Контекст (Р0-4): только порт `CurrentUserProvider { UUID currentUserId(); }`, без Keycloak. Для запуска приложения
нужна хотя бы одна реализация-бин; `default-id` и ID в JSON/query исключены.

**1. Временная реализация порта до Keycloak.**
Варианты: а) `HeaderCurrentUserProvider` — читает заголовок `X-User-Id` (UUID); нет или не UUID → `401 UNAUTHORIZED`. Заголовок — временная замена `Authorization: Bearer`, в DTO и тела не попадает; 
б) `UnsupportedCurrentUserProvider` — всегда `401`; работает только в тестах через `@MockitoBean`, вручную посты не создать.
**Рекомендация:** а) ручные тесты и MockMvc различают Алису и Бориса; с Keycloak меняется только реализация.
Ответ: Принимается

**2. Где лежит порт и реализация.**
Порт `CurrentUserProvider` — в `shared.security` (нужен posts и subscriptions); реализация — там же, `shared.security.HeaderCurrentUserProvider`. Код `CommonErrorCode.UNAUTHORIZED` + исключение `UnauthenticatedException` → `401` в `GlobalExceptionHandler`.
**Рекомендация:** так и сделать.
Ответ: Принимается

**3. `followerId` у ленты (Р1-1 заменено: теперь есть `CurrentUserProvider`).**
Варианты: а) убрать, лента текущего пользователя из порта, как в уроке; б) оставить обязательным, порт в ленте не использовать; в) необязательный, без него — из порта.
**Рекомендация:** а) по Р0-4 пользователь берётся только из порта; иначе любой читает чужую ленту.
Ответ: Принимается

</details>

## К сведению
- Порт используют: POST/PUT/DELETE постов, `GET /feed` (если в.3 = а), POST/DELETE `/subscribes` (Р0-3). friendships и GET-списки subscriptions не трогаем.
- В контракте заголовок описывается через `components.securitySchemes` (apiKey, header `X-User-Id`) — на сгенерированные интерфейсы не влияет.
- Сценарный MockMvc-тест передаёт `X-User-Id` Алисы/Бориса — `@MockitoBean` не нужен.
- Раунд 2: ответы перенесены в decisions.md Р2-1…Р2-3 (Р2-1, Р2-2 заменены Р0-2, Р0-3; Р0-2 заменено Р0-4).
- Раунд 1: ответы перенесены в decisions.md Р1-1…Р1-10; режим ментора — Р0-1.
