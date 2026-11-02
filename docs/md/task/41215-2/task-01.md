# Задание 01. Миграции: `007`–`009` заново и `010` — частичный индекс опубликованных постов

**Раздел README:** 2.2.
**Зависит от:** —.

## Цель

1. Заново создать миграции `007`–`009` и сверить их с эталонами `*-b.sql`.
2. Добавить индекс, из которого лента отбирает посты каждого автора без чтения таблицы (Index Only Scan) и без удалённых постов. Старый индекс `idx_posts_author_created` новым полностью перекрывается и удаляется.

## Контекст

- **Эталоны `*-b.sql`.** В `src/main/resources/db/changes/` лежат `007-create-posts-b.sql`, `008-create-posts-indexes-b.sql`, `009-friendship-offer-unique-pair-b.sql`. Это не миграции: они не подключены в master changelog и нужны только для сравнения с миграциями `007`–`009`, которые создаются в этом задании. По содержимому они совпадают с `007`–`009` из коммита `38036d9`. После сравнения `*-b.sql` удаляются.
- **Сейчас `007`–`009` в рабочей копии нет**, а `db.changelog-master.yaml` на них ссылается: до шага 1 приложение и тесты не стартуют.
- **Содержимое `007`–`009` должно совпасть с эталоном.** Changeset `008` и прочие уже применены к локальной базе `docker compose`: при любом изменении changeset Liquibase упадёт на checksum. Имена файлов тоже прежние — Liquibase различает changeset по тройке «путь к файлу + id + author». Новый индекс поэтому — отдельным changeset `010`, а не правкой `008`.
- Сейчас у `posts` есть `idx_posts_author_created ON posts (author_id, created_at DESC, id DESC)` (changeset `008`). `status` в нём нет: каждый пост проверяется по таблице, удалённые читаются зря.
- Все чтения `posts` по автору выбирают только `PUBLISHED`; изменение и удаление поста идут по первичному ключу. После появления частичного индекса старый не нужен никому, а платить за него пришлось бы на каждой записи в `posts`.
- Правило проекта: индексы таблицы — в отдельном файле от самой таблицы, `--rollback` обязателен, changeset добавляется в master changelog по порядку.

## Что сделать

1. **Создать `007`–`009`** в `src/main/resources/db/changes/`:
   - `007-create-posts.sql` — таблица `posts` (комментарии с примерами `INSERT` перед `--changeset` — как в эталоне);
   - `008-create-posts-indexes.sql` — `idx_posts_author_created` и rollback;
   - `009-friendship-offer-unique-pair.sql` — `uq_friendship_offer_pair`.

   `db.changelog-master.yaml` не меняется: в нём уже эти имена.

Новый SQL должны: учесть пожелания в описани в docs/md/task/41215-2/descr.md 

2. **Дать пользователю время сверить с эталонами** 
   ```shell
   cd src/main/resources/db/changes
   diff 007-create-posts.sql 007-create-posts-b.sql
   diff 008-create-posts-indexes.sql 008-create-posts-indexes-b.sql
   diff 009-friendship-offer-unique-pair.sql 009-friendship-offer-unique-pair-b.sql
   ```

В файлах должен быть SQL позволяющий создавать (генерировать случайные...) 2 млн. постов (для запуска EXPLAIN) и их удаления
   
**Файл `010-posts-published-index.sql`**:
   ```sql
   --liquibase formatted sql

   --changeset student:010
   CREATE INDEX idx_posts_published_author_created
       ON posts (author_id, created_at DESC, id DESC)
       WHERE status = 'PUBLISHED';
   DROP INDEX IF EXISTS idx_posts_author_created;

   --rollback CREATE INDEX idx_posts_author_created ON posts (author_id, created_at DESC, id DESC);
   --rollback DROP INDEX IF EXISTS idx_posts_published_author_created;
   ```

3. `db.changelog-master.yaml` — `include` файла `010` последним.


## Критерии приёмки

- `007`–`009` созданы с прежними именами и совпадают отвечают новым требованиям.
- Новый файл `010` с `--rollback`, подключён в master changelog последним.
- Упоминаний `idx_posts_author_created` в коде и документации не осталось (кроме `008`, `010` и истории в `docs/md/task/41215`) — документацию правит задание 05.
