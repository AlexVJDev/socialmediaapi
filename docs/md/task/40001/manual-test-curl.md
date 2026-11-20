# 40001 — ручная проверка через curl

Проверяет на запущенном приложении: `POST /api/v1/friendship-offer/{create,accept,reject}` берут действующего
пользователя из заголовка `X-User-Id` (Р0-1, Р1-1); старые поля `fromUserId`/`actorId` в теле игнорируются (Р1-2);
ответы прежние (Р1-3); заявка самому себе → `400` (Р1-6); невалидное тело без заголовка → `400` (Р1-5).

В конце печатает итог `OK`/`FAIL`. Нужны `curl`, `jq`, `uuidgen`. Id пользователей каждый раз новые — скрипт можно запускать повторно.

Сценарий: Алиса и Вика отправляют заявки Борису; Борис принимает заявку Алисы и отклоняет заявку Вики.
Дима — посторонний.

## Подготовка

```shell
docker compose up -d postgres
./mvnw spring-boot:run          # в отдельном терминале, http://localhost:8080
```

## Скрипт

Скопировать целиком в терминал (`zsh`/`bash`) или сохранить в файл и запустить `bash file.sh`.

```shell
BASE=${BASE:-http://localhost:8080/api/v1/friendship-offer}
new() { uuidgen | tr 'A-Z' 'a-z'; }
ALICE=$(new); BORIS=$(new); VIKA=$(new); DIMA=$(new)
PASS=0; FAIL=0
# check <название> <ожидаемое> <фактическое>
check() {
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "OK   $1";
  else FAIL=$((FAIL+1)); echo "FAIL $1: ожидалось '$2', получено '$3'"; fi
}
# post <путь> <X-User-Id или -> <тело>  → тело ответа в $BODY, статус в $CODE
post() {
  local args=(-s -o /tmp/40001-body -w '%{http_code}' -X POST "$BASE$1" -H 'Content-Type: application/json' -d "$3")
  [ "$2" != "-" ] && args+=(-H "X-User-Id: $2")
  CODE=$(curl "${args[@]}"); BODY=$(cat /tmp/40001-body)
}
echo "alice=$ALICE boris=$BORIS vika=$VIKA dima=$DIMA"

# 1. create
post /create "$ALICE" "{\"toUserId\":\"$BORIS\"}"
check "Алиса → Борис → 201"                                201 "$CODE"
check "  fromUserId = Алиса (из X-User-Id)"                "$ALICE" "$(jq -r .fromUserId <<<"$BODY")"
check "  toUserId = Борис, status = PENDING"               "$BORIS PENDING" "$(jq -r '"\(.toUserId) \(.status)"' <<<"$BODY")"
OFFER_ALICE=$(jq -r .id <<<"$BODY")

post /create "$VIKA" "{\"fromUserId\":\"$DIMA\",\"toUserId\":\"$BORIS\"}"
check "Вика → Борис, в теле старое fromUserId=Дима → 201"  201 "$CODE"
check "  fromUserId = Вика: поле из тела игнорируется"     "$VIKA" "$(jq -r .fromUserId <<<"$BODY")"
OFFER_VIKA=$(jq -r .id <<<"$BODY")

post /create - "{\"toUserId\":\"$BORIS\"}"
check "create без X-User-Id → 401"                         401 "$CODE"
check "  code = UNAUTHORIZED"                              UNAUTHORIZED "$(jq -r .code <<<"$BODY")"

post /create "abc" "{\"toUserId\":\"$BORIS\"}"
check "create, X-User-Id не UUID → 401"                    401 "$CODE"

post /create "$ALICE" "{\"toUserId\":\"$ALICE\"}"
check "create самому себе (toUserId = X-User-Id) → 400"    400 "$CODE"
check "  code = VALIDATION_ERROR"                          VALIDATION_ERROR "$(jq -r .code <<<"$BODY")"

post /create - '{}'
check "create, нет toUserId и нет X-User-Id → 400"         400 "$CODE"

# 2. accept
post /accept "$ALICE" "{\"offerId\":\"$OFFER_ALICE\"}"
check "accept от отправителя (Алиса) → 404"                404 "$CODE"
check "  code = FRIENDSHIP_OFFER_NOT_FOUND"                FRIENDSHIP_OFFER_NOT_FOUND "$(jq -r .code <<<"$BODY")"

post /accept "$DIMA" "{\"offerId\":\"$OFFER_ALICE\",\"actorId\":\"$BORIS\"}"
check "accept от Димы, в теле старое actorId=Борис → 404"  404 "$CODE"

post /accept - "{\"offerId\":\"$OFFER_ALICE\"}"
check "accept без X-User-Id → 401"                         401 "$CODE"

post /accept "$BORIS" "{\"offerId\":\"$OFFER_ALICE\"}"
check "accept получателем (Борис) → 200"                   200 "$CODE"
check "  status = ACCEPTED, fromUserId = Алиса"            "ACCEPTED $ALICE" "$(jq -r '"\(.status) \(.fromUserId)"' <<<"$BODY")"

# 3. reject
post /reject - "{\"offerId\":\"$OFFER_VIKA\"}"
check "reject без X-User-Id → 401"                         401 "$CODE"

post /reject "$DIMA" "{\"offerId\":\"$OFFER_VIKA\"}"
check "reject посторонним (Дима) → 404"                    404 "$CODE"

post /reject "$BORIS" "{\"offerId\":\"$OFFER_VIKA\"}"
check "reject получателем (Борис) → 200"                   200 "$CODE"
check "  status = REJECTED, toUserId = Борис"              "REJECTED $BORIS" "$(jq -r '"\(.status) \(.toUserId)"' <<<"$BODY")"

post /reject "$BORIS" '{"offerId":"42"}'
check "reject, offerId не UUID → 400"                      400 "$CODE"

echo "------"
echo "Итого: OK=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ] && echo "РЕЗУЛЬТАТ: OK" || echo "РЕЗУЛЬТАТ: FAIL"
```
