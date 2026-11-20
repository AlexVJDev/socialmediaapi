# 40000 — ручная проверка через curl

Проверяет новый функционал задачи на запущенном приложении:
- `GET /api/v1/friendship-offer` — заявки текущего пользователя (`X-User-Id`), `direction`, `status`, курсор (Р3-2, Р4-1, Р4-2);
- `GET /api/v1/friendships?userId=` — публичная страница друзей `{friends, next}` (Р1-1, Р2-1, Р2-4).

В конце печатает итог `OK`/`FAIL`. Нужны `curl`, `jq`, `uuidgen`. Id пользователей каждый раз новые — скрипт можно запускать повторно.

Сценарий: Алиса, Вика и Дима отправляют заявки Борису (в этом порядке), Борис принимает заявку Вики.
У Бориса две входящие `PENDING` (Дима — новее, Алиса), одна `ACCEPTED` (Вика) и один друг — Вика.

## Подготовка

```shell
docker compose up -d postgres
./mvnw spring-boot:run          # в отдельном терминале, http://localhost:8080
```

## Скрипт

Скопировать целиком в терминал (`zsh`/`bash`) или сохранить в файл и запустить `bash file.sh`.

```shell
BASE=${BASE:-http://localhost:8080/api/v1}
JSON='Content-Type: application/json'
new() { uuidgen | tr 'A-Z' 'a-z'; }
ALICE=$(new); BORIS=$(new); VIKA=$(new); DIMA=$(new)
PASS=0; FAIL=0
# check <название> <ожидаемое> <фактическое>
check() {
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "OK   $1";
  else FAIL=$((FAIL+1)); echo "FAIL $1: ожидалось '$2', получено '$3'"; fi
}
# post <путь> <X-User-Id> <тело>  → тело ответа в $BODY, статус в $CODE
post() {
  CODE=$(curl -s -o /tmp/40000-body -w '%{http_code}' -X POST "$BASE$1" -H "$JSON" -H "X-User-Id: $2" -d "$3")
  BODY=$(cat /tmp/40000-body)
}
# get <путь> <X-User-Id или -> [query-параметр ...]  → тело ответа в $BODY, статус в $CODE
get() {
  local path=$1 user=$2; shift 2
  local args=(-s -G -o /tmp/40000-body -w '%{http_code}' "$BASE$path")
  [ "$user" != "-" ] && args+=(-H "X-User-Id: $user")
  for p in "$@"; do args+=(--data-urlencode "$p"); done
  CODE=$(curl "${args[@]}"); BODY=$(cat /tmp/40000-body)
}
echo "alice=$ALICE boris=$BORIS vika=$VIKA dima=$DIMA"

# 0. Подготовка: заявки Борису, Борис принимает заявку Вики
post /friendship-offer/create "$ALICE" "{\"toUserId\":\"$BORIS\"}"
check "Алиса → Борис: заявка создана → 201"            201 "$CODE"
OFFER_ALICE=$(jq -r .id <<<"$BODY")
post /friendship-offer/create "$VIKA" "{\"toUserId\":\"$BORIS\"}"
check "Вика → Борис: заявка создана → 201"             201 "$CODE"
OFFER_VIKA=$(jq -r .id <<<"$BODY")
post /friendship-offer/create "$DIMA" "{\"toUserId\":\"$BORIS\"}"
check "Дима → Борис: заявка создана → 201"             201 "$CODE"
OFFER_DIMA=$(jq -r .id <<<"$BODY")
post /friendship-offer/accept "$BORIS" "{\"offerId\":\"$OFFER_VIKA\"}"
check "Борис принимает заявку Вики → 200"              200 "$CODE"

# 1. GET /friendship-offer — заявки текущего пользователя
get /friendship-offer "$BORIS" direction=INCOMING
check "INCOMING Бориса → 200"                          200 "$CODE"
check "  только PENDING: Дима, Алиса (новые первыми)"  "$OFFER_DIMA $OFFER_ALICE" "$(jq -r '[.offers[].id] | join(" ")' <<<"$BODY")"
check "  next = null"                                  null "$(jq -c .next <<<"$BODY")"

get /friendship-offer "$BORIS" direction=INCOMING limit=1
check "INCOMING Бориса, limit=1 → Дима"                "$OFFER_DIMA" "$(jq -r '[.offers[].id] | join(" ")' <<<"$BODY")"
NEXT_AT=$(jq -r .next.createdAt <<<"$BODY"); NEXT_ID=$(jq -r .next.id <<<"$BODY")
check "  next.id = заявка Димы"                        "$OFFER_DIMA" "$NEXT_ID"
get /friendship-offer "$BORIS" direction=INCOMING limit=1 "afterCreatedAt=$NEXT_AT" "afterId=$NEXT_ID"
check "INCOMING, вторая страница по next → Алиса"      "$OFFER_ALICE" "$(jq -r '[.offers[].id] | join(" ")' <<<"$BODY")"
check "  next = null"                                  null "$(jq -c .next <<<"$BODY")"

get /friendship-offer "$BORIS" direction=INCOMING status=ACCEPTED
check "INCOMING status=ACCEPTED → заявка Вики"         "$OFFER_VIKA" "$(jq -r '[.offers[].id] | join(" ")' <<<"$BODY")"
check "  status = ACCEPTED"                            ACCEPTED "$(jq -r '.offers[0].status' <<<"$BODY")"

get /friendship-offer "$ALICE" direction=OUTGOING
check "OUTGOING Алисы → её заявка Борису"              "$OFFER_ALICE" "$(jq -r '[.offers[].id] | join(" ")' <<<"$BODY")"
get /friendship-offer "$ALICE" direction=INCOMING
check "INCOMING Алисы → пусто"                         '{"offers":[],"next":null}' "$(jq -c . <<<"$BODY")"
get /friendship-offer "$(new)" direction=INCOMING
check "Неизвестный пользователь → 200, пусто"          '{"offers":[],"next":null}' "$(jq -c . <<<"$BODY")"

get /friendship-offer "$BORIS"
check "Без direction → 400"                            400 "$CODE"
check "  code = VALIDATION_ERROR"                      VALIDATION_ERROR "$(jq -r .code <<<"$BODY")"
get /friendship-offer "$BORIS" direction=FOO
check "direction=FOO → 400"                            400 "$CODE"
get /friendship-offer "$BORIS" direction=INCOMING status=FOO
check "status=FOO → 400"                               400 "$CODE"
get /friendship-offer "$BORIS" direction=INCOMING "afterId=$OFFER_DIMA"
check "Только afterId без afterCreatedAt → 400"        400 "$CODE"
get /friendship-offer - direction=INCOMING
check "Без X-User-Id → 401"                            401 "$CODE"
check "  code = UNAUTHORIZED"                          UNAUTHORIZED "$(jq -r .code <<<"$BODY")"

get /friendship-offer/incoming "$BORIS"
check "Удалённый /friendship-offer/incoming → не 200"  true "$([ "$CODE" != 200 ] && echo true || echo false)"

# 2. GET /friendships — друзья
get /friendships - "userId=$BORIS"
check "Друзья Бориса → 200"                            200 "$CODE"
check "  друг — Вика"                                  "$VIKA" "$(jq -r '[.friends[].userId] | join(" ")' <<<"$BODY")"
check "  next = null"                                  null "$(jq -c .next <<<"$BODY")"
FRIENDSHIP_ID=$(jq -r '.friends[0].id' <<<"$BODY")
get /friendships - "userId=$VIKA"
check "Друзья Вики → Борис"                            "$BORIS" "$(jq -r '[.friends[].userId] | join(" ")' <<<"$BODY")"
check "  id дружбы тот же"                             "$FRIENDSHIP_ID" "$(jq -r '.friends[0].id' <<<"$BODY")"
get /friendships - "userId=$ALICE"
check "Друзья Алисы (заявка не принята) → пусто"       '{"friends":[],"next":null}' "$(jq -c . <<<"$BODY")"
get /friendships - "userId=abc"
check "userId=abc → 400"                               400 "$CODE"
check "  code = VALIDATION_ERROR"                      VALIDATION_ERROR "$(jq -r .code <<<"$BODY")"
get /friendships - "userId=$BORIS" limit=0
check "limit=0 → 400"                                  400 "$CODE"

echo "------"
echo "Итого: OK=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ] && echo "РЕЗУЛЬТАТ: OK" || echo "РЕЗУЛЬТАТ: FAIL"
```
