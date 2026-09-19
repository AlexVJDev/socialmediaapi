# socialmediaapi
Educational project

## PostgreSQL

The database runs in Docker with the following default settings:

- database: `postgres`
- schema: `public`
- user: `postgresql`
- password: `password`
- port: `5432`

Start PostgreSQL:

```shell
docker compose up -d
```

Check container status:

```shell
docker compose ps
```

Stop PostgreSQL:

```shell
docker compose down
```

To stop PostgreSQL and delete all database data:

```shell
docker compose down -v
```

## Application

Start:

```shell
./mvnw spring-boot:run
```

Liquibase runs automatically during application startup and creates the
application tables in the `public` schema. A successful startup contains
`Liquibase: Update has been successful` in the log.

List the created tables directly through the container:

```shell
docker compose exec postgres psql -U postgresql -d postgres -c "\dt public.*"
```

If a database manager was open before the migration, refresh or reconnect the
`public` schema to update its object tree.

Stop:

```text
Ctrl + C
```
