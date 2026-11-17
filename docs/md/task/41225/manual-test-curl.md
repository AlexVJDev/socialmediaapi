# 41225 — ручная проверка через curl

Проверяет новый функционал задачи на запущенном приложении: посты (создание, изменение, удаление,
посты пользователя), подписка/отписка, лента текущего пользователя. Текущий пользователь —
заголовок `X-User-Id` (Р3-1). В конце печатает итог `OK`/`FAIL`.

Нужны `curl`, `jq`, `uuidgen`. Id пользователей каждый раз новые — скрипт можно запускать повторно.

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
ALICE=$(new); BORIS=$(new)
PASS=0; FAIL=0
# check <название> <ожидаемое> <фактическое>
check() {
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "OK   $1";
  else FAIL=$((FAIL+1)); echo "FAIL $1: ожидалось '$2', получено '$3'"; fi
}
# req <метод> <путь> <X-User-Id или -> [тело]  → тело ответа в $BODY, статус в $CODE
req() {
  local args=(-s -o /tmp/41225-body -w '%{http_code}' -X "$1" "$BASE$2")
  [ "$3" != "-" ] && args+=(-H "X-User-Id: $3")
  [ -n "$4" ] && args+=(-H "$JSON" -d "$4")
  CODE=$(curl "${args[@]}"); BODY=$(cat /tmp/41225-body)
}
echo "alice=$ALICE boris=$BORIS"

# 1. Посты
req POST /posts - '{"title":"Пост","content":"Текст"}'
check "POST /posts без X-User-Id → 401"                401 "$CODE"
check "  code = UNAUTHORIZED"                         UNAUTHORIZED "$(jq -r .code <<<"$BODY")"
req POST /posts not-a-uuid '{"title":"Пост","content":"Текст"}'
check "POST /posts с X-User-Id не UUID → 401"          401 "$CODE"
req POST /posts "$BORIS" '{"title":"   ","content":"Текст"}'
check "POST /posts, title из пробелов → 400"           400 "$CODE"
req POST /posts "$BORIS" '{"content":"Текст"}'
check "POST /posts без title → 400"                    400 "$CODE"

LOCATION=$(curl -s -D - -o /tmp/41225-body -X POST "$BASE/posts" -H "X-User-Id: $BORIS" -H "$JSON" \
  -d '{"title":"Пост Бориса","content":"Текст Бориса"}' | tr -d '\r' | awk -F': ' 'tolower($1)=="location"{print $2}')
BODY=$(cat /tmp/41225-body); POST_ID=$(jq -r .id <<<"$BODY")
check "POST /posts Борисом → автор Борис"              "$BORIS" "$(jq -r .authorId <<<"$BODY")"
check "  Location = /api/v1/posts/{id}"               "/api/v1/posts/$POST_ID" "$LOCATION"

req GET "/posts/$POST_ID" -
check "GET /posts/{id} → 200"                          200 "$CODE"
req PUT "/posts/$POST_ID" "$ALICE" '{"title":"Чужой","content":"Чужой"}'
check "PUT чужого поста (Алиса) → 404"                 404 "$CODE"
check "  code = POST_NOT_FOUND"                       POST_NOT_FOUND "$(jq -r .code <<<"$BODY")"
req PUT "/posts/$POST_ID" "$BORIS" '{"title":"Новый заголовок","content":"Новый текст"}'
check "PUT своего поста (Борис) → 200"                 200 "$CODE"
check "  title изменён"                               "Новый заголовок" "$(jq -r .title <<<"$BODY")"
req GET "/users/$BORIS/posts" -
check "GET /users/{Борис}/posts → пост в списке"       true "$(jq --arg id "$POST_ID" '[.posts[].id] | index($id) != null' <<<"$BODY")"
req GET "/users/$(new)/posts" -
check "GET /users/{неизвестный}/posts → пусто"         0 "$(jq '.posts | length' <<<"$BODY")"

# 2. Подписка и лента
req POST /subscribes "$ALICE" "{\"userId\":\"$BORIS\"}"
check "POST /subscribes Алиса → Борис → 201"           201 "$CODE"
SUB_ID=$(jq -r .id <<<"$BODY")
req POST /subscribes "$ALICE" "{\"userId\":\"$BORIS\"}"
check "Повторная подписка → 200"                       200 "$CODE"
check "  та же подписка"                              "$SUB_ID" "$(jq -r .id <<<"$BODY")"
req POST /subscribes "$ALICE" "{\"userId\":\"$ALICE\"}"
check "Подписка на себя → 400"                         400 "$CODE"
req POST /subscribes - "{\"userId\":\"$BORIS\"}"
check "POST /subscribes без X-User-Id → 401"           401 "$CODE"

req GET /feed "$ALICE"
check "GET /feed Алисы → пост Бориса в ленте"          true "$(jq --arg id "$POST_ID" '[.posts[].id] | index($id) != null' <<<"$BODY")"
req GET /feed -
check "GET /feed без X-User-Id → 401"                  401 "$CODE"

req DELETE "/subscribes/$BORIS" "$ALICE"
check "DELETE /subscribes/{Борис} → 204"               204 "$CODE"
req DELETE "/subscribes/$BORIS" "$ALICE"
check "Повторная отписка → 204"                        204 "$CODE"
req GET /feed "$ALICE"
check "GET /feed после отписки → поста нет"            false "$(jq --arg id "$POST_ID" '[.posts[].id] | index($id) != null' <<<"$BODY")"

# 3. Удаление поста
req DELETE "/posts/$POST_ID" "$ALICE"
check "DELETE чужого поста → 404"                      404 "$CODE"
req DELETE "/posts/$POST_ID" "$BORIS"
check "DELETE своего поста → 204"                      204 "$CODE"
req DELETE "/posts/$POST_ID" "$BORIS"
check "Повторное удаление → 404"                       404 "$CODE"
req GET "/posts/$POST_ID" -
check "GET удалённого поста → 404"                     404 "$CODE"

echo "------"
echo "Итого: OK=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ] && echo "РЕЗУЛЬТАТ: OK" || echo "РЕЗУЛЬТАТ: FAIL"
```
