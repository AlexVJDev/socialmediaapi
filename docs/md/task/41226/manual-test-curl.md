# 41226 — ручная проверка через curl

Проверяет публикацию контракта и Swagger UI на запущенном приложении (Р-8):
контракт и его `$ref`-файлы отдаются как статика `/openapi/**` (Р-5), в контракте есть `servers` и `bearerAuth`
(Р-3, Р-6), Swagger UI открывается и настроен на наш файл (Р-10), запрос Try it out «создать предложение
дружбы» работает, UI отключается переменной `SWAGGER_UI_ENABLED` (Р-4). В конце печатает итог `OK`/`FAIL`.

Нужны `curl`, `jq`, `uuidgen`. Id пользователей каждый раз новые — скрипт можно запускать повторно.

## Подготовка

```shell
docker compose up -d postgres
./mvnw clean spring-boot:run          # в отдельном терминале, http://localhost:8080
```

`clean` нужен, если контракт менялся: UI показывает копию из `target/classes/static/openapi`.

## Скрипт 1. Контракт, Swagger UI, Try it out

Скопировать целиком в терминал (`zsh`/`bash`) или сохранить в файл и запустить `bash file.sh`.

```shell
HOST=${HOST:-http://localhost:8080}
new() { uuidgen | tr 'A-Z' 'a-z'; }
PASS=0; FAIL=0
# check <название> <ожидаемое> <фактическое>
check() {
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "OK   $1";
  else FAIL=$((FAIL+1)); echo "FAIL $1: ожидалось '$2', получено '$3'"; fi
}
# code <путь> [доп. опции curl] → HTTP-статус
code() { local p=$1; shift; curl -s -o /dev/null -w '%{http_code}' "$@" "$HOST$p"; }
# has <путь> <строка> → yes/no
has() { curl -s "$HOST$1" | grep -qF -- "$2" && echo yes || echo no; }

# 1. Контракт как статика (Р-5)
check "GET /openapi/openapi.yaml → 200"                       200 "$(code /openapi/openapi.yaml)"
check "  есть /api/v1/friendship-offer/create"               yes "$(has /openapi/openapi.yaml /api/v1/friendship-offer/create)"
check "  есть servers (Р-3)"                                 yes "$(has /openapi/openapi.yaml 'servers:')"
check "  есть bearerAuth (Р-6)"                              yes "$(has /openapi/openapi.yaml 'bearerAuth:')"
check "GET /openapi/paths/friendship-offer-create.yaml → 200" 200 "$(code /openapi/paths/friendship-offer-create.yaml)"
check "GET /openapi/schemas/friendship-offer.yaml → 200"      200 "$(code /openapi/schemas/friendship-offer.yaml)"
check "GET /openapi/modules/friendships.yaml → 404 (не публикуется)" 404 "$(code /openapi/modules/friendships.yaml)"

# 2. Swagger UI (Р-10)
check "GET /swagger-ui.html → 302"                            302 "$(code /swagger-ui.html)"
check "GET /swagger-ui.html (-L) → 200"                       200 "$(code /swagger-ui.html -L)"
CONFIG=$(curl -s "$HOST/v3/api-docs/swagger-config")
check "  swagger-config: url = /openapi/openapi.yaml"        /openapi/openapi.yaml "$(jq -r .url <<<"$CONFIG")"
check "  swagger-config: tryItOutEnabled"                    true "$(jq -r .tryItOutEnabled <<<"$CONFIG")"
check "GET /v3/api-docs → 200 (включён, UI его не использует)" 200 "$(code /v3/api-docs)"

# 3. Запрос, который отправляет Try it out для create offer (servers: / → тот же хост)
FROM=$(new); TO=$(new)
CODE=$(curl -s -o /tmp/41226-body -w '%{http_code}' -X POST "$HOST/api/v1/friendship-offer/create" \
  -H 'Content-Type: application/json' -d "{\"fromUserId\":\"$FROM\",\"toUserId\":\"$TO\"}")
check "POST /api/v1/friendship-offer/create → 201"            201 "$CODE"
check "  status = PENDING"                                   PENDING "$(jq -r .status /tmp/41226-body)"
check "  fromUserId"                                         "$FROM" "$(jq -r .fromUserId /tmp/41226-body)"

echo "----"; echo "Итог: OK=$PASS FAIL=$FAIL"; [ "$FAIL" -eq 0 ] && echo OK || echo FAIL
```

## Скрипт 2. Отключение Swagger UI (Р-4)

Перезапустить приложение с выключенным UI:

```shell
SWAGGER_UI_ENABLED=false ./mvnw spring-boot:run
```

```shell
HOST=${HOST:-http://localhost:8080}
PASS=0; FAIL=0
check() {
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "OK   $1";
  else FAIL=$((FAIL+1)); echo "FAIL $1: ожидалось '$2', получено '$3'"; fi
}
code() { curl -s -o /dev/null -w '%{http_code}' "$HOST$1"; }

check "GET /swagger-ui.html → 404"                    404 "$(code /swagger-ui.html)"
check "GET /swagger-ui/index.html → 404"              404 "$(code /swagger-ui/index.html)"
check "GET /openapi/openapi.yaml → 200 (статика остаётся)" 200 "$(code /openapi/openapi.yaml)"

echo "----"; echo "Итог: OK=$PASS FAIL=$FAIL"; [ "$FAIL" -eq 0 ] && echo OK || echo FAIL
```

После проверки перезапустить приложение без `SWAGGER_UI_ENABLED`.

## Чек-лист в браузере

Открыть http://localhost:8080/swagger-ui.html (приложение запущено без `SWAGGER_UI_ENABLED=false`).

Чтобы запросы с подставленными примерами находили данные (а не 404), запустить приложение с контекстом `dev` (Р-13):

```shell
LIQUIBASE_CONTEXTS=dev ./mvnw spring-boot:run
```

Данные пользователей-примеров (`0b6f3c9e…` A, `5e2a7d40…` B, `7d1f0a52…` C, `a3c8e5f1…` D, `22222222…` P)
пересоздаются при каждом запуске — перезапуск отменяет accept/reject/delete, сделанные через Try it out.
Описание данных — в начале `src/main/resources/db/dev/swagger-examples.sql`. Заголовок `X-User-Id` (Authorize →
`userIdHeader`): для операций с постом-примером — P, для ленты и подписок — A.
`incoming`/`outgoing` пока отвечают 501, `GET /api/v1/friendships` — 404 (контроллер не реализован).

- [ ] В строке Explore вверху — `/openapi/openapi.yaml`, а не petstore и не `/v3/api-docs`.
- [ ] Видны все разделы: FriendshipOffer (дружба), подписки, посты, лента.
- [ ] Схемы запросов и ответов раскрываются, ошибок `Could not resolve reference` нет — `$ref` подгрузились.
- [ ] Servers: `/` — «Текущий хост».
- [ ] Authorize показывает две схемы: `userIdHeader` (X-User-Id) и `bearerAuth` (JWT).
- [ ] FriendshipOffer → `POST /api/v1/friendship-offer/create` → Try it out → тело с двумя новыми UUID → Execute:
      в Curl видно отправленный JSON, Server response — `201`, тело с `status: PENDING`.
- [ ] Повтор того же запроса → `200` с `code: FRIENDSHIP_OFFER_ALREADY_PENDING`.
